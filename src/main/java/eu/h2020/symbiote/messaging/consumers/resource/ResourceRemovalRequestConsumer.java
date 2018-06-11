package eu.h2020.symbiote.messaging.consumers.resource;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.gson.JsonSyntaxException;
import com.rabbitmq.client.AMQP;
import com.rabbitmq.client.Channel;
import com.rabbitmq.client.DefaultConsumer;
import com.rabbitmq.client.Envelope;
import eu.h2020.symbiote.core.internal.CoreResource;
import eu.h2020.symbiote.core.internal.CoreResourceRegistryRequest;
import eu.h2020.symbiote.core.internal.CoreResourceRegistryResponse;
import eu.h2020.symbiote.core.internal.DescriptionType;
import eu.h2020.symbiote.managers.AuthorizationManager;
import eu.h2020.symbiote.managers.RabbitManager;
import eu.h2020.symbiote.managers.RepositoryManager;
import eu.h2020.symbiote.model.cim.Resource;
import eu.h2020.symbiote.model.persistenceResults.AuthorizationResult;
import eu.h2020.symbiote.model.persistenceResults.ResourcePersistenceResult;
import eu.h2020.symbiote.utils.RegistryUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.http.HttpStatus;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * RabbitMQ Consumer implementation used for Resource Removal actions.
 * Response contains List of removed IDs in JSON format as body
 * <p>
 * Created by mateuszl
 */
public class ResourceRemovalRequestConsumer extends DefaultConsumer {

    private static Log log = LogFactory.getLog(ResourceRemovalRequestConsumer.class);
    private ObjectMapper mapper;
    private AuthorizationManager authorizationManager;
    private RepositoryManager repositoryManager;
    private RabbitManager rabbitManager;
    private CoreResourceRegistryResponse response; //containing List of removed IDs in JSON format
    private Envelope envelope;
    private AMQP.BasicProperties properties;

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
                                          RabbitManager rabbitManager,
                                          AuthorizationManager authorizationManager) {
        super(channel);
        this.repositoryManager = repositoryManager;
        this.rabbitManager = rabbitManager;
        this.authorizationManager = authorizationManager;
        this.mapper = new ObjectMapper();
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
        Map<String, ResourcePersistenceResult> resourceRemovalMap = new HashMap<>();
        List<Resource> resourcesRemoved = new ArrayList<>();
        Map<String, Resource> resources;
        this.envelope = envelope;
        this.properties = properties;
        this.response = new CoreResourceRegistryResponse();

        CoreResourceRegistryRequest request;
        ResourcePersistenceResult resourceRemovalResult;

        String message = new String(body, "UTF-8");
        log.info(" [x] Received resource to remove");

        try {
            try {
                request = mapper.readValue(message, CoreResourceRegistryRequest.class);
            } catch (JsonSyntaxException | JsonMappingException e) {
                log.error("Error occurred during getting Operation Request from Json", e);
                prepareAndSendErrorResponse(400, "Error occurred during getting Operation Request from Json");
                return;
            }

            if (request != null) {
                AuthorizationResult tokenAuthorizationResult = authorizationManager.checkSinglePlatformOperationAccess(request.getSecurityRequest(), request.getPlatformId());
                if (!tokenAuthorizationResult.isValidated()) {
                    prepareAndSendErrorResponse(400, "Token invalid: \"" + tokenAuthorizationResult.getMessage() + "\"");
                    return;
                }
            } else {
                prepareAndSendErrorResponse(400, "Request is null");
                return;
            }

            try {
                resources = mapper.readValue(request.getBody(), new TypeReference<Map<String, Resource>>() {
                });
            } catch (JsonSyntaxException | JsonMappingException e) {
                prepareAndSendErrorResponse(400, "Error occurred during getting Resources from Json" + e);
                return;
            }

            AuthorizationResult resourcesAccessAuthorizationResult =
                    authorizationManager.checkIfResourcesBelongToPlatform(resources, request.getPlatformId());

            if (!resourcesAccessAuthorizationResult.isValidated()) {
                prepareAndSendErrorResponse(400, resourcesAccessAuthorizationResult.getMessage() + resources);
                return;
            }

            for (String key : resources.keySet()) {
                if (resources.get(key) == null) {
                    prepareAndSendErrorResponse(410, "Resources list contains a NULL resource!" + resources);
                    return;
                } else {
                    if (resources.get(key).getId() != null || !resources.get(key).getId().isEmpty()) {
                        resourceRemovalResult = this.repositoryManager.removeResource(resources.get(key));
                    } else {
                        prepareAndSendErrorResponse(400, "Given Resource has id null or empty");
                        return;
                    }
                    resourceRemovalMap.put(key, resourceRemovalResult);
                }
            }

            List<ResourcePersistenceResult> resourcePersistenceResultList = resourceRemovalMap.values().stream().collect(Collectors.toList());
            if (checkIfRemovalWasSuccessful(resourcePersistenceResultList, resourcesRemoved, resources)) {

                sendFanoutMessage(resourcePersistenceResultList);

                response.setMessage("Success");
                response.setStatus(200);
                response.setDescriptionType(DescriptionType.BASIC);
                response.setServiceResponse(authorizationManager.generateServiceResponse());

                List<String> removedResourcesIdsList = resourcePersistenceResultList.stream()
                        .map(p -> p.getResource().getId())
                        .collect(Collectors.toList());

                response.setBody(mapper.writerFor(new TypeReference<List<String>>() {
                }).writeValueAsString(removedResourcesIdsList));

                rabbitManager.sendRPCReplyMessage(this, properties, envelope, mapper.writeValueAsString(response));

            } else {
                prepareAndSendErrorResponse(410, "Operation not performed");
            }
        } catch (Exception e) {
            prepareAndSendErrorResponse(HttpStatus.SC_INTERNAL_SERVER_ERROR, "Consumer critical exception!");
        }
    }

    private void sendFanoutMessage(List<ResourcePersistenceResult> resourceRemovalResultList) {
        List<String> resourcesIds = resourceRemovalResultList.stream()
                .map(ResourcePersistenceResult::getResource)
                .map(CoreResource::getId)
                .collect(Collectors.toList());

        log.info("Sending fanout message with content: " + resourcesIds);

        rabbitManager.sendResourcesRemovalMessage(resourcesIds);
    }

    private boolean checkIfRemovalWasSuccessful(List<ResourcePersistenceResult> resourceRemovalResultList,
                                                List<Resource> resourcesRemoved, Map<String, Resource> resources) {
        for (ResourcePersistenceResult result : resourceRemovalResultList) {
            if (result.getStatus() == 200) {
                resourcesRemoved.add(result.getResource());
            }
        }
        if (resourcesRemoved.size() != resources.size()) {
            rollback(resourcesRemoved);
            return false;
        }
        return true;
    }

    private void prepareAndSendErrorResponse(int status, String message) throws IOException {
        log.error(message);
        this.response.setStatus(status);
        this.response.setMessage(message);
        this.response.setDescriptionType(DescriptionType.BASIC);
        this.response.setServiceResponse(authorizationManager.generateServiceResponse());

        rabbitManager.sendRPCReplyMessage(this, properties, envelope, mapper.writeValueAsString(response));
    }

    private void rollback(List<Resource> resourcesRemoved) {
        for (Resource resource : resourcesRemoved) {
            this.repositoryManager.saveResource(RegistryUtils.convertResourceToCoreResource(resource));
            log.info("Removed resources rollback performed.");
        }
    }
}