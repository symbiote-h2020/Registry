package eu.h2020.symbiote;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.rabbitmq.client.*;
import eu.h2020.symbiote.core.internal.CoreResourceRegistryRequest;
import eu.h2020.symbiote.core.model.Platform;
import eu.h2020.symbiote.core.model.resources.Resource;
import eu.h2020.symbiote.messaging.RabbitManager;
import eu.h2020.symbiote.model.AuthorizationResult;
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
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import static eu.h2020.symbiote.TestSetupConfig.*;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.*;

/**
 * Created by mateuszl on 16.02.2017.
 */
@RunWith(MockitoJUnitRunner.class)
public class MessagingTests {

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

        ReflectionTestUtils.setField(rabbitManager, "platformCreationRequestedRoutingKey", PLATFORM_CREATION_REQUESTED_RK);
        ReflectionTestUtils.setField(rabbitManager, "platformModificationRequestedRoutingKey", PLATFORM_MODIFICATION_REQUESTED_RK);
        ReflectionTestUtils.setField(rabbitManager, "platformRemovalRequestedRoutingKey", PLATFORM_REMOVAL_REQUESTED_RK);
        ReflectionTestUtils.setField(rabbitManager, "resourceCreationRequestedRoutingKey", RESOURCE_CREATION_REQUESTED_RK);
        ReflectionTestUtils.setField(rabbitManager, "resourceModificationRequestedRoutingKey", RESOURCE_MODIFICATION_REQUESTED_RK);
        ReflectionTestUtils.setField(rabbitManager, "resourceRemovalRequestedRoutingKey", RESOURCE_REMOVAL_REQUESTED_RK);


        ReflectionTestUtils.setField(rabbitManager, "platformCreatedRoutingKey", PLATFORM_CREATED_ROUTING_KEY);
        ReflectionTestUtils.setField(rabbitManager, "platformRemovedRoutingKey", PLATFORM_REMOVED_ROUTING_KEY);
        ReflectionTestUtils.setField(rabbitManager, "platformModifiedRoutingKey", PLATFORM_MODIFIED_ROUTING_KEY);
        ReflectionTestUtils.setField(rabbitManager, "resourceCreatedRoutingKey", RESOURCE_CREATED_ROUTING_KEY);
        ReflectionTestUtils.setField(rabbitManager, "resourceRemovedRoutingKey", RESOURCE_REMOVED_ROUTING_KEY);
        ReflectionTestUtils.setField(rabbitManager, "resourceModifiedRoutingKey", RESOURCE_MODIFIED_ROUTING_KEY);

        ReflectionTestUtils.setField(rabbitManager, "jsonResourceTranslationRequestedRoutingKey", RESOURCE_TRANSLATION_REQUESTED_QUEUE);
//        ReflectionTestUtils.setField(rabbitManager, "jsonResourceValidationRequestedRoutingKey", RESOURCE_VALIDATION_REQUESTED_QUEUE);

        ReflectionTestUtils.invokeMethod(rabbitManager, "init");

        mockedRepository = mock(RepositoryManager.class);
        mockedAuthorizationManager = mock(AuthorizationManager.class);
        mapper = new ObjectMapper();
        connection = rabbitManager.getConnection();
        channel = connection.createChannel();
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
    public void resourceCreationRequestConsumerHappyPathTest() throws InterruptedException, IOException, TimeoutException {

        String queueName = "RPCqueueCreation";

        rabbitManager.startConsumerOfResourceCreationMessages(mockedRepository, mockedAuthorizationManager);

        Resource resource1 = generateResource();
        Resource resource2 = generateResource();
        CoreResourceRegistryRequest coreResourceRegistryRequest = generateCoreResourceRegistryRequest(resource1, resource2);
        String message = mapper.writeValueAsString(coreResourceRegistryRequest);

        when(mockedAuthorizationManager.checkResourceOperationAccess(coreResourceRegistryRequest.getToken(),
                coreResourceRegistryRequest.getPlatformId())).thenReturn(new AuthorizationResult("", true));
        when(mockedAuthorizationManager.checkIfResourcesBelongToPlatform(any(), anyString())).thenReturn(new AuthorizationResult("ok", true));

        this.channel.queueDeclare(queueName, true, false, false, null);
        this.channel.queueBind(queueName, RESOURCE_EXCHANGE_NAME, RESOURCE_CREATION_REQUESTED_RK);

        this.channel.basicConsume(queueName, new DefaultConsumer(this.channel) {
            @Override
            public void handleDelivery(String consumerTag, Envelope envelope, AMQP.BasicProperties properties, byte[] body) throws IOException {
                String messageReceived = new String(body);
                assertEquals(message, messageReceived);

                assertNotNull(properties);

                String correlationId = properties.getCorrelationId();
                String replyQueueName = properties.getReplyTo();

                assertNotNull(correlationId);
                assertNotNull(replyQueueName);

            }
        });

        rabbitManager.sendCustomMessage(RESOURCE_EXCHANGE_NAME, RESOURCE_CREATION_REQUESTED_RK, message, Resource.class.getCanonicalName());
    }


    @Test
    public void resourceModificationRequestConsumerTest() throws InterruptedException, IOException {
        String queueName = "RPCqueueModification";

        rabbitManager.startConsumerOfResourceModificationMessages(mockedRepository, mockedAuthorizationManager);

        Resource resource1 = generateResource();
        Resource resource2 = generateResource();
        CoreResourceRegistryRequest coreResourceRegistryRequest = generateCoreResourceRegistryRequest(resource1, resource2);
        String message = mapper.writeValueAsString(coreResourceRegistryRequest);

        when(mockedAuthorizationManager.checkResourceOperationAccess(coreResourceRegistryRequest.getToken(),
                coreResourceRegistryRequest.getPlatformId())).thenReturn(new AuthorizationResult("", true));
        when(mockedAuthorizationManager.checkIfResourcesBelongToPlatform(any(), anyString())).thenReturn(new AuthorizationResult("ok", true));

        this.channel.queueDeclare(queueName, true, false, false, null);
        this.channel.queueBind(queueName, RESOURCE_EXCHANGE_NAME, RESOURCE_MODIFICATION_REQUESTED_RK);

        this.channel.basicConsume(queueName, new DefaultConsumer(this.channel) {
            @Override
            public void handleDelivery(String consumerTag, Envelope envelope, AMQP.BasicProperties properties, byte[] body) throws IOException {
                String messageReceived = new String(body);
                assertEquals(message, messageReceived);

                assertNotNull(properties);

                String correlationId = properties.getCorrelationId();
                String replyQueueName = properties.getReplyTo();

                assertNotNull(correlationId);
                assertNotNull(replyQueueName);

            }
        });

        rabbitManager.sendCustomMessage(RESOURCE_EXCHANGE_NAME, RESOURCE_MODIFICATION_REQUESTED_RK, message, Resource.class.getCanonicalName());
    }

    @Test
    public void resourceRemovalRequestConsumerTest() throws IOException, InterruptedException {
        rabbitManager.startConsumerOfResourceRemovalMessages(mockedRepository, mockedAuthorizationManager);

        Resource resource1 = generateResource();
        addIdToResource(resource1);
        Resource resource2 = generateResource();
        addIdToResource(resource2);
        CoreResourceRegistryRequest coreResourceRegistryRequest = generateCoreResourceRegistryRequest(resource1, resource2);

        String message = mapper.writeValueAsString(coreResourceRegistryRequest);

        RegistryPersistenceResult registryPersistenceResult1 = new RegistryPersistenceResult();
        registryPersistenceResult1.setStatus(200);
        registryPersistenceResult1.setMessage("ok");
        registryPersistenceResult1.setResource(RegistryUtils.convertResourceToCoreResource(resource1));

        when(mockedRepository.removeResource(any())).thenReturn(registryPersistenceResult1);
        when(mockedAuthorizationManager.checkResourceOperationAccess(coreResourceRegistryRequest.getToken(),
                coreResourceRegistryRequest.getPlatformId())).thenReturn(new AuthorizationResult("", true));
        when(mockedAuthorizationManager.checkIfResourcesBelongToPlatform(any(), anyString())).thenReturn(new AuthorizationResult("ok", true));

        rabbitManager.sendCustomMessage(RESOURCE_EXCHANGE_NAME, RESOURCE_REMOVAL_REQUESTED_RK, message, Resource.class.getCanonicalName());

        // Sleep to make sure that the message has been delivered
        TimeUnit.MILLISECONDS.sleep(300);

        ArgumentCaptor<Resource> argument = ArgumentCaptor.forClass(Resource.class);
        verify(mockedRepository, times(2)).removeResource(argument.capture());

    }

    @Test
    public void platformCreationRequestConsumerTest() throws Exception {
        rabbitManager.startConsumerOfPlatformCreationMessages(mockedRepository, mockedAuthorizationManager);

        Platform requestPlatform = generateSymbiotePlatformA();
        String message = mapper.writeValueAsString(requestPlatform);

        PlatformResponse platformResponse = new PlatformResponse();
        platformResponse.setStatus(200);
        platformResponse.setMessage("ok");
        platformResponse.setPlatform(requestPlatform);

        when(mockedRepository.savePlatform(any())).thenReturn(platformResponse);

        rabbitManager.sendCustomMessage(PLATFORM_EXCHANGE_NAME, PLATFORM_CREATION_REQUESTED_RK, message, RegistryPlatform.class.getCanonicalName());

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
    public void platformCreationRequestConsumerNullFailTest() throws Exception {
        rabbitManager.startConsumerOfPlatformCreationMessages(mockedRepository, mockedAuthorizationManager);

        Platform requestPlatform = generateSymbiotePlatformA();
        requestPlatform.setName(null);
        String message = mapper.writeValueAsString(requestPlatform);

        rabbitManager.sendCustomMessage(PLATFORM_EXCHANGE_NAME, PLATFORM_CREATION_REQUESTED_RK, message, RegistryPlatform.class.getCanonicalName());

        // Sleep to make sure that the message has been delivered
        TimeUnit.MILLISECONDS.sleep(300);

        verifyZeroInteractions(mockedRepository);
    }

    @Test
    public void platformCreationRequestConsumerJsonFailTest() throws Exception {
        rabbitManager.startConsumerOfPlatformCreationMessages(mockedRepository, mockedAuthorizationManager);

        String message = "[wrong json]";

        rabbitManager.sendCustomMessage(PLATFORM_EXCHANGE_NAME, PLATFORM_CREATION_REQUESTED_RK, message, RegistryPlatform.class.getCanonicalName());

        // Sleep to make sure that the message has been delivered
        TimeUnit.MILLISECONDS.sleep(300);

        verifyZeroInteractions(mockedRepository);
    }

    @Test
    public void platformModificationRequestConsumerHappyPathTest() throws IOException, InterruptedException {
        rabbitManager.startConsumerOfPlatformModificationMessages(mockedRepository, mockedAuthorizationManager);

        Platform requestPlatform = generateSymbiotePlatformA();
        String message = mapper.writeValueAsString(requestPlatform);

        PlatformResponse platformResponse = new PlatformResponse();
        platformResponse.setStatus(200);
        platformResponse.setMessage("ok");
        platformResponse.setPlatform(requestPlatform);

        when(mockedRepository.modifyPlatform(any())).thenReturn(platformResponse);

        rabbitManager.sendCustomMessage(PLATFORM_EXCHANGE_NAME, PLATFORM_MODIFICATION_REQUESTED_RK, message, RegistryPlatform.class.getCanonicalName());

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
    public void platformModificationRequestConsumerMongoFailTest() throws IOException, InterruptedException, TimeoutException {
        rabbitManager.startConsumerOfPlatformModificationMessages(mockedRepository, mockedAuthorizationManager);

        Platform requestPlatform = generateSymbiotePlatformA();
        String message = mapper.writeValueAsString(requestPlatform);

        PlatformResponse platformResponse = new PlatformResponse();
        platformResponse.setStatus(400);
        platformResponse.setMessage("mongo fail");
        platformResponse.setPlatform(requestPlatform);

        when(mockedRepository.modifyPlatform(any())).thenReturn(platformResponse);

        rabbitManager.sendCustomRpcMessage(PLATFORM_EXCHANGE_NAME, PLATFORM_MODIFICATION_REQUESTED_RK, message,
                new DefaultConsumer(this.channel) {
            @Override
            public void handleDelivery(String consumerTag, Envelope envelope, AMQP.BasicProperties properties, byte[] body) throws IOException {
                String messageReceived = new String(body);
                PlatformResponse platformResponseReceived = mapper.readValue(messageReceived, PlatformResponse.class);

                assertNotNull(properties);
                String correlationId = properties.getCorrelationId();
                assertNotNull(correlationId);

                assertEquals(400, platformResponseReceived.getStatus());
                assertEquals(requestPlatform, platformResponseReceived.getPlatform());

                log.info("Received reply message!");
            }
        });

        // Sleep to make sure that the message has been delivered
        TimeUnit.MILLISECONDS.sleep(500);

    }

    @Test
    public void platformModificationRequestConsumerJsonFailTest() throws Exception {
        rabbitManager.startConsumerOfPlatformModificationMessages(mockedRepository, mockedAuthorizationManager);

        String message = "[wrong json]";

        rabbitManager.sendCustomMessage(PLATFORM_EXCHANGE_NAME, PLATFORM_MODIFICATION_REQUESTED_RK, message, RegistryPlatform.class.getCanonicalName());

        // Sleep to make sure that the message has been delivered
        TimeUnit.MILLISECONDS.sleep(300);

        verifyZeroInteractions(mockedRepository);
    }

    @Test
    public void platformRemovalRequestConsumerTest() throws IOException, InterruptedException {
        rabbitManager.startConsumerOfPlatformRemovalMessages(mockedRepository, mockedAuthorizationManager);

        Platform requestPlatform = generateSymbiotePlatformA();
        String message = mapper.writeValueAsString(requestPlatform);

        PlatformResponse platformResponse = new PlatformResponse();
        platformResponse.setStatus(200);
        platformResponse.setMessage("ok");
        platformResponse.setPlatform(requestPlatform);

        when(mockedRepository.removePlatform(any())).thenReturn(platformResponse);

        rabbitManager.sendCustomMessage(PLATFORM_EXCHANGE_NAME, PLATFORM_REMOVAL_REQUESTED_RK, message, RegistryPlatform.class.getCanonicalName());

        // Sleep to make sure that the message has been delivered
        TimeUnit.MILLISECONDS.sleep(300);

        ArgumentCaptor<RegistryPlatform> argument = ArgumentCaptor.forClass(RegistryPlatform.class);
        verify(mockedRepository).removePlatform(argument.capture());
    }

    @Test
    public void platformRemovalRequestConsumerJsonFailTest() throws Exception {
        rabbitManager.startConsumerOfPlatformRemovalMessages(mockedRepository, mockedAuthorizationManager);

        String message = "[wrong json]";

        rabbitManager.sendCustomMessage(PLATFORM_EXCHANGE_NAME, PLATFORM_REMOVAL_REQUESTED_RK, message, RegistryPlatform.class.getCanonicalName());

        // Sleep to make sure that the message has been delivered
        TimeUnit.MILLISECONDS.sleep(300);

        verifyZeroInteractions(mockedRepository);
    }
}
