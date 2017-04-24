package eu.h2020.symbiote.messaging;

import com.google.gson.Gson;
import com.google.gson.JsonSyntaxException;
import com.rabbitmq.client.AMQP;
import com.rabbitmq.client.Channel;
import com.rabbitmq.client.DefaultConsumer;
import com.rabbitmq.client.Envelope;
import eu.h2020.symbiote.model.InformationModel;
import eu.h2020.symbiote.model.InformationModelResponse;
import eu.h2020.symbiote.model.RegistryRequest;
import eu.h2020.symbiote.model.RegistryResponse;
import eu.h2020.symbiote.repository.RepositoryManager;
import eu.h2020.symbiote.utils.RegistryUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.http.HttpStatus;

import java.io.IOException;

/**
 * For next release...
 *
 * Created by mateuszl on 27.03.2017.
 */
public class InformationModelCreationRequestConsumer extends DefaultConsumer {

    private static Log log = LogFactory.getLog(InformationModelCreationRequestConsumer.class);
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
    public InformationModelCreationRequestConsumer(Channel channel,
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
        Gson gson = new Gson();
        RegistryRequest request = null;
        RegistryResponse semanticResponse = new RegistryResponse();
        semanticResponse.setStatus(HttpStatus.SC_BAD_REQUEST);
        String response;
        String message = new String(body, "UTF-8");
        InformationModel informationModel = new InformationModel();
        InformationModelResponse informationModelResponse = new InformationModelResponse();

        log.info(" [x] Received information model to create: '" + message + "'");

        try {
            request = gson.fromJson(message, RegistryRequest.class);
        } catch (JsonSyntaxException e) {
            log.error("Error occured during getting Operation Request from Json", e);
            informationModelResponse.setStatus(HttpStatus.SC_BAD_REQUEST);
            informationModelResponse.setMessage("Error occured during getting Operation Request from Json");
            informationModelResponse.setInformationModel(informationModel);
        }


        if (request != null) {
            if (RegistryUtils.checkToken(request.getToken())) {
                switch (request.getType()) {
                    case RDF:
                        try {
                            semanticResponse = RegistryUtils.getInformationModelFromRdf(request.getBody());
                        } catch (JsonSyntaxException e) {
                            log.error("Error occured during getting model from Json received from Semantic Manager", e);
                            informationModelResponse.setStatus(HttpStatus.SC_BAD_REQUEST);
                            informationModelResponse.setMessage("Error occured during getting Platforms from Json");
                            informationModelResponse.setInformationModel(informationModel);
                        }
                    case BASIC:
                        try {
                            informationModel = gson.fromJson(request.getBody(), InformationModel.class);
                        } catch (JsonSyntaxException e) {
                            log.error("Error occured during getting Information Model from Json", e);
                            informationModelResponse.setStatus(HttpStatus.SC_BAD_REQUEST);
                            informationModelResponse.setMessage("Error occured during getting Information Model from Json");
                            informationModelResponse.setInformationModel(informationModel);
                        }
                }

            } else {
                log.error("Token invalid");
                informationModelResponse.setStatus(HttpStatus.SC_UNAUTHORIZED);
                informationModelResponse.setMessage("Token invalid");
                informationModelResponse.setInformationModel(informationModel);
            }
        } else {
            log.error("Request is null");
            informationModelResponse.setStatus(HttpStatus.SC_BAD_REQUEST);
            informationModelResponse.setMessage("Request is null");
            informationModelResponse.setInformationModel(informationModel);
        }

        if (RegistryUtils.validateFields(informationModel)) {
            if (informationModel.getBody()==null) informationModel = RegistryUtils.getRdfBodyForObject(informationModel);
            informationModelResponse = this.repositoryManager.saveInformationModel(informationModel);
            if (informationModelResponse.getStatus() == 200) {
                rabbitManager.sendInformationModelCreatedMessage(informationModelResponse.getInformationModel());
            }
        } else {
            log.error("Given Information Model has some fields null or empty");
            informationModelResponse.setMessage("Given Information Model has some fields null or empty");
            informationModelResponse.setStatus(HttpStatus.SC_BAD_REQUEST);
            informationModelResponse.setInformationModel(informationModel);
        }

        response = gson.toJson(informationModelResponse);
        rabbitManager.sendRPCReplyMessage(this, properties, envelope, response);
    }
}
