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
import eu.h2020.symbiote.core.internal.ResourceInstanceValidationResult;
import eu.h2020.symbiote.core.model.internal.CoreResource;
import eu.h2020.symbiote.core.model.resources.Resource;
import eu.h2020.symbiote.model.CoreResourcePersistenceOperationResult;
import eu.h2020.symbiote.model.ResourceOperationType;
import eu.h2020.symbiote.repository.RepositoryManager;
import eu.h2020.symbiote.utils.RegistryUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Created by mateuszl on 30.03.2017.
 */
public class ResourceJsonValidationResponseConsumer extends DefaultConsumer {

    private static Log log = LogFactory.getLog(PlatformCreationRequestConsumer.class);
    private DefaultConsumer rpcConsumer;
    private AMQP.BasicProperties rpcProperties;
    private Envelope rpcEnvelope;
    private RepositoryManager repositoryManager;
    private RabbitManager rabbitManager;
    private String resourcesPlatformId;
    private ResourceOperationType operationType;

    private boolean bulkRequestSuccess = true;
    private List<CoreResource> savedCoreResourcesList;
    private List<CoreResourcePersistenceOperationResult> persistenceOperationResultsList;
    private ObjectMapper mapper;

    /**
     * Constructs a new instance and records its association to the passed-in channel.
     * Managers beans passed as parameters because of lack of possibility to inject it to consumer.
     *
     * @param channel           the channel to which this consumer is attached
     * @param rabbitManager     rabbit manager bean passed for access to messages manager
     * @param repositoryManager repository manager bean passed for persistence actions
     */
    public ResourceJsonValidationResponseConsumer(DefaultConsumer rpcConsumer,
                                                  AMQP.BasicProperties rpcProperties,
                                                  Envelope rpcEnvelope,
                                                  Channel channel,
                                                  RepositoryManager repositoryManager,
                                                  RabbitManager rabbitManager,
                                                  String resourcesPlatformId,
                                                  ResourceOperationType operationType) {
        super(channel);
        this.repositoryManager = repositoryManager;
        this.rabbitManager = rabbitManager;
        this.rpcConsumer = rpcConsumer;
        this.rpcEnvelope = rpcEnvelope;
        this.rpcProperties = rpcProperties;
        this.resourcesPlatformId = resourcesPlatformId;
        this.operationType = operationType;

        this.persistenceOperationResultsList = new ArrayList<>();
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

        String message = new String(body, "UTF-8");
        CoreResourceRegistryResponse registryResponse = new CoreResourceRegistryResponse();
        ResourceInstanceValidationResult resourceInstanceValidationResult = new ResourceInstanceValidationResult();
        List<CoreResource> coreResources = new ArrayList<>();

        log.info("[x] Received validation result: '" + message + "'");

        try {
            //otrzymuje i odpakowauje odpowiedz od semantic managera
            resourceInstanceValidationResult = mapper.readValue(message, ResourceInstanceValidationResult.class);
        } catch (JsonSyntaxException e) {
            log.error("Unable to get resource validation result from Message body!");
            e.printStackTrace();
            registryResponse.setStatus(500);
            registryResponse.setMessage("VALIDATION CONTENT CORRUPTED:\n" + message);
        }

        if (resourceInstanceValidationResult.isSuccess()) {
            try {
                //wyciagam z niej CoreResourcy
                coreResources = resourceInstanceValidationResult.getObjectDescription();
            } catch (JsonSyntaxException e) {
                log.error("Unable to get Resources List from semantic response body!");
                e.printStackTrace();
            }
        } else {
            registryResponse.setStatus(500);
            registryResponse.setMessage("VALIDATION ERROR");
        }

        registryResponse = makePersistenceOperations(coreResources, registryResponse);

        prepareContentAndSendMessage(registryResponse);
    }

    private CoreResourceRegistryResponse makePersistenceOperations(List<CoreResource> coreResources,
                                                                   CoreResourceRegistryResponse registryResponse) {
        switch (operationType) {
            case CREATION:

                for (CoreResource resource : coreResources) {
                    //zapisuje kazdy z Core resourców
                    CoreResourcePersistenceOperationResult resourceSavingResult =
                            this.repositoryManager.saveResource(resource);
                    persistenceOperationResultsList.add(resourceSavingResult);
                }

                break;
            case MODIFICATION:

                for (CoreResource resource : coreResources) {
                    //zapisuje kazdy z Core resourców
                    CoreResourcePersistenceOperationResult resourceSavingResult =
                            this.repositoryManager.modifyResource(resource);
                    persistenceOperationResultsList.add(resourceSavingResult);
                }

                break;
        }

        for (CoreResourcePersistenceOperationResult persistenceResult : persistenceOperationResultsList) {
            if (persistenceResult.getStatus() != 200) {
                rollback(persistenceResult.getResource());
                this.bulkRequestSuccess = false;
                registryResponse.setStatus(500);
                registryResponse.setMessage("One of objects could not be registered. Check list of response " +
                        "objects for details.");
            }
        }

        return registryResponse;
    }

    private void prepareContentAndSendMessage(CoreResourceRegistryResponse registryResponse) {

        if (bulkRequestSuccess) {
            savedCoreResourcesList = persistenceOperationResultsList.stream()
                    .map(CoreResourcePersistenceOperationResult::getResource)
                    .collect(Collectors.toList());

            CoreResourceRegisteredOrModifiedEventPayload payload =
                    new CoreResourceRegisteredOrModifiedEventPayload();
            payload.setResources(savedCoreResourcesList);
            payload.setPlatformId(resourcesPlatformId);
            sendMessage(payload);

            registryResponse.setStatus(200);
            registryResponse.setMessage("Bulk operation successful! (" + this.operationType.toString() + ")");

            List<Resource> resources = RegistryUtils.convertCoreResourcesToResources(savedCoreResourcesList);

            //usatwienie zawartosci body odpowiedzi na liste resourców uzupelniona o ID'ki
            try {
                registryResponse.setBody(mapper.writerFor(new TypeReference<List<Resource>>() {
                }).writeValueAsString(resources));
            } catch (JsonProcessingException e) {
                e.printStackTrace();
            }

        } else {
            registryResponse.setStatus(500);
            registryResponse.setMessage("BULK SAVE ERROR");
        }

        String response = "";
        try {
            response = mapper.writeValueAsString(registryResponse);
        } catch (JsonProcessingException e) {
            e.printStackTrace();
        }

        //odeslanie na RPC core response (z listą resourców z ID'kami jesli zapis sie powiódł)
        try {
            rabbitManager.sendRPCReplyMessage(rpcConsumer, rpcProperties, rpcEnvelope, response);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }


    private void sendMessage(CoreResourceRegisteredOrModifiedEventPayload payload) {
        switch (operationType) {
            case CREATION:

                //wysłanie całej listy zapisanych resourców
                rabbitManager.sendResourcesCreatedMessage(payload);

                break;
            case MODIFICATION:

                //wysłanie całej listy zmodyfikowanych resourców
                rabbitManager.sendResourcesModifiedMessage(payload);

                break;
        }

    }

    /**
     * Form of transaction rollback used for bulk registration, triggered for all succesfully saved objects when
     * any of given objects in list did not save successfully in database.
     *
     * @param resource
     */
    private void rollback(CoreResource resource) {
        repositoryManager.removeResource(resource);
    }


}