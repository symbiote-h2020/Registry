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
import eu.h2020.symbiote.managers.AuthorizationManager;
import eu.h2020.symbiote.managers.RabbitManager;
import eu.h2020.symbiote.managers.RepositoryManager;
import eu.h2020.symbiote.model.RegistryOperationType;
import eu.h2020.symbiote.model.persistenceResults.AuthorizationResult;
import eu.h2020.symbiote.model.persistenceResults.SdevPersistenceResult;
import eu.h2020.symbiote.utils.ValidationUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.http.HttpStatus;

import java.io.IOException;

public class SspSdevCreationRequestConsumer extends DefaultConsumer {

    private static Log log = LogFactory.getLog(SspSdevCreationRequestConsumer.class);
    private RepositoryManager repositoryManager;
    private RabbitManager rabbitManager;
    private AuthorizationManager authorizationManager;
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
    public SspSdevCreationRequestConsumer(Channel channel,
                                          RabbitManager rabbitManager,
                                          RepositoryManager repositoryManager,
                                          AuthorizationManager authorizationManager) {
        super(channel);
        this.repositoryManager = repositoryManager;
        this.rabbitManager = rabbitManager;
        this.authorizationManager = authorizationManager;
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

        log.info(" [x] Received Sdev (SspRegInfo) to create");
        log.info("Content: " + message);
        this.envelope = envelope;
        this.properties = properties;

        /////////////////// Request retrieval from message

        try {
            request = mapper.readValue(message, CoreSdevRegistryRequest.class);

        } catch (JsonSyntaxException | JsonMappingException e) {
            prepareAndSendErrorResponse(HttpStatus.SC_BAD_REQUEST, "Error occurred during Sdev (SspRegInfo) retrieving from message. " + e);
            return;
        }

        /////////////////// access verification

        try {
            //check if given ids have a match needed
            validateAccess(request);

        } catch (IllegalAccessException e) {
            prepareAndSendErrorResponse(HttpStatus.SC_BAD_REQUEST, e.getMessage());
            return;
        } catch (Exception e) {
            prepareAndSendErrorResponse(HttpStatus.SC_INTERNAL_SERVER_ERROR, e.getMessage());
            return;
        }

        /////////////////// checking access by token verification

        AuthorizationResult tokenAuthorizationResult = authorizationManager.checkSdevOperationAccess(
                request.getSecurityRequest(),
                request.getBody().getSymId()); //todo partially MOCKED

        if (!tokenAuthorizationResult.isValidated()) {
            log.error("Token invalid: \"" + tokenAuthorizationResult.getMessage() + "\"");
            prepareAndSendErrorResponse(400, String.format("Error: \" %s \"", tokenAuthorizationResult.getMessage()));
            return;
        }

        SspRegInfo sDev = request.getBody();
        response.setBody(sDev);

        if (ValidationUtils.validateFields(sDev)) {

            SdevPersistenceResult sdevPersistenceResult = this.repositoryManager.saveSdev(sDev);

            response.setStatus(sdevPersistenceResult.getStatus());
            response.setMessage(sdevPersistenceResult.getMessage());
            if (sdevPersistenceResult.getStatus() == 200) {
                rabbitManager.sendSdevOperationMessage(sdevPersistenceResult.getSdev(),
                        RegistryOperationType.CREATION);
            } else {
                prepareAndSendErrorResponse(HttpStatus.SC_BAD_REQUEST, "Error occurred during Sdev (SspRegInfo) saving in db, due to: " +
                        sdevPersistenceResult.getMessage());
            }

        } else {
            prepareAndSendErrorResponse(HttpStatus.SC_BAD_REQUEST, "Given Sdev (SspRegInfo) has some fields null or empty");
        }

        //sdev class has newly created symId
        response.setBody(sDev);
        rabbitManager.sendRPCReplyMessage(this, properties, envelope, mapper.writeValueAsString(response));

    }

    private void validateAccess(CoreSdevRegistryRequest request) throws IllegalAccessException {
        ValidationUtils.validateSdev(repositoryManager, request);
    }

    private void prepareAndSendErrorResponse(int status, String message) throws IOException {
        log.error(message);
        this.response.setStatus(status);
        this.response.setMessage(message);
        rabbitManager.sendRPCReplyMessage(this, properties, envelope, mapper.writeValueAsString(response));
    }
}
