package eu.h2020.symbiote.messaging;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.gson.JsonSyntaxException;
import com.google.gson.reflect.TypeToken;
import com.rabbitmq.client.AMQP;
import com.rabbitmq.client.Channel;
import com.rabbitmq.client.DefaultConsumer;
import com.rabbitmq.client.Envelope;
import eu.h2020.symbiote.core.internal.CoreResourceRegistryRequest;
import eu.h2020.symbiote.model.RegistryResponse;
import eu.h2020.symbiote.model.Resource;
import eu.h2020.symbiote.repository.RepositoryManager;
import eu.h2020.symbiote.utils.RegistryUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import java.io.IOException;
import java.lang.reflect.Type;
import java.util.ArrayList;

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
        ObjectMapper mapper = new ObjectMapper();
        CoreResourceRegistryRequest request = null;
        RegistryResponse registryResponse = new RegistryResponse();
        String message = new String(body, "UTF-8");

        log.info(" [x] Received resources to create (CoreResourceRegistryRequest): '" + message + "'");

        Type listType = new TypeToken<ArrayList<Resource>>() {
        }.getType();

        try {
            //otrzymuje request od CCI
            request = mapper.readValue(message, CoreResourceRegistryRequest.class);
        } catch (JsonSyntaxException e) {
            log.error("Unable to get RegistryRequest from Message body!");
            e.printStackTrace();
        }

        if (request != null) {
            if (RegistryUtils.checkToken(request.getToken())) {
                //sprawdzam typ requesta
                switch (request.getDescriptionType()) {
                    case RDF:

                        log.info("Message to Semantic Manager Sent. Content Type : RDF. Request: " + request.getBody());
                        //wysłanie RDFowej listy resourców do Sem.Man. i czekanie na odpowiedz consumerem
                        rabbitManager.sendRdfResourceValidationRpcMessage(this, properties, envelope, request.getBody());

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

                        log.info("Message to Semantic Manager Sent. Content Type : BASIC. Request: " + request.getBody());
                        //wysłanie JSONowej listy resourców do Sem.Man. i czekanie na odpowiedz consumerem
                        rabbitManager.sendJsonResourceValidationRpcMessage(this, properties, envelope, request.getBody());

                        /*


                        RPCINFO: this, properties, envelope,




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
                rabbitManager.sendRPCReplyMessage(this, properties, envelope, mapper.writeValueAsString(registryResponse));
            }
        }

    }
}
