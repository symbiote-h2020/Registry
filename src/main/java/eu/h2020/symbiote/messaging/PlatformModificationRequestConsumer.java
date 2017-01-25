package eu.h2020.symbiote.messaging;

import com.google.gson.Gson;
import com.google.gson.JsonSyntaxException;
import com.rabbitmq.client.AMQP;
import com.rabbitmq.client.Channel;
import com.rabbitmq.client.DefaultConsumer;
import com.rabbitmq.client.Envelope;
import eu.h2020.symbiote.model.Platform;
import eu.h2020.symbiote.model.PlatformResponse;
import eu.h2020.symbiote.repository.RepositoryManager;

import java.io.IOException;

/**
 * Created by mateuszl on 23.01.2017.
 */
public class PlatformModificationRequestConsumer extends DefaultConsumer {

    private RepositoryManager repositoryManager;
    private RabbitManager rabbitManager;
    /**
     * Constructs a new instance and records its association to the passed-in channel.
     *
     * @param channel the channel to which this consumer is attached
     */
    public PlatformModificationRequestConsumer(Channel channel,
                                               RepositoryManager repositoryManager,
                                               RabbitManager rabbitManager) {
        super(channel);
        this.repositoryManager = repositoryManager;
        this.rabbitManager = rabbitManager;
    }

    @Override
    public void handleDelivery(String consumerTag, Envelope envelope,
                               AMQP.BasicProperties properties, byte[] body)
            throws IOException {
        Gson gson = new Gson();
        String response;
        Platform platform;
        PlatformResponse platformResponse;

        String message = new String(body, "UTF-8");
        System.out.println(" [x] Received platform to modify: '" + message + "'");

        AMQP.BasicProperties replyProps = new AMQP.BasicProperties
                .Builder()
                .correlationId(properties.getCorrelationId())
                .build();

        try {
            platform = gson.fromJson(message, Platform.class);
            platformResponse = this.repositoryManager.modifyPlatform(platform);
            rabbitManager.sendPlatformModifiedMessage(platformResponse.getPlatform());
        } catch (JsonSyntaxException e) {
            e.printStackTrace();
            platformResponse = new PlatformResponse();
            platformResponse.setStatus(400);
        }

        response = gson.toJson(platformResponse);
        this.getChannel().basicPublish("", properties.getReplyTo(), replyProps, response.getBytes());
        System.out.println("-> Message sent back");

        this.getChannel().basicAck(envelope.getDeliveryTag(), false);
    }
}
