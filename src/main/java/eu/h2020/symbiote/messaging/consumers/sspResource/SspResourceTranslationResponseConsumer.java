package eu.h2020.symbiote.messaging.consumers.sspResource;

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.gson.JsonSyntaxException;
import com.rabbitmq.client.AMQP;
import com.rabbitmq.client.Channel;
import com.rabbitmq.client.DefaultConsumer;
import com.rabbitmq.client.Envelope;
import eu.h2020.symbiote.core.internal.CoreResource;
import eu.h2020.symbiote.core.internal.CoreResourceRegisteredOrModifiedEventPayload;
import eu.h2020.symbiote.core.internal.CoreSspResourceRegistryResponse;
import eu.h2020.symbiote.core.internal.ResourceInstanceValidationResult;
import eu.h2020.symbiote.managers.AuthorizationManager;
import eu.h2020.symbiote.managers.RabbitManager;
import eu.h2020.symbiote.managers.RepositoryManager;
import eu.h2020.symbiote.messaging.consumers.resource.ResourceValidationResponseConsumer;
import eu.h2020.symbiote.model.CoreSspResource;
import eu.h2020.symbiote.model.RegistryOperationType;
import eu.h2020.symbiote.model.cim.Resource;
import eu.h2020.symbiote.model.persistenceResults.AuthorizationResult;
import eu.h2020.symbiote.model.persistenceResults.CoreSspResourcePersistenceResult;
import eu.h2020.symbiote.security.accesspolicies.common.IAccessPolicySpecifier;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Created by mateuszl on 30.05.2018.
 */
public class SspResourceTranslationResponseConsumer extends DefaultConsumer {

    private static Log log = LogFactory.getLog(ResourceValidationResponseConsumer.class);
    private Map<String, Resource> receivedResourcesMap;
    private CoreSspResourceRegistryResponse registryResponse;
    private DefaultConsumer rpcConsumer;
    private AMQP.BasicProperties rpcProperties;
    private Envelope rpcEnvelope;
    private RepositoryManager repositoryManager;
    private RabbitManager rabbitManager;
    private String sDevId;
    private String sspId;
    private RegistryOperationType operationType;
    private boolean bulkRequestSuccess = true;
    private ObjectMapper mapper;
    private String response;
    private AuthorizationManager authorizationManager;
    private Map<String, IAccessPolicySpecifier> policiesMap;

    /**
     * Constructs a new instance and records its association to the passed-in channel.
     * Managers beans passed as parameters because of lack of possibility to inject it to consumer.
     *
     * @param channel           the channel to which this consumer is attached
     * @param rabbitManager     rabbit manager bean passed for access to messages manager
     * @param repositoryManager repository manager bean passed for persistence actions
     */
    public SspResourceTranslationResponseConsumer(DefaultConsumer rpcConsumer,
                                                  AMQP.BasicProperties rpcProperties,
                                                  Envelope rpcEnvelope,
                                                  Channel channel,
                                                  RepositoryManager repositoryManager,
                                                  RabbitManager rabbitManager,
                                                  String sDevId,
                                                  String sspId,
                                                  RegistryOperationType operationType,
                                                  AuthorizationManager authorizationManager,
                                                  Map<String, IAccessPolicySpecifier> policiesMap,
                                                  Map<String, Resource> receivedResourcesMap) {
        super(channel);
        this.repositoryManager = repositoryManager;
        this.rabbitManager = rabbitManager;
        this.authorizationManager = authorizationManager;
        this.rpcConsumer = rpcConsumer;
        this.rpcEnvelope = rpcEnvelope;
        this.rpcProperties = rpcProperties;
        this.sDevId = sDevId;
        this.sspId = sspId;
        this.operationType = operationType;
        this.policiesMap = policiesMap;
        this.receivedResourcesMap = receivedResourcesMap;

        this.mapper = new ObjectMapper();
        this.registryResponse = new CoreSspResourceRegistryResponse();
        response = "";
        log.info("Resource Validation Response Consumer created - waiting for answers from SM!");
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

        String message = new String(body, "UTF-8");
        ResourceInstanceValidationResult resourceInstanceValidationResult;
        Map<String, CoreResource> coreResourcesFromSM;

        log.info("[x] Received SspResource translation result: '" + message + "'");

        try {
            resourceInstanceValidationResult = readMessageFromSemanticManager(message);

            if (resourceInstanceValidationResult != null && resourceInstanceValidationResult.isSuccess()) {
                coreResourcesFromSM = resourceInstanceValidationResult.getObjectDescription();
                log.info("CoreResources received from SM! Content: " + coreResourcesFromSM);

                AuthorizationResult authorizationResult =
                        authorizationManager.checkIfCoreResourcesBelongToSdev(coreResourcesFromSM, sDevId); //// TODO: 01.06.2018 MOCKED!!

                if (authorizationResult.isValidated()) {
                    Map<String, CoreSspResourcePersistenceResult> persistenceResultMap =
                            makePersistenceOperations(coreResourcesFromSM, sDevId);
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

    private ResourceInstanceValidationResult readMessageFromSemanticManager(String message) throws IOException {
        try {
            //receive and read message from Semantic Manager
            return mapper.readValue(message, ResourceInstanceValidationResult.class);
        } catch (JsonSyntaxException | JsonMappingException | JsonParseException e) {
            log.error("Unable to get resource validation result from Message body!", e);
            registryResponse.setStatus(500);
            registryResponse.setMessage("VALIDATION CONTENT INVALID:\n" + message);
            return null;
        }
    }

    /**
     * Performing persistence operations accordingly - saving or modyfying resources in Mongo DB.
     *
     * @param coreResourcesFromSM
     */
    private Map<String, CoreSspResourcePersistenceResult> makePersistenceOperations(Map<String, CoreResource> coreResourcesFromSM,
                                                                                    String sDevId) {

        HashMap<String, CoreSspResource> coreSspResources = convertCoreResourcesToCoreSspResources(coreResourcesFromSM, sDevId);

        //List to make sure
        coreSspResources.values().stream().forEach( val -> System.out.println(val!=null?"After convert resource id is: " + val.getId():"After convert resource val is null"));

        Map<String, CoreSspResourcePersistenceResult> persistenceOperationResultsMap = new HashMap<>();

        switch (operationType) {

            case CREATION:
                for (String key : coreSspResources.keySet()) {
                    CoreSspResource sspResource = coreSspResources.get(key);
                    try {
                        sspResource.getResource().setPolicySpecifier(policiesMap.get(key));
                    } catch (Exception e) {
                        log.error("Couldn't get Access Policies for Core Ssp Resource. " + e);
                        persistenceOperationResultsMap.put(key,
                                new CoreSspResourcePersistenceResult(500,
                                        "Couldn't get Access Policies for Core Ssp Resource. " + e,
                                        sspResource));
                    }
                    CoreSspResourcePersistenceResult resourceSavingResult =
                            this.repositoryManager.saveCoreSspResource(sspResource);
                    persistenceOperationResultsMap.put(key, resourceSavingResult);
                }
                break;

            case MODIFICATION:
                for (String key : coreSspResources.keySet()) {
                    CoreSspResource coreResource = coreSspResources.get(key);
                    try {
                        coreResource.getResource().setPolicySpecifier(policiesMap.get(key));
                    } catch (Exception e) {
                        log.error("Couldn't get Access Policies for Core Resource. " + e);
                        persistenceOperationResultsMap.put(key,
                                new CoreSspResourcePersistenceResult(500,
                                        "Couldn't get Access Policies for Core Resource. " + e,
                                        coreResource));
                    }
                    CoreSspResourcePersistenceResult resourceModificationResult =
                            this.repositoryManager.modifyCoreSspResource(coreResource);
                    persistenceOperationResultsMap.put(key, resourceModificationResult);
                }
                break;
        }
        return persistenceOperationResultsMap;
    }

    private HashMap<String, CoreSspResource> convertCoreResourcesToCoreSspResources(Map<String, CoreResource> coreResourcesFromSM, String sDevId) {
        HashMap<String, CoreSspResource> coreSspResources = new HashMap<>();

        for (String s : coreResourcesFromSM.keySet()) {
            CoreResource coreResource = coreResourcesFromSM.get(s);
            coreSspResources.put(s, convertCoreResourceToCoreSspResource(coreResource, sDevId));
        }
        return coreSspResources;
    }

    private void checkIfBulkOperationSucceded(Map<String, CoreSspResourcePersistenceResult> persistenceOperationResultsMap) {
        if (persistenceOperationResultsMap.keySet().stream()
                .anyMatch(key -> persistenceOperationResultsMap.get(key).getStatus() != 200)) {
            this.bulkRequestSuccess = false;
        }
    }

    private CoreSspResource convertCoreResourceToCoreSspResource(CoreResource coreResource, String sDevId) {
        return new CoreSspResource(coreResource.getId(),sDevId, coreResource);
    }

    /**
     * prepares content of message with bulk save result
     */
    private void prepareContentOfMessage(Map<String, CoreSspResourcePersistenceResult> persistenceOperationResultsMap) {
        List<CoreResource> savedCoreSspResourcesList = new ArrayList<>(); //for fanout information sending
        Map<String, Resource> acceptedResourcesMapForReply = new HashMap<>(); //for reply message sending
        checkIfBulkOperationSucceded(persistenceOperationResultsMap);

        if (bulkRequestSuccess) {                                                                                       //if the whole bulk request was performed as planned, each of the resources is added to Map and List for massaging
            for (String key : persistenceOperationResultsMap.keySet()) {

                CoreResource persistenceResultSspResource = persistenceOperationResultsMap.get(key).getCoreSspResource().getResource();
                savedCoreSspResourcesList.add(persistenceResultSspResource);

                Resource receivedResource = receivedResourcesMap.get(key);
                receivedResource.setId(persistenceResultSspResource.getId());
                acceptedResourcesMapForReply.put(key, receivedResource);
            }

            log.info("Bulk operation successful! (" + this.operationType.toString() + ")");
            registryResponse.setStatus(200);
            registryResponse.setMessage("Bulk operation successful! (" + this.operationType.toString() + ")");
            registryResponse.setBody(acceptedResourcesMapForReply);

            sendFanoutMessage(savedCoreSspResourcesList);                                                               //all resources saved successfully are sent by Fanout in a form of a CoreSspResources

        } else {                                                                                                        //if bulk request was not performed successfully, saved resources are rolled back
            persistenceOperationResultsMap.keySet().stream()
                    .filter(key -> persistenceOperationResultsMap.get(key).getStatus() == 200)
                    .forEach(key -> rollback(persistenceOperationResultsMap.get(key).getCoreSspResource()));

            log.error("One (or more) of resources could not be processed. \nCheck list of response objects for details.");
            registryResponse.setStatus(500);
            registryResponse.setMessage("One (or more) of resources could not be processed. \nCheck list of response objects for details.");
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
            this.rabbitManager.sendRPCReplyMessage(rpcConsumer, rpcProperties, rpcEnvelope, response);
            this.rabbitManager.closeConsumer(this);
        } catch (IOException e) {
            log.error(e);
        }
    }

    /**
     * Sends a Fanout message with payload constisting of modified resources list.
     */
    private void sendFanoutMessage(List<CoreResource> savedCoreSspResourcesList) {
        CoreResourceRegisteredOrModifiedEventPayload payload = new CoreResourceRegisteredOrModifiedEventPayload();
        payload.setResources(savedCoreSspResourcesList);
        payload.setPlatformId(sspId);
        this.rabbitManager.sendResourceOperationMessage(payload, operationType);
    }

    /**
     * Type of transaction rollback used for bulk registration, triggered for all successfully saved objects when
     * any of given objects in list did not save successfully in database.
     *
     * @param resource
     */
    private void rollback(CoreSspResource resource) {
        switch (operationType) {
            case CREATION:
                repositoryManager.removeCoreSspResource(resource.getId());
                break;
            case MODIFICATION:
                log.error("ROLLBACK NOT IMPLEMENTED!");
                //todo ??
                break;
        }
    }
}