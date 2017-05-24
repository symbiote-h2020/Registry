package eu.h2020.symbiote;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import eu.h2020.symbiote.core.internal.CoreResourceRegistryRequest;
import eu.h2020.symbiote.core.model.Platform;
import eu.h2020.symbiote.core.model.resources.Resource;
import eu.h2020.symbiote.messaging.RabbitManager;
import eu.h2020.symbiote.model.PlatformResponse;
import eu.h2020.symbiote.model.RegistryPersistenceResult;
import eu.h2020.symbiote.model.RegistryPlatform;
import eu.h2020.symbiote.repository.RepositoryManager;
import eu.h2020.symbiote.utils.AuthorizationManager;
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
import java.util.Random;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import static eu.h2020.symbiote.TestSetupConfig.*;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.*;

/**
 * Created by mateuszl on 16.02.2017.
 */
@RunWith(MockitoJUnitRunner.class)
public class MessagingTests {

    private static Logger log = LoggerFactory.getLogger(MessagingTests.class);
    RepositoryManager mockedRepository;
    private Random rand;
    ObjectMapper mapper;
    @InjectMocks
    private RabbitManager rabbitManager;
    AuthorizationManager mockedAuthorizationManager;

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

        ReflectionTestUtils.setField(rabbitManager, "platformCreationRequestedRoutingKey", PLATFORM_CREATION_REQUESTED);
        ReflectionTestUtils.setField(rabbitManager, "platformModificationRequestedRoutingKey", PLATFORM_MODIFICATION_REQUESTED);
        ReflectionTestUtils.setField(rabbitManager, "platformRemovalRequestedRoutingKey", PLATFORM_REMOVAL_REQUESTED);
        ReflectionTestUtils.setField(rabbitManager, "resourceCreationRequestedRoutingKey", RESOURCE_CREATION_REQUESTED);
        ReflectionTestUtils.setField(rabbitManager, "resourceModificationRequestedRoutingKey", RESOURCE_MODIFICATION_REQUESTED);
        ReflectionTestUtils.setField(rabbitManager, "resourceRemovalRequestedRoutingKey", RESOURCE_REMOVAL_REQUESTED);


        ReflectionTestUtils.setField(rabbitManager, "platformCreatedRoutingKey", PLATFORM_CREATED_ROUTING_KEY);
        ReflectionTestUtils.setField(rabbitManager, "platformRemovedRoutingKey", PLATFORM_REMOVED_ROUTING_KEY);
        ReflectionTestUtils.setField(rabbitManager, "platformModifiedRoutingKey", PLATFORM_MODIFIED_ROUTING_KEY);
        ReflectionTestUtils.setField(rabbitManager, "resourceCreatedRoutingKey", RESOURCE_CREATED_ROUTING_KEY);
        ReflectionTestUtils.setField(rabbitManager, "resourceRemovedRoutingKey", RESOURCE_REMOVED_ROUTING_KEY);
        ReflectionTestUtils.setField(rabbitManager, "resourceModifiedRoutingKey", RESOURCE_MODIFIED_ROUTING_KEY);

        ReflectionTestUtils.invokeMethod(rabbitManager, "init");

        mockedRepository = mock(RepositoryManager.class);
        mockedAuthorizationManager = mock(AuthorizationManager.class);
        rand = new Random();
        mapper = new ObjectMapper();
    }

    @After
    public void teardown() {
        log.info("Rabbit cleaned!");
        try {
            Connection connection = rabbitManager.getConnection();
            Channel channel;
            if (connection != null && connection.isOpen()) {
                channel = connection.createChannel();
                channel.queueDelete(PLATFORM_CREATION_REQUESTED);
                channel.queueDelete(PLATFORM_MODIFICATION_REQUESTED);
                channel.queueDelete(PLATFORM_REMOVAL_REQUESTED);
                channel.queueDelete(RESOURCE_CREATION_REQUESTED);
                channel.queueDelete(RESOURCE_MODIFICATION_REQUESTED);
                channel.queueDelete(RESOURCE_REMOVAL_REQUESTED);
                channel.queueDelete(RESOURCE_CREATION_REQUESTED_QUEUE);
                channel.queueDelete(RESOURCE_MODIFICATION_REQUESTED_QUEUE);
                channel.queueDelete(RESOURCE_REMOVAL_REQUESTED_QUEUE);
                channel.queueDelete(PLATFORM_CREATION_REQUESTED_QUEUE);
                channel.queueDelete(PLATFORM_MODIFICATION_REQUESTED_QUEUE);
                channel.queueDelete(PLATFORM_REMOVAL_REQUESTED_QUEUE);
                channel.close();
                connection.close();
            }
        } catch (TimeoutException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Test
    public void resourceCreationRequestConsumerTest() throws InterruptedException, JsonProcessingException {
        rabbitManager.startConsumerOfResourceCreationMessages(mockedRepository, mockedAuthorizationManager);

        Resource resource1 = generateResource();
        Resource resource2 = generateResource();
        CoreResourceRegistryRequest coreResourceRegistryRequest = generateCoreResourceRegistryRequest(resource1, resource2);
        String message = "";
        try {
            message = mapper.writeValueAsString(coreResourceRegistryRequest);
        } catch (JsonProcessingException e) {
            e.printStackTrace();
        }



    }


    @Test
    public void resourceModificationRequestConsumerTest() {
    }

    @Test
    public void resourceRemovalRequestConsumerTest() throws IOException, InterruptedException {
        rabbitManager.startConsumerOfResourceRemovalMessages(mockedRepository, mockedAuthorizationManager);

        Resource resource1 = generateResource();
        Resource resource2 = generateResource();
        CoreResourceRegistryRequest coreResourceRegistryRequest = generateCoreResourceRegistryRequest(resource1, resource2);

        String message = mapper.writeValueAsString(coreResourceRegistryRequest);

        RegistryPersistenceResult registryPersistenceResult1 = new RegistryPersistenceResult();
        registryPersistenceResult1.setStatus(200);
        registryPersistenceResult1.setMessage("ok");
        registryPersistenceResult1.setResource(RegistryUtils.convertResourceToCoreResource(resource1));

        when(mockedRepository.removeResource(any())).thenReturn(registryPersistenceResult1);
        when(mockedAuthorizationManager.checkResourceOperationAccess(coreResourceRegistryRequest.getToken(),
                coreResourceRegistryRequest.getPlatformId())).thenReturn(true);
        when(mockedAuthorizationManager.checkIfResourcesBelongToPlatform(any(), anyString())).thenReturn(true);

        rabbitManager.sendCustomMessage(RESOURCE_EXCHANGE_NAME, RESOURCE_REMOVAL_REQUESTED, message, Resource.class.getCanonicalName());

        // Sleep to make sure that the message has been delivered
        TimeUnit.MILLISECONDS.sleep(300);

        ArgumentCaptor<Resource> argument = ArgumentCaptor.forClass(Resource.class);
        verify(mockedRepository, times(2)).removeResource(argument.capture());

    }

    @Test
    public void platformCreationRequestConsumerTest() throws Exception {
        rabbitManager.startConsumerOfPlatformCreationMessages(mockedRepository, mockedAuthorizationManager);

        Platform requestPlatform = generatePlatformA();
        String message = mapper.writeValueAsString(requestPlatform);

        PlatformResponse platformResponse = new PlatformResponse();
        platformResponse.setStatus(200);
        platformResponse.setMessage("ok");
        platformResponse.setPlatform(requestPlatform);

        when(mockedRepository.savePlatform(any())).thenReturn(platformResponse);

        rabbitManager.sendCustomMessage(PLATFORM_EXCHANGE_NAME, PLATFORM_CREATION_REQUESTED, message, RegistryPlatform.class.getCanonicalName());

        // Sleep to make sure that the message has been delivered
        TimeUnit.MILLISECONDS.sleep(300);

        ArgumentCaptor<RegistryPlatform> argument = ArgumentCaptor.forClass(RegistryPlatform.class);
        verify(mockedRepository).savePlatform(argument.capture());

        Assert.assertTrue(argument.getValue().getId().equals(requestPlatform.getPlatformId()));
        Assert.assertTrue(argument.getValue().getComments().get(0).equals(requestPlatform.getDescription()));
        Assert.assertTrue(argument.getValue().getLabels().get(0).equals(requestPlatform.getName()));
        Assert.assertTrue(argument.getValue().getInterworkingServices().get(0).getInformationModelId().equals(requestPlatform.getInformationModelId()));
    }

    @Test
    public void platformModificationRequestConsumerTest() throws IOException, InterruptedException {
        rabbitManager.startConsumerOfPlatformModificationMessages(mockedRepository, mockedAuthorizationManager);

        Platform requestPlatform = generatePlatformA();
        String message = mapper.writeValueAsString(requestPlatform);

        PlatformResponse platformResponse = new PlatformResponse();
        platformResponse.setStatus(200);
        platformResponse.setMessage("ok");
        platformResponse.setPlatform(requestPlatform);

        when(mockedRepository.modifyPlatform(any())).thenReturn(platformResponse);

        rabbitManager.sendCustomMessage(PLATFORM_EXCHANGE_NAME, PLATFORM_MODIFICATION_REQUESTED, message, RegistryPlatform.class.getCanonicalName());

        // Sleep to make sure that the message has been delivered
        TimeUnit.MILLISECONDS.sleep(300);

        ArgumentCaptor<RegistryPlatform> argument = ArgumentCaptor.forClass(RegistryPlatform.class);
        verify(mockedRepository).modifyPlatform(argument.capture());

        Assert.assertTrue(argument.getValue().getId().equals(requestPlatform.getPlatformId()));
        Assert.assertTrue(argument.getValue().getComments().get(0).equals(requestPlatform.getDescription()));
        Assert.assertTrue(argument.getValue().getLabels().get(0).equals(requestPlatform.getName()));
        Assert.assertTrue(argument.getValue().getInterworkingServices().get(0).getInformationModelId().equals(requestPlatform.getInformationModelId()));
    }

    @Test
    public void platformRemovalRequestConsumerTest() throws IOException, InterruptedException {
        rabbitManager.startConsumerOfPlatformRemovalMessages(mockedRepository, mockedAuthorizationManager);

        Platform requestPlatform = generatePlatformA();
        String message = mapper.writeValueAsString(requestPlatform);

        PlatformResponse platformResponse = new PlatformResponse();
        platformResponse.setStatus(200);
        platformResponse.setMessage("ok");
        platformResponse.setPlatform(requestPlatform);

        when(mockedRepository.removePlatform(any())).thenReturn(platformResponse);

        rabbitManager.sendCustomMessage(PLATFORM_EXCHANGE_NAME, PLATFORM_REMOVAL_REQUESTED, message, RegistryPlatform.class.getCanonicalName());

        // Sleep to make sure that the message has been delivered
        TimeUnit.MILLISECONDS.sleep(300);

        ArgumentCaptor<RegistryPlatform> argument = ArgumentCaptor.forClass(RegistryPlatform.class);
        verify(mockedRepository).removePlatform(argument.capture());
    }
}
