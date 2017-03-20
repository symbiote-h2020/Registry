package eu.h2020.symbiote.messaging;

import com.google.gson.Gson;
import com.google.gson.JsonSyntaxException;
import com.google.gson.reflect.TypeToken;
import com.rabbitmq.client.AMQP;
import com.rabbitmq.client.Channel;
import com.rabbitmq.client.DefaultConsumer;
import com.rabbitmq.client.Envelope;
import eu.h2020.symbiote.model.Resource;
import eu.h2020.symbiote.model.ResourceResponse;
import eu.h2020.symbiote.repository.RepositoryManager;
import eu.h2020.symbiote.utils.RegistryUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import java.io.IOException;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;

/**
 * RabbitMQ Consumer implementation used for Resource Modification actions
 *
 * Created by mateuszl
 */
public class ResourceModificationRequestConsumer extends DefaultConsumer {

    private static Log log = LogFactory.getLog(ResourceModificationRequestConsumer.class);
    private RepositoryManager repositoryManager;
    private RabbitManager rabbitManager;

    /**
     * Constructs a new instance and records its association to the passed-in channel.
     * Managers beans passed as parameters because of lack of possibility to inject it to consumer.
     *
     * @param channel the channel to which this consumer is attached
     * @param rabbitManager rabbit manager bean passed for access to messages manager
     * @param repositoryManager repository manager bean passed for persistence actions
     */
    public ResourceModificationRequestConsumer(Channel channel,
                                               RepositoryManager repositoryManager,
                                               RabbitManager rabbitManager) {
        super(channel);
        this.repositoryManager = repositoryManager;
        this.rabbitManager = rabbitManager;
    }

    /**
     * Called when a <code><b>basic.deliver</b></code> is received for this consumer.
     * @param consumerTag the <i>consumer tag</i> associated with the consumer
     * @param envelope packaging data for the message
     * @param properties content header data for the message
     * @param body the message body (opaque, client-specific byte array)
     * @throws IOException if the consumer encounters an I/O error while processing the message
     * @see Envelope
     */
    @Override
    public void handleDelivery(String consumerTag, Envelope envelope,
                               AMQP.BasicProperties properties, byte[] body)
            throws IOException {
        Gson gson = new Gson();
        String response;
        ResourceResponse resourceResponse = new ResourceResponse();
        List<Resource> resources;
        List<ResourceResponse> resourceResponseList = new ArrayList<>();
        String message = new String(body, "UTF-8");

        log.info(" [x] Received resource to modify: '" + message + "'");

        try {
            Type listType = new TypeToken<ArrayList<Resource>>() {
            }.getType();
            resources = gson.fromJson(message, listType);
            for (Resource resource:resources) {
                if (RegistryUtils.validate(resource)) {
                    resource = RegistryUtils.getRdfBodyForObject(resource);
                    resourceResponse = this.repositoryManager.modifyResource(resource);
                    if (resourceResponse.getStatus() == 200) {
                        rabbitManager.sendResourceModifiedMessage(resourceResponse.getResource());
                    }
                } else {
                    log.error("Given Resource has some fields null or empty");
                    resourceResponse.setStatus(400);
                }
                resourceResponseList.add(resourceResponse);
            }
        } catch (JsonSyntaxException e) {
            log.error("Error occured during getting Resources from Json", e);
            resourceResponse.setStatus(400);
            resourceResponse.setMessage("Error occured during getting Resources from Json");
            resourceResponseList.add(resourceResponse);
        }
        response = gson.toJson(resourceResponse);
        rabbitManager.sendReplyMessage(this, properties, envelope, response);
    }
}
