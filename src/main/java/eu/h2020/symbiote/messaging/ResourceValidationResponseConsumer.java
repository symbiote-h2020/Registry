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
import eu.h2020.symbiote.model.ResourceSavingResult;
import eu.h2020.symbiote.repository.RepositoryManager;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Created by mateuszl on 30.03.2017.
 */
public class ResourceValidationResponseConsumer extends DefaultConsumer {

    private static Log log = LogFactory.getLog(PlatformCreationRequestConsumer.class);
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
    public ResourceValidationResponseConsumer(Channel channel,
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
        String message = new String(body, "UTF-8");
        TypeReference listType = new TypeReference<ArrayList<CoreResource>>() {
        };

        boolean bulkRequestSuccess = true;
        String response;
        CoreResourceRegistryResponse registryResponse = new CoreResourceRegistryResponse();
        ResourceSavingResult resourceSavingResult;
        List<ResourceSavingResult> resourceSavingResultsList = new ArrayList<>();

        ResourceInstanceValidationResult resourceInstanceValidationResult = new ResourceInstanceValidationResult();
        List<CoreResource> resources = new ArrayList<>();

        log.info(" [x] Received validation result: '" + message + "'");

        try {
            //otrzymuje i odpakowauje odpowiedz od semantic managera
            resourceInstanceValidationResult = mapper.readValue(message, ResourceInstanceValidationResult.class);
        } catch (JsonSyntaxException e) {
            log.error("Unable to get resource validation result from Message body!");
            e.printStackTrace();
        }

        if (resourceInstanceValidationResult.isSuccess()) {
            try {
                //wyciagam z niej resourcy
                resources = resourceInstanceValidationResult.getObjectDescription();
            } catch (JsonSyntaxException e) {
                log.error("Unable to get Resources List from semantic response body!");
                e.printStackTrace();
            }


            for (CoreResource resource : resources) {
                //zapisuje kazdy z Core resourców
                resourceSavingResult = this.repositoryManager.saveResource(resource);
                resourceSavingResultsList.add(resourceSavingResult);
            }


            registryResponse = new CoreResourceRegistryResponse();

            for (ResourceSavingResult resourceSavingResult1 : resourceSavingResultsList) {
                if (resourceSavingResult1.getStatus() != 200) {
                    rollback(resourceSavingResult1.getResource());
                    bulkRequestSuccess = false;
                    registryResponse.setStatus(500);
                    registryResponse.setMessage("One of objects could not be registered. Check list of response " +
                            "objects for details.");
                }
            }
        } else {
            //todo ustawiam jakis błąd i messydż
            registryResponse.setStatus(500);
            registryResponse.setMessage("VALIDATION ERROR");
        }


        if (bulkRequestSuccess) {
            List<CoreResource> savedCoreResourcesList = resourceSavingResultsList.stream()
                    .map(ResourceSavingResult::getResource)
                    .collect(Collectors.toList());

            //wysłanie całej listy zapisanych resourców
            rabbitManager.sendResourcesCreatedMessage(savedCoreResourcesList);

            registryResponse.setStatus(200);
            registryResponse.setMessage("Bulk registration successful!");
        } else {
            //todo ustawiam jakis błąd i messydż
            registryResponse.setStatus(500);
            registryResponse.setMessage("BULK SAVE ERROR");
        }


        //utworzenie listy otrzymanych resourców z uzupelnionymi ID'kami

        registryResponse.setBody();


        response = mapper.writeValueAsString(registryResponse);

        //odeslanie core respnse z listą resourców z ID'kami
        rabbitManager.sendReplyMessage(CCIconsumer, CCIproperties, CCIenvelope, response);


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