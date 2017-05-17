package eu.h2020.symbiote.messaging;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.rabbitmq.client.*;
import eu.h2020.symbiote.core.internal.CoreResourceRegisteredOrModifiedEventPayload;
import eu.h2020.symbiote.core.internal.DescriptionType;
import eu.h2020.symbiote.core.model.Platform;
import eu.h2020.symbiote.model.RegistryOperationType;
import eu.h2020.symbiote.repository.RepositoryManager;
import eu.h2020.symbiote.utils.AuthorizationManager;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.annotation.PreDestroy;
import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.TimeoutException;

import static eu.h2020.symbiote.core.internal.DescriptionType.BASIC;
import static eu.h2020.symbiote.core.internal.DescriptionType.RDF;

/**
 * Bean used to manage internal communication using RabbitMQ.
 * It is responsible for declaring exchanges and using routing keys from centralized config server.
 * <p>
 * Created by mateuszl
 */
@Component
public class RabbitManager {

    //// TODO: 09.05.2017 REFACTOR

    //// TODO for release 3.: 27.03.2017 prepare and start Information Model queues and Consumers

    public static final String RDF_RESOURCE_VALIDATION_REQUESTED_QUEUE = "rdfResourceValidationRequestedQueue";
    public static final String JSON_RESOURCE_TRANSLATION_REQUESTED_QUEUE = "jsonResourceTranslationRequestedQueue";
    private static final String PLATFORM_REMOVAL_REQUESTED_QUEUE = "symbIoTe-Registry-platformRemovalRequestedQueue";
    private static final String RESOURCE_CREATION_REQUESTED_QUEUE = "symbIoTe-Registry-resourceCreationRequestedQueue";
    private static final String RESOURCE_MODIFICATION_REQUESTED_QUEUE = "symbIoTe-Registry-resourceModificationRequestedQueue";
    private static final String PLATFORM_CREATION_REQUESTED_QUEUE = "symbIoTe-Registry-platformCreationRequestedQueue";
    private static final String PLATFORM_MODIFICATION_REQUESTED_QUEUE = "symbIoTe-Registry-platformModificationRequestedQueue";
    private static final String RESOURCE_REMOVAL_REQUESTED_QUEUE = "symbIoTe-Registry-resourceRemovalRequestedQueue";
    private static final String ERROR_OCCURRED_WHEN_PARSING_OBJECT_TO_JSON = "Error occurred when parsing Resource object JSON: ";
    private static Log log = LogFactory.getLog(RabbitManager.class);
    private AuthorizationManager authorizationManager;
    private RepositoryManager repositoryManager;
    @Value("${rabbit.host}")
    private String rabbitHost;
    @Value("${rabbit.username}")
    private String rabbitUsername;
    @Value("${rabbit.password}")
    private String rabbitPassword;
    @Value("${rabbit.exchange.platform.name}")
    private String platformExchangeName;
    @Value("${rabbit.exchange.platform.type}")
    private String platformExchangeType;
    @Value("${rabbit.exchange.platform.durable}")
    private boolean plaftormExchangeDurable;
    @Value("${rabbit.exchange.platform.autodelete}")
    private boolean platformExchangeAutodelete;
    @Value("${rabbit.exchange.platform.internal}")
    private boolean platformExchangeInternal;
    @Value("${rabbit.routingKey.platform.creationRequested}")
    private String platformCreationRequestedRoutingKey;
    @Value("${rabbit.routingKey.platform.created}")
    private String platformCreatedRoutingKey;
    @Value("${rabbit.routingKey.platform.removalRequested}")
    private String platformRemovalRequestedRoutingKey;
    @Value("${rabbit.routingKey.platform.removed}")
    private String platformRemovedRoutingKey;
    @Value("${rabbit.routingKey.platform.modificationRequested}")
    private String platformModificationRequestedRoutingKey;
    @Value("${rabbit.routingKey.platform.modified}")
    private String platformModifiedRoutingKey;
    @Value("${rabbit.exchange.resource.name}")
    private String resourceExchangeName;
    @Value("${rabbit.exchange.resource.type}")
    private String resourceExchangeType;
    @Value("${rabbit.exchange.resource.durable}")
    private boolean resourceExchangeDurable;
    @Value("${rabbit.exchange.resource.autodelete}")
    private boolean resourceExchangeAutodelete;
    @Value("${rabbit.exchange.resource.internal}")
    private boolean resourceExchangeInternal;
    @Value("${rabbit.routingKey.resource.creationRequested}")
    private String resourceCreationRequestedRoutingKey;
    @Value("${rabbit.routingKey.resource.created}")
    private String resourceCreatedRoutingKey;
    @Value("${rabbit.routingKey.resource.removalRequested}")
    private String resourceRemovalRequestedRoutingKey;
    @Value("${rabbit.routingKey.resource.removed}")
    private String resourceRemovedRoutingKey;
    @Value("${rabbit.routingKey.resource.modificationRequested}")
    private String resourceModificationRequestedRoutingKey;
    @Value("${rabbit.routingKey.resource.modified}")
    private String resourceModifiedRoutingKey;
    private Connection connection;
    @Value("${rabbit.routingKey.resource.instance.translationRequested}")
    private String jsonResourceTranslationRequestedRoutingKey; //dla JSONów
    @Value("${rabbit.routingKey.resource.instance.validationRequested}")
    private String rdfResourceValidationRequestedRoutingKey; //dla RDFów


    private Channel rpcChannel;

    @Autowired
    public RabbitManager(RepositoryManager repositoryManager, AuthorizationManager authorizationManager) {
        this.repositoryManager = repositoryManager;
        this.authorizationManager = authorizationManager;
    }

    /**
     * Initiates connection with Rabbit server using parameters from ConfigProperties
     *
     * @throws IOException
     * @throws TimeoutException
     */
    private Connection getConnection() throws IOException, TimeoutException {
        if (connection == null) {
            ConnectionFactory factory = new ConnectionFactory();
            factory.setHost(this.rabbitHost);
            factory.setUsername(this.rabbitUsername);
            factory.setPassword(this.rabbitPassword);
            this.connection = factory.newConnection();
        }
        return this.connection;
    }

    /**
     * Method creates channel and declares Rabbit exchanges for Platform and Resources.
     * It triggers start of all consumers used in Registry communication.
     */
    public void init() {
        Channel channel = null;

        try {
            getConnection();
        } catch (IOException | TimeoutException e) {
            log.error(e);
        }

        if (connection != null) {
            try {
                channel = this.connection.createChannel();

                this.rpcChannel = connection.createChannel();

                channel.exchangeDeclare(this.platformExchangeName,
                        this.platformExchangeType,
                        this.plaftormExchangeDurable,
                        this.platformExchangeAutodelete,
                        this.platformExchangeInternal,
                        null);

                channel.exchangeDeclare(this.resourceExchangeName,
                        this.resourceExchangeType,
                        this.resourceExchangeDurable,
                        this.resourceExchangeAutodelete,
                        this.resourceExchangeInternal,
                        null);

                startConsumers();

            } catch (IOException e) {
                log.error(e);
            } finally {
                closeChannel(channel);
            }
        } else {
            log.error("Rabbit connection is null!");
        }
    }

    /**
     * Cleanup method for rabbit - set on pre destroy
     */
    @PreDestroy
    public void cleanup() {
        log.info("Rabbit cleaned!");
        try {
            Channel channel;
            if (this.connection != null && this.connection.isOpen()) {
                channel = connection.createChannel();
                channel.queueUnbind(PLATFORM_CREATION_REQUESTED_QUEUE, this.platformExchangeName,
                        this.platformCreationRequestedRoutingKey);
                channel.queueUnbind(PLATFORM_MODIFICATION_REQUESTED_QUEUE, this.platformExchangeName,
                        this.platformCreationRequestedRoutingKey);
                channel.queueUnbind(PLATFORM_REMOVAL_REQUESTED_QUEUE, this.platformExchangeName,
                        this.platformCreationRequestedRoutingKey);
                channel.queueUnbind(RESOURCE_CREATION_REQUESTED_QUEUE, this.resourceExchangeName,
                        this.resourceCreationRequestedRoutingKey);
                channel.queueUnbind(RESOURCE_MODIFICATION_REQUESTED_QUEUE, this.resourceExchangeName,
                        this.resourceCreationRequestedRoutingKey);
                channel.queueUnbind(RESOURCE_REMOVAL_REQUESTED_QUEUE, this.resourceExchangeName,
                        this.resourceCreationRequestedRoutingKey);
                channel.queueUnbind(RDF_RESOURCE_VALIDATION_REQUESTED_QUEUE, this.resourceExchangeName,
                        this.rdfResourceValidationRequestedRoutingKey);
                channel.queueUnbind(JSON_RESOURCE_TRANSLATION_REQUESTED_QUEUE, this.resourceExchangeName,
                        this.jsonResourceTranslationRequestedRoutingKey);
                channel.queueDelete(PLATFORM_CREATION_REQUESTED_QUEUE);
                channel.queueDelete(PLATFORM_MODIFICATION_REQUESTED_QUEUE);
                channel.queueDelete(PLATFORM_REMOVAL_REQUESTED_QUEUE);
                channel.queueDelete(RESOURCE_CREATION_REQUESTED_QUEUE);
                channel.queueDelete(RESOURCE_MODIFICATION_REQUESTED_QUEUE);
                channel.queueDelete(RESOURCE_REMOVAL_REQUESTED_QUEUE);
                channel.queueDelete(RDF_RESOURCE_VALIDATION_REQUESTED_QUEUE);
                channel.queueDelete(JSON_RESOURCE_TRANSLATION_REQUESTED_QUEUE);
                closeChannel(channel);
                this.connection.close();
            }
        } catch (IOException e) {
            log.error(e);
        }
    }

    /**
     * Method gathers all of the rabbit consumer starter methods
     */
    private void startConsumers() {
        try {
            startConsumerOfPlatformCreationMessages();
            startConsumerOfResourceCreationMessages();
            startConsumerOfPlatformRemovalMessages();
            startConsumerOfResourceRemovalMessages();
            startConsumerOfPlatformModificationMessages();
            startConsumerOfResourceModificationMessages();
        } catch (InterruptedException | IOException e) {
            log.error(e);
        }
    }

    /**
     * Method creates queue and binds it globally available exchange and adequate Routing Key.
     * It also creates a consumer for messages incoming to this queue, regarding to Platform creation requests.
     *
     * @throws InterruptedException
     * @throws IOException
     */
    private void startConsumerOfPlatformCreationMessages() throws InterruptedException, IOException {
        Channel channel;
        try {
            channel = this.connection.createChannel();
            channel.queueDeclare(PLATFORM_CREATION_REQUESTED_QUEUE, true, false, false, null);
            channel.queueBind(PLATFORM_CREATION_REQUESTED_QUEUE, this.platformExchangeName, this.platformCreationRequestedRoutingKey);
//            channel.basicQos(1); // to spread the load over multiple servers we set the prefetchCount setting

            log.info("Receiver waiting for Platform Creation messages....");

            Consumer consumer = new PlatformCreationRequestConsumer(channel, repositoryManager, this);
            channel.basicConsume(PLATFORM_CREATION_REQUESTED_QUEUE, false, consumer);
        } catch (IOException e) {
            log.error(e);
        }
    }

    /**
     * Method creates queue and binds it globally available exchange and adequate Routing Key.
     * It also creates a consumer for messages incoming to this queue, regarding to Platform removal requests.
     *
     * @throws InterruptedException
     * @throws IOException
     */
    private void startConsumerOfPlatformRemovalMessages() throws InterruptedException, IOException {
        Channel channel;
        try {
            channel = this.connection.createChannel();
            channel.queueDeclare(PLATFORM_REMOVAL_REQUESTED_QUEUE, true, false, false, null);
            channel.queueBind(PLATFORM_REMOVAL_REQUESTED_QUEUE, this.platformExchangeName, this.platformRemovalRequestedRoutingKey);
//            channel.basicQos(1); // to spread the load over multiple servers we set the prefetchCount setting

            log.info("Receiver waiting for Platform Removal messages....");

            Consumer consumer = new PlatformRemovalRequestConsumer(channel, repositoryManager, this);
            channel.basicConsume(PLATFORM_REMOVAL_REQUESTED_QUEUE, false, consumer);
        } catch (IOException e) {
            log.error(e);
        }
    }

    /**
     * Method creates queue and binds it globally available exchange and adequate Routing Key.
     * It also creates a consumer for messages incoming to this queue, regarding to Platform modification requests.
     *
     * @throws InterruptedException
     * @throws IOException
     */
    private void startConsumerOfPlatformModificationMessages() throws InterruptedException, IOException {
        Channel channel;
        try {
            channel = this.connection.createChannel();
            channel.queueDeclare(PLATFORM_MODIFICATION_REQUESTED_QUEUE, true, false, false, null);
            channel.queueBind(PLATFORM_MODIFICATION_REQUESTED_QUEUE, this.platformExchangeName, this.platformModificationRequestedRoutingKey);
//            channel.basicQos(1); // to spread the load over multiple servers we set the prefetchCount setting

            log.info("Receiver waiting for Platform Modification messages....");

            Consumer consumer = new PlatformModificationRequestConsumer(channel, repositoryManager, this);
            channel.basicConsume(PLATFORM_MODIFICATION_REQUESTED_QUEUE, false, consumer);
        } catch (IOException e) {
            log.error(e);
        }
    }

    /**
     * Method creates queue and binds it globally available exchange and adequate Routing Key.
     * It also creates a consumer for messages incoming to this queue, regarding to Resource creation requests.
     *
     * @throws InterruptedException
     * @throws IOException
     */
    private void startConsumerOfResourceCreationMessages() throws InterruptedException, IOException {
        Channel channel;
        try {
            channel = this.connection.createChannel();
            channel.queueDeclare(RESOURCE_CREATION_REQUESTED_QUEUE, true, false, false, null);
            channel.queueBind(RESOURCE_CREATION_REQUESTED_QUEUE, this.resourceExchangeName, this.resourceCreationRequestedRoutingKey);
//            channel.basicQos(1); // to spread the load over multiple servers we set the prefetchCount setting

            log.info("Receiver waiting for Resource Creation messages....");

            Consumer consumer = new ResourceCreationRequestConsumer(channel, this, authorizationManager);
            channel.basicConsume(RESOURCE_CREATION_REQUESTED_QUEUE, false, consumer);
        } catch (IOException e) {
            log.error(e);
        }
    }

    /**
     * Method creates queue and binds it globally available exchange and adequate Routing Key.
     * It also creates a consumer for messages incoming to this queue, regarding to Resource removal requests.
     *
     * @throws InterruptedException
     * @throws IOException
     */
    private void startConsumerOfResourceRemovalMessages() throws InterruptedException, IOException {
        Channel channel;
        try {
            channel = this.connection.createChannel();
            channel.queueDeclare(RESOURCE_REMOVAL_REQUESTED_QUEUE, true, false, false, null);
            channel.queueBind(RESOURCE_REMOVAL_REQUESTED_QUEUE, this.resourceExchangeName, this.resourceRemovalRequestedRoutingKey);
//            channel.basicQos(1); // to spread the load over multiple servers we set the prefetchCount setting

            log.info("Receiver waiting for Resource Removal messages....");

            Consumer consumer = new ResourceRemovalRequestConsumer(channel, repositoryManager, this, authorizationManager);
            channel.basicConsume(RESOURCE_REMOVAL_REQUESTED_QUEUE, false, consumer);
        } catch (IOException e) {
            log.error(e);
        }
    }

    /**
     * Method creates queue and binds it globally available exchange and adequate Routing Key.
     * It also creates a consumer for messages incoming to this queue, regarding to Resource modification requests.
     *
     * @throws InterruptedException
     * @throws IOException
     */
    private void startConsumerOfResourceModificationMessages() throws InterruptedException, IOException {
        Channel channel;
        try {
            channel = this.connection.createChannel();
            channel.queueDeclare(RESOURCE_MODIFICATION_REQUESTED_QUEUE, true, false, false, null);
            channel.queueBind(RESOURCE_MODIFICATION_REQUESTED_QUEUE, this.resourceExchangeName,
                    this.resourceModificationRequestedRoutingKey);
//            channel.basicQos(1); // to spread the load over multiple servers we set the prefetchCount setting

            log.info("Receiver waiting for Resource Modification messages....");

            Consumer consumer = new ResourceModificationRequestConsumer(channel, this, authorizationManager);
            channel.basicConsume(RESOURCE_MODIFICATION_REQUESTED_QUEUE, false, consumer);
        } catch (IOException e) {
            log.error(e);
        }
    }


    /**
     * Triggers sending message containing Platform accordingly to Operation Type.
     *
     * @param platform
     * @param operationType
     */
    public void sendPlatformOperationMessage(Platform platform, RegistryOperationType operationType) {
        try {
            ObjectMapper mapper = new ObjectMapper();
            String message = mapper.writeValueAsString(platform);

            switch (operationType) {
                case CREATION:
                    sendMessage(this.platformExchangeName, this.platformCreatedRoutingKey, message,
                            platform.getClass().getCanonicalName());
                    log.info("- platform created message sent");
                    break;
                case MODIFICATION:
                    sendMessage(this.platformExchangeName, this.platformModifiedRoutingKey, message,
                            platform.getClass().getCanonicalName());
                    log.info("- platform modified message sent");
                    break;
                case REMOVAL:
                    sendMessage(this.platformExchangeName, this.platformRemovedRoutingKey, message,
                            platform.getClass().getCanonicalName());
                    log.info("- platform removed message sent");
                    break;
            }

        } catch (JsonProcessingException e) {
            log.error(ERROR_OCCURRED_WHEN_PARSING_OBJECT_TO_JSON + platform, e);
        }
    }

    public void sendResourceOperationMessage(CoreResourceRegisteredOrModifiedEventPayload payload,
                                             RegistryOperationType operationType) {
        try {
            ObjectMapper mapper = new ObjectMapper();
            String message = mapper.writeValueAsString(payload);

            switch (operationType) {
                case CREATION:
                    sendMessage(this.resourceExchangeName, this.resourceCreatedRoutingKey, message,
                            payload.getClass().getCanonicalName());
                    break;
                case MODIFICATION:
                    sendMessage(this.resourceExchangeName, this.resourceModifiedRoutingKey, message,
                            payload.getClass().getCanonicalName());
                    break;
            }
            log.info("- resources operation (" + operationType + ") message sent (fanout). Contents:\n" + message);
        } catch (JsonProcessingException e) {
            log.error(ERROR_OCCURRED_WHEN_PARSING_OBJECT_TO_JSON + payload, e);
        }
    }

    public void sendResourcesRemovalMessage(List<String> resourcesIds){
        ObjectMapper mapper = new ObjectMapper();
        String message = "";
        try {
            message = mapper.writerFor(new TypeReference<List<String>>() {
            }).writeValueAsString(resourcesIds);
        } catch (JsonProcessingException e) {
            log.error(e);
        }
        sendMessage(this.resourceExchangeName, this.resourceRemovedRoutingKey, message,
                resourcesIds.getClass().getCanonicalName());
    }

    public void sendResourceRdfValidationRpcMessage(DefaultConsumer rpcConsumer,
                                                    AMQP.BasicProperties rpcProperties,
                                                    Envelope rpcEnvelope,
                                                    String message,
                                                    String platformId,
                                                    RegistryOperationType operationType) {
        sendRpcMessageToSemanticManager(rpcConsumer, rpcProperties, rpcEnvelope,
                this.resourceExchangeName,
                this.rdfResourceValidationRequestedRoutingKey,
                RDF,
                operationType,
                message,
                platformId);
        log.info("- rdf resource to validation message sent");
    }

    public void sendResourceJsonTranslationRpcMessage(DefaultConsumer rpcConsumer,
                                                      AMQP.BasicProperties rpcProperties,
                                                      Envelope rpcEnvelope,
                                                      String message,
                                                      String platformId,
                                                      RegistryOperationType operationType) {
        sendRpcMessageToSemanticManager(rpcConsumer, rpcProperties, rpcEnvelope,
                this.resourceExchangeName,
                this.jsonResourceTranslationRequestedRoutingKey,
                BASIC,
                operationType,
                message,
                platformId);
    }

    /**
     * Sends reply message with given body to rabbit queue, for specified RPC sender.
     *
     * @param consumer
     * @param properties
     * @param envelope
     * @param response
     * @throws IOException
     */
    public void sendRPCReplyMessage(DefaultConsumer consumer, AMQP.BasicProperties properties, Envelope envelope,
                                    String response) throws IOException {
        if (properties.getReplyTo() != null || properties.getCorrelationId() != null) {

            AMQP.BasicProperties replyProps = new AMQP.BasicProperties
                    .Builder()
                    .correlationId(properties.getCorrelationId())
                    .build();

            consumer.getChannel().basicPublish("", properties.getReplyTo(), replyProps, response.getBytes());
            log.info("- RPC reply Message sent back! Content: " + response);
        } else {
            log.error("Received RPC message without ReplyTo or CorrelationId props.");
        }
        consumer.getChannel().basicAck(envelope.getDeliveryTag(), false);
    }

    public void sendRpcMessageToSemanticManager(DefaultConsumer rpcConsumer, AMQP.BasicProperties rpcProperties,
                                                Envelope rpcEnvelope, String exchangeName, String routingKey,
                                                DescriptionType descriptionType, RegistryOperationType operationType,
                                                String message, String platformId) {
        try {
            String replyQueueName = rpcChannel.queueDeclare().getQueue();

            String correlationId = UUID.randomUUID().toString();
            AMQP.BasicProperties props = new AMQP.BasicProperties()
                    .builder()
                    .correlationId(correlationId)
                    .replyTo(replyQueueName)
                    .build();

            ResourceValidationResponseConsumer responseConsumer =
                    new ResourceValidationResponseConsumer(rpcConsumer, rpcProperties, rpcEnvelope,
                            rpcChannel, repositoryManager, this, platformId, operationType, descriptionType,
                            authorizationManager);

            rpcChannel.basicConsume(replyQueueName, true, responseConsumer);

            log.info("Sending RPC message to Semantic Manager... \nMessage params:\nExchange name: "
                    + exchangeName + "\nRouting key: " + routingKey + "\nProps: " + props + "\nMessage: "
                    + message);

            rpcChannel.basicPublish(exchangeName, routingKey, true, props, message.getBytes());

        } catch (IOException e) {
            log.error(e);
        }
    }

    public void closeConsumer (DefaultConsumer consumerToClose, Channel channel) throws IOException {

        channel.basicCancel(consumerToClose.getConsumerTag());
    }

    /**
     * Method publishes given message to the given exchange and routing key.
     * Props are set for correct message handle on the receiver side.
     *
     * @param exchange   name of the proper Rabbit exchange, adequate to topic of the communication
     * @param routingKey name of the proper Rabbit routing key, adequate to topic of the communication
     * @param message    message content in JSON String format
     * @param classType  message content in JSON String format
     */
    private void sendMessage(String exchange, String routingKey, String message, String classType) {
        Channel channel = null;
        try {
            channel = this.connection.createChannel();
            Map<String, Object> headers = new HashMap<>();
            headers.put("__TypeId__", classType);
            headers.put("__ContentTypeId__", Object.class.getCanonicalName());
            AMQP.BasicProperties props = new AMQP.BasicProperties()
                    .builder()
                    .contentType("application/json")
                    .headers(headers)
                    .build();

            log.info("Sending message. Content: " + message);
            channel.basicPublish(exchange, routingKey, props, message.getBytes());
        } catch (IOException e) {
            log.error(e);
        } finally {
            closeChannel(channel);
        }
    }

    /**
     * Closes given channel if it exists and is open.
     *
     * @param channel rabbit channel to close
     */
    private void closeChannel(Channel channel) {
        try {
            if (channel != null && channel.isOpen())
                channel.close();
        } catch (IOException | TimeoutException e) {
            log.error(e);
        }
    }
}