package eu.h2020.symbiote.messaging.consumers.federation;

import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.gson.JsonSyntaxException;
import com.rabbitmq.client.AMQP;
import com.rabbitmq.client.Channel;
import com.rabbitmq.client.DefaultConsumer;
import com.rabbitmq.client.Envelope;
import eu.h2020.symbiote.core.internal.FederationRegistryResponse;
import eu.h2020.symbiote.managers.RabbitManager;
import eu.h2020.symbiote.managers.RepositoryManager;
import eu.h2020.symbiote.model.RegistryOperationType;
import eu.h2020.symbiote.model.mim.Federation;
import eu.h2020.symbiote.model.persistenceResults.FederationPersistenceResult;
import eu.h2020.symbiote.utils.ValidationUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import java.io.IOException;

/**
 * Created by mateuszl on 07.08.2017.
 */
public class FederationRemovalRequestConsumer extends DefaultConsumer {

    private static Log log = LogFactory.getLog(FederationRemovalRequestConsumer.class);
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
    public FederationRemovalRequestConsumer(Channel channel,
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
        FederationRegistryResponse federationResponse = new FederationRegistryResponse();
        log.info(" [x] Received Federation to remove");

        try {
            Federation requestFederation = mapper.readValue(message, Federation.class);
            federationResponse.setBody(requestFederation);

            if (ValidationUtils.validateFields(requestFederation)) {
                FederationPersistenceResult federationPersistenceResult = this.repositoryManager.removeFederation(requestFederation);
                if (federationPersistenceResult.getStatus() == 200) {
                    rabbitManager.sendFederationOperationMessage(federationPersistenceResult.getFederation(),
                            RegistryOperationType.REMOVAL);
                } else {
                    log.error("Error occurred during Federation removing from db, due to: " +
                            federationPersistenceResult.getMessage());
                    federationResponse.setMessage("Error occurred during Federation removing from db, due to: " +
                            federationPersistenceResult.getMessage());
                    federationResponse.setStatus(500);
                }
            } else {
                log.error("Given Federation has some fields null or empty");
                federationResponse.setMessage("Given Federation has some fields null or empty");
                federationResponse.setStatus(400);
            }
        } catch (JsonSyntaxException | JsonMappingException e) {
            log.error("Error occurred during Federation retrieving from message", e);
            federationResponse.setMessage("Error occurred during Federation retrieving from message");
            federationResponse.setStatus(400);
        }
        rabbitManager.sendRPCReplyMessage(this, properties, envelope, mapper.writeValueAsString(federationResponse));
    }
}

