package eu.h2020.symbiote.messaging;

import com.google.gson.Gson;
import com.rabbitmq.client.AMQP;
import com.rabbitmq.client.Channel;
import com.rabbitmq.client.DefaultConsumer;
import com.rabbitmq.client.Envelope;
import eu.h2020.symbiote.model.Location;
import eu.h2020.symbiote.model.Resource;
import eu.h2020.symbiote.model.ResourceCreationResponse;
import eu.h2020.symbiote.repository.RepositoryManager;

import java.io.IOException;

/**
 * Created by mateuszl on 17.01.2017.
 */
public class ResourceRequestConsumer extends DefaultConsumer {

    private RepositoryManager repositoryManager;

    /**
     * Constructs a new instance and records its association to the passed-in channel.
     *
     * @param channel the channel to which this consumer is attached
     */
    public ResourceRequestConsumer(Channel channel, RepositoryManager repositoryManager) {
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

        Resource resource = gson.fromJson(message, Resource.class);
        Location savedLocation = this.repositoryManager.saveLocation(resource.getLocation());
        resource.setLocation(savedLocation);
        ResourceCreationResponse resourceCreationResponse = this.repositoryManager.saveResource(resource);
        response = gson.toJson(resourceCreationResponse);

        System.out.println(properties.getReplyTo());

        this.getChannel().basicPublish("", properties.getReplyTo(), replyProps, response.getBytes());
        System.out.println("-> Message sent back");

        this.getChannel().basicAck(envelope.getDeliveryTag(), false);
    }
}
