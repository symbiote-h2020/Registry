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

    public void addResourceAndPlatformToDb() throws InvalidArgumentsException {
        CoreResource coreResource = TestSetupConfig.generateCoreResourceWithoutId();
        coreResource.setId("someId2");

        repositoryManager.saveResource(coreResource);
        repositoryManager.savePlatform(generatePlatformB());
    }


    @Test
    public void resourceCreationMessageTest() throws IOException, InvalidArgumentsException, InterruptedException {
        rabbitManager.startConsumerOfResourceCreationMessages(authorizationManager);

        repositoryManager.savePlatform(generatePlatformB());

        Resource resource1 = generateCoreResourceSensorWithoutId();
        Resource resource2 = generateCoreResourceSensorWithoutId();
        CoreResourceRegistryRequest coreResourceRegistryRequest = generateCoreResourceRegistryRequestBasicType(resource1, resource2);
        String message = mapper.writeValueAsString(coreResourceRegistryRequest);

        mockSemanticManagerResourceTranslationCommunication(message);

        rabbitManager.sendCustomMessage(RESOURCE_EXCHANGE_NAME, RESOURCE_CREATION_REQUESTED_RK, message, CoreResourceRegistryRequest.class.getCanonicalName());

        TimeUnit.MILLISECONDS.sleep(1000);
    }

    @Test
    public void testClearResourcesDataRequest() throws InvalidArgumentsException, JsonProcessingException {
        rabbitManager.startConsumerOfClearDataMessages(authorizationManager);

        addResourceAndPlatformToDb();

        ClearDataRequest cdr = new ClearDataRequest();
        cdr.setBody(PLATFORM_B_ID);

        String message = mapper.writeValueAsString(cdr);

        rabbitManager.sendCustomMessage(RESOURCE_EXCHANGE_NAME, RESOURCE_CLEAR_DATA_REQUESTED_RK, message, ClearDataRequest.class.getCanonicalName());

    }


    private void mockSemanticManagerResourceTranslationCommunication(String message) throws IOException {
        HashMap<String,Object> queueArgs = new HashMap<>();
        queueArgs.put("x-message-ttl", 20000);
        this.channel.queueDeclare(TEMP_QUEUE, true, false, false, queueArgs);
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
}
