package eu.h2020.symbiote.messaging;

import com.google.gson.Gson;
import com.google.gson.JsonSyntaxException;
import com.rabbitmq.client.AMQP;
import com.rabbitmq.client.Channel;
import com.rabbitmq.client.DefaultConsumer;
import com.rabbitmq.client.Envelope;
import eu.h2020.symbiote.model.Resource;
import eu.h2020.symbiote.model.ResourceRemovalResponse;
import eu.h2020.symbiote.repository.RepositoryManager;

import java.io.IOException;

/**
 * Created by mateuszl on 20.01.2017.
 */
public class ResourceRemovalRequestConsumer extends DefaultConsumer {

    private RepositoryManager repositoryManager;
    /**
     * Constructs a new instance and records its association to the passed-in channel.
     *
     * @param channel the channel to which this consumer is attached
     */
    public ResourceRemovalRequestConsumer(Channel channel, RepositoryManager repositoryManager) {
        super(channel);
        this.repositoryManager = repositoryManager;
    }

    @Override
    public void handleDelivery(String consumerTag, Envelope envelope,
                               AMQP.BasicProperties properties, byte[] body)
            throws IOException {
        Gson gson = new Gson();
        String response = "";
        String message = new String(body, "UTF-8");
        System.out.println(" [x] Received '" + message + "'");

        AMQP.BasicProperties replyProps = new AMQP.BasicProperties
                .Builder()
                .correlationId(properties.getCorrelationId())
                .build();

        Resource resource;
        ResourceRemovalResponse resourceRemovalResponse = null;
        try {
            resource = gson.fromJson(message, Resource.class);
            resourceRemovalResponse = this.repositoryManager.removeResource(resource);
        } catch (JsonSyntaxException e) {
            e.printStackTrace();
            resourceRemovalResponse = new ResourceRemovalResponse();
            resourceRemovalResponse.setStatus(400);
        }

        response = gson.toJson(resourceRemovalResponse);

        this.getChannel().basicPublish("", properties.getReplyTo(), replyProps, response.getBytes());
        System.out.println("-> Message sent back");

        this.getChannel().basicAck(envelope.getDeliveryTag(), false);
    }
}
