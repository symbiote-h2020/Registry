package eu.h2020.symbiote.messaging.consumers.informationModel;

import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.gson.JsonSyntaxException;
import com.rabbitmq.client.AMQP;
import com.rabbitmq.client.Channel;
import com.rabbitmq.client.DefaultConsumer;
import com.rabbitmq.client.Envelope;
import eu.h2020.symbiote.core.cci.InformationModelRequest;
import eu.h2020.symbiote.core.cci.InformationModelResponse;
import eu.h2020.symbiote.core.model.InformationModel;
import eu.h2020.symbiote.managers.AuthorizationManager;
import eu.h2020.symbiote.managers.RabbitManager;
import eu.h2020.symbiote.model.RegistryOperationType;
import eu.h2020.symbiote.utils.RegistryUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import java.io.IOException;

/**
 * Created by mateuszl on 08.08.2017.
 */
public class InformationModelModificationRequestConsumer extends DefaultConsumer {

    private static Log log = LogFactory.getLog(InformationModelModificationRequestConsumer.class);
    private AuthorizationManager authorizationManager;
    private RabbitManager rabbitManager;

    public InformationModelModificationRequestConsumer(Channel channel,
                                                       RabbitManager rabbitManager,
                                                       AuthorizationManager authorizationManager) {
        super(channel);
        this.rabbitManager = rabbitManager;
        this.authorizationManager = authorizationManager;
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
        log.info(" [x] Received Information Model to modify: '" + message + "'");

        InformationModelRequest informationModelRequest;
        InformationModel informationModelReceived;
        InformationModelResponse response = new InformationModelResponse();

        try {
            informationModelRequest = mapper.readValue(message, InformationModelRequest.class);
            informationModelReceived = informationModelRequest.getInformationModel();
            response.setInformationModel(informationModelReceived);

            // TODO: 18.08.2017 authorization check!

            if (RegistryUtils.validateFields(informationModelReceived)) {
                if (RegistryUtils.validateNullOrEmptyId(informationModelReceived)) {
                    log.error("Given Information Model has not ID! It should have an ID!");
                    response.setMessage("Given Information Model has no ID! It should  have an ID!");
                    response.setStatus(400);
                    rabbitManager.sendRPCReplyMessage(this, properties, envelope, mapper.writeValueAsString(response));
                } else {
                    log.info("Message to Semantic Manager Sent. Request: " + informationModelRequest);
                    //sending JSON content to Semantic Manager and passing responsibility to another consumer
                    rabbitManager.sendInformationModelValidationRpcMessage(this, properties, envelope,
                            mapper.writeValueAsString(informationModelReceived),
                            RegistryOperationType.MODIFICATION);
                }
            } else {
                log.error("Given IM has some fields null or empty");
                response.setMessage("Given IM has some fields null or empty");
                response.setStatus(400);
                rabbitManager.sendRPCReplyMessage(this, properties, envelope, mapper.writeValueAsString(response));
            }
        } catch (JsonSyntaxException | JsonMappingException e) {
            log.error("Error occurred during IM modification in db", e);
            response.setMessage("Error occurred during IM modification in db");
            response.setStatus(400);
            rabbitManager.sendRPCReplyMessage(this, properties, envelope, mapper.writeValueAsString(response));
        }
    }
}