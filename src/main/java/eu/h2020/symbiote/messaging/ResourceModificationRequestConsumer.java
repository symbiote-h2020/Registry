package eu.h2020.symbiote.messaging;

import com.google.gson.Gson;
import com.google.gson.JsonSyntaxException;
import com.rabbitmq.client.AMQP;
import com.rabbitmq.client.Channel;
import com.rabbitmq.client.DefaultConsumer;
import com.rabbitmq.client.Envelope;
import eu.h2020.symbiote.model.Location;
import eu.h2020.symbiote.model.Resource;
import eu.h2020.symbiote.model.ResourceResponse;
import eu.h2020.symbiote.repository.RepositoryManager;

import java.io.IOException;

/**
 * Created by mateuszl on 23.01.2017.
 */
public class ResourceModificationRequestConsumer extends DefaultConsumer {

    private RepositoryManager repositoryManager;
    private RabbitManager rabbitManager;

    /**
     * Constructs a new instance and records its association to the passed-in channel.
     *
     * @param channel the channel to which this consumer is attached
     */
    public ResourceModificationRequestConsumer(Channel channel,
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
        Resource resource;
        ResourceResponse resourceResponse;
        String response = "";

        String message = new String(body, "UTF-8");
        System.out.println(" [x] Received resource to modify: '" + message + "'");

        AMQP.BasicProperties replyProps = new AMQP.BasicProperties
                .Builder()
                .correlationId(properties.getCorrelationId())
                .build();

        try {
            resource = gson.fromJson(message, Resource.class);
            //todo something with location
            if (resource.getLocation() != null) {
                Location savedLocation = this.repositoryManager.saveLocation(resource.getLocation());
                resource.setLocation(savedLocation);
            }
            resourceResponse = this.repositoryManager.modifyResource(resource);
            if (resourceResponse.getStatus() == 200) {
                rabbitManager.sendResourceModifiedMessage(resourceResponse.getResource());
            }
        } catch (JsonSyntaxException e) {
            e.printStackTrace();
            resourceResponse = new ResourceResponse();
            resourceResponse.setStatus(400);
        }

        response = gson.toJson(resourceResponse);

        this.getChannel().basicPublish("", properties.getReplyTo(), replyProps, response.getBytes());
        System.out.println("-> Message sent back");

        this.getChannel().basicAck(envelope.getDeliveryTag(), false);
    }
}
