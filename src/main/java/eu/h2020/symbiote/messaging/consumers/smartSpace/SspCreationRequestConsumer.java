package eu.h2020.symbiote.messaging.consumers.smartSpace;

import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.gson.JsonSyntaxException;
import com.rabbitmq.client.AMQP;
import com.rabbitmq.client.Channel;
import com.rabbitmq.client.DefaultConsumer;
import com.rabbitmq.client.Envelope;
import eu.h2020.symbiote.core.cci.SspRegistryResponse;
import eu.h2020.symbiote.core.internal.CoreSspResourceRegistryResponse;
import eu.h2020.symbiote.managers.RabbitManager;
import eu.h2020.symbiote.managers.RepositoryManager;
import eu.h2020.symbiote.model.RegistryOperationType;
import eu.h2020.symbiote.model.persistenceResults.SspPersistenceResult;
import eu.h2020.symbiote.model.mim.SmartSpace;
import eu.h2020.symbiote.utils.RegistryUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import java.io.IOException;

/**
 * Created by mateuszl on 25.05.2018.
 */
public class SspCreationRequestConsumer extends DefaultConsumer {

    private Log log = LogFactory.getLog(this.getClass());
    private RepositoryManager repositoryManager;
    private RabbitManager rabbitManager;

    /**
     * Constructs a new instance and records its association to the passed-in channel.
     *
     * @param channel the channel to which this consumer is attached
     */
    public SspCreationRequestConsumer(Channel channel,
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
        log.info(" [x] Received SSP to create: '" + message + "'");

        SmartSpace requestSsp;

        SspRegistryResponse sspResponse = new SspRegistryResponse();

        try {
            requestSsp = mapper.readValue(message, SmartSpace.class);
            sspResponse.setBody(requestSsp);

            //// TODO: 11.08.2017 should i check some information given in ssp?

            if (RegistryUtils.validateFields(requestSsp)) {
                SspPersistenceResult sspPersistenceResult = this.repositoryManager.saveSsp(requestSsp);
                if (sspPersistenceResult.getStatus() == 200) {
                    sspResponse.setMessage(
                            sspPersistenceResult.getMessage());
                    sspResponse.setStatus(200);
                    rabbitManager.sendPlatformOperationMessage(sspPersistenceResult.getSmartSpace(),
                            RegistryOperationType.CREATION);
                } else {
                    log.error("Error occurred during Platform saving in db, due to: " +
                            sspPersistenceResult.getMessage());
                    sspResponse.setMessage("Error occurred during Platform saving in db, due to: " +
                            sspPersistenceResult.getMessage());
                    sspResponse.setStatus(500);
                }
            } else {
                log.error("Given Platform has some fields null or empty");
                sspResponse.setMessage("Given Platform has some fields null or empty");
                sspResponse.setStatus(400);
            }
        } catch (JsonSyntaxException | JsonMappingException e) {
            log.error("Error occurred during Platform retrieving from json", e);
            sspResponse.setMessage("Error occurred during Platform retrieving from json");
            sspResponse.setStatus(400);
        }

        response = mapper.writeValueAsString(sspResponse);

        rabbitManager.sendRPCReplyMessage(this, properties, envelope, response);
    }

}
