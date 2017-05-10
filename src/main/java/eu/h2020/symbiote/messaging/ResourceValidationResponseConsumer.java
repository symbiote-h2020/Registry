package eu.h2020.symbiote.messaging;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.gson.JsonSyntaxException;
import com.rabbitmq.client.AMQP;
import com.rabbitmq.client.Channel;
import com.rabbitmq.client.DefaultConsumer;
import com.rabbitmq.client.Envelope;
import eu.h2020.symbiote.core.internal.CoreResourceRegisteredOrModifiedEventPayload;
import eu.h2020.symbiote.core.internal.CoreResourceRegistryResponse;
import eu.h2020.symbiote.core.internal.DescriptionType;
import eu.h2020.symbiote.core.internal.ResourceInstanceValidationResult;
import eu.h2020.symbiote.core.model.internal.CoreResource;
import eu.h2020.symbiote.core.model.resources.Resource;
import eu.h2020.symbiote.model.RegistryPersistenceResult;
import eu.h2020.symbiote.model.RegistryOperationType;
import eu.h2020.symbiote.repository.RepositoryManager;
import eu.h2020.symbiote.utils.RegistryUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * RPC Consumer waiting for messages from Semantic Manager. Acts accordingly to received validation/translation results.
 * Created when creation/modification of Resource action is triggered.
 *
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
    private List<CoreResource> savedCoreResourcesList;
    private List<RegistryPersistenceResult> persistenceOperationResultsList;
    private ObjectMapper mapper;
    private DescriptionType descriptionType;
    private String response;

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
                                              DescriptionType descriptionType) {
        super(channel);
        this.repositoryManager = repositoryManager;
        this.rabbitManager = rabbitManager;
        this.rpcConsumer = rpcConsumer;
        this.rpcEnvelope = rpcEnvelope;
        this.rpcProperties = rpcProperties;
        this.resourcesPlatformId = resourcesPlatformId;
        this.operationType = operationType;
        this.descriptionType = descriptionType;
        this.persistenceOperationResultsList = new ArrayList<>();
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
        List<CoreResource> coreResources = new ArrayList<>();
        registryResponse.setDescriptionType(descriptionType);

        log.info("[x] Received '" + descriptionType + "' validation result: '" + message + "'");

        try {
            //receive and read message from Semantic Manager
            resourceInstanceValidationResult = mapper.readValue(message, ResourceInstanceValidationResult.class);
        } catch (JsonSyntaxException e) {
            log.error("Unable to get resource validation result from Message body!", e);
            registryResponse.setStatus(500);
            registryResponse.setMessage("VALIDATION CONTENT INVALID:\n" + message);
        }

        if (resourceInstanceValidationResult.isSuccess()) {
            try {
                coreResources = resourceInstanceValidationResult.getObjectDescription();
                log.info("CoreResources received from SM! Content: " + coreResources);
            } catch (JsonSyntaxException e) {
                log.error("Unable to get Resources List from semantic response body!", e);
            }
        } else {
            registryResponse.setStatus(500);
            registryResponse.setMessage("Validation Error. Semantic Manager message: "
                    + resourceInstanceValidationResult.getMessage());
        }

        if (checkPlatformAndInterworkingServices(coreResources)) {
            log.info("Checking OK...");
            makePersistenceOperations(coreResources);
            prepareContentOfMessage();
        } else {
            registryResponse.setStatus(500);
            registryResponse.setMessage("There is no such platform or it has no Interworking Service with given URL");
        }

        sendRpcResponse();
    }

    private boolean checkPlatformAndInterworkingServices(List<CoreResource> coreResources) {
        log.info("Checking Platform And Interworking Services...");

        /* TODO commented for tests
        for (CoreResource resource : coreResources) {
            //normalization of Interworking Services Urls
            if (resource.getInterworkingServiceURL().trim().charAt(resource.getInterworkingServiceURL().length() - 1)
                    != "/".charAt(0)) {
                resource.setInterworkingServiceURL(resource.getInterworkingServiceURL().trim() + "/");
            }
            //performing check of given platform ID and IS URL
            if (!repositoryManager.checkIfPlatformExistsAndHasInterworkingServiceUrl
                    (resourcesPlatformId, resource.getInterworkingServiceURL())) {
                return false;
            }
        }
        */

        return true;
    }

    /**
     * Performing persistence operations accordingly - saving or modyfying resources in Mongo DB.
     *
     * @param coreResources
     */
    private void makePersistenceOperations(List<CoreResource> coreResources) {
        switch (operationType) {
            case CREATION:
                for (CoreResource resource : coreResources) {
                    RegistryPersistenceResult resourceSavingResult =
                            this.repositoryManager.saveResource(resource);
                    persistenceOperationResultsList.add(resourceSavingResult);
                }
                break;
            case MODIFICATION:
                for (CoreResource resource : coreResources) {
                    RegistryPersistenceResult resourceModificationResult =
                            this.repositoryManager.modifyResource(resource);
                    persistenceOperationResultsList.add(resourceModificationResult);
                }
                break;
        }

        for (RegistryPersistenceResult persistenceResult : persistenceOperationResultsList) {
            if (persistenceResult.getStatus() != 200) {
                this.bulkRequestSuccess = false;
                registryResponse.setStatus(500);
                registryResponse.setMessage("One (or more) of resources could not be processed. " +
                        "Check list of response objects for details.");
            }
        }
    }

    /**
     * prepares content of message with bulk save result
     */
    private void prepareContentOfMessage() {
        if (bulkRequestSuccess) {
            savedCoreResourcesList = persistenceOperationResultsList.stream()
                    .map(RegistryPersistenceResult::getResource)
                    .collect(Collectors.toList());

            sendFanoutMessage();

            registryResponse.setStatus(200);
            registryResponse.setMessage("Bulk operation successful! (" + this.operationType.toString() + ")");

            List<Resource> resources = RegistryUtils.convertCoreResourcesToResources(savedCoreResourcesList);

            try {
                registryResponse.setBody(mapper.writerFor(new TypeReference<List<Resource>>() {
                }).writeValueAsString(resources));
            } catch (JsonProcessingException e) {
                log.error("Could not map list of resource to JSON", e);
            }

        } else {
            for (RegistryPersistenceResult persistenceResult : persistenceOperationResultsList) {
                if (persistenceResult.getStatus() == 200) {
                    rollback(persistenceResult.getResource());
                }
            }
            registryResponse.setStatus(500);
            registryResponse.setMessage("BULK SAVE ERROR");
        }
    }

    /**
     * Sending RPC response message with list of Resources (with IDs added if process succeed) and status code
     //odeslanie na RPC core response (z listą resourców z ID'kami jesli zapis sie powiódł)
     */
    private void sendRpcResponse() {
        try {
            response = mapper.writeValueAsString(registryResponse);
        } catch (JsonProcessingException e) {
            log.error(e);
        }

        try {
            rabbitManager.sendRPCReplyMessage(rpcConsumer, rpcProperties, rpcEnvelope, response);
        } catch (IOException e) {
            log.error(e);
        }
    }

    /**
     * Sends a Fanout message with payload constisting of modified resources list and its Platform Id.
     */
    private void sendFanoutMessage() {
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
                //todo ??
                break;
        }
    }
}