package eu.h2020.symbiote.messaging;

import com.google.gson.Gson;
import com.google.gson.JsonSyntaxException;
import com.google.gson.reflect.TypeToken;
import com.rabbitmq.client.AMQP;
import com.rabbitmq.client.Channel;
import com.rabbitmq.client.DefaultConsumer;
import com.rabbitmq.client.Envelope;
import eu.h2020.symbiote.core.model.resources.Resource;
import eu.h2020.symbiote.model.RegistryRequest;
import eu.h2020.symbiote.model.RegistryResponse;
import eu.h2020.symbiote.model.CoreResourceSavingResult;
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
 * <p>
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
     * @param channel           the channel to which this consumer is attached
     * @param rabbitManager     rabbit manager bean passed for access to messages manager
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
     *
     * @param consumerTag the <i>consumer tag</i> associated with the consumer
     * @param envelope    packaging data for the message
     * @param properties  content header data for the message
     * @param body        the message body (opaque, client-specific byte array)
     * @throws IOException if the consumer encounters an I/O error while processing the message
     * @see Envelope
     */
    @Override
    public void handleDelivery(String consumerTag, Envelope envelope,
                               AMQP.BasicProperties properties, byte[] body)
            throws IOException {
        Gson gson = new Gson();
        RegistryRequest request;
        RegistryResponse registryResponse;
        String response;
        List<Resource> resources = new ArrayList<>();
        CoreResourceSavingResult resourceSavingResult = new CoreResourceSavingResult();
        List<CoreResourceSavingResult> resourceSavingResultList = new ArrayList<>();
        String message = new String(body, "UTF-8");
        Type listType = new TypeToken<ArrayList<Resource>>() {
        }.getType();
        log.info(" [x] Received resources to modify: '" + message + "'");

        try {
            request = gson.fromJson(message, RegistryRequest.class);
            if (RegistryUtils.checkToken(request.getToken())) {
                switch (request.getType()) {
                    case RDF:
                        try {
                            registryResponse = RegistryUtils.getResourcesFromRdf(request.getBody());
                            if (registryResponse.getStatus() == 200) {
                                resources = gson.fromJson(registryResponse.getBody(), listType);
                            } else {
                                log.error("Error occured during rdf verification. Semantic Manager info: "
                                        + registryResponse.getMessage());
                                resourceSavingResult.setStatus(400);
                                resourceSavingResult.setMessage("Error occured during rdf verification. Semantic Manager info: "
                                        + registryResponse.getMessage());
                                resourceSavingResultList.add(resourceSavingResult);
                            }
                        } catch (JsonSyntaxException e) {
                            log.error("Error occured during getting Resources from Json received from Semantic Manager", e);
                            resourceSavingResult.setStatus(400);
                            resourceSavingResult.setMessage("Error occured during getting Resources from Json");
                            resourceSavingResultList.add(resourceSavingResult);
                        }
                    case BASIC:
                        try {
                            resources = gson.fromJson(request.getBody(), listType);
                        } catch (JsonSyntaxException e) {
                            log.error("Error occured during getting Resources from Json", e);
                            resourceSavingResult.setStatus(400);
                            resourceSavingResult.setMessage("Error occured during getting Resources from Json");
                            resourceSavingResultList.add(resourceSavingResult);
                        }
                }
            } else {
                log.error("Token invalid");
                resourceSavingResult.setStatus(400);
                resourceSavingResult.setMessage("Token invalid");
                resourceSavingResultList.add(resourceSavingResult);
            }
        } catch (JsonSyntaxException e) {
            log.error("Unable to get RegistryRequest from Message body!");
            e.printStackTrace();
        }

        /*
        for (Resource resource : resources) {
            if (RegistryUtils.validateFields(resource)) {
                resource = RegistryUtils.getRdfBodyForObject(resource);
                resourceSavingResult = this.repositoryManager.modifyResource(resource);
                if (resourceSavingResult.getStatus() == 200) {
                    rabbitManager.sendResourceModifiedMessage(resourceSavingResult.getResource());
                }
            } else {
                log.error("Given Resource has some fields null or empty");
                resourceSavingResult.setMessage("Given Resource has some fields null or empty");
                resourceSavingResult.setStatus(400);
            }
            resourceSavingResultList.add(resourceSavingResult);
        }
*/

        //if resources List is empty, resourceSavingResultList will still contain needed information
        response = gson.toJson(resourceSavingResultList);
        rabbitManager.sendRPCReplyMessage(this, properties, envelope, response);
    }
}
