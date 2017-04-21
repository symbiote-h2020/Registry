package eu.h2020.symbiote.messaging;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.gson.JsonSyntaxException;
import com.rabbitmq.client.AMQP;
import com.rabbitmq.client.Channel;
import com.rabbitmq.client.DefaultConsumer;
import com.rabbitmq.client.Envelope;
import eu.h2020.symbiote.core.internal.CoreResourceRegistryRequest;
import eu.h2020.symbiote.core.internal.CoreResourceRegistryResponse;
import eu.h2020.symbiote.core.internal.DescriptionType;
import eu.h2020.symbiote.core.model.resources.Resource;
import eu.h2020.symbiote.model.CoreResourcePersistenceOperationResult;
import eu.h2020.symbiote.repository.RepositoryManager;
import eu.h2020.symbiote.utils.RegistryUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * RabbitMQ Consumer implementation used for Resource Removal actions
 * <p>
 * Created by mateuszl
 */
public class ResourceRemovalRequestConsumer extends DefaultConsumer {

    private static Log log = LogFactory.getLog(ResourceRemovalRequestConsumer.class);
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
    public ResourceRemovalRequestConsumer(Channel channel,
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
        ObjectMapper mapper = new ObjectMapper();
        CoreResourceRegistryRequest request = null;
        CoreResourceRegistryResponse response = new CoreResourceRegistryResponse();
        CoreResourcePersistenceOperationResult resourceRemovalResult = new CoreResourcePersistenceOperationResult();
        List<Resource> resources = new ArrayList<>();
        List<CoreResourcePersistenceOperationResult> resourceRemovalResultList = new ArrayList<>();
        String message = new String(body, "UTF-8");
        List<Resource> resourcesRemoved = new ArrayList<>();

        log.info(" [x] Received resource to remove: '" + message + "'");

        try {
            request = mapper.readValue(message, CoreResourceRegistryRequest.class);
        } catch (JsonSyntaxException e) {
            log.error("Error occured during getting Operation Request from Json", e);
            response.setStatus(400);
            response.setMessage("Error occured during getting Operation Request from Json");
        }

        if (request != null) {
            if (RegistryUtils.checkToken(request.getToken())) {
                try {
                    resources = mapper.readValue(request.getBody(), new TypeReference<List<Resource>>() {
                    });
                } catch (JsonSyntaxException e) {
                    log.error("Error occured during getting Resources from Json", e);
                    response.setStatus(400);
                    response.setMessage("Error occured during getting Resources from Json");
                }
            } else {
                log.error("Token invalid");
                response.setStatus(400);
                response.setMessage("Token invalid");
            }
        }

        for (Resource resource : resources) {
            if (resource.getId() != null || !resource.getId().isEmpty()) {
                resourceRemovalResult = this.repositoryManager.removeResource(resource);
                response.setMessage("Success");
                response.setStatus(200);
                response.setDescriptionType(DescriptionType.BASIC);
            } else {
                log.error("Given Resource has id null or empty");
                resourceRemovalResult.setMessage("Given Resource has ID null or empty");
                resourceRemovalResult.setStatus(400);
            }
            resourceRemovalResultList.add(resourceRemovalResult);
        }

        for (CoreResourcePersistenceOperationResult result : resourceRemovalResultList) {
            if (result.getStatus() == 200) {
                resourcesRemoved.add(result.getResource());
                //todo rollback
            }
        }

        rabbitManager.sendResourcesRemovedMessage(resourceRemovalResultList.stream()
                .map(coreResourcePersistenceOperationResult ->
                        coreResourcePersistenceOperationResult.getResource().getId())
                .collect(Collectors.toList())
        );
        log.info("- List with removed resources id's sent (fanout).");

        response.setBody(mapper.writerFor(new TypeReference<List<Resource>>() {
                }).writeValueAsString(resourceRemovalResultList.stream()
                        .map(coreResourcePersistenceOperationResult ->
                                RegistryUtils.convertCoreResourceToResource
                                        (coreResourcePersistenceOperationResult.getResource()))
                        .collect(Collectors.toList())
                )
        );

        rabbitManager.sendRPCReplyMessage(this, properties, envelope, mapper.writeValueAsString(response));
        log.info("- rpc response message sent. Content: " + response);
    }
}