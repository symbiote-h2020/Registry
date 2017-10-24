package eu.h2020.symbiote.messaging.consumers.platform;

import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.gson.JsonSyntaxException;
import com.rabbitmq.client.AMQP;
import com.rabbitmq.client.Channel;
import com.rabbitmq.client.DefaultConsumer;
import com.rabbitmq.client.Envelope;
import eu.h2020.symbiote.core.cci.PlatformRegistryResponse;
import eu.h2020.symbiote.managers.RabbitManager;
import eu.h2020.symbiote.managers.RepositoryManager;
import eu.h2020.symbiote.model.PlatformPersistenceResult;
import eu.h2020.symbiote.model.RegistryOperationType;
import eu.h2020.symbiote.model.mim.Platform;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import java.io.IOException;

/**
 * Created by mateuszl on 07.08.2017.
 */
public class PlatformModificationRequestConsumer extends DefaultConsumer {

    private static Log log = LogFactory.getLog(PlatformModificationRequestConsumer.class);
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
    public PlatformModificationRequestConsumer(Channel channel,
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
        PlatformRegistryResponse platformResponse = new PlatformRegistryResponse();
        String message = new String(body, "UTF-8");
        log.info(" [x] Received platform to modify: '" + message + "'");

        Platform requestPlatform;

        try {
            requestPlatform = mapper.readValue(message, Platform.class);
            platformResponse.setBody(requestPlatform);

            //// TODO: 11.08.2017 should i check some information given in platform?

            PlatformPersistenceResult platformPersistenceResult = this.repositoryManager.modifyPlatform(requestPlatform);

            if (platformPersistenceResult.getStatus() == 200) {
                platformResponse.setMessage(
                        platformPersistenceResult.getMessage());
                platformResponse.setStatus(200);
                rabbitManager.sendPlatformOperationMessage(platformPersistenceResult.getPlatform(),
                        RegistryOperationType.MODIFICATION);
            } else {
                log.error("Error occurred during Platform modifying in db, due to: " +
                        platformPersistenceResult.getMessage());
                platformResponse.setMessage("Error occurred during Platform modifying in db, due to: " +
                        platformPersistenceResult.getMessage());
                platformResponse.setStatus(500);
            }
        } catch (JsonSyntaxException | JsonMappingException e) {
            log.error("Error occurred during Platform mapping from message", e);
            platformResponse.setMessage("Error occurred during Platform mapping from message");
            platformResponse.setStatus(400);
        }

        response = mapper.writeValueAsString(platformResponse);

        rabbitManager.sendRPCReplyMessage(this, properties, envelope, response);
    }
}
