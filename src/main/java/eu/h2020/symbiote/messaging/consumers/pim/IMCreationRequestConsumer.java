package eu.h2020.symbiote.messaging.consumers.pim;

import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.gson.JsonSyntaxException;
import com.rabbitmq.client.AMQP;
import com.rabbitmq.client.Channel;
import com.rabbitmq.client.DefaultConsumer;
import com.rabbitmq.client.Envelope;
import eu.h2020.symbiote.core.cci.InformationModelResponse;
import eu.h2020.symbiote.core.cci.PlatformRegistryResponse;
import eu.h2020.symbiote.core.model.InformationModel;
import eu.h2020.symbiote.core.model.Platform;
import eu.h2020.symbiote.managers.RepositoryManager;
import eu.h2020.symbiote.messaging.RabbitManager;
import eu.h2020.symbiote.model.RegistryOperationType;
import eu.h2020.symbiote.utils.RegistryUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import java.io.IOException;

/**
 * Created by mateuszl on 08.08.2017.
 */
public class IMCreationRequestConsumer extends DefaultConsumer {

    private static Log log = LogFactory.getLog(IMCreationRequestConsumer.class);
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
    public IMCreationRequestConsumer(Channel channel,
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
        log.info(" [x] Received Information Model to create: '" + message + "'");

        InformationModel informationModelReceived;
        InformationModelResponse response = new InformationModelResponse();

        try {
            informationModelReceived = mapper.readValue(message, InformationModel.class);
            response.setInformationModel(informationModelReceived);

            //// TODO: 11.08.2017 should i check some informations given in platform?

            if (RegistryUtils.validateFields(informationModelReceived)) {
                response = this.repositoryManager.sa(informationModelReceived);
                if (response.getStatus() == 200) {
                    rabbitManager.sendInformationModelOperationMessage(response.getInformationModel(),
                            RegistryOperationType.CREATION);
                }
            } else {
                log.error("Given Platform has some fields null or empty");
                response.setMessage("Given Platform has some fields null or empty");
                response.setStatus(400);
            }
        } catch (JsonSyntaxException | JsonMappingException e) {
            log.error("Error occured during Platform saving to db", e);
            response.setMessage("Error occured during Platform saving to db");
            response.setStatus(400);
        }
        rabbitManager.sendRPCReplyMessage(this, properties, envelope, mapper.writeValueAsString(response));
    }
}