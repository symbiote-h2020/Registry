package eu.h2020.symbiote.messaging;

import com.rabbitmq.client.*;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import java.io.IOException;
import java.util.UUID;
import java.util.concurrent.TimeoutException;

/**
 * Bean used to manage internal communication using RabbitMQ.
 * It is responsible for declaring exchanges and using routing keys from centralized config server.
 */
@Component
public class RabbitManager {

    private static Log log = LogFactory.getLog( RabbitManager.class );

    /*
    @Value("${rabbit.host}")
    private String rabbitHost;

    @Value("${rabbit.username}")
    private String rabbitUsername;

    @Value("${rabbit.password}")
    private String rabbitPassword;
*/
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

    private Connection connection;

    private String queueName;

    /**
     * Initialization method.
     */
    @PostConstruct
    private void init() {
        //FIXME check if there is better exception handling in @postconstruct method
        Channel channel = null;
        try {
            ConnectionFactory factory = new ConnectionFactory();
            factory.setHost("127.0.0.1"); //todo value from properties
//            factory.setUsername(this.rabbitUsername);
//            factory.setPassword(this.rabbitPassword);
            this.connection = factory.newConnection();

            channel = this.connection.createChannel();
            channel.exchangeDeclare(this.platformExchangeName,
                    this.platformExchangeType,
                    this.plaftormExchangeDurable,
                    this.platformExchangeAutodelete,
                    this.platformExchangeInternal,
                    null);

        } catch (IOException e) {
            e.printStackTrace();
        } catch (TimeoutException e) {
            e.printStackTrace();
        } finally {
            closeChannel(channel);
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

    //todo during implementation
    private String receiveMessage(){
        String receivedMessage = "";
        Channel channel = null;
        try {
            channel = this.connection.createChannel();

            channel.queueDeclare(queueName, false, false, false, null);

            log.info("Receiver waiting for messages....");

            Consumer consumer = new DefaultConsumer(channel) {
                @Override
                public void handleDelivery(String consumerTag, Envelope envelope, AMQP.BasicProperties properties, byte[] body)
                        throws IOException {
                    String message = new String(body, "UTF-8");
                    System.out.println(" [x] Received '" + message + "'");
                    //TODO use the message
                }
            };
            channel.basicConsume(queueName, true, consumer);

            return receivedMessage;

        } catch (IOException e) {
            e.printStackTrace();
            return e.toString();
        } finally {
            closeChannel(channel);
        }
    }

    private void sendMessage(String exchange, String routingKey, String message) {
        Channel channel = null;
        try {
            channel = this.connection.createChannel();

            channel.basicPublish(exchange, routingKey, null, message.getBytes());

        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            closeChannel(channel);
        }
    }

    private String sendRpcMessage(String exchange, String routingKey, String message) {
        Channel channel = null;
        try {
            channel = this.connection.createChannel();

            String replyQueueName = channel.queueDeclare().getQueue();
            QueueingConsumer consumer = new QueueingConsumer(channel);
            channel.basicConsume(replyQueueName, true, consumer);

            String response = null;
            String correlationId = UUID.randomUUID().toString();

            AMQP.BasicProperties props = new AMQP.BasicProperties()
                    .builder()
                    .correlationId(correlationId)
                    .replyTo(replyQueueName)
                    .build();

            channel.basicPublish(exchange, routingKey, props, message.getBytes());


            //TODO true or something else?
            while (true) {
                QueueingConsumer.Delivery delivery = consumer.nextDelivery();
                if (delivery.getProperties().getCorrelationId().equals(correlationId)) {
                    response = new String(delivery.getBody());
                    break;
                }
            }

            System.out.println(response);

            return response;

        } catch (IOException e) {
            e.printStackTrace();
        } catch (InterruptedException e) {
            e.printStackTrace();
        } finally {
            closeChannel(channel);
        }
        return null;
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
