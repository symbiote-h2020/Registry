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
import eu.h2020.symbiote.managers.RabbitManager;
import eu.h2020.symbiote.managers.RepositoryManager;
import eu.h2020.symbiote.model.InformationModelPersistenceResult;
import eu.h2020.symbiote.model.RegistryOperationType;
import eu.h2020.symbiote.utils.RegistryUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import java.io.IOException;

/**
 * Created by mateuszl on 08.08.2017.
 */
public class InformationModelRemovalRequestConsumer extends DefaultConsumer {

    private static Log log = LogFactory.getLog(InformationModelRemovalRequestConsumer.class);
    private RabbitManager rabbitManager;
    private RepositoryManager repositoryManager;

    public InformationModelRemovalRequestConsumer(Channel channel,
                                                  RabbitManager rabbitManager,
                                                  RepositoryManager repositoryManager) {
        super(channel);
        this.rabbitManager = rabbitManager;
        this.repositoryManager = repositoryManager;
    }

    @Override
    public void handleDelivery(String consumerTag, Envelope envelope, AMQP.BasicProperties properties, byte[] body) throws IOException {
        super.handleDelivery(consumerTag, envelope, properties, body);

        ObjectMapper mapper = new ObjectMapper();
        String message = new String(body, "UTF-8");
        log.info(" [x] Received Information Model to create");

        InformationModelRequest informationModelRequest;
        InformationModel informationModelReceived;
        InformationModelResponse response = new InformationModelResponse();

        try {
            informationModelRequest = mapper.readValue(message, InformationModelRequest.class);
            informationModelReceived = informationModelRequest.getInformationModel();
            response.setInformationModel(informationModelReceived);

            if (RegistryUtils.validateNullOrEmptyId(informationModelReceived)) {
                log.error("Given Information Model has ID null or empty");
                response.setMessage("Given Information Model has ID null or empty");
                response.setStatus(400);
                rabbitManager.sendRPCReplyMessage(this, properties, envelope, mapper.writeValueAsString(response));
            } else {

                InformationModelPersistenceResult informationModelPersistenceResult = repositoryManager.removeInformationModel(informationModelReceived);

                if (informationModelPersistenceResult.getStatus() == 200) {
                    rabbitManager.sendInformationModelOperationMessage(informationModelReceived, RegistryOperationType.REMOVAL);
                    log.info("Information Model removed successfully!");
                    response.setMessage("Information Model removed successfully!");
                    response.setStatus(200);
                    rabbitManager.sendRPCReplyMessage(this, properties, envelope, mapper.writeValueAsString(response));
                } else {
                    log.error("Operation unsuccessful due to: " + informationModelPersistenceResult.getMessage());
                    response.setMessage("Operation unsuccessful due to: " + informationModelPersistenceResult.getMessage());
                    response.setStatus(informationModelPersistenceResult.getStatus());
                    rabbitManager.sendRPCReplyMessage(this, properties, envelope, mapper.writeValueAsString(response));
                }
            }
        } catch (JsonSyntaxException | JsonMappingException e) {
            log.error("Error occurred during Information Model saving to db", e);
            response.setMessage("Error occurred during Information Model saving to db");
            response.setStatus(400);
            rabbitManager.sendRPCReplyMessage(this, properties, envelope, mapper.writeValueAsString(response));
        }
    }
}
