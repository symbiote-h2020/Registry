package eu.h2020.symbiote.messaging.consumers.pim;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.gson.JsonSyntaxException;
import com.rabbitmq.client.AMQP;
import com.rabbitmq.client.Channel;
import com.rabbitmq.client.DefaultConsumer;
import com.rabbitmq.client.Envelope;
import eu.h2020.symbiote.core.cci.InformationModelResponse;
import eu.h2020.symbiote.core.internal.CoreResourceRegisteredOrModifiedEventPayload;
import eu.h2020.symbiote.core.internal.DescriptionType;
import eu.h2020.symbiote.core.internal.ResourceInstanceValidationResult;
import eu.h2020.symbiote.core.model.internal.CoreResource;
import eu.h2020.symbiote.core.model.resources.Resource;
import eu.h2020.symbiote.managers.AuthorizationManager;
import eu.h2020.symbiote.managers.RepositoryManager;
import eu.h2020.symbiote.messaging.RabbitManager;
import eu.h2020.symbiote.model.AuthorizationResult;
import eu.h2020.symbiote.model.RegistryOperationType;
import eu.h2020.symbiote.model.ResourcePersistenceResult;
import eu.h2020.symbiote.utils.RegistryUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Created by mateuszl on 16.08.2017.
 */
public class InformationModelValidationResponseConsumer extends DefaultConsumer {


    //MOCKED !!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!

    private static Log log = LogFactory.getLog(InformationModelValidationResponseConsumer.class);
    private InformationModelResponse informationModelResponse;
    private DefaultConsumer rpcConsumer;
    private AMQP.BasicProperties rpcProperties;
    private Envelope rpcEnvelope;
    private RepositoryManager repositoryManager;
    private RabbitManager rabbitManager;
    private RegistryOperationType operationType;
    private boolean bulkRequestSuccess = true;
    private ObjectMapper mapper;
    private String response;
    private AuthorizationManager authorizationManager;

    /**
     * Constructs a new instance and records its association to the passed-in channel.
     * Managers beans passed as parameters because of lack of possibility to inject it to consumer.
     *
     * @param channel           the channel to which this consumer is attached
     * @param rabbitManager     rabbit manager bean passed for access to messages manager
     * @param repositoryManager repository manager bean passed for persistence actions
     */
    public InformationModelValidationResponseConsumer(DefaultConsumer rpcConsumer,
                                              AMQP.BasicProperties rpcProperties,
                                              Envelope rpcEnvelope,
                                              Channel channel,
                                              RepositoryManager repositoryManager,
                                              RabbitManager rabbitManager,
                                              RegistryOperationType operationType,
                                              AuthorizationManager authorizationManager) {
        super(channel);
        this.repositoryManager = repositoryManager;
        this.rabbitManager = rabbitManager;
        this.rpcConsumer = rpcConsumer;
        this.rpcEnvelope = rpcEnvelope;
        this.rpcProperties = rpcProperties;
        this.operationType = operationType;
        this.authorizationManager = authorizationManager;
        this.mapper = new ObjectMapper();
        this.informationModelResponse = new InformationModelResponse();
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

        log.info("[x] Received validation result: '" + message + "'");

        try {
            //receive and read message from Semantic Manager
            resourceInstanceValidationResult = mapper.readValue(message, ResourceInstanceValidationResult.class);
        } catch (JsonSyntaxException | JsonMappingException e) {
            log.error("Unable to get resource validation result from Message body!", e);
            informationModelResponse.setStatus(500);
            informationModelResponse.setMessage("VALIDATION CONTENT INVALID:\n" + message);
        }

        if (resourceInstanceValidationResult.isSuccess()) {
            coreResources = resourceInstanceValidationResult.getObjectDescription();
            log.info("CoreResources received from SM! Content: " + coreResources);

            /* for future Security updates
            AuthorizationResult authorizationResult = authorizationManager.checkToken(token);

            if (authorizationResult.isValidated()) {
                Map<String, ResourcePersistenceResult> persistenceOperationResultsList = makePersistenceOperations(coreResources);
                prepareContentOfMessage(persistenceOperationResultsList);
            } else {
                informationModelResponse.setStatus(400);
                informationModelResponse.setMessage(authorizationResult.getMessage());
            }
            */

            Map<String, ResourcePersistenceResult> stringResourcePersistenceResultMap = makePersistenceOperations(coreResources);

        } else {
            informationModelResponse.setStatus(500);
            informationModelResponse.setMessage("Validation Error. Semantic Manager message: "
                    + resourceInstanceValidationResult.getMessage());
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
                    ResourcePersistenceResult resourceSavingResult =
                            this.repositoryManager.saveResource(coreResources.get(key));
                    persistenceOperationResultsMap.put(key, resourceSavingResult);
                }
                break;
            case MODIFICATION:
                for (String key : coreResources.keySet()) {
                    ResourcePersistenceResult resourceModificationResult =
                            this.repositoryManager.modifyResource(coreResources.get(key));
                    persistenceOperationResultsMap.put(key, resourceModificationResult);
                }
                break;
        }
        for (String key : persistenceOperationResultsMap.keySet()) {
            if (persistenceOperationResultsMap.get(key).getStatus() != 200) {
                this.bulkRequestSuccess = false;
                log.error("One (or more) of resources could not be processed. " +
                        "Check list of response objects for details.");
                informationModelResponse.setStatus(500);
                informationModelResponse.setMessage("One (or more) of resources could not be processed. " +
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
                ResourcePersistenceResult resourcePersistenceResult = persistenceOperationResultsMap.get(key);
                savedCoreResourcesList.add(resourcePersistenceResult.getResource());
                savedResourcesMap.put(key, RegistryUtils.convertCoreResourceToResource(resourcePersistenceResult.getResource()));
            }
            sendFanoutMessage(savedCoreResourcesList);

            log.info("Bulk operation successful! (" + this.operationType.toString() + ")");
            informationModelResponse.setStatus(200);
            informationModelResponse.setMessage("Bulk operation successful! (" + this.operationType.toString() + ")");

            try {
                informationModelResponse.setBody(mapper.writerFor(new TypeReference<Map<String, Resource>>() {
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
            informationModelResponse.setStatus(500);
            informationModelResponse.setMessage("Bulk request ERROR");
        }
    }

    /**
     * Sending RPC response message with list of Resources (with IDs added if process succeed) and status code
     * //odeslanie na RPC core response (z listą resourców z ID'kami jesli zapis sie powiódł)
     */
    private void sendRpcResponse() {
        try {
            response = mapper.writeValueAsString(informationModelResponse);
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

    //MOCKED !!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!
}
