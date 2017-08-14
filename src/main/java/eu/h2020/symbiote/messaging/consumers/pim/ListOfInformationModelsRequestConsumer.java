package eu.h2020.symbiote.messaging.consumers.pim;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.rabbitmq.client.AMQP;
import com.rabbitmq.client.Channel;
import com.rabbitmq.client.DefaultConsumer;
import com.rabbitmq.client.Envelope;
import eu.h2020.symbiote.core.internal.InformationModelListResponse;
import eu.h2020.symbiote.core.model.InformationModel;
import eu.h2020.symbiote.managers.AuthorizationManager;
import eu.h2020.symbiote.managers.RepositoryManager;
import eu.h2020.symbiote.messaging.RabbitManager;
import eu.h2020.symbiote.model.AuthorizationResult;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.http.HttpStatus;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by mateuszl on 07.08.2017.
 */
public class ListOfInformationModelsRequestConsumer extends DefaultConsumer {

    private static Log log = LogFactory.getLog(ListOfInformationModelsRequestConsumer.class);
    private ObjectMapper mapper;
    private RabbitManager rabbitManager;
    private AuthorizationManager authorizationManager;
    private RepositoryManager repositoryManager;

    /**
     * Constructs a new instance and records its association to the passed-in channel.
     * Managers beans passed as parameters because of lack of possibility to inject it to consumer.
     *
     * @param channel       the channel to which this consumer is attached
     * @param rabbitManager rabbit manager bean passed for access to messages manager
     */
    public ListOfInformationModelsRequestConsumer(Channel channel,
                                                  RepositoryManager repositoryManager,
                                                  RabbitManager rabbitManager,
                                                  AuthorizationManager authorizationManager) {
        super(channel);
        this.rabbitManager = rabbitManager;
        this.repositoryManager = repositoryManager;
        this.authorizationManager = authorizationManager;
        this.mapper = new ObjectMapper();
    }
    @Override
    public void handleDelivery(String consumerTag, Envelope envelope, AMQP.BasicProperties properties, byte[] body) throws IOException {
        super.handleDelivery(consumerTag, envelope, properties, body);

        InformationModelListResponse informationModelListResponse = new InformationModelListResponse();
        informationModelListResponse.setInformationModels(new ArrayList<>());
        informationModelListResponse.setStatus(400);
        List<InformationModel> informationModels;
        AuthorizationResult authorizationResult = null;
        String message = new String(body, "UTF-8");
        log.info(" [x] Received request to retrieve list of existing Information Models: '" + message + "'");

        /* Token verification in this internal communication is not used at this point
        if (message != null) {
            try {
                authorizationResult = authorizationManager.checkToken(request.getToken());
            } catch (NullArgumentException e) {
                log.error(e);
                informationModelListResponse.setMessage("Request invalid!");
                sendRpcReplyMessage(envelope, properties, informationModelListResponse);
                return;
            }
        } else {
            log.error("Request is null!");
            informationModelListResponse.setMessage("Request is null!");
            sendRpcReplyMessage(envelope, properties, informationModelListResponse);
            return;
        }

        if (!authorizationResult.isValidated()) {
            log.error("Token invalid! " + authorizationResult.getMessage());
            informationModelListResponse.setMessage(authorizationResult.getMessage());
            sendRpcReplyMessage(envelope, properties, informationModelListResponse);
            return;
        }
        */

        informationModels = repositoryManager.getAllInformationModels();
        informationModelListResponse.setStatus(HttpStatus.SC_OK);
        informationModelListResponse.setMessage("OK. " + informationModels.size() + " Information Models found!");
        informationModelListResponse.setInformationModels(informationModels);
        sendRpcReplyMessage(envelope, properties, informationModelListResponse);
    }

    private void sendRpcReplyMessage(Envelope envelope, AMQP.BasicProperties properties,
                                     InformationModelListResponse informationModelListResponse) throws IOException {
        rabbitManager.sendRPCReplyMessage(this, properties, envelope, mapper.writeValueAsString(informationModelListResponse));
    }
}
