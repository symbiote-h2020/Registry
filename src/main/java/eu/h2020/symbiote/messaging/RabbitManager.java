package eu.h2020.symbiote.messaging;

import com.google.gson.Gson;
import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.ConnectionFactory;
import com.rabbitmq.client.Consumer;
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
import java.util.concurrent.TimeoutException;

/**
 * Bean used to manage internal communication using RabbitMQ.
 * It is responsible for declaring exchanges and using routing keys from centralized config server.
 */
@Component
public class RabbitManager {

    private static Log log = LogFactory.getLog(RabbitManager.class);
    @Autowired
    RepositoryManager repositoryManager;
    Channel channel;
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
    private Connection connection;

    public void init() {
        try {
            ConnectionFactory factory = new ConnectionFactory();

//            factory.setHost("localhost"); //todo value from properties
            factory.setHost(this.rabbitHost);
            factory.setUsername(this.rabbitUsername);
            factory.setPassword(this.rabbitPassword);

            this.connection = factory.newConnection();

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
        } catch (TimeoutException e) {
            e.printStackTrace();
        }
    }

    /**
     * Cleanup method
     */
    @PreDestroy
    public void cleanup() {
        //FIXME check if there is better exception handling in @predestroy method
        System.out.println("RABBIT CLEANED");
        try {
            if (this.connection != null && this.connection.isOpen())
                channel.queueUnbind("platformCreationRequestedQueue", this.platformExchangeName,
                        this.platformCreationRequestedRoutingKey);
            channel.queueUnbind("resourceCreationRequestedQueue", this.resourceExchangeName,
                    this.resourceCreationRequestedRoutingKey);
            channel.queueDelete("platformCreationRequestedQueue");
            channel.queueDelete("resourceCreationRequestedQueue");
            closeChannel(channel);
            this.connection.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void startConsumers(){
        try {
            startConsumerOfPlatformCreationMessages();
            startConsumerOfResourceCreationMessages();
            startConsumerOfPlatformRemovalMessages();
            startConsumerOfResourceRemovalMessages();
        } catch (InterruptedException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void sendPlatformCreatedMessage(Platform platform) {
        Gson gson = new Gson();
        String message = gson.toJson(platform);
        sendMessage(this.platformExchangeName, this.platformCreatedRoutingKey, message);
        System.out.println("- platform created message sent");
    }

    public void sendPlatformRemovedMessage(Platform platform) {
        Gson gson = new Gson();
        String message = gson.toJson(platform);
        sendMessage(this.platformExchangeName, this.platformRemovedRoutingKey, message);
        System.out.println("- platform removed message sent");
    }

    public void sendResourceCreatedMessage(Resource resource) {
        Gson gson = new Gson();
        String message = gson.toJson(resource);
        sendMessage(this.resourceExchangeName, this.resourceCreatedRoutingKey, message);
        System.out.println("- resource created message sent");
    }

    public void sendResourceRemovedMessage(Resource resource) {
        Gson gson = new Gson();
        String message = gson.toJson(resource);
        sendMessage(this.resourceExchangeName, this.resourceCreatedRoutingKey, message);
        System.out.println("- resource removed message sent");
    }

    private void startConsumerOfPlatformCreationMessages() throws InterruptedException, IOException {
        String queueName = "platformCreationRequestedQueue";
        try {
            channel = this.connection.createChannel();
            channel.queueDeclare(queueName, true, false, false, null);
            channel.queueBind(queueName, this.platformExchangeName, this.platformCreationRequestedRoutingKey);

//            channel.basicQos(1); // to spread the load over multiple servers we set the prefetchCount setting

            log.info("Receiver waiting for Platform Creation messages....");

            Consumer consumer = new PlatformCreationRequestConsumer(channel, repositoryManager);
            channel.basicConsume(queueName, false, consumer);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void startConsumerOfPlatformRemovalMessages() throws InterruptedException, IOException {
        String queueName = "platformRemovalRequestedQueue";
        try {
            channel = this.connection.createChannel();
            channel.queueDeclare(queueName, true, false, false, null);
            channel.queueBind(queueName, this.platformExchangeName, this.platformRemovalRequestedRoutingKey);

//            channel.basicQos(1); // to spread the load over multiple servers we set the prefetchCount setting

            log.info("Receiver waiting for Platform Removal messages....");

            Consumer consumer = new PlatformRemovalRequestConsumer(channel, repositoryManager);
            channel.basicConsume(queueName, false, consumer);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void startConsumerOfResourceCreationMessages() throws InterruptedException, IOException {
        String queueName = "resourceCreationRequestedQueue";
        try {
            channel = this.connection.createChannel();
            channel.queueDeclare(queueName, true, false, false, null);
            channel.queueBind(queueName, this.resourceExchangeName, this.resourceCreationRequestedRoutingKey);

//            channel.basicQos(1); // to spread the load over multiple servers we set the prefetchCount setting

            log.info("Receiver waiting for Resource Creation messages....");

            Consumer consumer = new ResourceCreationRequestConsumer(channel, repositoryManager);
            channel.basicConsume(queueName, false, consumer);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }


    private void startConsumerOfResourceRemovalMessages() throws InterruptedException, IOException {
        String queueName = "resourceRemovalRequestedQueue";
        try {
            channel = this.connection.createChannel();
            channel.queueDeclare(queueName, true, false, false, null);
            channel.queueBind(queueName, this.resourceExchangeName, this.resourceRemovalRequestedRoutingKey);

//            channel.basicQos(1); // to spread the load over multiple servers we set the prefetchCount setting

            log.info("Receiver waiting for Resource Removal messages....");

            Consumer consumer = new ResourceCreationRequestConsumer(channel, repositoryManager);
            channel.basicConsume(queueName, false, consumer);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void sendMessage(String exchange, String routingKey, String message) {
        try {
            channel = this.connection.createChannel();
            channel.basicPublish(exchange, routingKey, null, message.getBytes());
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

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
}