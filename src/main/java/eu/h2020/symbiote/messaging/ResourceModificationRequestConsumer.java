package eu.h2020.symbiote.messaging;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.gson.JsonSyntaxException;
import com.rabbitmq.client.AMQP;
import com.rabbitmq.client.Channel;
import com.rabbitmq.client.DefaultConsumer;
import com.rabbitmq.client.Envelope;
import eu.h2020.symbiote.core.internal.CoreResourceRegistryRequest;
import eu.h2020.symbiote.model.RegistryResponse;
import eu.h2020.symbiote.model.ResourceOperationType;
import eu.h2020.symbiote.repository.RepositoryManager;
import eu.h2020.symbiote.utils.RegistryUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.http.HttpStatus;

import java.io.IOException;

/**
 * RabbitMQ Consumer implementation used for Resource Modification actions
 * <p>
 * Created by mateuszl
 */
public class ResourceModificationRequestConsumer extends DefaultConsumer {

    private static Log log = LogFactory.getLog(ResourceModificationRequestConsumer.class);
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
    public ResourceModificationRequestConsumer(Channel channel,
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

        log.info(" [x] Received resources to modify (CoreResourceRegistryRequest):'" + message + "'");

        try {
            //request from CCI received and deserialized
            request = mapper.readValue(message, CoreResourceRegistryRequest.class);
        } catch (JsonSyntaxException e) {
            log.error("Unable to get CoreResourceRegistryRequest from Message body!", e);
            registryResponse.setStatus(HttpStatus.SC_BAD_REQUEST);
            registryResponse.setMessage("Content invalid. Could not deserialize. Resources not modified!");
            rabbitManager.sendRPCReplyMessage(this, properties, envelope, mapper.writeValueAsString(registryResponse));
        }

        if (request != null) {
            if (RegistryUtils.checkToken(request.getToken())) {
                //contact with Semantic Manager accordingly to Type of object Description received
                switch (request.getDescriptionType()) {
                    case RDF:
                        log.info("Message to Semantic Manager Sent. Content Type : RDF. Request: " + request.getBody());
                        //sending RDF content to Semantic Manager and passing responsibility to another consumer
                        rabbitManager.sendResourceRdfValidationRpcMessage(this, properties, envelope,
                                message, request.getPlatformId(), ResourceOperationType.MODIFICATION);
                        break;
                    case BASIC:
                        log.info("Message to Semantic Manager Sent. Content Type : BASIC. Request: " + request.getBody());
                        //sending JSON content to Semantic Manager and passing responsibility to another consumer
                        rabbitManager.sendResourceJsonTranslationRpcMessage(this, properties, envelope,
                                message, request.getPlatformId(), ResourceOperationType.MODIFICATION);
                        break;
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
