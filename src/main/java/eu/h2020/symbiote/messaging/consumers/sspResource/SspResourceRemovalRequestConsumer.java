package eu.h2020.symbiote.messaging.consumers.sspResource;

import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.gson.JsonSyntaxException;
import com.rabbitmq.client.AMQP;
import com.rabbitmq.client.Channel;
import com.rabbitmq.client.DefaultConsumer;
import com.rabbitmq.client.Envelope;
import eu.h2020.symbiote.core.internal.CoreResource;
import eu.h2020.symbiote.core.internal.CoreSspResourceRegistryRequest;
import eu.h2020.symbiote.core.internal.CoreSspResourceRegistryResponse;
import eu.h2020.symbiote.managers.AuthorizationManager;
import eu.h2020.symbiote.managers.RabbitManager;
import eu.h2020.symbiote.managers.RepositoryManager;
import eu.h2020.symbiote.model.CoreSspResource;
import eu.h2020.symbiote.model.cim.Resource;
import eu.h2020.symbiote.model.persistenceResults.AuthorizationResult;
import eu.h2020.symbiote.model.persistenceResults.CoreSspResourcePersistenceResult;
import eu.h2020.symbiote.utils.ValidationUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.http.HttpStatus;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Created by mateuszl on 30.05.2018.
 */
public class SspResourceRemovalRequestConsumer extends DefaultConsumer {

    private static Log log = LogFactory.getLog(SspResourceRemovalRequestConsumer.class);
    private ObjectMapper mapper;
    private AuthorizationManager authorizationManager;
    private RepositoryManager repositoryManager;
    private RabbitManager rabbitManager;
    private CoreSspResourceRegistryResponse response;
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
    public SspResourceRemovalRequestConsumer(Channel channel,
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
        Map<String, CoreSspResourcePersistenceResult> resourcesRemovalResultMap = new HashMap<>();
        Map<String, Resource> resources;
        this.envelope = envelope;
        this.properties = properties;
        this.response = new CoreSspResourceRegistryResponse();

        CoreSspResourceRegistryRequest request;
        CoreSspResourcePersistenceResult resourceRemovalResult;

        String message = new String(body, "UTF-8");
        log.info(" [x] Received ssp resource to remove");


        try {
            request = mapper.readValue(message, CoreSspResourceRegistryRequest.class);
        } catch (JsonSyntaxException | JsonMappingException e) {
            log.error("Error occurred during getting Operation Request from Json", e);
            prepareAndSendErrorResponse(400, "Error occurred during getting Operation Request from Json");
            return;
        }


        //checking access by token verification
        AuthorizationResult tokenAuthorizationResult = authorizationManager.checkSdevOperationAccess(
                request.getSecurityRequest(),
                request.getSspId()); //todo partially MOCKED

        if (!tokenAuthorizationResult.isValidated()) {
            log.error("Token invalid: \"" + tokenAuthorizationResult.getMessage() + "\"");
            prepareAndSendErrorResponse(400, String.format("Error: \" %s \"", tokenAuthorizationResult.getMessage()));
            return;
        }

        //checking access by verification of fields needed for that operation
        if (!validateAccess(request)) return;


        try {
            resources = request.getBody();
            Map<String, CoreSspResource> sspResourcesMap = convertResourceToCoreSspResourceMap(resources, request.getSdevId());

            //// TODO: 05.06.2018 check if the resource is bounded to existing SSP - if not return and reply with 400

            for (String key : sspResourcesMap.keySet()) {
                if (sspResourcesMap.get(key) == null) {
                    prepareAndSendErrorResponse(410, "Resources list contains a NULL ssp resource!" + sspResourcesMap);
                    return;
                } else {
                    if (sspResourcesMap.get(key).getId() != null || !sspResourcesMap.get(key).getId().isEmpty()) {
                        resourceRemovalResult = this.repositoryManager.removeCoreSspResource(sspResourcesMap.get(key).getId());
                    } else {
                        prepareAndSendErrorResponse(400, "Given Ssp Resource has id null or empty");
                        return;
                    }
                    resourcesRemovalResultMap.put(key, resourceRemovalResult);
                }
            }

            if (checkIfRemovalWasSuccessful(resourcesRemovalResultMap, resources)) {

                sendFanoutMessage(resourcesRemovalResultMap);

                response.setMessage("Success");
                response.setStatus(200);
                response.setServiceResponse(authorizationManager.generateServiceResponse());

                response.setBody(convertCoreSspResourceToResourceMap(resourcesRemovalResultMap));

                rabbitManager.sendRPCReplyMessage(this, properties, envelope, mapper.writeValueAsString(response));

            } else {
                prepareAndSendErrorResponse(410, "Operation od Ssp Resource Removal not performed!");
            }
        } catch (Exception e) {
            prepareAndSendErrorResponse(HttpStatus.SC_INTERNAL_SERVER_ERROR, "Consumer critical exception!");
        }
    }


    private boolean validateAccess(CoreSspResourceRegistryRequest request) throws IOException {
        try {
            ValidationUtils.validateSspResource(request, repositoryManager);
        } catch (IllegalArgumentException e) {
            prepareAndSendErrorResponse(HttpStatus.SC_BAD_REQUEST, e.getMessage());
            return false;
        } catch (Exception e) {
            prepareAndSendErrorResponse(HttpStatus.SC_INTERNAL_SERVER_ERROR, e.getMessage());
            return false;
        }

        try {
            ValidationUtils.checkIfResourcesHaveNullOrEmptyId(request);
        } catch (IllegalArgumentException e) {
            prepareAndSendErrorResponse(HttpStatus.SC_BAD_REQUEST, e.getMessage());
            return false;
        }

        return true;
    }


    private Map<String, Resource> convertCoreSspResourceToResourceMap(Map<String, CoreSspResourcePersistenceResult> resourcesRemovingResultMap) {
        HashMap<String, Resource> resourcesMap = new HashMap<>();

        resourcesRemovingResultMap.keySet().stream()
                .forEach(key -> {
                    CoreSspResource coreSspResource = resourcesRemovingResultMap.get(key).getCoreSspResource();
                    Resource resource = coreSspResource.getResource();
                    resourcesMap.put(key, resource);
                });

        return resourcesMap;
    }

    private Map<String, CoreSspResource> convertResourceToCoreSspResourceMap(Map<String, Resource> resources, String sDevId) {
        HashMap<String, CoreSspResource> coreSspResourcesMap = new HashMap<>();

        resources.keySet().stream().forEach(
                key -> {
                    Resource resource = resources.get(key);
                    coreSspResourcesMap.put(key, new CoreSspResource(sDevId, (CoreResource) resource)); //todo czy to zadziała jak należy?
                }
        );

        return coreSspResourcesMap;
    }

    private void sendFanoutMessage(Map<String, CoreSspResourcePersistenceResult> resourceRemovalResultsMap) {
        List<String> resourcesIds = resourceRemovalResultsMap.keySet().stream()
                .map(key -> resourceRemovalResultsMap.get(key).getCoreSspResource().getId())
                .collect(Collectors.toList());

        log.info("Sending fanout message with content: " + resourcesIds);

        rabbitManager.sendResourcesRemovalMessage(resourcesIds);
    }

    private boolean checkIfRemovalWasSuccessful(Map<String, CoreSspResourcePersistenceResult> resourceRemovalResultsMap,
                                                Map<String, Resource> resources) {
        List<CoreSspResource> resourcesRemovedSuccessfully = resourceRemovalResultsMap.keySet().stream()
                .filter(key -> resourceRemovalResultsMap.get(key).getStatus() == 200)
                .map(key -> resourceRemovalResultsMap.get(key).getCoreSspResource())
                .collect(Collectors.toList());

        if (resourcesRemovedSuccessfully.size() != resources.size()) {
            rollback(resourcesRemovedSuccessfully);
            return false;
        }
        return true;
    }

    private void prepareAndSendErrorResponse(int status, String message) throws IOException {
        log.error(message);
        this.response.setStatus(status);
        this.response.setMessage(message);
        this.response.setServiceResponse(authorizationManager.generateServiceResponse());

        rabbitManager.sendRPCReplyMessage(this, properties, envelope, mapper.writeValueAsString(response));
    }

    private void rollback(List<CoreSspResource> resourcesRemoved) {
        for (CoreSspResource resource : resourcesRemoved) {
            this.repositoryManager.saveCoreSspResource(resource);
            log.info("Removed resources rollback performed.");
        }
    }
}