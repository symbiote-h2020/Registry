package eu.h2020.symbiote.messaging.consumers.resource;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.gson.JsonSyntaxException;
import com.rabbitmq.client.AMQP;
import com.rabbitmq.client.Channel;
import com.rabbitmq.client.DefaultConsumer;
import com.rabbitmq.client.Envelope;
import eu.h2020.symbiote.core.internal.*;
import eu.h2020.symbiote.managers.AuthorizationManager;
import eu.h2020.symbiote.managers.RabbitManager;
import eu.h2020.symbiote.managers.RepositoryManager;
import eu.h2020.symbiote.model.AuthorizationResult;
import eu.h2020.symbiote.model.RegistryOperationType;
import eu.h2020.symbiote.model.ResourcePersistenceResult;
import eu.h2020.symbiote.model.cim.Resource;
import eu.h2020.symbiote.security.accesspolicies.common.IAccessPolicySpecifier;
import eu.h2020.symbiote.utils.RegistryUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * RPC Consumer waiting for messages from Semantic Manager. Acts accordingly to received validation/translation results.
 * Created when creation/modification of Resource action is triggered.
 * <p>
 * Created by mateuszl on 30.03.2017.
 */
public class ResourceValidationResponseConsumer extends DefaultConsumer {

    private static Log log = LogFactory.getLog(ResourceValidationResponseConsumer.class);
    private CoreResourceRegistryResponse registryResponse;
    private DefaultConsumer rpcConsumer;
    private AMQP.BasicProperties rpcProperties;
    private Envelope rpcEnvelope;
    private RepositoryManager repositoryManager;
    private RabbitManager rabbitManager;
    private String resourcesPlatformId;
    private RegistryOperationType operationType;
    private boolean bulkRequestSuccess = true;
    private ObjectMapper mapper;
    private DescriptionType descriptionType;
    private String response;
    private AuthorizationManager authorizationManager;
    private Map<String, IAccessPolicySpecifier> policiesMap;
    private String requestBody;
    Map<String, Resource> requestedResourcesMap;

    /**
     * Constructs a new instance and records its association to the passed-in channel.
     * Managers beans passed as parameters because of lack of possibility to inject it to consumer.
     *
     * @param channel           the channel to which this consumer is attached
     * @param rabbitManager     rabbit manager bean passed for access to messages manager
     * @param repositoryManager repository manager bean passed for persistence actions
     */
    public ResourceValidationResponseConsumer(DefaultConsumer rpcConsumer,
                                              AMQP.BasicProperties rpcProperties,
                                              Envelope rpcEnvelope,
                                              Channel channel,
                                              RepositoryManager repositoryManager,
                                              RabbitManager rabbitManager,
                                              String resourcesPlatformId,
                                              RegistryOperationType operationType,
                                              DescriptionType descriptionType,
                                              AuthorizationManager authorizationManager,
                                              Map<String, IAccessPolicySpecifier> policiesMap,
                                              String requestBody) {
        super(channel);
        this.repositoryManager = repositoryManager;
        this.rabbitManager = rabbitManager;
        this.rpcConsumer = rpcConsumer;
        this.rpcEnvelope = rpcEnvelope;
        this.rpcProperties = rpcProperties;
        this.resourcesPlatformId = resourcesPlatformId;
        this.operationType = operationType;
        this.descriptionType = descriptionType;
        this.authorizationManager = authorizationManager;
        this.policiesMap = policiesMap;
        this.requestBody = requestBody;
        this.mapper = new ObjectMapper();
        this.registryResponse = new CoreResourceRegistryResponse();
        response = "";
    }

    //fixme when creating resources, reply could include original list of resources with added IDs instead of list with new resources

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

        String message = new String(body, "UTF-8");
        ResourceInstanceValidationResult resourceInstanceValidationResult = new ResourceInstanceValidationResult();
        Map<String, CoreResource> coreResources;
        registryResponse.setDescriptionType(descriptionType);

        log.info("[x] Received '" + descriptionType + "' validation result: '" + message + "'");

        try {
            try {
                //receive and read message from Semantic Manager
                resourceInstanceValidationResult = mapper.readValue(message, ResourceInstanceValidationResult.class);
            } catch (JsonSyntaxException | JsonMappingException e) {
                log.error("Unable to get resource validation result from Message body!", e);
                registryResponse.setStatus(500);
                registryResponse.setMessage("VALIDATION CONTENT INVALID:\n" + message);
            }

            try {
                requestedResourcesMap = mapper.readValue(requestBody, new TypeReference<Map<String, Resource>>() {});
            } catch (Exception e) {
                log.error("Unable to get resources from request body! ", e);
            }

            if (resourceInstanceValidationResult.isSuccess()) {
                coreResources = resourceInstanceValidationResult.getObjectDescription();
                log.info("CoreResources received from SM! Content: " + coreResources);

                AuthorizationResult authorizationResult = authorizationManager.checkIfResourcesBelongToPlatform
                        (RegistryUtils.convertCoreResourcesToResourcesMap(coreResources), resourcesPlatformId);

                if (authorizationResult.isValidated()) {
                    Map<String, ResourcePersistenceResult> persistenceResultMap = makePersistenceOperations(coreResources);
                    prepareContentOfMessage(persistenceResultMap);
                } else {
                    registryResponse.setStatus(400);
                    registryResponse.setMessage(authorizationResult.getMessage());
                }
            } else {
                registryResponse.setStatus(500);
                registryResponse.setMessage("Validation Error. Semantic Manager message: "
                        + resourceInstanceValidationResult.getMessage());
            }
        } catch (Exception e) {
            log.error(e);
            registryResponse.setStatus(500);
            registryResponse.setMessage(e.toString());
        }
        sendRpcResponse();
    }

    /**
     * Performing persistence operations accordingly - saving or modyfying resources in Mongo DB.
     *
     * @param coreResources
     */
    private Map<String, ResourcePersistenceResult> makePersistenceOperations(Map<String, CoreResource> coreResources) {
        Map<String, ResourcePersistenceResult> persistenceOperationResultsMap = new HashMap<>();
        switch (operationType) {
            case CREATION:
                for (String key : coreResources.keySet()) {
                    CoreResource coreResource = coreResources.get(key);
                    try {
                        coreResource.setPolicySpecifier(policiesMap.get(key));
                        ResourcePersistenceResult resourceSavingResult =
                                this.repositoryManager.saveResource(coreResource);
                        persistenceOperationResultsMap.put(key, resourceSavingResult);
                    } catch (Exception e) {
                        log.error("Couldn't get Access Policies for Core Resource. " + e);
                        persistenceOperationResultsMap.put(key,
                                new ResourcePersistenceResult(500, "Couldn't get Access Policies for Core Resource. " + e, coreResource));
                    }
                }
                break;
            case MODIFICATION:
                for (String key : coreResources.keySet()) {
                    CoreResource coreResource = coreResources.get(key);
                    try {
                        coreResource.setPolicySpecifier(policiesMap.get(key));
                        ResourcePersistenceResult resourceModificationResult =
                                this.repositoryManager.modifyResource(coreResources.get(key));
                        persistenceOperationResultsMap.put(key, resourceModificationResult);
                    } catch (Exception e) {
                        log.error("Couldn't get Access Policies for Core Resource. " + e);
                        persistenceOperationResultsMap.put(key,
                                new ResourcePersistenceResult(500, "Couldn't get Access Policies for Core Resource. " + e, coreResource));
                    }
                }
                break;
        }
        for (String key : persistenceOperationResultsMap.keySet()) {
            if (persistenceOperationResultsMap.get(key).getStatus() != 200) {
                this.bulkRequestSuccess = false;
                log.error("One (or more) of resources could not be processed. " +
                        "Check list of response objects for details.");
                registryResponse.setStatus(500);
                registryResponse.setMessage("One (or more) of resources could not be processed. " +
                        "Check list of response objects for details.");
            }
        }
        return persistenceOperationResultsMap;
    }

    /**
     * prepares content of message with bulk save result
     */
    private void prepareContentOfMessage(Map<String, ResourcePersistenceResult> persistenceOperationResultsMap) {
        List<CoreResource> savedCoreResourcesList = new ArrayList<>();
        Map<String, Resource> savedResourcesMap = new HashMap<>();
        if (bulkRequestSuccess) {
            for (String key : persistenceOperationResultsMap.keySet()) {
                CoreResource persistenceResultResource = persistenceOperationResultsMap.get(key).getResource();
                Resource requestedResource = requestedResourcesMap.get(key);

                savedCoreResourcesList.add(persistenceResultResource);

                requestedResource.setId(persistenceResultResource.getId());
                savedResourcesMap.put(key, requestedResource);
            }
            sendFanoutMessage(savedCoreResourcesList);

            log.info("Bulk operation successful! (" + this.operationType.toString() + ")");
            registryResponse.setStatus(200);
            registryResponse.setMessage("Bulk operation successful! (" + this.operationType.toString() + ")");

            try {
                registryResponse.setBody(mapper.writerFor(new TypeReference<Map<String, Resource>>() {
                }).writeValueAsString(savedResourcesMap));
            } catch (JsonProcessingException e) {
                log.error("Could not map list of resource to JSON", e);
            }

        } else {
            for (String key : persistenceOperationResultsMap.keySet()) {
                if (persistenceOperationResultsMap.get(key).getStatus() == 200) {
                    rollback(persistenceOperationResultsMap.get(key).getResource());
                }
            }
            log.error("Bulk request ERROR");
            registryResponse.setStatus(500);
            registryResponse.setMessage("Bulk request ERROR");
        }
    }

    /**
     * Sending RPC response message with list of Resources (with IDs added if process succeed) and status code
     * //odeslanie na RPC core response (z listą resourców z ID'kami jesli zapis sie powiódł)
     */
    private void sendRpcResponse() {
        try {
            registryResponse.setServiceResponse(authorizationManager.generateServiceResponse());
            response = mapper.writeValueAsString(registryResponse);
        } catch (JsonProcessingException e) {
            log.error(e);
        }

        try {
            rabbitManager.sendRPCReplyMessage(rpcConsumer, rpcProperties, rpcEnvelope, response);
            rabbitManager.closeConsumer(this, this.getChannel());
        } catch (IOException e) {
            log.error(e);
        }
    }

    /**
     * Sends a Fanout message with payload constisting of modified resources list and its Platform Id.
     */
    private void sendFanoutMessage(List<CoreResource> savedCoreResourcesList) {
        CoreResourceRegisteredOrModifiedEventPayload payload = new CoreResourceRegisteredOrModifiedEventPayload();
        payload.setResources(savedCoreResourcesList);
        payload.setPlatformId(resourcesPlatformId);
        rabbitManager.sendResourceOperationMessage(payload, operationType);
    }

    /**
     * Type of transaction rollback used for bulk registration, triggered for all successfully saved objects when
     * any of given objects in list did not save successfully in database.
     *
     * @param resource
     */
    private void rollback(CoreResource resource) {
        switch (operationType) {
            case CREATION:
                repositoryManager.removeResource(resource);
                break;
            case MODIFICATION:
                log.error("ROLLBACK NOT IMPLEMENTED!");
                //todo ??
                break;
        }
    }
}