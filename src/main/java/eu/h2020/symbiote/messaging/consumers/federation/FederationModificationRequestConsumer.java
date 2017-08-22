package eu.h2020.symbiote.messaging.consumers.federation;

import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.gson.JsonSyntaxException;
import com.rabbitmq.client.AMQP;
import com.rabbitmq.client.Channel;
import com.rabbitmq.client.DefaultConsumer;
import com.rabbitmq.client.Envelope;
import eu.h2020.symbiote.core.model.Federation;
import eu.h2020.symbiote.managers.RepositoryManager;
import eu.h2020.symbiote.messaging.RabbitManager;
import eu.h2020.symbiote.model.FederationPersistenceResult;
import eu.h2020.symbiote.model.FederationRegistryResponse;
import eu.h2020.symbiote.model.RegistryOperationType;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import java.io.IOException;

/**
 * Created by mateuszl on 07.08.2017.
 */
public class FederationModificationRequestConsumer extends DefaultConsumer {

    //// TODO: 22.08.2017 MOCKED, CHANGE!!

    private static Log log = LogFactory.getLog(FederationModificationRequestConsumer.class);
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
    public FederationModificationRequestConsumer(Channel channel,
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
        String response;
        FederationRegistryResponse federationResponse = new FederationRegistryResponse();
        String message = new String(body, "UTF-8");
        log.info(" [x] Received Federation to modify: '" + message + "'");

        Federation federation;

        try {
            federation = mapper.readValue(message, Federation.class);
            federationResponse.setFederation(federation);

            //// TODO: 11.08.2017 should i check some information given in platform?

            FederationPersistenceResult federationPersistenceResult = this.repositoryManager.modifyFederation(federation);
            if (federationPersistenceResult.getStatus() == 200) {
                rabbitManager.sendFederationOperationMessage(federationPersistenceResult.getFederation(),
                        RegistryOperationType.MODIFICATION);
            } else {
                log.error("Error occurred during Federation modifying in db, due to: " +
                        federationPersistenceResult.getMessage());
                federationResponse.setMessage("Error occurred during Federation modifying in db, due to: " +
                        federationPersistenceResult.getMessage());
                federationResponse.setStatus(500);
            }
        } catch (JsonSyntaxException | JsonMappingException e) {
            log.error("Error occurred during Federation retrieving from message", e);
            federationResponse.setMessage("Error occurred during Federation retrieving from message");
            federationResponse.setStatus(400);
        }

        response = mapper.writeValueAsString(federationResponse);

        rabbitManager.sendRPCReplyMessage(this, properties, envelope, response);
    }
}
