package eu.h2020.symbiote.messaging.consumers.informationModel;

import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.rabbitmq.client.AMQP;
import com.rabbitmq.client.Channel;
import com.rabbitmq.client.DefaultConsumer;
import com.rabbitmq.client.Envelope;
import eu.h2020.symbiote.core.cci.InformationModelRequest;
import eu.h2020.symbiote.core.cci.InformationModelResponse;
import eu.h2020.symbiote.managers.RabbitManager;
import eu.h2020.symbiote.managers.RepositoryManager;
import eu.h2020.symbiote.model.RegistryOperationType;
import eu.h2020.symbiote.model.mim.InformationModel;
import eu.h2020.symbiote.model.persistenceResults.InformationModelPersistenceResult;
import eu.h2020.symbiote.utils.ValidationUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.http.HttpStatus;

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
        InformationModelResponse response = new InformationModelResponse();

        log.info(" [x] Received Information Model to create");

        try {
            InformationModelRequest informationModelRequest = mapper.readValue(message, InformationModelRequest.class);
            InformationModel informationModelReceived = informationModelRequest.getBody();
            response.setBody(informationModelReceived);

            ValidationUtils.validateInformationModelForRemoval(informationModelReceived);

            InformationModelPersistenceResult informationModelPersistenceResult = repositoryManager.removeInformationModel(informationModelReceived);

            if (informationModelPersistenceResult.getStatus() == 200) {
                rabbitManager.sendInformationModelOperationMessage(informationModelReceived, RegistryOperationType.REMOVAL);
                log.info("Information Model removed successfully!");
                response.setMessage("Information Model removed successfully!");
                response.setStatus(200);
                rabbitManager.sendRPCReplyMessage(this, properties, envelope, mapper.writeValueAsString(response));
            } else throw new InterruptedException(informationModelPersistenceResult.getMessage());

        } catch (IllegalArgumentException | JsonMappingException | NullPointerException | InterruptedException e) {
            log.error(e.getMessage());
            response.setMessage(e.getMessage());
            response.setStatus(HttpStatus.SC_BAD_REQUEST);
            rabbitManager.sendRPCReplyMessage(this, properties, envelope, mapper.writeValueAsString(response));

        } catch (Exception e) {
            log.error(e.getMessage());
            response.setMessage(e.getMessage());
            response.setStatus(HttpStatus.SC_INTERNAL_SERVER_ERROR);
            rabbitManager.sendRPCReplyMessage(this, properties, envelope, mapper.writeValueAsString(response));
        }
    }
}