package eu.h2020.symbiote.messaging;

import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.gson.JsonSyntaxException;
import com.rabbitmq.client.AMQP;
import com.rabbitmq.client.Channel;
import com.rabbitmq.client.DefaultConsumer;
import com.rabbitmq.client.Envelope;
import eu.h2020.symbiote.core.cci.PlatformRegistryResponse;
import eu.h2020.symbiote.core.model.Platform;
import eu.h2020.symbiote.managers.RepositoryManager;
import eu.h2020.symbiote.model.RegistryOperationType;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import java.io.IOException;

/**
 * Created by mateuszl on 07.08.2017.
 */
public class PlatformRemovalRequestConsumer extends DefaultConsumer {

    private static Log log = LogFactory.getLog(PlatformRemovalRequestConsumer.class);
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
    public PlatformRemovalRequestConsumer(Channel channel,
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

        //// TODO: 07.08.2017 CHANGE TO NEW MODEL!!

        ObjectMapper mapper = new ObjectMapper();
        String response;
        String message = new String(body, "UTF-8");
        PlatformRegistryResponse platformResponse = new PlatformRegistryResponse();
        log.info(" [x] Received platform to remove: '" + message + "'");

        Platform requestPlatform;

        try {
            requestPlatform = mapper.readValue(message, Platform.class);
            platformResponse.setPlatform(requestPlatform);

            platformResponse = this.repositoryManager.removePlatform(requestPlatform);
            if (platformResponse.getStatus() == 200) {
                rabbitManager.sendPlatformOperationMessage(platformResponse.getPlatform(),
                        RegistryOperationType.REMOVAL);
            }
        } catch (JsonSyntaxException | JsonMappingException e) {
            log.error("Error occured during Platform deleting in db", e);
            platformResponse.setMessage("Error occured during Platform deleting in db");
            platformResponse.setStatus(400);
        }

        response = mapper.writeValueAsString(platformResponse);

        rabbitManager.sendRPCReplyMessage(this, properties, envelope, response);
    }
}

