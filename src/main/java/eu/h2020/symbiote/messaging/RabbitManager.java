package eu.h2020.symbiote.messaging;

import com.rabbitmq.client.*;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
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

    private Connection connection;
    private final String queueName = "platformCreationRequestedQueue"; //todo from properties
    RequestConsumer consumer;

    /**
     * Initialization method.
     */
    @PostConstruct
    private void init() throws InterruptedException {
        //FIXME check if there is better exception handling in @postconstruct method

        try {
            ConnectionFactory factory = new ConnectionFactory();

            factory.setHost("localhost"); //todo value from properties

//            factory.setHost(this.rabbitHost);
//            factory.setUsername(this.rabbitUsername);
//            factory.setPassword(this.rabbitPassword);

            this.connection = factory.newConnection();

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
    private void cleanup() {
        //FIXME check if there is better exception handling in @predestroy method
        try {
            if (this.connection != null && this.connection.isOpen())
                this.connection.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void sendPlatformCreatedMessage( String message ) {
        sendMessage( this.platformExchangeName, this.platformCreatedRoutingKey, message);
    }

    //todo during implementation
    public void receiveMessages() throws InterruptedException, IOException {
        Channel channel = null;
        try {
            channel = this.connection.createChannel();
            channel.exchangeDeclare("symbiote.platform", "topic", true, false, null);

            channel.queueDeclare(queueName, true, false, false, null);

//            channel.basicQos(1); // to spread the load over multiple servers we set the prefetchCount setting

            System.out.println("Receiver waiting for messages....");

            consumer = new RequestConsumer(channel);
            channel.basicConsume(queueName, false, consumer);


        } catch (IOException e){
            e.printStackTrace();
        }
    }

    private void sendMessage(String exchange, String routingKey, String message) {
        Channel channel = null;
        try {
            channel = this.connection.createChannel();

            channel.exchangeDeclare(this.platformExchangeName,
                    this.platformExchangeType,
                    this.plaftormExchangeDurable,
                    this.platformExchangeAutodelete,
                    this.platformExchangeInternal,
                    null);

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