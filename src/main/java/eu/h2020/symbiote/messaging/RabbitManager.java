package eu.h2020.symbiote.messaging;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.rabbitmq.client.*;
import eu.h2020.symbiote.core.model.internal.CoreResource;
import eu.h2020.symbiote.model.InformationModel;
import eu.h2020.symbiote.model.Platform;
import eu.h2020.symbiote.model.Resource;
import eu.h2020.symbiote.repository.RepositoryManager;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.annotation.PreDestroy;
import java.io.IOException;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.TimeoutException;

/**
 * Bean used to manage internal communication using RabbitMQ.
 * It is responsible for declaring exchanges and using routing keys from centralized config server.
 * <p>
 * Created by mateuszl
 */
@Component
public class RabbitManager {

    //// TODO: 27.03.2017 prepare and start Information Model queues and Consumers

    private static Log log = LogFactory.getLog(RabbitManager.class);

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
    RepositoryManager repositoryManager;

    @Value("${rabbit.routingKey.resource.instance.validationRequested}")
    private String jsonResourceValidationRequestedRoutingKey;
    @Value("${rabbit.routingKey.resource.instance.translationRequested}")
    private String rdfResourceValidationRequestedRoutingKey;


    @Autowired
    public RabbitManager(RepositoryManager repositoryManager) {
        this.repositoryManager = repositoryManager;
    }

    /**
     * Initiates connection with Rabbit server using parameters from ConfigProperties
     *
     * @throws IOException
     * @throws TimeoutException
     */
    public Connection getConnection() throws IOException, TimeoutException {
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
        } catch (IOException e) {
            e.printStackTrace();
        } catch (TimeoutException e) {
            e.printStackTrace();
        }

        if (connection != null) {
            try {
                channel = this.connection.createChannel();

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
                e.printStackTrace();
            } finally {
                closeChannel(channel);
            }
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
                channel.queueUnbind("platformCreationRequestedQueue", this.platformExchangeName,
                        this.platformCreationRequestedRoutingKey);
                channel.queueUnbind("platformModificationRequestedQueue", this.platformExchangeName,
                        this.platformCreationRequestedRoutingKey);
                channel.queueUnbind("platformRemovalRequestedQueue", this.platformExchangeName,
                        this.platformCreationRequestedRoutingKey);
                channel.queueUnbind("resourceCreationRequestedQueue", this.resourceExchangeName,
                        this.resourceCreationRequestedRoutingKey);
                channel.queueUnbind("resourceModificationRequestedQueue", this.resourceExchangeName,
                        this.resourceCreationRequestedRoutingKey);
                channel.queueUnbind("resourceRemovalRequestedQueue", this.resourceExchangeName,
                        this.resourceCreationRequestedRoutingKey);
                channel.queueDelete("platformCreationRequestedQueue");
                channel.queueDelete("platformModificationRequestedQueue");
                channel.queueDelete("platformRemovalRequestedQueue");
                channel.queueDelete("resourceCreationRequestedQueue");
                channel.queueDelete("resourceModificationRequestedQueue");
                channel.queueDelete("resourceRemovalRequestedQueue");
                closeChannel(channel);
                this.connection.close();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * Method gathers all of the rabbit consumer starter methods
     */
    public void startConsumers() {
        try {
            startConsumerOfPlatformCreationMessages();
            startConsumerOfResourceCreationMessages();
            startConsumerOfPlatformRemovalMessages();
            startConsumerOfResourceRemovalMessages();
            startConsumerOfPlatformModificationMessages();
            startConsumerOfResourceModificationMessages();
        } catch (InterruptedException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void sendPlatformCreatedMessage(Platform platform) {
        try {
            ObjectMapper mapper = new ObjectMapper();
            String message = mapper.writeValueAsString(platform);
            sendMessage(this.platformExchangeName, this.platformCreatedRoutingKey, message);
            log.info("- platform created message sent");
        } catch( JsonProcessingException e ) {
            log.error("Error occurred when parsing Resource object JSON: " + platform, e);
        }

    }

    public void sendPlatformRemovedMessage(Platform platform) {
        try {
            ObjectMapper mapper = new ObjectMapper();
            String message = mapper.writeValueAsString(platform);
            sendMessage(this.platformExchangeName, this.platformRemovedRoutingKey, message);
            log.info("- platform removed message sent");
        } catch( JsonProcessingException e ) {
            log.error("Error occurred when parsing Resource object JSON: " + platform, e);
        }
    }

    public void sendPlatformModifiedMessage(Platform platform) {
        try {
            ObjectMapper mapper = new ObjectMapper();
            String message = mapper.writeValueAsString(platform);
            sendMessage(this.platformExchangeName, this.platformModifiedRoutingKey, message);
            log.info("- platform modified message sent");
        } catch( JsonProcessingException e ) {
            log.error("Error occurred when parsing Resource object JSON: " + platform, e);
        }
    }

    public void sendResourcesCreatedMessage(List<CoreResource> resources) {
        try {
            ObjectMapper mapper = new ObjectMapper();
            String message = mapper.writeValueAsString(resources);
            sendMessage(this.resourceExchangeName, this.resourceCreatedRoutingKey, message);
            log.info("- resource created message sent");
        } catch( JsonProcessingException e ) {
            log.error("Error occurred when parsing Resource object JSON: " + resources, e);
        }
    }

    public void sendResourceRemovedMessage(Resource resource) {
        try {
            ObjectMapper mapper = new ObjectMapper();
            String message = mapper.writeValueAsString(resource);
            sendMessage(this.resourceExchangeName, this.resourceRemovedRoutingKey, message);
            log.info("- resource removed message sent");
        } catch( JsonProcessingException e ) {
            log.error("Error occurred when parsing Resource object JSON: " + resource, e);
        }
    }

    public void sendResourceModifiedMessage(Resource resource) {
        try {
            ObjectMapper mapper = new ObjectMapper();
            String message = mapper.writeValueAsString(resource);
            sendMessage(this.resourceExchangeName, this.resourceModifiedRoutingKey, message);
            log.info("- resource modified message sent");
        } catch (JsonProcessingException e) {
            log.error("Error occurred when parsing Resource object JSON: " + resource, e);
        }
    }

    public void sendRdfResourceValidationRpcMessage(DefaultConsumer rpcConsumer, AMQP.BasicProperties rpcProperties,
                                                    Envelope rpcEnvelope, String message) {
        sendRpcMessageToSemanticManager(rpcConsumer, rpcProperties, rpcEnvelope,
                this.resourceExchangeName, this.rdfResourceValidationRequestedRoutingKey,
                message); //todo check
        log.info("- rdf resource to validation message sent");
    }

    public void sendJsonResourceValidationRpcMessage(DefaultConsumer rpcConsumer, AMQP.BasicProperties rpcProperties,
                                                     Envelope rpcEnvelope, String message) {
        sendRpcMessageToSemanticManager(rpcConsumer, rpcProperties, rpcEnvelope,
                this.resourceExchangeName, this.jsonResourceValidationRequestedRoutingKey,
                message); //todo check
        log.info("- json resource to validation message sent");
    }

    public void sendCustomMessage(String exchange, String routingKey, String objectInJson) {
        sendMessage(exchange, routingKey, objectInJson);
        log.info("- Custom message sent");
    }

    /**
     * Method creates queue and binds it globally available exchange and adequate Routing Key.
     * It also creates a consumer for messages incoming to this queue, regarding to Platform creation requests.
     *
     * @throws InterruptedException
     * @throws IOException
     */
    private void startConsumerOfPlatformCreationMessages() throws InterruptedException, IOException {
        String queueName = "platformCreationRequestedQueue";
        Channel channel;
        try {
            channel = this.connection.createChannel();
            channel.queueDeclare(queueName, true, false, false, null);
            channel.queueBind(queueName, this.platformExchangeName, this.platformCreationRequestedRoutingKey);
//            channel.basicQos(1); // to spread the load over multiple servers we set the prefetchCount setting

            log.info("Receiver waiting for Platform Creation messages....");

            Consumer consumer = new PlatformCreationRequestConsumer(channel, repositoryManager, this);
            channel.basicConsume(queueName, false, consumer);
        } catch (IOException e) {
            e.printStackTrace();
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
        String queueName = "platformRemovalRequestedQueue";
        Channel channel;
        try {
            channel = this.connection.createChannel();
            channel.queueDeclare(queueName, true, false, false, null);
            channel.queueBind(queueName, this.platformExchangeName, this.platformRemovalRequestedRoutingKey);
//            channel.basicQos(1); // to spread the load over multiple servers we set the prefetchCount setting

            log.info("Receiver waiting for Platform Removal messages....");

            Consumer consumer = new PlatformRemovalRequestConsumer(channel, repositoryManager, this);
            channel.basicConsume(queueName, false, consumer);
        } catch (IOException e) {
            e.printStackTrace();
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
        String queueName = "platformModificationRequestedQueue";
        Channel channel;
        try {
            channel = this.connection.createChannel();
            channel.queueDeclare(queueName, true, false, false, null);
            channel.queueBind(queueName, this.platformExchangeName, this.platformModificationRequestedRoutingKey);
//            channel.basicQos(1); // to spread the load over multiple servers we set the prefetchCount setting

            log.info("Receiver waiting for Platform Modification messages....");

            Consumer consumer = new PlatformModificationRequestConsumer(channel, repositoryManager, this);
            channel.basicConsume(queueName, false, consumer);
        } catch (IOException e) {
            e.printStackTrace();
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
        String queueName = "resourceCreationRequestedQueue";
        Channel channel;
        try {
            channel = this.connection.createChannel();
            channel.queueDeclare(queueName, true, false, false, null);
            channel.queueBind(queueName, this.resourceExchangeName, this.resourceCreationRequestedRoutingKey);
//            channel.basicQos(1); // to spread the load over multiple servers we set the prefetchCount setting

            log.info("Receiver waiting for Resource Creation messages....");

            Consumer consumer = new ResourceCreationRequestConsumer(channel, repositoryManager, this);
            channel.basicConsume(queueName, false, consumer);
        } catch (IOException e) {
            e.printStackTrace();
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
        String queueName = "resourceRemovalRequestedQueue";
        Channel channel;
        try {
            channel = this.connection.createChannel();
            channel.queueDeclare(queueName, true, false, false, null);
            channel.queueBind(queueName, this.resourceExchangeName, this.resourceRemovalRequestedRoutingKey);
//            channel.basicQos(1); // to spread the load over multiple servers we set the prefetchCount setting

            log.info("Receiver waiting for Resource Removal messages....");

            Consumer consumer = new ResourceRemovalRequestConsumer(channel, repositoryManager, this);
            channel.basicConsume(queueName, false, consumer);
        } catch (IOException e) {
            e.printStackTrace();
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
        String queueName = "resourceModificationRequestedQueue";
        Channel channel;
        try {
            channel = this.connection.createChannel();
            channel.queueDeclare(queueName, true, false, false, null);
            channel.queueBind(queueName, this.resourceExchangeName, this.resourceModificationRequestedRoutingKey);
//            channel.basicQos(1); // to spread the load over multiple servers we set the prefetchCount setting

            log.info("Receiver waiting for Resource Modification messages....");

            Consumer consumer = new ResourceModificationRequestConsumer(channel, repositoryManager, this);
            channel.basicConsume(queueName, false, consumer);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /*
    private void startConsumerOfResourceValidationMessages() throws InterruptedException, IOException {
        String queueName = "resourceValidationRequestedQueue";
        Channel channel;
        try {
            channel = this.connection.createChannel();
            channel.queueDeclare(queueName, true, false, false, null);
            channel.queueBind(queueName, this.platformExchangeName, this.rdfResourceValidationRequestedRoutingKey);
//            channel.basicQos(1); // to spread the load over multiple servers we set the prefetchCount setting

            log.info("Receiver waiting for Semantic Manager messages....");

            Consumer consumer = new ResourceJsonValidationResponseConsumer(channel, repositoryManager, this);
            channel.basicConsume(queueName, false, consumer);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
*/

    /**
     * Method publishes given message to the given exchange and routing key.
     * Props are set for correct message handle on the receiver side.
     *
     * @param exchange   name of the proper Rabbit exchange, adequate to topic of the communication
     * @param routingKey name of the proper Rabbit routing key, adequate to topic of the communication
     * @param message    message content in JSON String format
     */
    private void sendMessage(String exchange, String routingKey, String message) {
        Channel channel = null;
        try {
            channel = this.connection.createChannel();
            AMQP.BasicProperties props = new AMQP.BasicProperties()
                    .builder()
                    .contentType("application/json")
                    .build();

            channel.basicPublish(exchange, routingKey, props, message.getBytes());
        } catch (IOException e) {
            e.printStackTrace();
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
        } catch (IOException e) {
            e.printStackTrace();
        } catch (TimeoutException e) {
            e.printStackTrace();
        }
    }

    /**Sends reply message with given body to rabbit queue, for specified RPC sender.
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
            log.info("Message sent back"); //fixme show content of message also?
        } else {
            log.warn("Received RPC message without ReplyTo or CorrelationId props.");
        }
        consumer.getChannel().basicAck(envelope.getDeliveryTag(), false);
    }

    public void sendInformationModelCreatedMessage(InformationModel informationModel) {
        //// TODO: 27.03.2017
    }


    public void sendRpcMessageToSemanticManager(DefaultConsumer rpcConsumer, AMQP.BasicProperties rpcProperties,
                                                Envelope rpcEnvelope, String exchangeName, String routingKey,
                                                String message) {
        try {
            log.info("Sending RPC message to Semantic Manager...");

            Channel channel = this.connection.createChannel();

            String replyQueueName = channel.queueDeclare().getQueue();

            String correlationId = UUID.randomUUID().toString();
            AMQP.BasicProperties props = new AMQP.BasicProperties()
                    .builder()
                    .correlationId(correlationId)
                    .replyTo(replyQueueName)
                    .build();

            ResourceJsonValidationResponseConsumer consumer =
                    new ResourceJsonValidationResponseConsumer(message, rpcConsumer, rpcProperties, rpcEnvelope,
                            channel, repositoryManager, this);
            channel.basicConsume(replyQueueName, true, consumer);

            channel.basicPublish(exchangeName, routingKey, true, props, message.getBytes());

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

}