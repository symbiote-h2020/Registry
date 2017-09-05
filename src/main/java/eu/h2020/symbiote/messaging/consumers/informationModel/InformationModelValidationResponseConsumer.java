package eu.h2020.symbiote.messaging.consumers.informationModel;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.gson.JsonSyntaxException;
import com.rabbitmq.client.AMQP;
import com.rabbitmq.client.Channel;
import com.rabbitmq.client.DefaultConsumer;
import com.rabbitmq.client.Envelope;
import eu.h2020.symbiote.core.cci.InformationModelResponse;
import eu.h2020.symbiote.core.internal.InformationModelValidationResult;
import eu.h2020.symbiote.core.model.InformationModel;
import eu.h2020.symbiote.managers.AuthorizationManager;
import eu.h2020.symbiote.managers.RepositoryManager;
import eu.h2020.symbiote.managers.RabbitManager;
import eu.h2020.symbiote.model.InformationModelPersistenceResult;
import eu.h2020.symbiote.model.RegistryOperationType;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import java.io.IOException;

/**
 * Created by mateuszl on 16.08.2017.
 */
public class InformationModelValidationResponseConsumer extends DefaultConsumer {

    private static Log log = LogFactory.getLog(InformationModelValidationResponseConsumer.class);
    private InformationModelResponse informationModelResponse;
    private DefaultConsumer rpcConsumer;
    private AMQP.BasicProperties rpcProperties;
    private Envelope rpcEnvelope;
    private RepositoryManager repositoryManager;
    private RabbitManager rabbitManager;
    private RegistryOperationType operationType;
    private ObjectMapper mapper;
    private AuthorizationManager authorizationManager;

    /**
     * Constructs a new instance and records its association to the passed-in channel.
     * Managers beans passed as parameters because of lack of possibility to inject it to consumer.
     *
     * @param channel           the channel to which this consumer is attached
     * @param rabbitManager     rabbit manager bean passed for access to messages manager
     * @param repositoryManager repository manager bean passed for persistence actions
     */
    public InformationModelValidationResponseConsumer(DefaultConsumer rpcConsumer,
                                                      AMQP.BasicProperties rpcProperties,
                                                      Envelope rpcEnvelope,
                                                      Channel channel,
                                                      RepositoryManager repositoryManager,
                                                      RabbitManager rabbitManager,
                                                      RegistryOperationType operationType,
                                                      AuthorizationManager authorizationManager) {
        super(channel);
        this.repositoryManager = repositoryManager;
        this.rabbitManager = rabbitManager;
        this.rpcConsumer = rpcConsumer;
        this.rpcEnvelope = rpcEnvelope;
        this.rpcProperties = rpcProperties;
        this.operationType = operationType;
        this.authorizationManager = authorizationManager;
        this.mapper = new ObjectMapper();
        this.informationModelResponse = new InformationModelResponse();
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
        String message = new String(body, "UTF-8");
        InformationModelValidationResult informationModelValidationResult = new InformationModelValidationResult();
        InformationModel informationModel;

        log.info("[x] Received IM validation result");

        try {
            //receive and read message from Semantic Manager
            informationModelValidationResult = mapper.readValue(message, InformationModelValidationResult.class);
        } catch (JsonSyntaxException | JsonMappingException e) {
            log.error("Unable to get Information Model validation result from Message body!", e);
            informationModelResponse.setStatus(500);
            informationModelResponse.setMessage("VALIDATION CONTENT INVALID:\n" + message);
        }

        if (informationModelValidationResult.isSuccess()) {

            /* for future Security updates
            AuthorizationResult authorizationResult = authorizationManager.checkToken(token);

            if (authorizationResult.isValidated()) {
                Map<String, ResourcePersistenceResult> persistenceOperationResultsList = makePersistenceOperations(informationModel);
                prepareContentOfRPCResponse(persistenceOperationResultsList);
            } else {
                informationModelResponse.setStatus(400);
                informationModelResponse.setMessage(authorizationResult.getMessage());
            }
            */


            informationModel = informationModelValidationResult.getObjectDescription();
            log.info("Information Model received from Semantic Manager! IM id: " + informationModel.getId());

            InformationModelPersistenceResult informationModelPersistenceResult = makePersistenceOperations(informationModel);
            prepareContentOfRPCResponse(informationModelPersistenceResult);

        } else {
            informationModelResponse.setStatus(500);
            informationModelResponse.setMessage("Validation Error. Semantic Manager message: "
                    + informationModelValidationResult.getMessage());
        }

        sendRpcResponse();
    }

    /**
     * Performing persistence operations accordingly - saving or modyfying resources in Mongo DB.
     *
     * @param informationModel
     */
    private InformationModelPersistenceResult makePersistenceOperations(InformationModel informationModel) {
        InformationModelPersistenceResult informationModelPersistenceResult = new InformationModelPersistenceResult() {
        };
        switch (operationType) {
            case CREATION:
                informationModelPersistenceResult = this.repositoryManager.saveInformationModel(informationModel);
                break;
            case MODIFICATION:
                informationModelPersistenceResult = this.repositoryManager.modifyInformationModel(informationModel);
                break;
        }
        if (informationModelPersistenceResult.getStatus() != 200) {
            log.error("Information Model could not be processed. Check response object for details.");
            informationModelResponse.setStatus(500);
            informationModelResponse.setMessage("Information Model could not be processed. Check response object for details.");
        }
        return informationModelPersistenceResult;
    }

    /**
     * prepares content of message with bulk save result
     */
    private void prepareContentOfRPCResponse(InformationModelPersistenceResult informationModelPersistenceResult) {
        InformationModel informationModel = informationModelPersistenceResult.getInformationModel();
        if (informationModelPersistenceResult.getStatus() == 200) {

            rabbitManager.sendInformationModelOperationMessage(informationModel, operationType);

            log.info("IM operation successful! (" + this.operationType.toString() + ")");
            informationModelResponse.setStatus(200);
            informationModelResponse.setMessage("IM operation successful! (" + this.operationType.toString() + ")");
            informationModelResponse.setInformationModel(informationModel);

        } else {
            rollback(informationModel);

            log.error("IM operation request ERROR");
            informationModelResponse.setStatus(500);
            informationModelResponse.setMessage("IM operation request ERROR");
        }
    }

    /**
     * Sending RPC response message with list of Resources (with IDs added if process succeed) and status code
     * //odeslanie na RPC core response (z listą resourców z ID'kami jesli zapis sie powiódł)
     */
    private void sendRpcResponse() {
        String response = "error";
        try {
            response = mapper.writeValueAsString(informationModelResponse);
        } catch (JsonProcessingException e) {
            log.error(e);
        }

        try {
            rabbitManager.sendRPCReplyMessage(rpcConsumer, rpcProperties, rpcEnvelope, response);

            rabbitManager.closeConsumer(this, this.getChannel());
        } catch (IOException e) {
            log.error(e);
        }
    }

    /**
     * Type of transaction rollback used for bulk registration, triggered for all successfully saved objects when
     * any of given objects in list did not save successfully in database.
     *
     * @param informationModel
     */
    private void rollback(InformationModel informationModel) {
        switch (operationType) {
            case CREATION:
                repositoryManager.removeInformationModel(informationModel);
                break;
            case MODIFICATION:
                log.error("ROLLBACK NOT IMPLEMENTED!");
                //todo ??
                break;
        }
    }
}
