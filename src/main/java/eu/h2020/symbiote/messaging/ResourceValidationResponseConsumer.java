package eu.h2020.symbiote.messaging;

import com.google.gson.Gson;
import com.google.gson.JsonSyntaxException;
import com.google.gson.reflect.TypeToken;
import com.rabbitmq.client.AMQP;
import com.rabbitmq.client.Channel;
import com.rabbitmq.client.DefaultConsumer;
import com.rabbitmq.client.Envelope;
import eu.h2020.symbiote.model.*;
import eu.h2020.symbiote.repository.RepositoryManager;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import java.io.IOException;
import java.lang.reflect.Type;
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
        Gson gson = new Gson();
        String message = new String(body, "UTF-8");
        Type listType = new TypeToken<ArrayList<Resource>>() {
        }.getType();


        RegistryRequest request = null;
        SemanticResponse semanticResponse = new SemanticResponse();
        String response;
        RegistryResponse registryResponse = new RegistryResponse();
        ResourceResponse resourceResponse = new ResourceResponse();
        List<CoreResource> resources = new ArrayList<>();
        List<ResourceResponse> resourceResponseList = new ArrayList<>();


        log.info(" [x] Received validated resources: '" + message + "'");

        try {
            //otrzymuje odpowiedz od semantic managera
            semanticResponse = gson.fromJson(message, SemanticResponse.class);
        } catch (JsonSyntaxException e) {
            log.error("Unable to get semantic response from Message body!");
            e.printStackTrace();
        }

        try {
            //wyciagam z niej resourcy
            resources = gson.fromJson(semanticResponse.getBody(), listType);
        } catch (JsonSyntaxException e) {
            log.error("Unable to get Resources List from semantic response body!");
            e.printStackTrace();
        }


        for (CoreResource resource : resources) {
                resourceResponse = this.repositoryManager.saveResource(resource);
                if (resourceResponse.getStatus() == 200) {
                    rabbitManager.sendResourceCreatedMessage(resourceResponse.getResource());
                }
            resourceResponseList.add(resourceResponse);
        }



    }









}