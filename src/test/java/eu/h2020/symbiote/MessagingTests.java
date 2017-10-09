package eu.h2020.symbiote;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.rabbitmq.client.*;
import eu.h2020.symbiote.core.cci.PlatformRegistryResponse;
import eu.h2020.symbiote.core.cci.RDFResourceRegistryRequest;
import eu.h2020.symbiote.core.internal.CoreResourceRegistryRequest;
import eu.h2020.symbiote.core.internal.ResourceInstanceValidationResult;
import eu.h2020.symbiote.core.internal.ResourceListResponse;
import eu.h2020.symbiote.core.model.InterworkingService;
import eu.h2020.symbiote.core.model.Platform;
import eu.h2020.symbiote.core.model.RDFFormat;
import eu.h2020.symbiote.core.model.RDFInfo;
import eu.h2020.symbiote.core.model.internal.CoreResource;
import eu.h2020.symbiote.core.model.resources.Resource;
import eu.h2020.symbiote.managers.AuthorizationManager;
import eu.h2020.symbiote.managers.RabbitManager;
import eu.h2020.symbiote.managers.RepositoryManager;
import eu.h2020.symbiote.model.AuthorizationResult;
import eu.h2020.symbiote.model.PlatformPersistenceResult;
import eu.h2020.symbiote.model.ResourcePersistenceResult;
import eu.h2020.symbiote.utils.RegistryUtils;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.runners.MockitoJUnitRunner;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.test.util.ReflectionTestUtils;

import java.io.IOException;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import static eu.h2020.symbiote.TestSetupConfig.*;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.*;

/**
 * Created by mateuszl on 16.02.2017.
 */
@RunWith(MockitoJUnitRunner.class)
public class MessagingTests {

    public static final String TEMP_QUEUE = "RPCqueue";
    private static Logger log = LoggerFactory.getLogger(MessagingTests.class);
    private RepositoryManager mockedRepository;
    private ObjectMapper mapper;
    private AuthorizationManager mockedAuthorizationManager;
    @InjectMocks
    private RabbitManager rabbitManager;
    private Connection connection;
    private Channel channel;

    @Before
    public void setup() throws IOException, TimeoutException {

        ReflectionTestUtils.setField(rabbitManager, "rabbitHost", "localhost");
        ReflectionTestUtils.setField(rabbitManager, "rabbitUsername", "guest");
        ReflectionTestUtils.setField(rabbitManager, "rabbitPassword", "guest");

        ReflectionTestUtils.setField(rabbitManager, "platformExchangeName", PLATFORM_EXCHANGE_NAME);
        ReflectionTestUtils.setField(rabbitManager, "platformExchangeType", "topic");
        ReflectionTestUtils.setField(rabbitManager, "plaftormExchangeDurable", true);
        ReflectionTestUtils.setField(rabbitManager, "platformExchangeAutodelete", false);
        ReflectionTestUtils.setField(rabbitManager, "platformExchangeInternal", false);

        ReflectionTestUtils.setField(rabbitManager, "resourceExchangeName", RESOURCE_EXCHANGE_NAME);
        ReflectionTestUtils.setField(rabbitManager, "resourceExchangeType", "topic");
        ReflectionTestUtils.setField(rabbitManager, "resourceExchangeDurable", true);
        ReflectionTestUtils.setField(rabbitManager, "resourceExchangeAutodelete", false);
        ReflectionTestUtils.setField(rabbitManager, "resourceExchangeInternal", false);

        ReflectionTestUtils.setField(rabbitManager, "federationExchangeName", FEDERATION_EXCHANGE_NAME);
        ReflectionTestUtils.setField(rabbitManager, "federationExchangeType", "topic");
        ReflectionTestUtils.setField(rabbitManager, "federationExchangeDurable", true);
        ReflectionTestUtils.setField(rabbitManager, "federationExchangeAutodelete", false);
        ReflectionTestUtils.setField(rabbitManager, "federationExchangeInternal", false);

        ReflectionTestUtils.setField(rabbitManager, "informationModelExchangeName", INFORMATION_MODEL_EXCHANGE_NAME);
        ReflectionTestUtils.setField(rabbitManager, "informationModelExchangeType", "topic");
        ReflectionTestUtils.setField(rabbitManager, "informationModelExchangeDurable", true);
        ReflectionTestUtils.setField(rabbitManager, "informationModelExchangeAutodelete", false);
        ReflectionTestUtils.setField(rabbitManager, "informationModelExchangeInternal", false);

        ReflectionTestUtils.setField(rabbitManager, "platformCreationRequestedRoutingKey", PLATFORM_CREATION_REQUESTED_RK);
        ReflectionTestUtils.setField(rabbitManager, "platformModificationRequestedRoutingKey", PLATFORM_MODIFICATION_REQUESTED_RK);
        ReflectionTestUtils.setField(rabbitManager, "platformRemovalRequestedRoutingKey", PLATFORM_REMOVAL_REQUESTED_RK);
        ReflectionTestUtils.setField(rabbitManager, "resourceCreationRequestedRoutingKey", RESOURCE_CREATION_REQUESTED_RK);
        ReflectionTestUtils.setField(rabbitManager, "resourceModificationRequestedRoutingKey", RESOURCE_MODIFICATION_REQUESTED_RK);
        ReflectionTestUtils.setField(rabbitManager, "resourceRemovalRequestedRoutingKey", RESOURCE_REMOVAL_REQUESTED_RK);

        ReflectionTestUtils.setField(rabbitManager, "federationCreationRequestedRoutingKey", FEDERATION_CREATION_REQUESTED_RK);
        ReflectionTestUtils.setField(rabbitManager, "federationModificationRequestedRoutingKey", FEDERATION_MODIFICATION_REQUESTED_RK);
        ReflectionTestUtils.setField(rabbitManager, "federationRemovalRequestedRoutingKey", FEDERATION_REMOVAL_REQUESTED_RK);

        ReflectionTestUtils.setField(rabbitManager, "informationModelCreationRequestedRoutingKey", INFORMATION_MODEL_CREATION_REQUESTED_RK);
        ReflectionTestUtils.setField(rabbitManager, "informationModelModificationRequestedRoutingKey", INFORMATION_MODEL_MODIFICATION_REQUESTED_RK);
        ReflectionTestUtils.setField(rabbitManager, "informationModelRemovalRequestedRoutingKey", INFORMATION_MODEL_REMOVAL_REQUESTED_RK);

        ReflectionTestUtils.setField(rabbitManager, "platformCreatedRoutingKey", PLATFORM_CREATED_ROUTING_KEY);
        ReflectionTestUtils.setField(rabbitManager, "platformRemovedRoutingKey", PLATFORM_REMOVED_ROUTING_KEY);
        ReflectionTestUtils.setField(rabbitManager, "platformModifiedRoutingKey", PLATFORM_MODIFIED_ROUTING_KEY);
        ReflectionTestUtils.setField(rabbitManager, "resourceCreatedRoutingKey", RESOURCE_CREATED_ROUTING_KEY);
        ReflectionTestUtils.setField(rabbitManager, "resourceRemovedRoutingKey", RESOURCE_REMOVED_ROUTING_KEY);
        ReflectionTestUtils.setField(rabbitManager, "resourceModifiedRoutingKey", RESOURCE_MODIFIED_ROUTING_KEY);

        ReflectionTestUtils.setField(rabbitManager, "federationsRequestedRoutingKey", GET_ALL_FEDERATIONS_RK);
        ReflectionTestUtils.setField(rabbitManager, "federationRequestedRoutingKey", GET_FEDERATION_FOR_PLATFORM_RK);

        ReflectionTestUtils.setField(rabbitManager, "aamExchangeName", AAM_EXCHANGE_NAME);
        ReflectionTestUtils.setField(rabbitManager, "aamGetPlatformOwners", AAM_GET_PLATFORM_OWNERS_RK);

        ReflectionTestUtils.setField(rabbitManager, "platformResourcesRequestedRoutingKey", RESOURCES_FOR_PLATFORM_REQUESTED_RK);

        ReflectionTestUtils.setField(rabbitManager, "jsonResourceTranslationRequestedRoutingKey", RESOURCE_TRANSLATION_REQUESTED_RK);
        ReflectionTestUtils.setField(rabbitManager, "rdfResourceValidationRequestedRoutingKey", RESOURCE_VALIDATION_REQUESTED_RK);

        ReflectionTestUtils.invokeMethod(rabbitManager, "init");

        mockedRepository = mock(RepositoryManager.class);
        mockedAuthorizationManager = mock(AuthorizationManager.class);
        mapper = new ObjectMapper();
        connection = rabbitManager.getConnection();
        channel = connection.createChannel();

        ReflectionTestUtils.setField(rabbitManager, "repositoryManager", mockedRepository);

    }

    @After
    public void teardown() {
        log.info("Rabbit cleaned!");
        try {
            connection = rabbitManager.getConnection();
            if (connection != null && connection.isOpen()) {
                channel = connection.createChannel();
                channel.queueDelete(PLATFORM_CREATION_REQUESTED_RK);
                channel.queueDelete(PLATFORM_MODIFICATION_REQUESTED_RK);
                channel.queueDelete(PLATFORM_REMOVAL_REQUESTED_RK);
                channel.queueDelete(RESOURCE_CREATION_REQUESTED_RK);
                channel.queueDelete(RESOURCE_MODIFICATION_REQUESTED_RK);
                channel.queueDelete(RESOURCE_REMOVAL_REQUESTED_RK);
                channel.queueDelete(RESOURCE_CREATION_REQUESTED_QUEUE);
                channel.queueDelete(RESOURCE_MODIFICATION_REQUESTED_QUEUE);
                channel.queueDelete(RESOURCE_REMOVAL_REQUESTED_QUEUE);
                channel.queueDelete(PLATFORM_CREATION_REQUESTED_QUEUE);
                channel.queueDelete(PLATFORM_MODIFICATION_REQUESTED_QUEUE);
                channel.queueDelete(PLATFORM_REMOVAL_REQUESTED_QUEUE);
                channel.queueDelete(RESOURCES_FOR_PLATFORM_REQUESTED_RK);
                channel.queueDelete(PLATFORM_RESOURCES_REQUESTED_QUEUE);
                channel.queueDelete(TEMP_QUEUE);
                channel.close();
                connection.close();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void setRabbitManagerMockedManagers() {
        ReflectionTestUtils.setField(rabbitManager, "repositoryManager", mockedRepository);
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

    @Test
    public void resourceCreationRequestConsumerHappyPathTest() throws InterruptedException, IOException, TimeoutException {
        rabbitManager.startConsumerOfResourceCreationMessages(mockedAuthorizationManager);
        setRabbitManagerMockedManagers();

        Resource resource1 = generateResourceWithoutId();
        Resource resource2 = generateResourceWithoutId();
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

    /*
        @Test
        public void resourceCreationRequestConsumerRPCHappyPathTest() throws IOException {
            rabbitManager.startConsumerOfResourceCreationMessages(mockedAuthorizationManager);
            setRabbitManagerMockedManagers();

            Resource resource1 = generateResourceWithoutId();
            Resource resource2 = generateResourceWithoutId();
            CoreResourceRegistryRequest coreResourceRegistryRequest = generateCoreResourceRegistryRequestRdfType(resource1, resource2);
            String message = mapper.writeValueAsString(coreResourceRegistryRequest);

            when(mockedAuthorizationManager.checkSinglePlatformOperationAccess(any(), any())).thenReturn(new AuthorizationResult("", true));
            when(mockedAuthorizationManager.checkIfResourcesBelongToPlatform(any(), anyString())).thenReturn(new AuthorizationResult("ok", true));
            when(mockedRepository.saveResource(any())).thenReturn(new ResourcePersistenceResult(200, "ok", RegistryUtils.convertResourceToCoreResource(resource1)));
            when(mockedRepository.getInformationModelIdByInterworkingServiceUrl(any(), any())).thenReturn("mocked");

            mockSemanticManagerResourceValidationCommunication(message);

            rabbitManager.sendCustomMessage(RESOURCE_EXCHANGE_NAME, RESOURCE_CREATION_REQUESTED_RK, message, CoreResourceRegistryRequest.class.getCanonicalName());

            // Timeout to make sure that the message has been delivered
            verify(mockedRepository, timeout(500).times(2)).saveResource(any());
        }
    */
    @Test
    public void resourceModificationRequestConsumerHappyPathTest() throws InterruptedException, IOException {
        rabbitManager.startConsumerOfResourceModificationMessages(mockedAuthorizationManager);
        setRabbitManagerMockedManagers();

        Resource resource1 = generateResourceWithoutId();
        addIdToResource(resource1);
        Resource resource2 = generateResourceWithoutId();
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
    public void resourceRemovalRequestConsumerHappyPathTest() throws IOException, InterruptedException {
        rabbitManager.startConsumerOfResourceRemovalMessages(this.mockedAuthorizationManager);
        setRabbitManagerMockedManagers();

        Resource resource1 = generateResourceWithoutId();
        addIdToResource(resource1);
        Resource resource2 = generateResourceWithoutId();
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
    public void resourceCreationRequestConsumerAuthFailTest() throws IOException, InterruptedException {
        rabbitManager.startConsumerOfResourceCreationMessages(mockedAuthorizationManager);
        setRabbitManagerMockedManagers();

        Resource resource1 = generateResourceWithoutId();
        Resource resource2 = generateResourceWithoutId();
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
    public void resourceModificationRequestConsumerAuthFailTest() throws InterruptedException, IOException {
        rabbitManager.startConsumerOfResourceModificationMessages(mockedAuthorizationManager);
        setRabbitManagerMockedManagers();

        Resource resource1 = generateResourceWithoutId();
        Resource resource2 = generateResourceWithoutId();
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
    public void resourceRemovalRequestConsumerAuthFailTest() throws JsonProcessingException, InterruptedException {
        rabbitManager.startConsumerOfResourceRemovalMessages(mockedAuthorizationManager);
        setRabbitManagerMockedManagers();

        Resource resource1 = generateResourceWithoutId();
        Resource resource2 = generateResourceWithoutId();
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
        setRabbitManagerMockedManagers();

        //generating wrong payload for this communication
        Resource resource1 = generateResourceWithoutId();
        String message = mapper.writeValueAsString(resource1);

        rabbitManager.sendCustomMessage(RESOURCE_EXCHANGE_NAME, RESOURCE_CREATION_REQUESTED_RK, message, CoreResourceRegistryRequest.class.getCanonicalName());

        // Sleep to make sure that the message has been delivered
        TimeUnit.MILLISECONDS.sleep(300);
        verifyZeroInteractions(mockedRepository);
    }

    @Test
    public void resourceModificationRequestConsumerJsonFailTest() throws IOException, InterruptedException {
        rabbitManager.startConsumerOfResourceModificationMessages(mockedAuthorizationManager);
        setRabbitManagerMockedManagers();

        //generating wrong payload for this communication
        Resource resource1 = generateResourceWithoutId();
        String message = mapper.writeValueAsString(resource1);

        rabbitManager.sendCustomMessage(RESOURCE_EXCHANGE_NAME, RESOURCE_MODIFICATION_REQUESTED_RK, message, CoreResourceRegistryRequest.class.getCanonicalName());

        // Sleep to make sure that the message has been delivered
        TimeUnit.MILLISECONDS.sleep(300);
        verifyZeroInteractions(mockedRepository);
    }

    @Test
    public void resourceRemovalRequestConsumerJsonFailTest() throws IOException, InterruptedException {
        rabbitManager.startConsumerOfResourceRemovalMessages(mockedAuthorizationManager);
        setRabbitManagerMockedManagers();

        //generating wrong payload for this communication
        Resource resource1 = generateResourceWithoutId();
        String message = mapper.writeValueAsString(resource1);

        rabbitManager.sendCustomMessage(RESOURCE_EXCHANGE_NAME, RESOURCE_REMOVAL_REQUESTED_RK, message, CoreResourceRegistryRequest.class.getCanonicalName());

        // Sleep to make sure that the message has been delivered
        TimeUnit.MILLISECONDS.sleep(300);
        verifyZeroInteractions(mockedRepository);
    }

    @Test
    public void resourceCreationRequestConsumerResourceWithIdFailTest() throws IOException, InterruptedException {
        rabbitManager.startConsumerOfResourceCreationMessages(mockedAuthorizationManager);
        setRabbitManagerMockedManagers();

        //generating resource with ID (should not pass verification in consumer)
        Resource resource1 = generateResourceWithoutId();
        resource1 = addIdToResource(resource1);
        Resource resource2 = generateResourceWithoutId();
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
    public void resourceModificationRequestConsumerResourceWithoutIdFailTest() throws IOException, InterruptedException {
        rabbitManager.startConsumerOfResourceModificationMessages(mockedAuthorizationManager);
        setRabbitManagerMockedManagers();

        //generating resource with ID (should not pass verification in consumer)
        Resource resource1 = generateResourceWithoutId();
        Resource resource2 = generateResourceWithoutId();
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
    public void resourceRemovalRequestConsumerWithoutIdFailTest() throws JsonProcessingException, InterruptedException {
        rabbitManager.startConsumerOfResourceRemovalMessages(mockedAuthorizationManager);
        setRabbitManagerMockedManagers();

        Resource resource1 = generateResourceWithoutId();
        Resource resource2 = generateResourceWithoutId();
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
    public void resourceCreationRequestConsumerNullBodyFailTest() throws IOException, InterruptedException {
        rabbitManager.startConsumerOfResourceCreationMessages(mockedAuthorizationManager);
        setRabbitManagerMockedManagers();

        Resource resource1 = generateResourceWithoutId();
        Resource resource2 = generateResourceWithoutId();
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

    private void mockSemanticManagerResourceTranslationCommunication(String message) throws IOException {
        this.channel.queueDeclare(TEMP_QUEUE, true, false, false, null);
        this.channel.queueBind(TEMP_QUEUE, RESOURCE_EXCHANGE_NAME, RESOURCE_TRANSLATION_REQUESTED_RK);

        this.channel.basicConsume(TEMP_QUEUE, new DefaultConsumer(this.channel) {
            @Override
            public void handleDelivery(String consumerTag, Envelope envelope, AMQP.BasicProperties properties, byte[] body) throws IOException {
                mockSemanticManagerResourceTranslationReply(envelope, properties, body, message);
            }
        });
    }

    public void mockSemanticManagerResourceTranslationReply(Envelope envelope, AMQP.BasicProperties properties, byte[] body, String message) throws IOException {
        log.debug("\n|||||||| //MOCKED  SM REPLY ............ \nSemantic Manager received request!");

        String messageReceived = new String(body);
        assertEquals(message, messageReceived);
        CoreResourceRegistryRequest request = mapper.readValue(messageReceived, CoreResourceRegistryRequest.class);

        assertNotNull(properties);
        String correlationId = properties.getCorrelationId();
        String replyQueueName = properties.getReplyTo();
        assertNotNull(correlationId);
        assertNotNull(replyQueueName);

        Map<String, CoreResource> resources = new HashMap<>();
        try {
            resources = mapper.readValue(request.getBody(), new TypeReference<Map<String, CoreResource>>() {
            });
        } catch (IOException e) {
            log.error("Could not deserialize content of request!" + e);
            throw e;
        }

        ResourceInstanceValidationResult validationResult = new ResourceInstanceValidationResult();
        validationResult.setSuccess(true);
        validationResult.setMessage("ok");
        validationResult.setModelValidated("ok");
        validationResult.setModelValidatedAgainst("ok");
        validationResult.setObjectDescription(resources);

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
                mockSemanticManagerResourceValidationReply(envelope, properties, body, message);
            }
        });
    }


    public void mockSemanticManagerResourceValidationReply(Envelope envelope, AMQP.BasicProperties properties, byte[] body, String message) throws IOException {
        log.debug("\n|||||||| //MOCKED  SM REPLY ............ \nSemantic Manager received request!");

        String messageReceived = new String(body);
//        assertEquals(message, messageReceived);
        RDFResourceRegistryRequest request = mapper.readValue(messageReceived, RDFResourceRegistryRequest.class);

        assertNotNull(properties);
        String correlationId = properties.getCorrelationId();
        String replyQueueName = properties.getReplyTo();
        assertNotNull(correlationId);
        assertNotNull(replyQueueName);

        RDFInfo rdfInfo = request.getBody();


        Resource resource1 = generateResourceWithoutId();
        Resource resource2 = generateResourceWithoutId();

        Map<String, CoreResource> resources = new HashMap<>();

        resources.put("1", RegistryUtils.convertResourceToCoreResource(resource1));
        resources.put("2", RegistryUtils.convertResourceToCoreResource(resource2));

        ResourceInstanceValidationResult validationResult = new ResourceInstanceValidationResult();
        validationResult.setSuccess(true);
        validationResult.setMessage("ok");
        validationResult.setModelValidated("ok");
        validationResult.setModelValidatedAgainst("ok");
        validationResult.setObjectDescription(resources);

        byte[] responseBytes = mapper.writeValueAsBytes(validationResult);

        AMQP.BasicProperties replyProps = new AMQP.BasicProperties
                .Builder()
                .correlationId(properties.getCorrelationId())
                .build();

        this.channel.basicPublish("", properties.getReplyTo(), replyProps, responseBytes);
        this.channel.basicAck(envelope.getDeliveryTag(), false);
        log.debug("-> Semantic Manager replied: \n" + validationResult.toString() + "\n......... //MOCKED SM REPLY |||||||||||||| ");
    }

    @Test
    public void platformCreationRequestConsumerTest() throws Exception {
        rabbitManager.startConsumerOfPlatformCreationMessages();
        setRabbitManagerMockedManagers();

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
        Assert.assertTrue(platformArgumentCaptor.getValue().getComments().get(0).equals(requestPlatform.getComments().get(0)));
        Assert.assertTrue(platformArgumentCaptor.getValue().getLabels().get(0).equals(requestPlatform.getLabels().get(0)));
        Assert.assertTrue(platformArgumentCaptor.getValue().getInterworkingServices().get(0).getInformationModelId().
                equals(requestPlatform.getInterworkingServices().get(0).getInformationModelId()));
    }

    @Test
    public void platformCreationRequestConsumerRPCTest() throws Exception {
        rabbitManager.startConsumerOfPlatformCreationMessages();
        setRabbitManagerMockedManagers();

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
        Assert.assertTrue(platformResponse.getComments().get(0).equals(requestPlatform.getComments().get(0)));
        Assert.assertTrue(platformResponse.getLabels().get(0).equals(requestPlatform.getLabels().get(0)));
        Assert.assertTrue(platformResponse.getInterworkingServices().get(0).getInformationModelId().
                equals(requestPlatform.getInterworkingServices().get(0).getInformationModelId()));
    }

    @Test
    public void platformCreationRequestConsumerNullNameFailTest() throws Exception {
        rabbitManager.startConsumerOfPlatformCreationMessages();
        setRabbitManagerMockedManagers();

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
        setRabbitManagerMockedManagers();

        Platform requestPlatform = generatePlatformWithNullLabels();

        String message = mapper.writeValueAsString(requestPlatform);

        String response = rabbitManager.sendRpcMessageAndConsumeResponse(PLATFORM_EXCHANGE_NAME, PLATFORM_CREATION_REQUESTED_RK, message);

        PlatformRegistryResponse platformRegistryResponse = mapper.readValue(response, PlatformRegistryResponse.class);

        Platform platformResponse = platformRegistryResponse.getBody();
        Assert.assertNotNull(platformRegistryResponse.getMessage());
        Assert.assertNotEquals(platformRegistryResponse.getStatus(), 200);

        Assert.assertTrue(platformResponse.getId().equals(requestPlatform.getId()));
        Assert.assertTrue(platformResponse.getComments().get(0).equals(requestPlatform.getComments().get(0)));
        Assert.assertTrue(platformResponse.getInterworkingServices().get(0).getInformationModelId().
                equals(requestPlatform.getInterworkingServices().get(0).getInformationModelId()));

        Assert.assertNull(platformResponse.getLabels());

        verifyZeroInteractions(mockedRepository);
    }

    private Platform generatePlatformWithNullLabels() {
        Platform requestPlatform = new Platform();
        requestPlatform.setId(PLATFORM_A_ID);
        requestPlatform.setComments(Arrays.asList(PLATFORM_A_DESCRIPTION));
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
        setRabbitManagerMockedManagers();

        String message = "[wrong json]";

        rabbitManager.sendCustomMessage(PLATFORM_EXCHANGE_NAME, PLATFORM_CREATION_REQUESTED_RK, message, Platform.class.getCanonicalName());

        // Sleep to make sure that the message has been delivered
        TimeUnit.MILLISECONDS.sleep(300);

        verifyZeroInteractions(mockedRepository);
    }

    @Test
    public void platformCreationRequestConsumerJsonFailRPCTest() throws Exception {
        rabbitManager.startConsumerOfPlatformCreationMessages();
        setRabbitManagerMockedManagers();

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
        setRabbitManagerMockedManagers();

        Platform requestPlatform = generateSymbiotePlatformA();
        String message = mapper.writeValueAsString(requestPlatform);

        PlatformPersistenceResult platformPersistenceResult = new PlatformPersistenceResult(200, "ok", requestPlatform);

        when(mockedRepository.modifyPlatform(any())).thenReturn(platformPersistenceResult);

        rabbitManager.sendCustomMessage(PLATFORM_EXCHANGE_NAME, PLATFORM_MODIFICATION_REQUESTED_RK, message, Platform.class.getCanonicalName());

        ArgumentCaptor<Platform> argument = ArgumentCaptor.forClass(Platform.class);
        // Timeout to make sure that the message has been delivered
        verify(mockedRepository, timeout(500)).modifyPlatform(argument.capture());

        Assert.assertTrue(argument.getValue().getId().equals(requestPlatform.getId()));
        Assert.assertTrue(argument.getValue().getComments().get(0).equals(requestPlatform.getComments().get(0)));
        Assert.assertTrue(argument.getValue().getLabels().get(0).equals(requestPlatform.getLabels().get(0)));
        Assert.assertTrue(argument.getValue().getInterworkingServices().get(0).getInformationModelId().equals(requestPlatform.getInterworkingServices().get(0).getInformationModelId()));
    }

    @Test
    public void platformModificationRequestConsumerHappyPathRpcTest() throws IOException, InterruptedException {
        rabbitManager.startConsumerOfPlatformModificationMessages();
        setRabbitManagerMockedManagers();

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
        Assert.assertTrue(platformResponse.getComments().get(0).equals(requestPlatform.getComments().get(0)));
        Assert.assertTrue(platformResponse.getLabels().get(0).equals(requestPlatform.getLabels().get(0)));
        Assert.assertTrue(platformResponse.getInterworkingServices().get(0).getInformationModelId().
                equals(requestPlatform.getInterworkingServices().get(0).getInformationModelId()));
    }

    @Test
    public void platformModificationRequestConsumerMongoFailRPCTest() throws IOException, InterruptedException, TimeoutException {
        rabbitManager.startConsumerOfPlatformModificationMessages();
        setRabbitManagerMockedManagers();

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
        setRabbitManagerMockedManagers();

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
        setRabbitManagerMockedManagers();

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
        setRabbitManagerMockedManagers();

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
        Assert.assertTrue(platformResponse.getComments().get(0).equals(requestPlatform.getComments().get(0)));
        Assert.assertTrue(platformResponse.getLabels().get(0).equals(requestPlatform.getLabels().get(0)));
        Assert.assertTrue(platformResponse.getInterworkingServices().get(0).getInformationModelId().
                equals(requestPlatform.getInterworkingServices().get(0).getInformationModelId()));
    }

    @Test
    public void platformRemovalRequestConsumerJsonFailTest() throws Exception {
        rabbitManager.startConsumerOfPlatformRemovalMessages();
        setRabbitManagerMockedManagers();

        String message = "[wrong json]";

        rabbitManager.sendCustomMessage(PLATFORM_EXCHANGE_NAME, PLATFORM_REMOVAL_REQUESTED_RK, message, Platform.class.getCanonicalName());

        // Sleep to make sure that the message has been delivered
        TimeUnit.MILLISECONDS.sleep(500);

        verifyZeroInteractions(mockedRepository);
    }

    @Test
    public void platformRemovalRequestConsumerJsonFailRPCTest() throws Exception {
        rabbitManager.startConsumerOfPlatformRemovalMessages();
        setRabbitManagerMockedManagers();

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
        setRabbitManagerMockedManagers();

        Resource resource1 = generateResourceWithoutId();
        addIdToResource(resource1);
        Resource resource2 = generateResourceWithoutId();
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
        setRabbitManagerMockedManagers();

        String message = "[]";

        String response = rabbitManager.sendRpcMessageAndConsumeResponse(PLATFORM_EXCHANGE_NAME, RESOURCES_FOR_PLATFORM_REQUESTED_RK, message);

        ResourceListResponse resourceListResponse = mapper.readValue(response, ResourceListResponse.class);

        List<Resource> resources = resourceListResponse.getBody();
        Assert.assertNotNull(resourceListResponse.getMessage());
        Assert.assertEquals(400, resourceListResponse.getStatus());
        Assert.assertTrue(resources.size() == 0);

        verifyZeroInteractions(mockedRepository);
    }

    /* //todo
    @Test
    public void platformResourcesRequestedConsumerNullTokenFailTest() throws Exception {
        rabbitManager.startConsumerOfPlatformResourcesRequestsMessages();
        setRabbitManagerMockedManagers();
        CoreResourceRegistryRequest coreResourceRegistryRequest = generateCoreResourceRegistryRequestBasicType();
        coreResourceRegistryRequest.setSecurityRequest(null);
        String message = mapper.writeValueAsString(coreResourceRegistryRequest);

        when(mockedAuthorizationManager.checkSinglePlatformOperationAccess(any(),any())).thenReturn(new AuthorizationResult("null token", false));

        rabbitManager.sendCustomRpcMessage(PLATFORM_EXCHANGE_NAME, RESOURCES_FOR_PLATFORM_REQUESTED_RK, message,
                new DefaultConsumer(this.channel) {
                    @Override
                    public void handleDelivery(String consumerTag, Envelope envelope, AMQP.BasicProperties properties, byte[] body) throws IOException {
                        String messageReceived = new String(body);
                        ResourceRegistryResponse responseReceived;
                        Map<String, Resource> resourcesReceived = new HashMap<>();
                        try {
                            responseReceived = mapper.readValue(messageReceived, ResourceRegistryResponse.class);
                            resourcesReceived = responseReceived.getBody();
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                        assertNotNull(properties);
                        String correlationId = properties.getCorrelationId();
                        assertNotNull(correlationId);

                        assertEquals(new ArrayList<>(), resourcesReceived);
                        log.info("Received reply message: " + messageReceived);
                    }
                });

        // Sleep to make sure that the message has been delivered
        TimeUnit.MILLISECONDS.sleep(500);

        verifyZeroInteractions(mockedRepository);
    }
*/
}
