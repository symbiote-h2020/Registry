package eu.h2020.symbiote;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.rabbitmq.client.*;
import eu.h2020.symbiote.core.cci.InformationModelRequest;
import eu.h2020.symbiote.core.cci.InformationModelResponse;
import eu.h2020.symbiote.core.cci.PlatformRegistryResponse;
import eu.h2020.symbiote.core.internal.*;
import eu.h2020.symbiote.managers.AuthorizationManager;
import eu.h2020.symbiote.managers.RabbitManager;
import eu.h2020.symbiote.managers.RepositoryManager;
import eu.h2020.symbiote.model.persistenceResults.AuthorizationResult;
import eu.h2020.symbiote.model.persistenceResults.InformationModelPersistenceResult;
import eu.h2020.symbiote.model.persistenceResults.PlatformPersistenceResult;
import eu.h2020.symbiote.model.persistenceResults.ResourcePersistenceResult;
import eu.h2020.symbiote.model.cim.Resource;
import eu.h2020.symbiote.model.mim.InformationModel;
import eu.h2020.symbiote.model.mim.InterworkingService;
import eu.h2020.symbiote.model.mim.Platform;
import eu.h2020.symbiote.security.commons.exceptions.custom.InvalidArgumentsException;
import eu.h2020.symbiote.utils.RegistryUtils;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import static eu.h2020.symbiote.TestSetupConfig.*;
import static eu.h2020.symbiote.utils.RegistryUtils.convertResourceToCoreResource;
import static eu.h2020.symbiote.utils.RegistryUtils.getTypeForResource;
import static org.junit.Assert.*;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.*;

/**
 * Created by mateuszl
 */
@RunWith(MockitoJUnitRunner.class)
public class MessagingTests {

    private static Logger log = LoggerFactory.getLogger(MessagingTests.class);
    private ObjectMapper mapper;
    private Connection connection;
    private Channel channel;
    @Mock
    private RepositoryManager mockedRepository;
    @Mock
    private AuthorizationManager mockedAuthorizationManager;
    @InjectMocks
    private RabbitManager rabbitManager;

    @Before
    public void setup() throws IOException, TimeoutException {
        initializeRabbitManager(rabbitManager);
        mapper = new ObjectMapper();
        connection = rabbitManager.getConnection();
        channel = connection.createChannel();
    }

    @After
    public void teardown() {
        deleteRabbitQueues(rabbitManager);
        log.info("Rabbit cleaned!");
    }

    @Test
    public void resourceCreationRequestConsumerHappyPathTest() throws InterruptedException, IOException, TimeoutException, InvalidArgumentsException {
        rabbitManager.startConsumerOfResourceCreationMessages(mockedAuthorizationManager);

        Resource resource1 = generateCoreResourceSensorWithoutId();
        Resource resource2 = generateCoreResourceSensorWithoutId();
        CoreResourceRegistryRequest coreResourceRegistryRequest = generateCoreResourceRegistryRequestBasicType(resource1, resource2);
        String message = mapper.writeValueAsString(coreResourceRegistryRequest);

        when(mockedAuthorizationManager.checkSinglePlatformOperationAccess(any(), any())).thenReturn(new AuthorizationResult("", true));
        when(mockedAuthorizationManager.checkIfResourcesBelongToPlatform(any(), anyString())).thenReturn(new AuthorizationResult("ok", true));
        when(mockedRepository.saveResource(any())).thenReturn(new ResourcePersistenceResult(200, "ok", RegistryUtils.convertResourceToCoreResource(resource1)));

        mockSemanticManagerResourceTranslationCommunication(message);

        //todo without using Rabbit real server  + mock Channel
        //ResourceCreationRequestConsumer resourceCreationRequestConsumer = new ResourceCreationRequestConsumer(channel, rabbitManager, mockedAuthorizationManager, mockedRepository);
        //resourceCreationRequestConsumer.handleDelivery("", new Envelope(5, false, RESOURCE_EXCHANGE_NAME, RESOURCE_CREATION_REQUESTED_RK), getProps(channel), message.getBytes());

        rabbitManager.sendCustomMessage(RESOURCE_EXCHANGE_NAME, RESOURCE_CREATION_REQUESTED_RK, message, CoreResourceRegistryRequest.class.getCanonicalName());

        // Timeout to make sure that the message has been delivered
        verify(mockedRepository, timeout(500).times(2)).saveResource(any());
    }

    @Test
    public void resourceCreationRPCHappyPathTest() throws Exception {
        rabbitManager.startConsumerOfResourceCreationMessages(mockedAuthorizationManager);

        Resource resource1 = generateStationaryResourceSensor();
        Resource resource2 = generateStationaryResourceSensor();
        CoreResourceRegistryRequest coreResourceRegistryRequestWithResources = generateCoreResourceRegistryRequestBasicType(resource1, resource2);
        CoreResource coreResource1 = convertResourceToCoreResource(resource1);
        CoreResource coreResource2 = convertResourceToCoreResource(resource2);
        CoreResourceRegistryRequest coreResourceRegistryRequestWithCoreResources = generateCoreResourceRegistryRequestBasicType(coreResource1, coreResource2);
        String resourcesMessage = mapper.writeValueAsString(coreResourceRegistryRequestWithResources);
        String coreResourcesMessage = mapper.writeValueAsString(coreResourceRegistryRequestWithCoreResources);

        when(mockedAuthorizationManager.checkSinglePlatformOperationAccess(any(), any())).thenReturn(new AuthorizationResult("", true));
        when(mockedAuthorizationManager.checkIfResourcesBelongToPlatform(any(), anyString())).thenReturn(new AuthorizationResult("ok", true));
        addIdToResource(resource1);
        when(mockedRepository.saveResource(any())).thenReturn(new ResourcePersistenceResult(200, "ok", RegistryUtils.convertResourceToCoreResource(resource1)));

        mockSemanticManagerResourceTranslationCommunication(coreResourcesMessage);

        String response = rabbitManager.sendRpcMessageAndConsumeResponse(RESOURCE_EXCHANGE_NAME, RESOURCE_CREATION_REQUESTED_RK, resourcesMessage);

        CoreResourceRegistryResponse resourceRegistryResponse = mapper.readValue(response, CoreResourceRegistryResponse.class);

        String resourceMapString = resourceRegistryResponse.getBody();
        Map<String, Resource> responseResourceMap = mapper.readValue(resourceMapString, new TypeReference<Map<String, Resource>>() {
        });

        Assert.assertNotNull(resourceRegistryResponse.getMessage());
        Assert.assertEquals(resourceRegistryResponse.getStatus(), 200);

        verify(mockedRepository, times(2)).saveResource(any());

        Map<String, Resource> requestResourceMap = mapper.readValue(coreResourceRegistryRequestWithResources.getBody(), new TypeReference<Map<String, Resource>>() {
        });

        for (String key : responseResourceMap.keySet()) {
            Assert.assertTrue(responseResourceMap.get(key) != null);
            Assert.assertTrue(responseResourceMap.get(key).getId() != null);
            log.debug("- Received in response Resource with key: " + key + " : " + responseResourceMap.get(key).toString());
            CoreResourceType typeForRequestedResource = getTypeForResource(requestResourceMap.get(key));
            Assert.assertNotNull(typeForRequestedResource);
            Assert.assertNotNull(getTypeForResource(responseResourceMap.get(key)));
            Assert.assertTrue(getTypeForResource(responseResourceMap.get(key)).equals(typeForRequestedResource));
        }
    }

    @Test
    public void resourceCreationRequestConsumerRPCSemanticContentFailTest() throws IOException, InterruptedException, InvalidArgumentsException {
        rabbitManager.startConsumerOfResourceCreationMessages(mockedAuthorizationManager);

        Resource resource1 = generateCoreResourceSensorWithoutId();
        Resource resource2 = generateCoreResourceSensorWithoutId();
        CoreResourceRegistryRequest coreResourceRegistryRequest = generateCoreResourceRegistryRequestRdfType(resource1, resource2);
        String message = mapper.writeValueAsString(coreResourceRegistryRequest);

        when(mockedAuthorizationManager.checkSinglePlatformOperationAccess(any(), any())).thenReturn(new AuthorizationResult("", true));
        when(mockedAuthorizationManager.checkIfResourcesBelongToPlatform(any(), anyString())).thenReturn(new AuthorizationResult("ok", true));
        when(mockedRepository.saveResource(any())).thenReturn(new ResourcePersistenceResult(200, "ok", RegistryUtils.convertResourceToCoreResource(resource1)));
        when(mockedRepository.getInformationModelIdByInterworkingServiceUrl(any(), any())).thenReturn("mocked");

        mockSemanticManagerResourceValidationCommunication(message);

        rabbitManager.sendCustomMessage(RESOURCE_EXCHANGE_NAME, RESOURCE_CREATION_REQUESTED_RK, message, CoreResourceRegistryRequest.class.getCanonicalName());

        // Sleep to make sure that the message has been delivered
        TimeUnit.MILLISECONDS.sleep(300);
        verify(mockedRepository, times(1)).getInformationModelIdByInterworkingServiceUrl(any(), any());
        verify(mockedRepository, times(0)).saveResource(any());
    }

    @Test

    public void resourceCreationRequestConsumerRPCNullInterworkingUrlFailTest() throws IOException, InterruptedException, InvalidArgumentsException {
        rabbitManager.startConsumerOfResourceCreationMessages(mockedAuthorizationManager);

        Resource resource1 = generateCoreResourceSensorWithoutId();
        Resource resource2 = generateCoreResourceSensorWithoutId();
        CoreResourceRegistryRequest coreResourceRegistryRequest = generateCoreResourceRegistryRequestRdfType(resource1, resource2);
        String message = mapper.writeValueAsString(coreResourceRegistryRequest);

        when(mockedAuthorizationManager.checkSinglePlatformOperationAccess(any(), any())).thenReturn(new AuthorizationResult("", true));
        when(mockedAuthorizationManager.checkIfResourcesBelongToPlatform(any(), anyString())).thenReturn(new AuthorizationResult("ok", true));
        when(mockedRepository.saveResource(any())).thenReturn(new ResourcePersistenceResult(200, "ok", RegistryUtils.convertResourceToCoreResource(resource1)));
        when(mockedRepository.getInformationModelIdByInterworkingServiceUrl(any(), any())).thenReturn(null);

        String response = rabbitManager.sendRpcMessageAndConsumeResponse(RESOURCE_EXCHANGE_NAME, RESOURCE_CREATION_REQUESTED_RK, message);

        CoreResourceRegistryResponse coreResourceRegistryResponse = mapper.readValue(response, CoreResourceRegistryResponse.class);

        Assert.assertNotNull(coreResourceRegistryResponse.getMessage());
        Assert.assertNotEquals(coreResourceRegistryResponse.getStatus(), 200);

//        mockSemanticManagerResourceValidationCommunication(message);

        verify(mockedRepository, times(1)).getInformationModelIdByInterworkingServiceUrl(any(), any());
        verify(mockedRepository, times(0)).saveResource(any());
    }


    @Test
    public void resourceModificationRequestConsumerHappyPathTest() throws InterruptedException, IOException, InvalidArgumentsException {
        rabbitManager.startConsumerOfResourceModificationMessages(mockedAuthorizationManager);

        Resource resource1 = generateStationaryResourceSensor();
        addIdToResource(resource1);
        Resource resource2 = generateStationaryResourceSensor();
        addIdToResource(resource2);
        CoreResourceRegistryRequest coreResourceRegistryRequest = generateCoreResourceRegistryRequestBasicType(resource1, resource2);
        String message = mapper.writeValueAsString(coreResourceRegistryRequest);

        when(mockedAuthorizationManager.checkSinglePlatformOperationAccess(any(), any())).thenReturn(new AuthorizationResult("ok", true));
        when(mockedAuthorizationManager.checkIfResourcesBelongToPlatform(any(), anyString())).thenReturn(new AuthorizationResult("ok", true));
        when(mockedRepository.modifyResource(any())).thenReturn(new ResourcePersistenceResult(200, "ok", RegistryUtils.convertResourceToCoreResource(resource1)));

        mockSemanticManagerResourceTranslationCommunication(message);

        rabbitManager.sendCustomMessage(RESOURCE_EXCHANGE_NAME, RESOURCE_MODIFICATION_REQUESTED_RK, message, CoreResourceRegistryRequest.class.getCanonicalName());

        // Timeout to make sure that the message has been delivered
        verify(mockedRepository, timeout(500).times(2)).modifyResource(any());
    }

    @Test
    public void resourceRemovalRequestConsumerHappyPathTest() throws IOException, InterruptedException, InvalidArgumentsException {
        rabbitManager.startConsumerOfResourceRemovalMessages(this.mockedAuthorizationManager);

        Resource resource1 = generateCoreResourceSensorWithoutId();
        addIdToResource(resource1);
        Resource resource2 = generateCoreResourceSensorWithoutId();
        addIdToResource(resource2);
        CoreResourceRegistryRequest coreResourceRegistryRequest = generateCoreResourceRegistryRequestBasicType(resource1, resource2);

        String message = mapper.writeValueAsString(coreResourceRegistryRequest);

        ResourcePersistenceResult resourcePersistenceResult1 = new ResourcePersistenceResult();
        resourcePersistenceResult1.setStatus(200);
        resourcePersistenceResult1.setMessage("ok");
        resourcePersistenceResult1.setResource(RegistryUtils.convertResourceToCoreResource(resource1));

        when(mockedRepository.removeResource(any())).thenReturn(resourcePersistenceResult1);
        when(mockedAuthorizationManager.checkSinglePlatformOperationAccess(any(), any())).thenReturn(new AuthorizationResult("ok", true));
        when(mockedAuthorizationManager.checkIfResourcesBelongToPlatform(any(), any())).thenReturn(new AuthorizationResult("ok", true));

        rabbitManager.sendCustomMessage(RESOURCE_EXCHANGE_NAME, RESOURCE_REMOVAL_REQUESTED_RK, message, CoreResourceRegistryRequest.class.getCanonicalName());

        // Sleep to make sure that the message has been delivered
        TimeUnit.MILLISECONDS.sleep(300);
        verify(mockedRepository, times(2)).removeResource(any());
    }

    @Test
    public void resourceCreationRequestConsumerAuthFailTest() throws IOException, InterruptedException, InvalidArgumentsException {
        rabbitManager.startConsumerOfResourceCreationMessages(mockedAuthorizationManager);

        Resource resource1 = generateCoreResourceSensorWithoutId();
        Resource resource2 = generateCoreResourceSensorWithoutId();
        CoreResourceRegistryRequest coreResourceRegistryRequest = generateCoreResourceRegistryRequestBasicType(resource1, resource2);
        String message = mapper.writeValueAsString(coreResourceRegistryRequest);

        when(mockedAuthorizationManager.checkSinglePlatformOperationAccess(any(), any())).thenReturn(new AuthorizationResult("", false));
        when(mockedAuthorizationManager.checkIfResourcesBelongToPlatform(any(), anyString())).thenReturn(new AuthorizationResult("", false));

        rabbitManager.sendCustomMessage(RESOURCE_EXCHANGE_NAME, RESOURCE_CREATION_REQUESTED_RK, message, CoreResourceRegistryRequest.class.getCanonicalName());

        // Sleep to make sure that the message has been delivered
        TimeUnit.MILLISECONDS.sleep(300);
        verifyZeroInteractions(mockedRepository);
    }

    @Test
    public void resourceModificationRequestConsumerAuthFailTest() throws InterruptedException, IOException, InvalidArgumentsException {
        rabbitManager.startConsumerOfResourceModificationMessages(mockedAuthorizationManager);

        Resource resource1 = generateCoreResourceSensorWithoutId();
        Resource resource2 = generateCoreResourceSensorWithoutId();
        CoreResourceRegistryRequest coreResourceRegistryRequest = generateCoreResourceRegistryRequestBasicType(resource1, resource2);
        String message = mapper.writeValueAsString(coreResourceRegistryRequest);

        when(mockedAuthorizationManager.checkSinglePlatformOperationAccess(any(), any())).thenReturn(new AuthorizationResult("", false));
        when(mockedAuthorizationManager.checkIfResourcesBelongToPlatform(any(), anyString())).thenReturn(new AuthorizationResult("", false));

        rabbitManager.sendCustomMessage(RESOURCE_EXCHANGE_NAME, RESOURCE_MODIFICATION_REQUESTED_RK, message, CoreResourceRegistryRequest.class.getCanonicalName());

        // Sleep to make sure that the message has been delivered
        TimeUnit.MILLISECONDS.sleep(300);
        verifyZeroInteractions(mockedRepository);
    }

    @Test
    public void resourceRemovalRequestConsumerAuthFailTest() throws JsonProcessingException, InterruptedException, InvalidArgumentsException {
        rabbitManager.startConsumerOfResourceRemovalMessages(mockedAuthorizationManager);

        Resource resource1 = generateCoreResourceSensorWithoutId();
        Resource resource2 = generateCoreResourceSensorWithoutId();
        CoreResourceRegistryRequest coreResourceRegistryRequest = generateCoreResourceRegistryRequestBasicType(resource1, resource2);
        String message = mapper.writeValueAsString(coreResourceRegistryRequest);

        when(mockedAuthorizationManager.checkSinglePlatformOperationAccess(any(), any())).thenReturn(new AuthorizationResult("", false));
        when(mockedAuthorizationManager.checkIfResourcesBelongToPlatform(any(), anyString())).thenReturn(new AuthorizationResult("", false));

        rabbitManager.sendCustomMessage(RESOURCE_EXCHANGE_NAME, RESOURCE_REMOVAL_REQUESTED_RK, message, CoreResourceRegistryRequest.class.getCanonicalName());

        // Sleep to make sure that the message has been delivered
        TimeUnit.MILLISECONDS.sleep(300);
        verifyZeroInteractions(mockedRepository);
    }

    @Test
    public void resourceCreationRequestConsumerJsonFailTest() throws IOException, InterruptedException {
        rabbitManager.startConsumerOfResourceCreationMessages(mockedAuthorizationManager);

        //generating wrong payload for this communication
        Resource resource1 = generateCoreResourceSensorWithoutId();
        String message = mapper.writeValueAsString(resource1);

        rabbitManager.sendCustomMessage(RESOURCE_EXCHANGE_NAME, RESOURCE_CREATION_REQUESTED_RK, message, CoreResourceRegistryRequest.class.getCanonicalName());

        // Sleep to make sure that the message has been delivered
        TimeUnit.MILLISECONDS.sleep(300);
        verifyZeroInteractions(mockedRepository);
    }

    @Test
    public void resourceModificationRequestConsumerJsonFailTest() throws IOException, InterruptedException {
        rabbitManager.startConsumerOfResourceModificationMessages(mockedAuthorizationManager);

        //generating wrong payload for this communication
        Resource resource1 = generateCoreResourceSensorWithoutId();
        String message = mapper.writeValueAsString(resource1);

        rabbitManager.sendCustomMessage(RESOURCE_EXCHANGE_NAME, RESOURCE_MODIFICATION_REQUESTED_RK, message, CoreResourceRegistryRequest.class.getCanonicalName());

        // Sleep to make sure that the message has been delivered
        TimeUnit.MILLISECONDS.sleep(300);
        verifyZeroInteractions(mockedRepository);
    }

    @Test
    public void resourceRemovalRequestConsumerJsonFailTest() throws IOException, InterruptedException {
        rabbitManager.startConsumerOfResourceRemovalMessages(mockedAuthorizationManager);

        //generating wrong payload for this communication
        Resource resource1 = generateCoreResourceSensorWithoutId();
        String message = mapper.writeValueAsString(resource1);

        rabbitManager.sendCustomMessage(RESOURCE_EXCHANGE_NAME, RESOURCE_REMOVAL_REQUESTED_RK, message, CoreResourceRegistryRequest.class.getCanonicalName());

        // Sleep to make sure that the message has been delivered
        TimeUnit.MILLISECONDS.sleep(300);
        verifyZeroInteractions(mockedRepository);
    }

    @Test
    public void resourceCreationRequestConsumerResourceWithIdFailTest() throws IOException, InterruptedException, InvalidArgumentsException {
        rabbitManager.startConsumerOfResourceCreationMessages(mockedAuthorizationManager);

        //generating resource with ID (should not pass verification in consumer)
        Resource resource1 = generateCoreResourceSensorWithoutId();
        resource1 = addIdToResource(resource1);
        Resource resource2 = generateCoreResourceSensorWithoutId();
        CoreResourceRegistryRequest coreResourceRegistryRequest = generateCoreResourceRegistryRequestBasicType(resource1, resource2);
        String message = mapper.writeValueAsString(coreResourceRegistryRequest);

        when(mockedAuthorizationManager.checkSinglePlatformOperationAccess(any(), any())).thenReturn(new AuthorizationResult("", true));
        when(mockedAuthorizationManager.checkIfResourcesBelongToPlatform(any(), anyString())).thenReturn(new AuthorizationResult("", true));

        rabbitManager.sendCustomMessage(RESOURCE_EXCHANGE_NAME, RESOURCE_CREATION_REQUESTED_RK, message, CoreResourceRegistryRequest.class.getCanonicalName());

        // Sleep to make sure that the message has been delivered
        TimeUnit.MILLISECONDS.sleep(300);
        verifyZeroInteractions(mockedRepository);
    }

    @Test
    public void resourceModificationRequestConsumerResourceWithoutIdFailTest() throws IOException, InterruptedException, InvalidArgumentsException {
        rabbitManager.startConsumerOfResourceModificationMessages(mockedAuthorizationManager);

        //generating resource with ID (should not pass verification in consumer)
        Resource resource1 = generateCoreResourceSensorWithoutId();
        Resource resource2 = generateCoreResourceSensorWithoutId();
        CoreResourceRegistryRequest coreResourceRegistryRequest = generateCoreResourceRegistryRequestBasicType(resource1, resource2);
        String message = mapper.writeValueAsString(coreResourceRegistryRequest);

        when(mockedAuthorizationManager.checkSinglePlatformOperationAccess(any(), any())).thenReturn(new AuthorizationResult("", true));
        when(mockedAuthorizationManager.checkIfResourcesBelongToPlatform(any(), anyString())).thenReturn(new AuthorizationResult("", true));

        rabbitManager.sendCustomMessage(RESOURCE_EXCHANGE_NAME, RESOURCE_MODIFICATION_REQUESTED_RK, message, CoreResourceRegistryRequest.class.getCanonicalName());

        // Sleep to make sure that the message has been delivered
        TimeUnit.MILLISECONDS.sleep(300);
        verifyZeroInteractions(mockedRepository);
    }

    @Test
    public void resourceRemovalRequestConsumerWithoutIdFailTest() throws JsonProcessingException, InterruptedException, InvalidArgumentsException {
        rabbitManager.startConsumerOfResourceRemovalMessages(mockedAuthorizationManager);

        Resource resource1 = generateCoreResourceSensorWithoutId();
        Resource resource2 = generateCoreResourceSensorWithoutId();
        CoreResourceRegistryRequest coreResourceRegistryRequest = generateCoreResourceRegistryRequestBasicType(resource1, resource2);
        String message = mapper.writeValueAsString(coreResourceRegistryRequest);

        when(mockedAuthorizationManager.checkSinglePlatformOperationAccess(any(), any())).thenReturn(new AuthorizationResult("", true));
        when(mockedAuthorizationManager.checkIfResourcesBelongToPlatform(any(), anyString())).thenReturn(new AuthorizationResult("", true));

        rabbitManager.sendCustomMessage(RESOURCE_EXCHANGE_NAME, RESOURCE_REMOVAL_REQUESTED_RK, message, CoreResourceRegistryRequest.class.getCanonicalName());

        // Sleep to make sure that the message has been delivered
        TimeUnit.MILLISECONDS.sleep(300);
        verifyZeroInteractions(mockedRepository);
    }

    @Test
    public void resourceCreationRequestConsumerNullBodyFailTest() throws IOException, InterruptedException, InvalidArgumentsException {
        rabbitManager.startConsumerOfResourceCreationMessages(mockedAuthorizationManager);

        Resource resource1 = generateCoreResourceSensorWithoutId();
        Resource resource2 = generateCoreResourceSensorWithoutId();
        CoreResourceRegistryRequest coreResourceRegistryRequest = generateCoreResourceRegistryRequestBasicType(resource1, resource2);
        coreResourceRegistryRequest.setBody(null);
        String message = mapper.writeValueAsString(coreResourceRegistryRequest);

        when(mockedAuthorizationManager.checkSinglePlatformOperationAccess(any(), any())).thenReturn(new AuthorizationResult("", true));
        when(mockedAuthorizationManager.checkIfResourcesBelongToPlatform(any(), anyString())).thenReturn(new AuthorizationResult("", true));

        rabbitManager.sendCustomMessage(RESOURCE_EXCHANGE_NAME, RESOURCE_CREATION_REQUESTED_RK, message, CoreResourceRegistryRequest.class.getCanonicalName());

        // Sleep to make sure that the message has been delivered
        TimeUnit.MILLISECONDS.sleep(300);
        verifyZeroInteractions(mockedRepository);
    }

    @Test
    public void platformCreationRequestConsumerTest() throws Exception {
        rabbitManager.startConsumerOfPlatformCreationMessages();

        Platform requestPlatform = generateSymbiotePlatformA();
        String message = mapper.writeValueAsString(requestPlatform);

        PlatformPersistenceResult platformPersistenceResult = new PlatformPersistenceResult(200, "ok", requestPlatform);

        when(mockedRepository.savePlatform(any())).thenReturn(platformPersistenceResult);


        rabbitManager.sendCustomMessage(PLATFORM_EXCHANGE_NAME, PLATFORM_CREATION_REQUESTED_RK, message, Platform.class.getCanonicalName());

        // Sleep to make sure that the message has been delivered
        TimeUnit.MILLISECONDS.sleep(300);

        ArgumentCaptor<Platform> platformArgumentCaptor = ArgumentCaptor.forClass(Platform.class);
        verify(mockedRepository).savePlatform(platformArgumentCaptor.capture());

        Assert.assertTrue(platformArgumentCaptor.getValue().getId().equals(requestPlatform.getId()));
        Assert.assertTrue(platformArgumentCaptor.getValue().getDescription().get(0).equals(requestPlatform.getDescription().get(0)));
        Assert.assertTrue(platformArgumentCaptor.getValue().getName().equals(requestPlatform.getName()));
        Assert.assertTrue(platformArgumentCaptor.getValue().getInterworkingServices().get(0).getInformationModelId().
                equals(requestPlatform.getInterworkingServices().get(0).getInformationModelId()));
    }

    @Test
    public void platformCreationRequestConsumerRPCTest() throws Exception {
        rabbitManager.startConsumerOfPlatformCreationMessages();

        Platform requestPlatform = generateSymbiotePlatformA();
        String message = mapper.writeValueAsString(requestPlatform);

        PlatformPersistenceResult platformPersistenceResult = new PlatformPersistenceResult(200, "ok", requestPlatform);

        when(mockedRepository.savePlatform(any())).thenReturn(platformPersistenceResult);

        String response = rabbitManager.sendRpcMessageAndConsumeResponse(PLATFORM_EXCHANGE_NAME, PLATFORM_CREATION_REQUESTED_RK, message);

        PlatformRegistryResponse platformRegistryResponse = mapper.readValue(response, PlatformRegistryResponse.class);

        Platform platformResponse = platformRegistryResponse.getBody();
        Assert.assertNotNull(platformRegistryResponse.getMessage());
        Assert.assertEquals(platformRegistryResponse.getStatus(), 200);

        verify(mockedRepository).savePlatform(any());

        Assert.assertTrue(platformResponse.getId().equals(requestPlatform.getId()));
        Assert.assertTrue(platformResponse.getDescription().get(0).equals(requestPlatform.getDescription().get(0)));
        Assert.assertTrue(platformResponse.getName().equals(requestPlatform.getName()));
        Assert.assertTrue(platformResponse.getInterworkingServices().get(0).getInformationModelId().
                equals(requestPlatform.getInterworkingServices().get(0).getInformationModelId()));
    }

    @Test
    public void platformCreationRequestConsumerNullNameFailTest() throws Exception {
        rabbitManager.startConsumerOfPlatformCreationMessages();

        Platform requestPlatform = generatePlatformWithNullLabels();

        String message = mapper.writeValueAsString(requestPlatform);

        rabbitManager.sendCustomMessage(PLATFORM_EXCHANGE_NAME, PLATFORM_CREATION_REQUESTED_RK, message, Platform.class.getCanonicalName());

        // Sleep to make sure that the message has been delivered
        TimeUnit.MILLISECONDS.sleep(300);

        verifyZeroInteractions(mockedRepository);
    }

    @Test
    public void platformCreationRequestConsumerNullNameFailRPCTest() throws Exception {
        rabbitManager.startConsumerOfPlatformCreationMessages();

        Platform requestPlatform = generatePlatformWithNullLabels();

        String message = mapper.writeValueAsString(requestPlatform);

        String response = rabbitManager.sendRpcMessageAndConsumeResponse(PLATFORM_EXCHANGE_NAME, PLATFORM_CREATION_REQUESTED_RK, message);

        PlatformRegistryResponse platformRegistryResponse = mapper.readValue(response, PlatformRegistryResponse.class);

        Platform platformResponse = platformRegistryResponse.getBody();
        Assert.assertNotNull(platformRegistryResponse.getMessage());
        Assert.assertNotEquals(platformRegistryResponse.getStatus(), 200);

        Assert.assertTrue(platformResponse.getId().equals(requestPlatform.getId()));
        Assert.assertTrue(platformResponse.getDescription().get(0).equals(requestPlatform.getDescription().get(0)));
        Assert.assertTrue(platformResponse.getInterworkingServices().get(0).getInformationModelId().
                equals(requestPlatform.getInterworkingServices().get(0).getInformationModelId()));

        Assert.assertNull(platformResponse.getName());

        verifyZeroInteractions(mockedRepository);
    }

    @Test
    public void platformCreationRequestConsumerMongoFailRPCTest() throws Exception {
        rabbitManager.startConsumerOfPlatformCreationMessages();

        Platform requestPlatform = generatePlatformB();

        String message = mapper.writeValueAsString(requestPlatform);

        PlatformPersistenceResult platformPersistenceResult = new PlatformPersistenceResult(500, "mongo fail mock", requestPlatform);

        when(mockedRepository.savePlatform(any())).thenReturn(platformPersistenceResult);

        String response = rabbitManager.sendRpcMessageAndConsumeResponse(PLATFORM_EXCHANGE_NAME, PLATFORM_CREATION_REQUESTED_RK, message);

        PlatformRegistryResponse platformRegistryResponse = mapper.readValue(response, PlatformRegistryResponse.class);

        Platform platformResponse = platformRegistryResponse.getBody();
        Assert.assertNotNull(platformRegistryResponse.getMessage());
        Assert.assertEquals(500, platformRegistryResponse.getStatus());

        verify(mockedRepository, times(1)).savePlatform(any());
    }

    private Platform generatePlatformWithNullLabels() {
        Platform requestPlatform = new Platform();
        requestPlatform.setId(PLATFORM_A_ID);
        requestPlatform.setDescription(Arrays.asList(PLATFORM_A_DESCRIPTION));
        InterworkingService interworkingService = new InterworkingService();
        interworkingService.setInformationModelId(INFORMATION_MODEL_ID_A);
        interworkingService.setUrl(INTERWORKING_SERVICE_URL_A);
        requestPlatform.setInterworkingServices(Arrays.asList(interworkingService));
        requestPlatform.setRdf("http://www.symbIoTe.com/");
        requestPlatform.setRdfFormat(RDFFormat.JSONLD);
        return requestPlatform;
    }

    @Test
    public void platformCreationRequestConsumerJsonFailTest() throws Exception {
        rabbitManager.startConsumerOfPlatformCreationMessages();

        String message = "[wrong json]";

        rabbitManager.sendCustomMessage(PLATFORM_EXCHANGE_NAME, PLATFORM_CREATION_REQUESTED_RK, message, Platform.class.getCanonicalName());

        // Sleep to make sure that the message has been delivered
        TimeUnit.MILLISECONDS.sleep(300);

        verifyZeroInteractions(mockedRepository);
    }

    @Test
    public void platformCreationRequestConsumerJsonFailRPCTest() throws Exception {
        rabbitManager.startConsumerOfPlatformCreationMessages();

        String message = "[wrong json]";

        String response = rabbitManager.sendRpcMessageAndConsumeResponse(PLATFORM_EXCHANGE_NAME, PLATFORM_CREATION_REQUESTED_RK, message);

        PlatformRegistryResponse platformRegistryResponse = mapper.readValue(response, PlatformRegistryResponse.class);

        Platform platformResponse = platformRegistryResponse.getBody();
        Assert.assertNotNull(platformRegistryResponse.getMessage());
        Assert.assertEquals(platformRegistryResponse.getStatus(), 400);

        verifyZeroInteractions(mockedRepository);
    }

    @Test
    public void platformModificationRequestConsumerHappyPathTest() throws IOException, InterruptedException {
        rabbitManager.startConsumerOfPlatformModificationMessages();

        Platform requestPlatform = generateSymbiotePlatformA();
        String message = mapper.writeValueAsString(requestPlatform);

        PlatformPersistenceResult platformPersistenceResult = new PlatformPersistenceResult(200, "ok", requestPlatform);

        when(mockedRepository.modifyPlatform(any())).thenReturn(platformPersistenceResult);

        rabbitManager.sendCustomMessage(PLATFORM_EXCHANGE_NAME, PLATFORM_MODIFICATION_REQUESTED_RK, message, Platform.class.getCanonicalName());

        ArgumentCaptor<Platform> argument = ArgumentCaptor.forClass(Platform.class);
        // Timeout to make sure that the message has been delivered
        verify(mockedRepository, timeout(500)).modifyPlatform(argument.capture());

        Assert.assertEquals(argument.getValue().getId(), requestPlatform.getId());
        Assert.assertEquals(argument.getValue().getDescription().get(0), requestPlatform.getDescription().get(0));
        Assert.assertEquals(argument.getValue().getName(), requestPlatform.getName());
        Assert.assertEquals(argument.getValue().getInterworkingServices().get(0).getInformationModelId(), requestPlatform.getInterworkingServices().get(0).getInformationModelId());
    }

    @Test
    public void platformModificationRequestConsumerHappyPathRpcTest() throws IOException, InterruptedException {
        rabbitManager.startConsumerOfPlatformModificationMessages();

        Platform requestPlatform = generateSymbiotePlatformA();
        String message = mapper.writeValueAsString(requestPlatform);

        PlatformPersistenceResult platformPersistenceResult = new PlatformPersistenceResult(200, "ok", requestPlatform);

        when(mockedRepository.modifyPlatform(any())).thenReturn(platformPersistenceResult);

        String response = rabbitManager.sendRpcMessageAndConsumeResponse(PLATFORM_EXCHANGE_NAME, PLATFORM_MODIFICATION_REQUESTED_RK, message);

        PlatformRegistryResponse platformRegistryResponse = mapper.readValue(response, PlatformRegistryResponse.class);

        Platform platformResponse = platformRegistryResponse.getBody();
        Assert.assertNotNull(platformRegistryResponse.getMessage());
        Assert.assertEquals(platformRegistryResponse.getStatus(), 200);

        verify(mockedRepository).modifyPlatform(any());

        Assert.assertTrue(platformResponse.getId().equals(requestPlatform.getId()));
        Assert.assertTrue(platformResponse.getDescription().get(0).equals(requestPlatform.getDescription().get(0)));
        Assert.assertTrue(platformResponse.getName().equals(requestPlatform.getName()));
        Assert.assertTrue(platformResponse.getInterworkingServices().get(0).getInformationModelId().
                equals(requestPlatform.getInterworkingServices().get(0).getInformationModelId()));
    }

    @Test
    public void platformModificationRequestConsumerMongoFailRPCTest() throws IOException, InterruptedException, TimeoutException {
        rabbitManager.startConsumerOfPlatformModificationMessages();

        Platform requestPlatform = generateSymbiotePlatformA();
        String message = mapper.writeValueAsString(requestPlatform);

        PlatformPersistenceResult platformPersistenceResult = new PlatformPersistenceResult(400, "mongo fail mock", requestPlatform);

        when(mockedRepository.modifyPlatform(any())).thenReturn(platformPersistenceResult);

        String response = rabbitManager.sendRpcMessageAndConsumeResponse(PLATFORM_EXCHANGE_NAME, PLATFORM_MODIFICATION_REQUESTED_RK, message);

        PlatformRegistryResponse platformRegistryResponse = mapper.readValue(response, PlatformRegistryResponse.class);

        Platform platformResponse = platformRegistryResponse.getBody();
        Assert.assertNotNull(platformRegistryResponse.getMessage());
        Assert.assertEquals(platformRegistryResponse.getStatus(), 500);

        verify(mockedRepository, times(1)).modifyPlatform(any());
    }

    @Test
    public void platformModificationRequestConsumerJsonFailRpcTest() throws Exception {
        //// TODO: 20.07.2017 Add consumer for RPC response and verify it in tests!
        rabbitManager.startConsumerOfPlatformModificationMessages();

        String message = "[wrong json]";

        String response = rabbitManager.sendRpcMessageAndConsumeResponse(PLATFORM_EXCHANGE_NAME, PLATFORM_MODIFICATION_REQUESTED_RK, message);

        PlatformRegistryResponse platformRegistryResponse = mapper.readValue(response, PlatformRegistryResponse.class);

        Platform platformResponse = platformRegistryResponse.getBody();
        Assert.assertNotNull(platformRegistryResponse.getMessage());
        Assert.assertEquals(platformRegistryResponse.getStatus(), 400);

        verifyZeroInteractions(mockedRepository);
    }

    @Test
    public void platformRemovalRequestConsumerTest() throws IOException, InterruptedException {
        rabbitManager.startConsumerOfPlatformRemovalMessages();

        Platform requestPlatform = generateSymbiotePlatformA();
        String message = mapper.writeValueAsString(requestPlatform);

        PlatformPersistenceResult platformPersistenceResult = new PlatformPersistenceResult(200, "ok", requestPlatform);

        when(mockedRepository.removePlatform(any())).thenReturn(platformPersistenceResult);

        rabbitManager.sendCustomMessage(PLATFORM_EXCHANGE_NAME, PLATFORM_REMOVAL_REQUESTED_RK, message, Platform.class.getCanonicalName());

        // Sleep to make sure that the message has been delivered
        TimeUnit.MILLISECONDS.sleep(300);

        ArgumentCaptor<Platform> argument = ArgumentCaptor.forClass(Platform.class);
        verify(mockedRepository).removePlatform(argument.capture());
    }

    @Test
    public void platformRemovalRequestConsumerRPCTest() throws IOException, InterruptedException {
        rabbitManager.startConsumerOfPlatformRemovalMessages();

        Platform requestPlatform = generateSymbiotePlatformA();
        String message = mapper.writeValueAsString(requestPlatform);

        PlatformPersistenceResult platformPersistenceResult = new PlatformPersistenceResult(200, "ok", requestPlatform);

        when(mockedRepository.removePlatform(any())).thenReturn(platformPersistenceResult);

        String response = rabbitManager.sendRpcMessageAndConsumeResponse(PLATFORM_EXCHANGE_NAME, PLATFORM_REMOVAL_REQUESTED_RK, message);

        PlatformRegistryResponse platformRegistryResponse = mapper.readValue(response, PlatformRegistryResponse.class);

        Platform platformResponse = platformRegistryResponse.getBody();
        Assert.assertNotNull(platformRegistryResponse.getMessage());
        Assert.assertEquals(platformRegistryResponse.getStatus(), 200);

        verify(mockedRepository).removePlatform(any());

        Assert.assertTrue(platformResponse.getId().equals(requestPlatform.getId()));
        Assert.assertTrue(platformResponse.getDescription().get(0).equals(requestPlatform.getDescription().get(0)));
        Assert.assertTrue(platformResponse.getName().equals(requestPlatform.getName()));
        Assert.assertTrue(platformResponse.getInterworkingServices().get(0).getInformationModelId().
                equals(requestPlatform.getInterworkingServices().get(0).getInformationModelId()));
    }

    @Test
    public void platformRemovalRequestConsumerJsonFailTest() throws Exception {
        rabbitManager.startConsumerOfPlatformRemovalMessages();

        String message = "[wrong json]";

        rabbitManager.sendCustomMessage(PLATFORM_EXCHANGE_NAME, PLATFORM_REMOVAL_REQUESTED_RK, message, Platform.class.getCanonicalName());

        // Sleep to make sure that the message has been delivered
        TimeUnit.MILLISECONDS.sleep(500);

        verifyZeroInteractions(mockedRepository);
    }

    @Test
    public void platformRemovalRequestConsumerJsonFailRPCTest() throws Exception {
        rabbitManager.startConsumerOfPlatformRemovalMessages();

        String message = "[wrong json]";

        String response = rabbitManager.sendRpcMessageAndConsumeResponse(PLATFORM_EXCHANGE_NAME, PLATFORM_REMOVAL_REQUESTED_RK, message);

        PlatformRegistryResponse platformRegistryResponse = mapper.readValue(response, PlatformRegistryResponse.class);

        Platform platformResponse = platformRegistryResponse.getBody();
        Assert.assertNotNull(platformRegistryResponse.getMessage());
        Assert.assertEquals(platformRegistryResponse.getStatus(), 400);

        verifyZeroInteractions(mockedRepository);
    }

    @Test
    public void platformResourcesRequestedConsumerTest() throws Exception {
        rabbitManager.startConsumerOfPlatformResourcesRequestsMessages(mockedAuthorizationManager);

        Resource resource1 = generateCoreResourceSensorWithoutId();
        addIdToResource(resource1);
        Resource resource2 = generateCoreResourceSensorWithoutId();
        addIdToResource(resource2);
        CoreResourceRegistryRequest coreResourceRegistryRequest = generateCoreResourceRegistryRequestBasicType(resource1, resource2);

        String message = mapper.writeValueAsString(coreResourceRegistryRequest);

        when(mockedAuthorizationManager.checkSinglePlatformOperationAccess(any(), any())).thenReturn(new AuthorizationResult("", true));
        when(mockedAuthorizationManager.checkIfResourcesBelongToPlatform(any(), anyString())).thenReturn(new AuthorizationResult("ok", true));

        List<CoreResource> coreResourcesFound = Arrays.asList(RegistryUtils.convertResourceToCoreResource(resource1),
                RegistryUtils.convertResourceToCoreResource(resource2));
        when(mockedRepository.getResourcesForPlatform(coreResourceRegistryRequest.getPlatformId())).
                thenReturn(coreResourcesFound);

        String response = rabbitManager.sendRpcMessageAndConsumeResponse(PLATFORM_EXCHANGE_NAME, RESOURCES_FOR_PLATFORM_REQUESTED_RK, message);

        ResourceListResponse resourceListResponse = mapper.readValue(response, ResourceListResponse.class);

        List<Resource> resources = resourceListResponse.getBody();
        Assert.assertNotNull(resourceListResponse.getMessage());
        Assert.assertEquals(resourceListResponse.getStatus(), 200);
        Assert.assertTrue(resources.size() == 2);
    }

    @Test
    public void platformResourcesRequestedConsumerJsonFailTest() throws Exception {
        rabbitManager.startConsumerOfPlatformResourcesRequestsMessages(mockedAuthorizationManager);

        String message = "[]";

        String response = rabbitManager.sendRpcMessageAndConsumeResponse(PLATFORM_EXCHANGE_NAME, RESOURCES_FOR_PLATFORM_REQUESTED_RK, message);

        ResourceListResponse resourceListResponse = mapper.readValue(response, ResourceListResponse.class);

        List<Resource> resources = resourceListResponse.getBody();
        Assert.assertNotNull(resourceListResponse.getMessage());
        Assert.assertEquals(400, resourceListResponse.getStatus());
        Assert.assertTrue(resources.size() == 0);

        verifyZeroInteractions(mockedRepository);
    }

    @Test
    public void platformResourcesRequestedConsumerAuthFailTest() throws Exception {
        rabbitManager.startConsumerOfPlatformResourcesRequestsMessages(mockedAuthorizationManager);

        Resource resource1 = generateCoreResourceSensorWithoutId();
        addIdToResource(resource1);
        Resource resource2 = generateCoreResourceSensorWithoutId();
        addIdToResource(resource2);
        CoreResourceRegistryRequest coreResourceRegistryRequest = generateCoreResourceRegistryRequestBasicType(resource1, resource2);

        coreResourceRegistryRequest.setSecurityRequest(null);
        String message = mapper.writeValueAsString(coreResourceRegistryRequest);

        when(mockedAuthorizationManager.checkSinglePlatformOperationAccess(any(), any())).thenReturn(new AuthorizationResult("null token", false));

        String response = rabbitManager.sendRpcMessageAndConsumeResponse(PLATFORM_EXCHANGE_NAME, RESOURCES_FOR_PLATFORM_REQUESTED_RK, message);

        ResourceListResponse resourceListResponse = mapper.readValue(response, ResourceListResponse.class);

        List<Resource> resources = resourceListResponse.getBody();
        Assert.assertNotNull(resourceListResponse.getMessage());
        Assert.assertEquals(400, resourceListResponse.getStatus());
        Assert.assertTrue(resources.size() == 0);

        verifyZeroInteractions(mockedRepository);
    }

    @Test
    public void platformDetailsRequestedConsumerTest() throws Exception {
        rabbitManager.startConsumerOfPlatformDetailsConsumer();

        Platform platform = generatePlatformB();

        when(mockedAuthorizationManager.checkSinglePlatformOperationAccess(any(), any())).thenReturn(new AuthorizationResult("", true));
        when(mockedAuthorizationManager.checkIfResourcesBelongToPlatform(any(), anyString())).thenReturn(new AuthorizationResult("ok", true));

        when(mockedRepository.getPlatformById(platform.getId())).thenReturn(platform);

        String response = rabbitManager.sendRpcMessageAndConsumeResponse(PLATFORM_EXCHANGE_NAME, PLATFORM_DETAILS_REQUESTED_RK, platform.getId());

        PlatformRegistryResponse platformRegistryResponse = mapper.readValue(response, PlatformRegistryResponse.class);

        Platform receivedPlatform = platformRegistryResponse.getBody();
        Assert.assertNotNull(platformRegistryResponse.getMessage());
        Assert.assertEquals(platformRegistryResponse.getStatus(), 200);
        Assert.assertTrue(receivedPlatform.getId().equals(platform.getId()));
    }

    @Test
    public void platformDetailsRequestedConsumerWrongPlatformIdTest() throws Exception {
        rabbitManager.startConsumerOfPlatformDetailsConsumer();

        Platform platform = generatePlatformB();

        when(mockedAuthorizationManager.checkSinglePlatformOperationAccess(any(), any())).thenReturn(new AuthorizationResult("", true));
        when(mockedAuthorizationManager.checkIfResourcesBelongToPlatform(any(), anyString())).thenReturn(new AuthorizationResult("ok", true));

        when(mockedRepository.getPlatformById(platform.getId())).thenReturn(null);

        String response = rabbitManager.sendRpcMessageAndConsumeResponse(PLATFORM_EXCHANGE_NAME, PLATFORM_DETAILS_REQUESTED_RK, platform.getId());

        PlatformRegistryResponse platformRegistryResponse = mapper.readValue(response, PlatformRegistryResponse.class);

        assertNull(platformRegistryResponse.getBody());
        Assert.assertNotNull(platformRegistryResponse.getMessage());
        Assert.assertNotEquals(platformRegistryResponse.getStatus(), 200);
    }

    @Test
    public void informationModelCreationRequestConsumerHappyPathTest() throws Exception {
        rabbitManager.startConsumerOfInformationModelCreationMessages();

        InformationModelRequest informationModelRequest = new InformationModelRequest();
        InformationModel informationModel = generateInformationModelWithoutID();
        informationModelRequest.setBody(informationModel);

        String message = mapper.writeValueAsString(informationModelRequest);


        mockIMVerificationCommunication(null);

        rabbitManager.sendCustomMessage(INFORMATION_MODEL_EXCHANGE_NAME, INFORMATION_MODEL_CREATION_REQUESTED_RK, message, InformationModelRequest.class.getCanonicalName());

        // Sleep to make sure that the message has been delivered
        TimeUnit.MILLISECONDS.sleep(300);
        verify(mockedRepository).saveInformationModel(any());
    }

    @Test
    public void informationModelModificationRequestConsumerHappyPathTest() throws Exception {
        rabbitManager.startConsumerOfInformationModelModificationMessages();

        InformationModelRequest informationModelRequest = new InformationModelRequest();
        InformationModel informationModel = generateInformationModelFull();
        informationModelRequest.setBody(informationModel);

        String message = mapper.writeValueAsString(informationModelRequest);

        mockIMVerificationCommunication(null);

        rabbitManager.sendCustomMessage(INFORMATION_MODEL_EXCHANGE_NAME, INFORMATION_MODEL_MODIFICATION_REQUESTED_RK, message, InformationModelRequest.class.getCanonicalName());

        // Sleep to make sure that the message has been delivered
        TimeUnit.MILLISECONDS.sleep(300);
        verify(mockedRepository).modifyInformationModel(any());
    }

    @Test
    public void informationModelRemovalRequestConsumerHappyPathTest() throws Exception {
        rabbitManager.startConsumerOfInformationModelRemovalMessages();

        InformationModelRequest informationModelRequest = new InformationModelRequest();
        InformationModel informationModel = generateInformationModelFull();
        informationModelRequest.setBody(informationModel);

        InformationModelPersistenceResult informationModelPersistenceResult = new InformationModelPersistenceResult(200, "ok", informationModel);

        when(mockedRepository.removeInformationModel(any())).thenReturn(informationModelPersistenceResult);

        String message = mapper.writeValueAsString(informationModelRequest);

        String response = rabbitManager.sendRpcMessageAndConsumeResponse(PLATFORM_EXCHANGE_NAME, INFORMATION_MODEL_REMOVAL_REQUESTED_RK, message);

        InformationModelResponse informationModelResponse = mapper.readValue(response, InformationModelResponse.class);

        InformationModel informationModelResponseBody = informationModelResponse.getBody();
        Assert.assertNotNull(informationModelResponseBody);
        Assert.assertNotNull(informationModelResponse.getMessage());
        Assert.assertEquals(informationModelResponse.getStatus(), 200);

        verify(mockedRepository).removeInformationModel(any());
    }

    @Test
    public void informationModelRemovalRequestConsumerRepoFailTest() throws Exception {
        rabbitManager.startConsumerOfInformationModelRemovalMessages();

        InformationModelRequest informationModelRequest = new InformationModelRequest();
        InformationModel informationModel = generateInformationModelFull();
        informationModelRequest.setBody(informationModel);

        InformationModelPersistenceResult informationModelPersistenceResult = new InformationModelPersistenceResult(400, "Mocked repo FAIL", informationModel);

        when(mockedRepository.removeInformationModel(any())).thenReturn(informationModelPersistenceResult);

        String message = mapper.writeValueAsString(informationModelRequest);

        String response = rabbitManager.sendRpcMessageAndConsumeResponse(PLATFORM_EXCHANGE_NAME, INFORMATION_MODEL_REMOVAL_REQUESTED_RK, message);

        InformationModelResponse informationModelResponse = mapper.readValue(response, InformationModelResponse.class);

        InformationModel informationModelResponseBody = informationModelResponse.getBody();
        Assert.assertNotNull(informationModelResponseBody);
        Assert.assertNotNull(informationModelResponse.getMessage());
        Assert.assertEquals(informationModelResponse.getStatus(), 400);
    }

    @Test
    public void informationModelCreationRequestConsumerWithIDFailTest() throws Exception {
        rabbitManager.startConsumerOfInformationModelCreationMessages();

        InformationModelRequest informationModelRequest = new InformationModelRequest();
        InformationModel informationModel = generateInformationModelFull();
        informationModel.setId("mocked id"); //Information Model should not have an ID to register!
        informationModelRequest.setBody(informationModel);

        String message = mapper.writeValueAsString(informationModelRequest);
        String response = rabbitManager.sendRpcMessageAndConsumeResponse(PLATFORM_EXCHANGE_NAME, INFORMATION_MODEL_CREATION_REQUESTED_RK, message);

        InformationModelResponse informationModelResponse = mapper.readValue(response, InformationModelResponse.class);

        InformationModel informationModelResponseBody = informationModelResponse.getBody();
        Assert.assertNotNull(informationModelResponseBody);
        Assert.assertNotNull(informationModelResponse.getMessage());
        Assert.assertEquals(informationModelResponse.getStatus(), 400);

        verifyZeroInteractions(mockedRepository);
    }

    @Test
    public void informationModelModificationRequestConsumerWithoutIDFailTest() throws Exception {
        rabbitManager.startConsumerOfInformationModelModificationMessages();

        InformationModelRequest informationModelRequest = new InformationModelRequest();
        InformationModel informationModel = generateInformationModelWithoutID();
        informationModelRequest.setBody(informationModel);

        String message = mapper.writeValueAsString(informationModelRequest);
        String response = rabbitManager.sendRpcMessageAndConsumeResponse(PLATFORM_EXCHANGE_NAME, INFORMATION_MODEL_MODIFICATION_REQUESTED_RK, message);

        InformationModelResponse informationModelResponse = mapper.readValue(response, InformationModelResponse.class);

        InformationModel informationModelResponseBody = informationModelResponse.getBody();
        Assert.assertNotNull(informationModelResponseBody);
        Assert.assertNotNull(informationModelResponse.getMessage());
        Assert.assertEquals(informationModelResponse.getStatus(), 400);

        verifyZeroInteractions(mockedRepository);
    }

    @Test
    public void informationModelRemovalRequestConsumerWithoutIDFailTest() throws Exception {
        rabbitManager.startConsumerOfInformationModelRemovalMessages();

        InformationModelRequest informationModelRequest = new InformationModelRequest();
        InformationModel informationModel = generateInformationModelWithoutID();
        informationModelRequest.setBody(informationModel);

        String message = mapper.writeValueAsString(informationModelRequest);
        String response = rabbitManager.sendRpcMessageAndConsumeResponse(PLATFORM_EXCHANGE_NAME, INFORMATION_MODEL_REMOVAL_REQUESTED_RK, message);

        InformationModelResponse informationModelResponse = mapper.readValue(response, InformationModelResponse.class);

        InformationModel informationModelResponseBody = informationModelResponse.getBody();
        Assert.assertNotNull(informationModelResponseBody);
        Assert.assertNotNull(informationModelResponse.getMessage());
        Assert.assertEquals(informationModelResponse.getStatus(), 400);

        verifyZeroInteractions(mockedRepository);
    }

    @Test
    public void informationModelCreationRequestConsumerJsonFailRPCTest() throws Exception {
        rabbitManager.startConsumerOfInformationModelCreationMessages();

        String message = "[wrong json]";

        String response = rabbitManager.sendRpcMessageAndConsumeResponse(PLATFORM_EXCHANGE_NAME, INFORMATION_MODEL_CREATION_REQUESTED_RK, message);

        InformationModelResponse informationModelResponse = mapper.readValue(response, InformationModelResponse.class);

        Assert.assertNotNull(informationModelResponse.getMessage());
        Assert.assertEquals(informationModelResponse.getStatus(), 400);

        verifyZeroInteractions(mockedRepository);
    }

    @Test
    public void informationModelModificationRequestConsumerJsonFailRPCTest() throws Exception {
        rabbitManager.startConsumerOfInformationModelModificationMessages();

        String message = "[wrong json]";

        String response = rabbitManager.sendRpcMessageAndConsumeResponse(PLATFORM_EXCHANGE_NAME, INFORMATION_MODEL_MODIFICATION_REQUESTED_RK, message);

        InformationModelResponse informationModelResponse = mapper.readValue(response, InformationModelResponse.class);

        Assert.assertNotNull(informationModelResponse.getMessage());
        Assert.assertEquals(informationModelResponse.getStatus(), 400);

        verifyZeroInteractions(mockedRepository);
    }

    @Test
    public void informationModelRemovalRequestConsumerJsonFailRPCTest() throws Exception {
        rabbitManager.startConsumerOfInformationModelRemovalMessages();

        String message = "[wrong json]";

        String response = rabbitManager.sendRpcMessageAndConsumeResponse(PLATFORM_EXCHANGE_NAME, INFORMATION_MODEL_REMOVAL_REQUESTED_RK, message);

        InformationModelResponse informationModelResponse = mapper.readValue(response, InformationModelResponse.class);

        Assert.assertNotNull(informationModelResponse.getMessage());
        Assert.assertEquals(informationModelResponse.getStatus(), 400);

        verifyZeroInteractions(mockedRepository);
    }

    @Test
    public void informationModelCreationRequestConsumerNullFieldsRPCTest() throws Exception {
        rabbitManager.startConsumerOfInformationModelCreationMessages();

        InformationModelRequest informationModelRequest = new InformationModelRequest();
        InformationModel informationModel = new InformationModel();
        informationModelRequest.setBody(informationModel);

        String message = mapper.writeValueAsString(informationModelRequest);

        String response = rabbitManager.sendRpcMessageAndConsumeResponse(PLATFORM_EXCHANGE_NAME, INFORMATION_MODEL_CREATION_REQUESTED_RK, message);

        InformationModelResponse informationModelResponse = mapper.readValue(response, InformationModelResponse.class);

        InformationModel informationModelResponseBody = informationModelResponse.getBody();
        Assert.assertNotNull(informationModelResponseBody);
        Assert.assertNotNull(informationModelResponse.getMessage());
        Assert.assertEquals(informationModelResponse.getStatus(), 400);

        verifyZeroInteractions(mockedRepository);
    }

    @Test
    public void informationModelModificationRequestConsumerNullFieldsRPCTest() throws Exception {
        rabbitManager.startConsumerOfInformationModelModificationMessages();

        InformationModelRequest informationModelRequest = new InformationModelRequest();
        InformationModel informationModel = new InformationModel();
        informationModelRequest.setBody(informationModel);

        String message = mapper.writeValueAsString(informationModelRequest);

        String response = rabbitManager.sendRpcMessageAndConsumeResponse(PLATFORM_EXCHANGE_NAME, INFORMATION_MODEL_MODIFICATION_REQUESTED_RK, message);

        InformationModelResponse informationModelResponse = mapper.readValue(response, InformationModelResponse.class);

        InformationModel informationModelResponseBody = informationModelResponse.getBody();
        Assert.assertNotNull(informationModelResponseBody);
        Assert.assertNotNull(informationModelResponse.getMessage());
        Assert.assertEquals(informationModelResponse.getStatus(), 400);

        verifyZeroInteractions(mockedRepository);
    }

    @Test
    public void informationModelRemovalRequestConsumerNullFieldsRPCTest() throws Exception {
        rabbitManager.startConsumerOfInformationModelRemovalMessages();

        InformationModelRequest informationModelRequest = new InformationModelRequest();
        InformationModel informationModel = new InformationModel();
        informationModelRequest.setBody(informationModel);

        String message = mapper.writeValueAsString(informationModelRequest);

        String response = rabbitManager.sendRpcMessageAndConsumeResponse(PLATFORM_EXCHANGE_NAME, INFORMATION_MODEL_REMOVAL_REQUESTED_RK, message);

        InformationModelResponse informationModelResponse = mapper.readValue(response, InformationModelResponse.class);

        InformationModel informationModelResponseBody = informationModelResponse.getBody();
        Assert.assertNotNull(informationModelResponseBody);
        Assert.assertNotNull(informationModelResponse.getMessage());
        Assert.assertEquals(informationModelResponse.getStatus(), 400);

        verifyZeroInteractions(mockedRepository);
    }


    @Test
    public void getAllInformationModelsHappyPathRPCTest() throws Exception {
        rabbitManager.startConsumerOfGetAllInformationModelsRequestsMessages();

        InformationModel informationModel = generateInformationModelFull();
        when(mockedRepository.getAllInformationModels()).thenReturn(Arrays.asList(informationModel));

        String message = "some string";

        String response = rabbitManager.sendRpcMessageAndConsumeResponse(PLATFORM_EXCHANGE_NAME, GET_ALL_INFORMATION_MODELS_REQUESTED_RK, message);

        InformationModelListResponse informationModelResponse = mapper.readValue(response, InformationModelListResponse.class);

        List<InformationModel> informationModels = informationModelResponse.getBody();
        Assert.assertNotNull(informationModels);
        Assert.assertFalse(informationModels.isEmpty());
        Assert.assertNotNull(informationModelResponse.getMessage());
        Assert.assertEquals(informationModelResponse.getStatus(), 200);

        verify(mockedRepository, times(1)).getAllInformationModels();
    }

    @Test
    public void getAllInformationModelsAndCleanRDFsRPCTest() throws Exception {
        rabbitManager.startConsumerOfGetAllInformationModelsRequestsMessages();

        InformationModel informationModel = generateInformationModelFull();
        when(mockedRepository.getAllInformationModels()).thenReturn(Arrays.asList(informationModel));

        assertNotNull(informationModel.getRdf());

        String message = "false";

        String response = rabbitManager.sendRpcMessageAndConsumeResponse(PLATFORM_EXCHANGE_NAME, GET_ALL_INFORMATION_MODELS_REQUESTED_RK, message);

        InformationModelListResponse informationModelResponse = mapper.readValue(response, InformationModelListResponse.class);

        List<InformationModel> informationModels = informationModelResponse.getBody();
        assertNotNull(informationModels);
        assertFalse(informationModels.isEmpty());
        assertNotNull(informationModelResponse.getMessage());
        assertNull(informationModels.get(0).getRdf()); //check if RDF content has been removed
        assertEquals(informationModelResponse.getStatus(), 200);

        verify(mockedRepository, times(1)).getAllInformationModels();
    }

    private void mockSemanticManagerResourceTranslationCommunication(String message) throws IOException {
        this.channel.queueDeclare(TEMP_QUEUE, true, false, false, null);
        this.channel.queueBind(TEMP_QUEUE, RESOURCE_EXCHANGE_NAME, RESOURCE_TRANSLATION_REQUESTED_RK);

        this.channel.basicConsume(TEMP_QUEUE, new DefaultConsumer(this.channel) {
            @Override
            public void handleDelivery(String consumerTag, Envelope envelope, AMQP.BasicProperties properties, byte[] body) throws IOException {
                mockSemanticManagerResourceTranslationReply(envelope, properties, body);
            }
        });
    }

    public void mockSemanticManagerResourceTranslationReply(Envelope envelope, AMQP.BasicProperties properties, byte[] body) throws IOException {
        log.debug("\n|||||||| //MOCKED  SM REPLY ............ \nSemantic Manager received request!");

        String messageReceived = new String(body);
//        assertEquals(message, messageReceived);
        CoreResourceRegistryRequest request = mapper.readValue(messageReceived, CoreResourceRegistryRequest.class);

        assertNotNull(properties);
        String correlationId = properties.getCorrelationId();
        String replyQueueName = properties.getReplyTo();
        assertNotNull(correlationId);
        assertNotNull(replyQueueName);

        Map<String, CoreResource> coreResourcesMap = new HashMap<>();

        Map<String, Resource> resourcesMap = new HashMap<>();
        try {
            resourcesMap = mapper.readValue(request.getBody(), new TypeReference<Map<String, Resource>>() {
            });
        } catch (IOException e) {
            log.error("Could not deserialize content of request! " + e);
            throw e;
        }

        for (String key : resourcesMap.keySet()) {
            Resource resource = resourcesMap.get(key);
            resource.setId("some generated id " + key);
            CoreResource coreResource = convertResourceToCoreResource(resource);
            coreResourcesMap.put(key, coreResource);
        }

        ResourceInstanceValidationResult validationResult = new ResourceInstanceValidationResult();
        validationResult.setSuccess(true);
        validationResult.setMessage("ok");
        validationResult.setModelValidated("ok");
        validationResult.setModelValidatedAgainst("ok");
        validationResult.setObjectDescription(coreResourcesMap);

        byte[] responseBytes = mapper.writeValueAsBytes(validationResult);

        AMQP.BasicProperties replyProps = new AMQP.BasicProperties
                .Builder()
                .correlationId(properties.getCorrelationId())
                .build();

        this.channel.basicPublish("", properties.getReplyTo(), replyProps, responseBytes);
        this.channel.basicAck(envelope.getDeliveryTag(), false);
        log.debug("-> Semantic Manager replied: \n" + validationResult.toString() + "\n......... //MOCKED SM REPLY |||||||||||||| ");
    }

    private void mockSemanticManagerResourceValidationCommunication(String message) throws IOException {
        this.channel.queueDeclare(TEMP_QUEUE, true, false, false, null);
        this.channel.queueBind(TEMP_QUEUE, RESOURCE_EXCHANGE_NAME, RESOURCE_VALIDATION_REQUESTED_RK);

        this.channel.basicConsume(TEMP_QUEUE, new DefaultConsumer(this.channel) {
            @Override
            public void handleDelivery(String consumerTag, Envelope envelope, AMQP.BasicProperties properties, byte[] body) throws IOException {
                mockSemanticManagerResourceValidationReply(envelope, properties);
            }
        });
    }

    public void mockSemanticManagerResourceValidationReply(Envelope envelope, AMQP.BasicProperties properties) throws IOException {
        log.debug("\n|||||||| //MOCKED  SM REPLY ............ \nSemantic Manager received request!");

        String correlationId = properties.getCorrelationId();
        String replyQueueName = properties.getReplyTo();
        assertNotNull(correlationId);
        assertNotNull(replyQueueName);

        ResourceInstanceValidationResult validationResult = new ResourceInstanceValidationResult();

        byte[] responseBytes = mapper.writeValueAsBytes(validationResult);

        AMQP.BasicProperties replyProps = new AMQP.BasicProperties
                .Builder()
                .correlationId(properties.getCorrelationId())
                .build();

        this.channel.basicPublish("", properties.getReplyTo(), replyProps, responseBytes);
        this.channel.basicAck(envelope.getDeliveryTag(), false);
        log.debug("-> Semantic Manager replied: \n" + validationResult.toString() + "\n......... //MOCKED SM REPLY |||||||||||||| ");
    }

    private void mockIMVerificationCommunication(String message) throws IOException {
        this.channel.queueDeclare(TEMP_QUEUE, true, false, false, null);
        this.channel.queueBind(TEMP_QUEUE, PLATFORM_EXCHANGE_NAME, INFORMATION_MODEL_VALIDATION_REQUESTED_RK);

        this.channel.basicConsume(TEMP_QUEUE, new DefaultConsumer(this.channel) {
            @Override
            public void handleDelivery(String consumerTag, Envelope envelope, AMQP.BasicProperties properties, byte[] body) throws IOException {
                mockIMVerificationReply(envelope, properties, body);
            }
        });
    }

    public void mockIMVerificationReply(Envelope envelope, AMQP.BasicProperties properties, byte[] body) throws IOException {
        log.debug("\n|||||||| //MOCKED  SM REPLY ............ \nSemantic Manager received request!");

        String messageReceived = "{\"body\":" + new String(body) + "}"; //// todo: 30.10.2017 Hardcoded - find out why it does not work without it!

//        assertEquals(message, messageReceived);
        InformationModelRequest request = mapper.readValue(messageReceived, InformationModelRequest.class);

        assertNotNull(properties);
        String correlationId = properties.getCorrelationId();
        String replyQueueName = properties.getReplyTo();
        assertNotNull(correlationId);
        assertNotNull(replyQueueName);

        InformationModel informationModel = request.getBody();

        InformationModelValidationResult validationResult = new InformationModelValidationResult();
        validationResult.setSuccess(true);
        validationResult.setMessage("ok");
        validationResult.setModelValidated("ok");
        validationResult.setModelValidatedAgainst("ok");
        validationResult.setObjectDescription(informationModel);

        byte[] responseBytes = mapper.writeValueAsBytes(validationResult);

        AMQP.BasicProperties replyProps = new AMQP.BasicProperties
                .Builder()
                .correlationId(properties.getCorrelationId())
                .build();

        this.channel.basicPublish("", properties.getReplyTo(), replyProps, responseBytes);
        this.channel.basicAck(envelope.getDeliveryTag(), false);
        log.debug("-> Semantic Manager replied: \n" + validationResult.toString() + "\n......... //MOCKED SM REPLY |||||||||||||| ");
    }

    private AMQP.BasicProperties getProps(Channel channel) throws IOException {
        String replyQueueName = "Queue" + Math.random();
        channel.queueDeclare(replyQueueName, true, false, true, null);

        String correlationId = UUID.randomUUID().toString();
        AMQP.BasicProperties props = new AMQP.BasicProperties()
                .builder()
                .correlationId(correlationId)
                .replyTo(replyQueueName)
                .build();
        return props;
    }
}
