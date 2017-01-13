package eu.h2020.symbiote.messaging;

import com.rabbitmq.client.*;

import java.io.IOException;

/**
 * Created by mateuszl on 12.01.2017.
 */
public class RequestConsumer extends DefaultConsumer {

    /**
     * Constructs a new instance and records its association to the passed-in channel.
     *
     * @param channel the channel to which this consumer is attached
     */
    public RequestConsumer(Channel channel) {
        super(channel);
        System.out.println("Consumer created!");
    }

    @Override
    public void handleConsumeOk(String consumerTag) {
        System.out.println("Consume ok");
        super.handleConsumeOk(consumerTag);
    }

    @Override
    public void handleShutdownSignal(String consumerTag, ShutdownSignalException sig) {
        System.out.println("Consumer shutdown");
        super.handleShutdownSignal(consumerTag, sig);
    }

    @Override
    public void handleDelivery(String consumerTag, Envelope envelope,
                               AMQP.BasicProperties properties, byte[] body)
            throws IOException {
        String message = new String(body, "UTF-8");
        System.out.println(" [x] Received '" + message + "'");

        AMQP.BasicProperties replyProps = new AMQP.BasicProperties
                .Builder()
                .correlationId(properties.getCorrelationId())
                .build();

        //todo save object to database here
        String id = "12345";

        //todo on successful save:
        this.getChannel().basicPublish("", properties.getReplyTo(), replyProps, id.getBytes());
        System.out.println("- message sent back");

        this.getChannel().basicAck(envelope.getDeliveryTag(), false);
    }
}