package eu.h2020.symbiote.messaging.consumers.informationModel;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.rabbitmq.client.AMQP;
import com.rabbitmq.client.Channel;
import com.rabbitmq.client.DefaultConsumer;
import com.rabbitmq.client.Envelope;
import eu.h2020.symbiote.core.internal.InformationModelListResponse;
import eu.h2020.symbiote.core.model.InformationModel;
import eu.h2020.symbiote.managers.RabbitManager;
import eu.h2020.symbiote.managers.RepositoryManager;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.http.HttpStatus;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by mateuszl on 07.08.2017.
 */
public class GetAllInformationModelsRequestConsumer extends DefaultConsumer {

    private static Log log = LogFactory.getLog(GetAllInformationModelsRequestConsumer.class);
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
    public GetAllInformationModelsRequestConsumer(Channel channel,
                                                  RepositoryManager repositoryManager,
                                                  RabbitManager rabbitManager) {
        super(channel);
        this.rabbitManager = rabbitManager;
        this.repositoryManager = repositoryManager;
        this.mapper = new ObjectMapper();
    }

    @Override
    public void handleDelivery(String consumerTag, Envelope envelope, AMQP.BasicProperties properties, byte[] body) throws IOException {
        super.handleDelivery(consumerTag, envelope, properties, body);

        InformationModelListResponse informationModelListResponse = new InformationModelListResponse();
        informationModelListResponse.setInformationModels(new ArrayList<>());
        informationModelListResponse.setStatus(400);
        List<InformationModel> informationModels;
        String message = new String(body, "UTF-8");
        log.info(" [x] Received request to retrieve list of existing Information Models.");

        informationModels = repositoryManager.getAllInformationModels();

        if (message.equalsIgnoreCase("false")) {
            clearRDFInfoFromModels(informationModels);
        }

        informationModelListResponse.setStatus(HttpStatus.SC_OK);
        informationModelListResponse.setMessage("OK. " + informationModels.size() + " Information Models found!");
        informationModelListResponse.setInformationModels(informationModels);
        rabbitManager.sendRPCReplyMessage(this, properties, envelope, mapper.writeValueAsString(informationModelListResponse));
    }

    private void clearRDFInfoFromModels(List<InformationModel> informationModels) {
        informationModels.forEach(informationModel -> informationModel.setRdf(null));
    }
}