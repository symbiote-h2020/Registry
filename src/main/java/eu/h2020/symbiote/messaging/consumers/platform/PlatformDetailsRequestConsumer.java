package eu.h2020.symbiote.messaging.consumers.platform;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.rabbitmq.client.AMQP;
import com.rabbitmq.client.Channel;
import com.rabbitmq.client.DefaultConsumer;
import com.rabbitmq.client.Envelope;
import eu.h2020.symbiote.core.cci.PlatformRegistryResponse;
import eu.h2020.symbiote.managers.RabbitManager;
import eu.h2020.symbiote.managers.RepositoryManager;
import eu.h2020.symbiote.model.mim.Platform;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.http.HttpStatus;

import java.io.IOException;

/**
 * Created by mateuszl on 13.09.2017.
 */
public class PlatformDetailsRequestConsumer extends DefaultConsumer {

    private static Log log = LogFactory.getLog(PlatformDetailsRequestConsumer.class);
    private ObjectMapper mapper;
    private RabbitManager rabbitManager;
    private RepositoryManager repositoryManager;

    /**
     * Constructs a new instance and records its association to the passed-in channel.
     * Managers beans passed as parameters because of lack of possibility to inject it to consumer.
     *
     * @param channel       the channel to which this consumer is attached
     * @param rabbitManager rabbit manager bean passed for access to messages manager
     */
    public PlatformDetailsRequestConsumer(Channel channel,
                                          RepositoryManager repositoryManager,
                                          RabbitManager rabbitManager) {
        super(channel);
        this.rabbitManager = rabbitManager;
        this.repositoryManager = repositoryManager;
        this.mapper = new ObjectMapper();
    }


    /**
     * Called when a <code><b>basic.deliver</b></code> is received for this consumer.
     * Waiting for message containing Id of the requested Platform.
     * RPC reply: PlatformRegistryResponse. Body can be null if status != 200.
     *
     * @param consumerTag the <i>consumer tag</i> associated with the consumer
     * @param envelope    packaging data for the message
     * @param properties  content header data for the message
     * @param body        the message body (opaque, client-specific byte array) In this case - String Id of requested Platform.
     * @throws IOException if the consumer encounters an I/O error while processing the message
     * @see Envelope
     */
    @Override
    public void handleDelivery(String consumerTag, Envelope envelope,
                               AMQP.BasicProperties properties, byte[] body)
            throws IOException {
        PlatformRegistryResponse platformResponse = new PlatformRegistryResponse();
        String requestedPlatformId = new String(body, "UTF-8");
        log.info(" [x] Received request to retrieve a platform with id: " + requestedPlatformId);

        try {
            Platform foundPlatform = repositoryManager.getPlatformById(requestedPlatformId);

            if (foundPlatform != null) {
                platformResponse.setStatus(HttpStatus.SC_OK);
                platformResponse.setMessage("OK. Platform with id '" + foundPlatform.getId() + "' found!");
            } else {
                log.debug("There is no Platform with given id (" + requestedPlatformId + ") in the system.");
                platformResponse.setStatus(HttpStatus.SC_BAD_REQUEST);
                platformResponse.setMessage("There is no Platform with given id (" + requestedPlatformId + ") in the system.");
            }

            platformResponse.setBody(foundPlatform);
        } catch (Exception e) {
            log.error(e);
            platformResponse.setStatus(HttpStatus.SC_INTERNAL_SERVER_ERROR);
            platformResponse.setMessage("Consumer critical error!");
        }
        rabbitManager.sendRPCReplyMessage(this, properties, envelope, mapper.writeValueAsString(platformResponse));
    }
}
