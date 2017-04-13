package eu.h2020.symbiote.messaging;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.gson.JsonSyntaxException;
import com.rabbitmq.client.AMQP;
import com.rabbitmq.client.Channel;
import com.rabbitmq.client.DefaultConsumer;
import com.rabbitmq.client.Envelope;
import eu.h2020.symbiote.core.internal.CoreResourceRegistryResponse;
import eu.h2020.symbiote.core.internal.ResourceInstanceValidationResult;
import eu.h2020.symbiote.core.model.internal.CoreResource;
import eu.h2020.symbiote.core.model.resources.Resource;
import eu.h2020.symbiote.model.CoreResourceSavingResult;
import eu.h2020.symbiote.repository.RepositoryManager;
import eu.h2020.symbiote.utils.RegistryUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.http.HttpStatus;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Created by mateuszl on 13.04.2017.
 */
public class ResourceRdfValidationResponseConsumer extends DefaultConsumer {

    private static Log log = LogFactory.getLog(PlatformCreationRequestConsumer.class);
    DefaultConsumer rpcConsumer;
    AMQP.BasicProperties rpcProperties;
    Envelope rpcEnvelope;
    String rdfDescription;
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
    public ResourceRdfValidationResponseConsumer(String rdfDescription,
                                                 DefaultConsumer rpcConsumer,
                                                 AMQP.BasicProperties rpcProperties,
                                                 Envelope rpcEnvelope,
                                                 Channel channel,
                                                 RepositoryManager repositoryManager,
                                                 RabbitManager rabbitManager) {
        super(channel);
        this.repositoryManager = repositoryManager;
        this.rabbitManager = rabbitManager;
        this.rpcConsumer = rpcConsumer;
        this.rpcEnvelope = rpcEnvelope;
        this.rpcProperties = rpcProperties;
        this.rdfDescription = rdfDescription;
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
        String message = new String(body, "UTF-8");
        List<CoreResource> savedCoreResourcesList = new ArrayList<>();

        boolean bulkRequestSuccess = true;
        CoreResourceRegistryResponse registryResponse = new CoreResourceRegistryResponse();
        CoreResourceSavingResult resourceSavingResult;
        List<CoreResourceSavingResult> resourceSavingResultsList = new ArrayList<>();

        ResourceInstanceValidationResult resourceInstanceValidationResult = new ResourceInstanceValidationResult();
        List<CoreResource> coreResources = new ArrayList<>();

        List<Resource> resources = new ArrayList<>();

        log.info(" [x] Received validation result: '" + message + "'");

        try {
            //otrzymuje i odpakowauje odpowiedz od semantic managera w której body to CoreResourcy
            resourceInstanceValidationResult = mapper.readValue(message, ResourceInstanceValidationResult.class);
        } catch (JsonSyntaxException e) {
            log.error("Unable to get resource validation result from Message body!");
            e.printStackTrace();
        }

        if (resourceInstanceValidationResult.isSuccess()) {
            try {
                //wyciagam z niej resourcy
                coreResources = resourceInstanceValidationResult.getObjectDescription();
            } catch (JsonSyntaxException e) {
                log.error("Unable to get Resources List from semantic response body!");
                e.printStackTrace();
            }

            for (CoreResource resource : coreResources) {
                //zapisuje kazdy z Core resourców
                resourceSavingResult = this.repositoryManager.saveResource(resource);
                resourceSavingResultsList.add(resourceSavingResult);
            }

            for (CoreResourceSavingResult resourceSavingResult1 : resourceSavingResultsList) {
                if (resourceSavingResult1.getStatus() != 200) {
                    rollback(resourceSavingResult1.getResource());
                    bulkRequestSuccess = false;
                    registryResponse.setStatus(500);
                    registryResponse.setMessage("One of objects could not be registered. Check list of response " +
                            "objects for details.");
                }
            }
        } else {
            registryResponse.setStatus(HttpStatus.SC_BAD_REQUEST);
            registryResponse.setMessage("VALIDATION ERROR. Message from SM:" + resourceInstanceValidationResult.getMessage());
        }

        if (bulkRequestSuccess) {
            savedCoreResourcesList = resourceSavingResultsList.stream()
                    .map(CoreResourceSavingResult::getResource)
                    .collect(Collectors.toList());

            //wysłanie całej listy zapisanych resourców
            rabbitManager.sendResourcesCreatedMessage(savedCoreResourcesList);

            registryResponse.setStatus(200);
            registryResponse.setMessage("Rdf registration successful!");

            resources = RegistryUtils.convertCoreResourcesToResources(savedCoreResourcesList);

            //usatwienie zawartosci body odpowiedzi na liste resourców uzupelniona o ID'ki
            registryResponse.setBody(mapper.writerFor(new TypeReference<List<Resource>>() {
            }).writeValueAsString(resources));


        } else {
            registryResponse.setStatus(500);
            registryResponse.setMessage("BULK SAVE ERROR");
        }

        String response = mapper.writeValueAsString(registryResponse);

        //odeslanie na RPC core response (z listą resourców z ID'kami jesl izapis sie powiódł)
        rabbitManager.sendRPCReplyMessage(rpcConsumer, rpcProperties, rpcEnvelope, response);

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