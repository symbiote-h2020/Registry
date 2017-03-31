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
import eu.h2020.symbiote.utils.RegistryUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import java.io.IOException;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;

/**
 * RabbitMQ Consumer implementation used for Resource Creation actions
 * <p>
 * Created by mateuszl
 */
public class ResourceCreationRequestConsumer extends DefaultConsumer {

    private static Log log = LogFactory.getLog(ResourceCreationRequestConsumer.class);
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
    public ResourceCreationRequestConsumer(Channel channel,
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
        RegistryRequest request = null;
        String response;
        RegistryResponse registryResponse = new RegistryResponse();
        ResourceResponse resourceResponse = new ResourceResponse();
        List<Resource> resources = new ArrayList<>();
        List<ResourceResponse> resourceResponseList = new ArrayList<>();
        String message = new String(body, "UTF-8");

        log.info(" [x] Received resources to create: '" + message + "'");

        Type listType = new TypeToken<ArrayList<Resource>>() {
        }.getType();


        try {
            //otrzymuje request
            request = gson.fromJson(message, RegistryRequest.class);
        } catch (JsonSyntaxException e) {
            log.error("Unable to get RegistryRequest from Message body!");
            e.printStackTrace();
        }


        if (request != null) {
            if (RegistryUtils.checkToken(request.getToken())) {
                //sprawdzam typ requesta
                switch (request.getType()) {
                    case RDF:

                        //wysłanie RDFowejlisty resourców do Sem.Man. i czekanie na odpowiedz consumerem
                        rabbitManager.sendRDFResourceValidationMessage(request.getBody());

                            /*

                        try {
                            semanticResponse = RegistryUtils.getResourcesFromRdf(request.getBody());

                            if (semanticResponse.getStatus() == 200) {
                                resources = gson.fromJson(semanticResponse.getBody(), listType);
                            } else {
                                log.error("Error occured during rdf verification! Semantic Manager info: "
                                        + semanticResponse.getMessage());

                                registryResponse.setStatus(400);
                                registryResponse.setMessage("Error occured during rdf verification. Semantic Manager info: "
                                        + semanticResponse.getMessage());

                            }
                        } catch (JsonSyntaxException e) {
                            log.error("Error occured during getting Resources from Json received from Semantic Manager", e);
                            resourceResponse.setStatus(400);
                            resourceResponse.setMessage("Error occured during getting Resources from Json");
                            resourceResponseList.add(resourceResponse);
                        }
                        */


                    case BASIC:

                        //wysłanie JSONowej listy resourców do Sem.Man. i czekanie na odpowiedz consumerem
                        rabbitManager.sendJSONResourceValidationMessage(request.getBody());

                        /*
                        try {
                            resources = gson.fromJson(message, listType);
                        } catch (JsonSyntaxException e) {
                            log.error("Error occured during getting Resources from Json", e);
                            resourceResponse.setStatus(400);
                            resourceResponse.setMessage("Error occured during getting Resources from Json");
                            resourceResponseList.add(resourceResponse);
                        }
                        */
                }
            } else {
                log.error("Token invalid");
                registryResponse.setStatus(400);
                registryResponse.setMessage("Token invalid");
                rabbitManager.sendReplyMessage(this, properties, envelope, gson.toJson(registryResponse));
            }
        }



    }
}
