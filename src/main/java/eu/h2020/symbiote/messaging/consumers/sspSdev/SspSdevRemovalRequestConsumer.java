package eu.h2020.symbiote.messaging.consumers.sspSdev;

import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.gson.JsonSyntaxException;
import com.rabbitmq.client.AMQP;
import com.rabbitmq.client.Channel;
import com.rabbitmq.client.DefaultConsumer;
import com.rabbitmq.client.Envelope;
import eu.h2020.symbiote.cloud.model.ssp.SspRegInfo;
import eu.h2020.symbiote.core.cci.SdevRegistryResponse;
import eu.h2020.symbiote.core.internal.CoreSdevRegistryRequest;
import eu.h2020.symbiote.managers.RabbitManager;
import eu.h2020.symbiote.managers.RepositoryManager;
import eu.h2020.symbiote.model.RegistryOperationType;
import eu.h2020.symbiote.model.persistenceResults.SdevPersistenceResult;
import eu.h2020.symbiote.utils.RegistryUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.http.HttpStatus;

import java.io.IOException;

/**
 * Created by mateuszl on 11.06.2018.
 */
public class SspSdevRemovalRequestConsumer extends DefaultConsumer {

    private static Log log = LogFactory.getLog(SspSdevRemovalRequestConsumer.class);
    private RepositoryManager repositoryManager;
    private RabbitManager rabbitManager;
    private SdevRegistryResponse response;
    private Envelope envelope;
    private AMQP.BasicProperties properties;
    private ObjectMapper mapper;

    /**
     * Constructs a new instance and records its association to the passed-in channel.
     * Managers beans passed as parameters because of lack of possibility to inject it to consumer.
     *
     * @param channel           the channel to which this consumer is attached
     * @param rabbitManager     rabbit manager bean passed for access to messages manager
     * @param repositoryManager repository manager bean passed for persistence actions
     */
    public SspSdevRemovalRequestConsumer(Channel channel,
                                         RabbitManager rabbitManager,
                                         RepositoryManager repositoryManager) {
        super(channel);
        this.repositoryManager = repositoryManager;
        this.rabbitManager = rabbitManager;
        mapper = new ObjectMapper();
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
        CoreSdevRegistryRequest request;
        String message = new String(body, "UTF-8");
        response = new SdevRegistryResponse();

        log.info(" [x] Received Sdev (SspRegInfo) to remove");
        log.info("Content: " + message);
        this.envelope = envelope;
        this.properties = properties;

        try {
            request = mapper.readValue(message, CoreSdevRegistryRequest.class);

            //// TODO: 20.06.2018 Check if given SspId exists
            //// TODO: 20.06.2018 security check

            SspRegInfo sDev = request.getBody();

            if (RegistryUtils.validateFields(sDev)) {

                SdevPersistenceResult sdevPersistenceResult = this.repositoryManager.removeSdev(sDev);


                response.setStatus(sdevPersistenceResult.getStatus());
                response.setMessage(sdevPersistenceResult.getMessage());
                if (sdevPersistenceResult.getStatus() == 200) {
                    rabbitManager.sendSdevOperationMessage(sdevPersistenceResult.getSdev(),
                            RegistryOperationType.REMOVAL);
                } else {
                    prepareAndSendErrorResponse(HttpStatus.SC_BAD_REQUEST, "Error occurred during Sdev (SspRegInfo) removing from db, due to: " +
                            sdevPersistenceResult.getMessage());
                }
            } else {
                prepareAndSendErrorResponse(HttpStatus.SC_BAD_REQUEST, "Given Sdev (SspRegInfo) has some fields null or empty");
            }
            rabbitManager.sendRPCReplyMessage(this, properties, envelope, mapper.writeValueAsString(response));

        } catch (JsonSyntaxException | JsonMappingException e) {
            prepareAndSendErrorResponse(HttpStatus.SC_BAD_REQUEST, "Error occurred during Sdev (SspRegInfo) retrieving from message" + e);
        }
    }

    private void prepareAndSendErrorResponse(int status, String message) throws IOException {
        log.error(message);
        this.response.setStatus(status);
        this.response.setMessage(message);
        rabbitManager.sendRPCReplyMessage(this, properties, envelope, mapper.writeValueAsString(response));
    }
}