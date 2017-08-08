package eu.h2020.symbiote.messaging;

import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.gson.JsonSyntaxException;
import com.rabbitmq.client.AMQP;
import com.rabbitmq.client.Channel;
import com.rabbitmq.client.DefaultConsumer;
import com.rabbitmq.client.Envelope;
import eu.h2020.symbiote.core.model.Platform;
import eu.h2020.symbiote.model.PlatformResponse;
import eu.h2020.symbiote.model.RegistryOperationType;
import eu.h2020.symbiote.model.RegistryPlatform;
import eu.h2020.symbiote.managers.RepositoryManager;
import eu.h2020.symbiote.utils.RegistryUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import java.io.IOException;

/**
 * RabbitMQ Consumer implementation used for Platform Creation actions
 * <p>
 * Created by mateuszl
 */
@Deprecated
public class PlatformCreationRequestConsumerOld extends DefaultConsumer {

    private static Log log = LogFactory.getLog(PlatformCreationRequestConsumerOld.class);
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
    public PlatformCreationRequestConsumerOld(Channel channel,
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
        String message = new String(body, "UTF-8");
        log.info(" [x] Received requestPlatform to create: '" + message + "'");

        Platform requestPlatform = null;
        RegistryPlatform registryPlatform;

        PlatformResponse platformResponse = new PlatformResponse();
        try {
            requestPlatform = mapper.readValue(message, Platform.class);
            platformResponse.setPlatform(requestPlatform);

            registryPlatform = RegistryUtils.convertRequestPlatformToRegistryPlatform(requestPlatform);

            log.info("Platform converted to RegistryPlatform: " + registryPlatform);

            if (RegistryUtils.validateFields(registryPlatform)) {
                platformResponse = this.repositoryManager.savePlatform(registryPlatform);
                if (platformResponse.getStatus() == 200) {
                    rabbitManager.sendPlatformOperationMessage(platformResponse.getPlatform(),
                            RegistryOperationType.CREATION);
                }
            } else {
                log.error("Given Platform has some fields null or empty");
                platformResponse.setMessage("Given Platform has some fields null or empty");
                platformResponse.setStatus(400);
            }
        } catch (JsonSyntaxException | JsonMappingException e) {
            log.error("Error occured during Platform saving to db", e);
            platformResponse.setMessage("Error occured during Platform saving to db");
            platformResponse.setStatus(400);
        }
        response = mapper.writeValueAsString(platformResponse);

        rabbitManager.sendRPCReplyMessage(this, properties, envelope, response);
    }
}