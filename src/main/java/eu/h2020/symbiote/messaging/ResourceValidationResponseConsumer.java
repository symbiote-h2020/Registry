package eu.h2020.symbiote.messaging;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.gson.JsonSyntaxException;
import com.rabbitmq.client.AMQP;
import com.rabbitmq.client.Channel;
import com.rabbitmq.client.DefaultConsumer;
import com.rabbitmq.client.Envelope;
import eu.h2020.symbiote.core.internal.CoreResourceRegistryRequest;
import eu.h2020.symbiote.core.internal.ResourceInstanceValidationResult;
import eu.h2020.symbiote.core.model.internal.CoreResource;
import eu.h2020.symbiote.model.*;
import eu.h2020.symbiote.repository.RepositoryManager;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

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
        TypeReference listType = new TypeReference<ArrayList<Resource>>(){};

        boolean bulkRequestSuccess = true;
        SemanticResponse semanticResponse = new SemanticResponse();
        String response;
        RegistryResponse registryResponse;
        ResourceResponse resourceResponse = new ResourceResponse();
        List<CoreResource> resources = new ArrayList<>();
        List<ResourceResponse> resourceResponseList = new ArrayList<>();


        ResourceInstanceValidationResult resourceInstanceValidationResult = new ResourceInstanceValidationResult();
        List<CoreResource> lista = resourceInstanceValidationResult.getObjectDescription();

        log.info(" [x] Received validated resources: '" + message + "'");

        try {
            //otrzymuje odpowiedz od semantic managera
            semanticResponse = mapper.readValue(message, SemanticResponse.class);
        } catch (JsonSyntaxException e) {
            log.error("Unable to get semantic response from Message body!");
            e.printStackTrace();
        }

        try {
            //wyciagam z niej resourcy
            resources = mapper.readValue(semanticResponse.getBody(), listType);
        } catch (JsonSyntaxException e) {
            log.error("Unable to get Resources List from semantic response body!");
            e.printStackTrace();
        }


        for (CoreResource resource : resources) {
            //zapisuje kazdy z Core resourców
            resourceResponse = this.repositoryManager.saveResource(resource);
            resourceResponseList.add(resourceResponse);
        }


        registryResponse = new RegistryResponse();

        for (ResourceResponse resourceResponse1 : resourceResponseList){
            if (resourceResponse1.getStatus() != 200) {
                rollback(resourceResponse1.getResource());
                bulkRequestSuccess = false;
                registryResponse.setStatus(500);
                registryResponse.setMessage("One of objects could not be registered. Check list of response " +
                        "objects for details.");
            }
        }



        if (bulkRequestSuccess) {

            //wysłanie całej listy zapisanych resourców
            rabbitManager.sendResourcesCreatedMessage(resourceResponseList);

            registryResponse.setStatus(200);
            registryResponse.setMessage("Bulk registration successful!");
        }

        responseBody = gson.toJson(platformResponseList);
        registryResponse.setBody(responseBody);
        response = gson.toJson(registryResponse);
        rabbitManager.sendReplyMessage(this, properties, envelope, response);


    }



    /** Form of transaction rollback used for bulk registration, triggered for all succesfully saved objects when
     * any of given objects in list did not save successfully in database.
     *
     * @param platform
     */
    private void rollback(CoreResource resource){
        repositoryManager.removeResource(resource);
    }




}