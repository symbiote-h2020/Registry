package eu.h2020.symbiote.messaging.consumers.resource;

import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.gson.JsonSyntaxException;
import com.rabbitmq.client.AMQP;
import com.rabbitmq.client.Channel;
import com.rabbitmq.client.DefaultConsumer;
import com.rabbitmq.client.Envelope;
import eu.h2020.symbiote.core.internal.ClearDataRequest;
import eu.h2020.symbiote.core.internal.ClearDataResponse;
import eu.h2020.symbiote.core.internal.CoreResource;
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
 * Consumer responsible for executing clearData request, which deletes all resources of a given platform.
 * <p>
 * Created by Szymon Mueller on 09/11/2017.
 */
public class ResourceClearDataRequestConsumer extends DefaultConsumer {

    private static Log log = LogFactory.getLog(ResourceClearDataRequestConsumer.class);
    private ObjectMapper mapper;
    private AuthorizationManager authorizationManager;
    private RepositoryManager repositoryManager;
    private RabbitManager rabbitManager;
    private ClearDataResponse response;

    /**
     * Constructs a new instance and records its association to the passed-in channel.
     * Managers beans passed as parameters because of lack of possibility to inject it to consumer.
     *
     * @param channel           the channel to which this consumer is attached
     * @param rabbitManager     rabbit manager bean passed for access to messages manager
     * @param repositoryManager repository manager bean passed for persistence actions
     */
    public ResourceClearDataRequestConsumer(Channel channel,
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
        List<CoreResource> resourceList;

        ClearDataRequest request;
        ResourcePersistenceResult resourceRemovalResult;

        String message = new String(body, "UTF-8");
        log.info(" [x] Received clear resource data");

        try {
            try {
                request = mapper.readValue(message, ClearDataRequest.class);
            } catch (JsonSyntaxException | JsonMappingException e) {
                log.error("Error occurred during getting Operation Request from Json", e);
                generateResponse(HttpStatus.SC_BAD_REQUEST, "Error occurred during getting Operation Request from Json");
                rabbitManager.sendRPCReplyMessage(this, properties, envelope, mapper.writeValueAsString(response));
                return;
            }

            if (request == null) {
                log.error("Request for clear data is null");
                generateResponse(HttpStatus.SC_BAD_REQUEST, "Request for clear data is null");
                rabbitManager.sendRPCReplyMessage(this, properties, envelope, mapper.writeValueAsString(response));
                return;
            }

            if (request.getSecurityRequest() != null) {
                //This happens if request came from external server, this check is skipped for internal communication from Admin
                AuthorizationResult tokenAuthorizationResult =
                        authorizationManager.checkSinglePlatformOperationAccess(request.getSecurityRequest(), request.getBody());
                if (!tokenAuthorizationResult.isValidated()) {
                    log.error("Token invalid: \"" + tokenAuthorizationResult.getMessage() + "\"");
                    generateResponse(HttpStatus.SC_BAD_REQUEST, "Token invalid: \"" + tokenAuthorizationResult.getMessage()
                            + "\" could not clear data for a platform");
                    rabbitManager.sendRPCReplyMessage(this, properties, envelope,
                            mapper.writeValueAsString(response));
                    return;
                }
            }

            //List all resources of a platform
            resourceList = repositoryManager.getResourcesForPlatform(request.getBody());
            log.debug("Found resources number: " + resourceList.size());

            if (resourceList.isEmpty()) {
                log.error("Request to clear the data - there are no resources to remove.");
                generateResponse(HttpStatus.SC_OK, "Request to clear the data - there are no resources to remove.");
                rabbitManager.sendRPCReplyMessage(this, properties, envelope, mapper.writeValueAsString(response));
                return;
            }

            for (CoreResource res : resourceList) {
                if (res == null) {
                    log.error("Resources list contains a NULL resource!");
                } else {
                    resourceRemovalResult = this.repositoryManager.removeResource(RegistryUtils.convertCoreResourceToResource(res));
                    resourceRemovalMap.put(res.getId(), resourceRemovalResult);
                }
            }

            if (checkIfRemovalWasSuccessful(resourceRemovalMap.values().stream().collect(Collectors.toList()), resourcesRemoved, resourceList)) {
                sendFanoutMessage(resourceRemovalMap.values().stream().collect(Collectors.toList()));
                generateResponse(HttpStatus.SC_OK, "Success");
            } else {
                generateResponse(410, "Operation not performed");
            }

            rabbitManager.sendRPCReplyMessage(this, properties, envelope, mapper.writeValueAsString(response));
        } catch (Exception e) {
            log.error(e);
            generateResponse(HttpStatus.SC_INTERNAL_SERVER_ERROR, "Consumer critical exception!");
            rabbitManager.sendRPCReplyMessage(this, properties, envelope, mapper.writeValueAsString(response));
        }
    }

    private ClearDataResponse generateResponse(int status, String msg) {
        this.response = new ClearDataResponse(status, msg, null);
        this.response.setServiceResponse(authorizationManager.generateServiceResponse());
        return response;
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
                                                List<Resource> resourcesRemoved, List<CoreResource> resources) {
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

    private void rollback(List<Resource> resourcesRemoved) {
        for (Resource resource : resourcesRemoved) {
            this.repositoryManager.saveResource(RegistryUtils.convertResourceToCoreResource(resource));
            log.info("Removed resources rollback performed.");
        }
    }
}
