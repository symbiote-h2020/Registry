package eu.h2020.symbiote;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.rabbitmq.client.*;
import eu.h2020.symbiote.core.internal.ClearDataRequest;
import eu.h2020.symbiote.core.internal.CoreResource;
import eu.h2020.symbiote.core.internal.CoreResourceRegistryRequest;
import eu.h2020.symbiote.core.internal.ResourceInstanceValidationResult;
import eu.h2020.symbiote.managers.AuthorizationManager;
import eu.h2020.symbiote.managers.RabbitManager;
import eu.h2020.symbiote.managers.RepositoryManager;
import eu.h2020.symbiote.model.cim.Resource;
import eu.h2020.symbiote.security.commons.exceptions.custom.InvalidArgumentsException;
import org.junit.After;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.test.util.ReflectionTestUtils;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import static eu.h2020.symbiote.TestSetupConfig.*;
import static eu.h2020.symbiote.utils.RegistryUtils.convertResourceToCoreResource;
import static org.junit.Assert.assertNotNull;

@RunWith(SpringRunner.class)
@SpringBootTest
@TestPropertySource(
        locations = {"classpath:test.properties"},
        properties = {"key=value"})
@Ignore //tests only for bugs debugging purposes
public class MongoRepositoryTests {


    public static final String TEMP_QUEUE = "RPCqueue";
    private static Logger log = LoggerFactory.getLogger(MessagingTests.class);
    @Autowired
    RepositoryManager repositoryManager;
    @Autowired
    AuthorizationManager authorizationManager;
    @Autowired
    RabbitManager rabbitManager;

    private ObjectMapper mapper;
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
        ReflectionTestUtils.setField(rabbitManager, "resourceClearDataRequestedRoutingKey", RESOURCE_CLEAR_DATA_REQUESTED_RK);

        ReflectionTestUtils.setField(rabbitManager, "federationCreationRequestedRoutingKey", FEDERATION_CREATION_REQUESTED_RK);
        ReflectionTestUtils.setField(rabbitManager, "federationModificationRequestedRoutingKey", FEDERATION_MODIFICATION_REQUESTED_RK);
        ReflectionTestUtils.setField(rabbitManager, "federationRemovalRequestedRoutingKey", FEDERATION_REMOVAL_REQUESTED_RK);

        ReflectionTestUtils.setField(rabbitManager, "informationModelCreationRequestedRoutingKey", INFORMATION_MODEL_CREATION_REQUESTED_RK);
        ReflectionTestUtils.setField(rabbitManager, "informationModelModificationRequestedRoutingKey", INFORMATION_MODEL_MODIFICATION_REQUESTED_RK);
        ReflectionTestUtils.setField(rabbitManager, "informationModelRemovalRequestedRoutingKey", INFORMATION_MODEL_REMOVAL_REQUESTED_RK);
        ReflectionTestUtils.setField(rabbitManager, "rdfInformationModelValidationRequestedRoutingKey", INFORMATION_MODEL_VALIDATION_REQUESTED_RK);
        ReflectionTestUtils.setField(rabbitManager, "informationModelRemovedRoutingKey", "not_important_RK");
        ReflectionTestUtils.setField(rabbitManager, "informationModelsRequestedRoutingKey", GET_ALL_INFORMATION_MODELS_REQUESTED_RK);

        ReflectionTestUtils.setField(rabbitManager, "platformCreatedRoutingKey", PLATFORM_CREATED_ROUTING_KEY);
        ReflectionTestUtils.setField(rabbitManager, "platformRemovedRoutingKey", PLATFORM_REMOVED_ROUTING_KEY);
        ReflectionTestUtils.setField(rabbitManager, "platformModifiedRoutingKey", PLATFORM_MODIFIED_ROUTING_KEY);
        ReflectionTestUtils.setField(rabbitManager, "resourceCreatedRoutingKey", RESOURCE_CREATED_ROUTING_KEY);
        ReflectionTestUtils.setField(rabbitManager, "resourceRemovedRoutingKey", RESOURCE_REMOVED_ROUTING_KEY);
        ReflectionTestUtils.setField(rabbitManager, "resourceModifiedRoutingKey", RESOURCE_MODIFIED_ROUTING_KEY);

        ReflectionTestUtils.setField(rabbitManager, "federationsRequestedRoutingKey", GET_ALL_FEDERATIONS_RK);
        ReflectionTestUtils.setField(rabbitManager, "federationRequestedRoutingKey", GET_FEDERATION_FOR_PLATFORM_RK);

        ReflectionTestUtils.setField(rabbitManager, "aamExchangeName", AAM_EXCHANGE_NAME);

        ReflectionTestUtils.setField(rabbitManager, "platformResourcesRequestedRoutingKey", RESOURCES_FOR_PLATFORM_REQUESTED_RK);
        ReflectionTestUtils.setField(rabbitManager, "platformDetailsRequestedRoutingKey", PLATFORM_DETAILS_REQUESTED_RK);

        ReflectionTestUtils.setField(rabbitManager, "jsonResourceTranslationRequestedRoutingKey", RESOURCE_TRANSLATION_REQUESTED_RK);
        ReflectionTestUtils.setField(rabbitManager, "rdfResourceValidationRequestedRoutingKey", RESOURCE_VALIDATION_REQUESTED_RK);

        ReflectionTestUtils.invokeMethod(rabbitManager, "init");

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
                channel.queueDelete(RESOURCES_FOR_PLATFORM_REQUESTED_RK);
                channel.queueDelete(PLATFORM_RESOURCES_REQUESTED_QUEUE);
                channel.queueDelete(INFORMATION_MODEL_CREATION_REQUESTED_QUEUE);
                channel.queueDelete(INFORMATION_MODEL_VALIDATION_REQUESTED_RK);
                channel.queueDelete(INFORMATION_MODEL_REMOVAL_REQUESTED_QUEUE);
                channel.queueDelete(INFORMATION_MODEL_MODIFICATION_REQUESTED_QUEUE);
                channel.queueDelete(GET_ALL_INFORMATION_MODELS_REQUESTED_RK);
                channel.queueDelete(TEMP_QUEUE);
                channel.close();
                connection.close();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

    }

    public void addResourceAndPlatformToDb() throws InvalidArgumentsException {
        System.out.println("test1");

        CoreResource coreResource = TestSetupConfig.generateCoreResourceWithoutId();
        coreResource.setId("someId2");

        repositoryManager.saveResource(coreResource);
        repositoryManager.savePlatform(generatePlatformB());
    }


    @Test
    public void resourceCreationMessageTest() throws IOException, InvalidArgumentsException, InterruptedException {
        rabbitManager.startConsumerOfResourceCreationMessages(authorizationManager);

        repositoryManager.savePlatform(generatePlatformB());

        System.out.println("TEST 111111111111111111111111111111");

        Resource resource1 = generateCoreResourceSensorWithoutId();
        Resource resource2 = generateCoreResourceSensorWithoutId();
        CoreResourceRegistryRequest coreResourceRegistryRequest = generateCoreResourceRegistryRequestBasicType(resource1, resource2);
        String message = mapper.writeValueAsString(coreResourceRegistryRequest);

        mockSemanticManagerResourceTranslationCommunication(message);

        rabbitManager.sendCustomMessage(RESOURCE_EXCHANGE_NAME, RESOURCE_CREATION_REQUESTED_RK, message, CoreResourceRegistryRequest.class.getCanonicalName());

        TimeUnit.MILLISECONDS.sleep(1000);
        System.out.println("TEST END !!!!!!!!!!!!!!!!!!!");
    }

    @Test
    public void testRemovingResources() throws InvalidArgumentsException, JsonProcessingException {
        rabbitManager.startConsumerOfClearDataMessages(authorizationManager);
        System.out.println("TEST 22222222222222222222");

        addResourceAndPlatformToDb();

        ClearDataRequest cdr = new ClearDataRequest();
        cdr.setBody(PLATFORM_B_ID);

        String message = mapper.writeValueAsString(cdr);

        rabbitManager.sendCustomMessage(RESOURCE_EXCHANGE_NAME, RESOURCE_CLEAR_DATA_REQUESTED_RK, message, ClearDataRequest.class.getCanonicalName());

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
                mockSemanticManagerResourceValidationReply(envelope, properties, body, message);
            }
        });
    }

    public void mockSemanticManagerResourceValidationReply(Envelope envelope, AMQP.BasicProperties properties, byte[] body, String message) throws IOException {
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


}
