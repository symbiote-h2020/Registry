package eu.h2020.symbiote;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.rabbitmq.client.*;
import eu.h2020.symbiote.core.cci.PlatformRegistryResponse;
import eu.h2020.symbiote.core.cci.ResourceRegistryResponse;
import eu.h2020.symbiote.core.internal.CoreResourceRegistryRequest;
import eu.h2020.symbiote.core.model.Platform;
import eu.h2020.symbiote.core.model.internal.CoreResource;
import eu.h2020.symbiote.core.model.resources.Resource;
import eu.h2020.symbiote.managers.AuthorizationManager;
import eu.h2020.symbiote.managers.RepositoryManager;
import eu.h2020.symbiote.messaging.RabbitManager;
import eu.h2020.symbiote.model.AuthorizationResult;
import eu.h2020.symbiote.model.PlatformPersistenceResult;
import eu.h2020.symbiote.utils.RegistryUtils;
import org.junit.After;
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

        ReflectionTestUtils.setField(rabbitManager, "platformResourcesRequestedRoutingKey", RESOURCES_FOR_PLATFORM_REQUESTED_RK);

        ReflectionTestUtils.setField(rabbitManager, "jsonResourceTranslationRequestedRoutingKey", RESOURCE_TRANSLATION_REQUESTED_RK);
//        ReflectionTestUtils.setField(rabbitManager, "jsonResourceValidationRequestedRoutingKey", RESOURCE_VALIDATION_REQUESTED_RK);

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
        } catch (TimeoutException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void setRabbitManagerMockedManagers() {
        ReflectionTestUtils.setField(rabbitManager, "repositoryManager", mockedRepository);
        ReflectionTestUtils.setField(rabbitManager, "authorizationManager", mockedAuthorizationManager);
    }

    /* //todo

    @Test
    public void resourceCreationRequestConsumerAndValidationConsumerIntegrationTest() throws InterruptedException, IOException, TimeoutException {
        rabbitManager.startConsumerOfResourceCreationMessages();
        setRabbitManagerMockedManagers();

        Resource resource1 = generateResource();
        Resource resource2 = generateResource();
        CoreResourceRegistryRequest coreResourceRegistryRequest = generateCoreResourceRegistryRequest(resource1, resource2);
        String message = mapper.writeValueAsString(coreResourceRegistryRequest);

        when(mockedAuthorizationManager.checkOperationAccess(coreResourceRegistryRequest.getToken(),
                coreResourceRegistryRequest.getPlatformId())).thenReturn(new AuthorizationResult("", true));
        when(mockedAuthorizationManager.checkIfResourcesBelongToPlatform(any(), anyString())).thenReturn(new AuthorizationResult("ok", true));
        when(mockedRepository.saveResource(any())).thenReturn(new ResourcePersistenceResult(200, "ok", RegistryUtils.convertResourceToCoreResource(resource1)));

        this.channel.queueDeclare(TEMP_QUEUE, true, false, false, null);
        this.channel.queueBind(TEMP_QUEUE, RESOURCE_EXCHANGE_NAME, RESOURCE_TRANSLATION_REQUESTED_RK);

        this.channel.basicConsume(TEMP_QUEUE, new DefaultConsumer(this.channel) {
            @Override
            public void handleDelivery(String consumerTag, Envelope envelope, AMQP.BasicProperties properties, byte[] body) throws IOException {
                log.debug("\n|||||||| //MOCKED ............ \nSemantic Manager received request!");

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

                this.getChannel().basicPublish("", properties.getReplyTo(), replyProps, responseBytes);
                this.getChannel().basicAck(envelope.getDeliveryTag(), false);
                log.debug("-> Semantic Manager replied: \n" + validationResult.toString() + "\n......... //MOCKED |||||||||||||| ");
            }
        });

        rabbitManager.sendCustomMessage(RESOURCE_EXCHANGE_NAME, RESOURCE_CREATION_REQUESTED_RK, message, Resource.class.getCanonicalName());

        // Timeout to make sure that the message has been delivered
        verify(mockedRepository, timeout(500).times(2)).saveResource(any());
    }

    @Test
    public void resourceModificationRequestConsumerTest() throws InterruptedException, IOException {
        // FIXME: 17.07.2017

        rabbitManager.startConsumerOfResourceModificationMessages();
        setRabbitManagerMockedManagers();

        Resource resource1 = generateResource();
        Resource resource2 = generateResource();
        CoreResourceRegistryRequest coreResourceRegistryRequest = generateCoreResourceRegistryRequest(resource1, resource2);
        String message = mapper.writeValueAsString(coreResourceRegistryRequest);

        when(mockedAuthorizationManager.checkOperationAccess(coreResourceRegistryRequest.getToken(),
                coreResourceRegistryRequest.getPlatformId())).thenReturn(new AuthorizationResult("", true));
        when(mockedAuthorizationManager.checkIfResourcesBelongToPlatform(any(), anyString())).thenReturn(new AuthorizationResult("ok", true));

        this.channel.queueDeclare(TEMP_QUEUE, true, false, false, null);
        this.channel.queueBind(TEMP_QUEUE, RESOURCE_EXCHANGE_NAME, RESOURCE_MODIFICATION_REQUESTED_RK);

        this.channel.basicConsume(TEMP_QUEUE, new DefaultConsumer(this.channel) {
            @Override
            public void handleDelivery(String consumerTag, Envelope envelope, AMQP.BasicProperties properties, byte[] body) throws IOException {
                String messageReceived = new String(body);
                assertEquals(message, messageReceived);

                assertNotNull(properties);

                String correlationId = properties.getCorrelationId();
                String replyQueueName = properties.getReplyTo();

                assertNotNull(correlationId);
                assertNotNull(replyQueueName);
                System.out.println("received reply!");
            }
        });

        rabbitManager.sendCustomMessage(RESOURCE_EXCHANGE_NAME, RESOURCE_MODIFICATION_REQUESTED_RK, message, Resource.class.getCanonicalName());
    }

    @Test
    public void resourceRemovalRequestConsumerTest() throws IOException, InterruptedException {
        //// TODO: 20.07.2017 Add consumer for RPC response and verify it in tests!
        rabbitManager.startConsumerOfResourceRemovalMessages();
        setRabbitManagerMockedManagers();

        Resource resource1 = generateResource();
        addIdToResource(resource1);
        Resource resource2 = generateResource();
        addIdToResource(resource2);
        CoreResourceRegistryRequest coreResourceRegistryRequest = generateCoreResourceRegistryRequest(resource1, resource2);

        String message = mapper.writeValueAsString(coreResourceRegistryRequest);

        ResourcePersistenceResult resourcePersistenceResult1 = new ResourcePersistenceResult();
        resourcePersistenceResult1.setStatus(200);
        resourcePersistenceResult1.setMessage("ok");
        resourcePersistenceResult1.setResource(RegistryUtils.convertResourceToCoreResource(resource1));

        when(mockedRepository.removeResource(any())).thenReturn(resourcePersistenceResult1);
        when(mockedAuthorizationManager.checkOperationAccess(coreResourceRegistryRequest.getToken(),
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
        //// TODO: 20.07.2017 Add consumer for RPC response and verify it in tests!
        rabbitManager.startConsumerOfPlatformCreationMessages();
        setRabbitManagerMockedManagers();

        eu.h2020.symbiote.core.model.Platform requestPlatform = generateSymbiotePlatformA();
        String message = mapper.writeValueAsString(requestPlatform);

        PlatformPersistenceResult platformPersistenceResult = new PlatformPersistenceResult(200, "ok", requestPlatform);

        when(mockedRepository.savePlatform(any())).thenReturn(platformPersistenceResult);

        rabbitManager.sendCustomMessage(PLATFORM_EXCHANGE_NAME, PLATFORM_CREATION_REQUESTED_RK, message, Platform.class.getCanonicalName());

        // Sleep to make sure that the message has been delivered
        TimeUnit.MILLISECONDS.sleep(300);

        ArgumentCaptor<Platform> argument = ArgumentCaptor.forClass(Platform.class);
        verify(mockedRepository).savePlatform(argument.capture());

        Assert.assertTrue(argument.getValue().getId().equals(requestPlatform.getId()));
        Assert.assertTrue(argument.getValue().getComments().get(0).equals(requestPlatform.getComments().get(0)));
        Assert.assertTrue(argument.getValue().getLabels().get(0).equals(requestPlatform.getLabels().get(0)));
        Assert.assertTrue(argument.getValue().getInterworkingServices().get(0).getInformationModelId().equals(requestPlatform.getInterworkingServices().get(0)));
    }

    @Test
    public void platformCreationRequestConsumerNullNameFailTest() throws Exception {
        //// TODO: 20.07.2017 Add consumer for RPC response and verify it in tests!
        rabbitManager.startConsumerOfPlatformCreationMessages();
        setRabbitManagerMockedManagers();

        Platform requestPlatform = generateSymbiotePlatformA();
        requestPlatform.setLabels(Arrays.asList(null));
        String message = mapper.writeValueAsString(requestPlatform);

        rabbitManager.sendCustomMessage(PLATFORM_EXCHANGE_NAME, PLATFORM_CREATION_REQUESTED_RK, message, Platform.class.getCanonicalName());

        // Sleep to make sure that the message has been delivered
        TimeUnit.MILLISECONDS.sleep(300);

        verifyZeroInteractions(mockedRepository);
    }

    @Test
    public void platformCreationRequestConsumerJsonFailTest() throws Exception {
        //// TODO: 20.07.2017 Add consumer for RPC response and verify it in tests!
        rabbitManager.startConsumerOfPlatformCreationMessages();
        setRabbitManagerMockedManagers();

        String message = "[wrong json]";

        rabbitManager.sendCustomMessage(PLATFORM_EXCHANGE_NAME, PLATFORM_CREATION_REQUESTED_RK, message, Platform.class.getCanonicalName());

        // Sleep to make sure that the message has been delivered
        TimeUnit.MILLISECONDS.sleep(300);

        verifyZeroInteractions(mockedRepository);
    }

    @Test
    public void platformModificationRequestConsumerHappyPathTest() throws IOException, InterruptedException {
        //// TODO: 20.07.2017 Add consumer for RPC response and verify it in tests!
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
*/
    @Test
    public void platformModificationRequestConsumerMongoFailTest() throws IOException, InterruptedException, TimeoutException {
        rabbitManager.startConsumerOfPlatformModificationMessages();
        setRabbitManagerMockedManagers();

        Platform requestPlatform = generateSymbiotePlatformA();
        String message = mapper.writeValueAsString(requestPlatform);

        PlatformPersistenceResult platformPersistenceResult = new PlatformPersistenceResult(400, "mongo fail mock", requestPlatform);

        when(mockedRepository.modifyPlatform(any())).thenReturn(platformPersistenceResult);

        rabbitManager.sendCustomRpcMessage(PLATFORM_EXCHANGE_NAME, PLATFORM_MODIFICATION_REQUESTED_RK, message,
                new DefaultConsumer(this.channel) {
                    @Override
                    public void handleDelivery(String consumerTag, Envelope envelope, AMQP.BasicProperties properties, byte[] body) throws IOException {
                        String messageReceived = new String(body);
                        PlatformRegistryResponse platformResponseReceived = mapper.readValue(messageReceived, PlatformRegistryResponse.class);

                        assertNotNull(properties);
                        String correlationId = properties.getCorrelationId();
                        assertNotNull(correlationId);

                        assertEquals(400, platformResponseReceived.getStatus());
                        assertEquals(requestPlatform, platformResponseReceived.getPlatform());

                        log.info("Received reply message!");
                    }
                });

        // Timeout to make sure that the message has been delivered
        verify(mockedRepository, timeout(500)).modifyPlatform(any());
    }

    @Test
    public void platformModificationRequestConsumerJsonFailTest() throws Exception {
        //// TODO: 20.07.2017 Add consumer for RPC response and verify it in tests!
        rabbitManager.startConsumerOfPlatformModificationMessages();
        setRabbitManagerMockedManagers();

        String message = "[wrong json]";

        rabbitManager.sendCustomMessage(PLATFORM_EXCHANGE_NAME, PLATFORM_MODIFICATION_REQUESTED_RK, message, Platform.class.getCanonicalName());

        // Sleep to make sure that the message has been delivered
        TimeUnit.MILLISECONDS.sleep(1000);

        verifyZeroInteractions(mockedRepository);
    }

    @Test
    public void platformRemovalRequestConsumerTest() throws IOException, InterruptedException {
        //// TODO: 20.07.2017 Add consumer for RPC response and verify it in tests!
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
    public void platformResourcesRequestedConsumerTest() throws Exception {
        rabbitManager.startConsumerOfPlatformResourcesRequestsMessages();
        setRabbitManagerMockedManagers();

        Resource resource1 = generateResource();
        addIdToResource(resource1);
        Resource resource2 = generateResource();
        addIdToResource(resource2);
        CoreResourceRegistryRequest coreResourceRegistryRequest = generateCoreResourceRegistryRequest(resource1, resource2);

        String message = mapper.writeValueAsString(coreResourceRegistryRequest);

        when(mockedAuthorizationManager.checkResourceOperationAccess(coreResourceRegistryRequest.getSecurityRequest(),
                coreResourceRegistryRequest.getPlatformId())).thenReturn(new AuthorizationResult("", true));
        when(mockedAuthorizationManager.checkIfResourcesBelongToPlatform(any(), anyString())).thenReturn(new AuthorizationResult("ok", true));

        List<CoreResource> coreResourcesFound = Arrays.asList(RegistryUtils.convertResourceToCoreResource(resource1),
                RegistryUtils.convertResourceToCoreResource(resource2));
        when(mockedRepository.getResourcesForPlatform(coreResourceRegistryRequest.getPlatformId())).
                thenReturn(coreResourcesFound);

        rabbitManager.sendCustomRpcMessage(PLATFORM_EXCHANGE_NAME, RESOURCES_FOR_PLATFORM_REQUESTED_RK, message,
                new DefaultConsumer(this.channel) {
                    @Override
                    public void handleDelivery(String consumerTag, Envelope envelope, AMQP.BasicProperties properties, byte[] body) throws IOException {
                        String messageReceived = new String(body);
                        List<Resource> resourcesReceived = mapper.readValue(messageReceived, new TypeReference<List<Resource>>() {
                        });
                        assertNotNull(properties);
                        String correlationId = properties.getCorrelationId();
                        assertNotNull(correlationId);

                        assertEquals(resource1.getId(), resourcesReceived.get(0).getId());

                        log.info("Received reply message!");
                    }
                });

        // Sleep to make sure that the message has been delivered
        TimeUnit.MILLISECONDS.sleep(500);
    }

    @Test
    public void platformResourcesRequestedConsumerJsonFailTest() throws Exception {
        rabbitManager.startConsumerOfPlatformResourcesRequestsMessages();
        setRabbitManagerMockedManagers();

        String message = "[]"; //// FIXME: 18.07.2017 core Resource Registry Request with error

        rabbitManager.sendCustomRpcMessage(PLATFORM_EXCHANGE_NAME, RESOURCES_FOR_PLATFORM_REQUESTED_RK, message,
                new DefaultConsumer(this.channel) {
                    @Override
                    public void handleDelivery(String consumerTag, Envelope envelope, AMQP.BasicProperties properties, byte[] body) throws IOException {
                        String messageReceived = new String(body);
                        ResourceRegistryResponse responseReceived;
                        Map<String, Resource> resourcesReceived = new HashMap<>();
                        try {
                            responseReceived = mapper.readValue(messageReceived, ResourceRegistryResponse.class);
                            resourcesReceived = responseReceived.getResources();
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

    @Test
    public void platformResourcesRequestedConsumerNullTokenFailTest() throws Exception {
        rabbitManager.startConsumerOfPlatformResourcesRequestsMessages();
        setRabbitManagerMockedManagers();
        CoreResourceRegistryRequest coreResourceRegistryRequest = generateCoreResourceRegistryRequest();
        coreResourceRegistryRequest.setSecurityRequest(null);
        String message = mapper.writeValueAsString(coreResourceRegistryRequest);

        when(mockedAuthorizationManager.checkResourceOperationAccess(any(),any())).thenReturn(new AuthorizationResult("null token", false));

        rabbitManager.sendCustomRpcMessage(PLATFORM_EXCHANGE_NAME, RESOURCES_FOR_PLATFORM_REQUESTED_RK, message,
                new DefaultConsumer(this.channel) {
                    @Override
                    public void handleDelivery(String consumerTag, Envelope envelope, AMQP.BasicProperties properties, byte[] body) throws IOException {
                        String messageReceived = new String(body);
                        ResourceRegistryResponse responseReceived;
                        Map<String, Resource> resourcesReceived = new HashMap<>();
                        try {
                            responseReceived = mapper.readValue(messageReceived, ResourceRegistryResponse.class);
                            resourcesReceived = responseReceived.getResources();
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
}
