package eu.h2020.symbiote.messaging.consumers.sspResource;

import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.gson.JsonSyntaxException;
import com.rabbitmq.client.AMQP;
import com.rabbitmq.client.Channel;
import com.rabbitmq.client.DefaultConsumer;
import com.rabbitmq.client.Envelope;
import eu.h2020.symbiote.core.internal.CoreSspResourceRegistryRequest;
import eu.h2020.symbiote.core.internal.CoreSspResourceRegistryResponse;
import eu.h2020.symbiote.managers.AuthorizationManager;
import eu.h2020.symbiote.managers.RabbitManager;
import eu.h2020.symbiote.managers.RepositoryManager;
import eu.h2020.symbiote.model.RegistryOperationType;
import eu.h2020.symbiote.model.persistenceResults.AuthorizationResult;
import eu.h2020.symbiote.utils.ValidationUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.http.HttpStatus;

import java.io.IOException;

/**
 * Created by mateuszl on 30.05.2018.
 */
public class SspResourceCreationRequestConsumer extends DefaultConsumer {

    //// TODO: 01.06.2018 change all heavy String concatenations to String.format !!

    private static Log log = LogFactory.getLog(SspResourceCreationRequestConsumer.class);
    private ObjectMapper mapper;
    private RabbitManager rabbitManager;
    private AuthorizationManager authorizationManager;
    private RepositoryManager repositoryManager;
    private CoreSspResourceRegistryResponse registryResponse;
    private Envelope envelope;
    private AMQP.BasicProperties properties;


    /**
     * Constructs a new instance and records its association to the passed-in channel.
     * Managers beans passed as parameters because of lack of possibility to inject it to consumer.
     * <p>
     * It should receive CoreSspResourceRegistryRequest which has Map<String, Resource> as body.
     * It passes the body to Semantic Manager for translation to RDF.
     *
     * @param channel       the channel to which this consumer is attached
     * @param rabbitManager rabbit manager bean passed for access to messages manager
     */
    public SspResourceCreationRequestConsumer(Channel channel,
                                              RabbitManager rabbitManager,
                                              AuthorizationManager authorizationManager,
                                              RepositoryManager repositoryManager) {
        super(channel);
        this.rabbitManager = rabbitManager;
        this.authorizationManager = authorizationManager;
        this.repositoryManager = repositoryManager;
        this.registryResponse = new CoreSspResourceRegistryResponse();
        this.mapper = new ObjectMapper();
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
        CoreSspResourceRegistryRequest request;
        String message = new String(body, "UTF-8");
        this.envelope = envelope;
        this.properties = properties;

        log.info(" [x] Received Ssp resources to create (CoreSspResourceRegistryRequest): \n" + message);

        try {
            try {
                //request from CCI received and deserialized
                request = mapper.readValue(message, CoreSspResourceRegistryRequest.class);
            } catch (JsonSyntaxException | JsonMappingException e) {
                log.error("Unable to get CoreSspResourceRegistryRequest from Message body!", e);
                sendErrorReply(HttpStatus.SC_BAD_REQUEST, "Content invalid. Could not deserialize. Resources not created!");
                return;
            }

            //checking access by token verification
            AuthorizationResult tokenAuthorizationResult = authorizationManager.checkSdevOperationAccess(
                    request.getSecurityRequest(),
                    request.getSspId()); //todo partially MOCKED

            log.debug("1");
            if( tokenAuthorizationResult == null  ) {
                log.error("Token authorization result is null");
                sendErrorReply(400, "Error: authorization result is null");
                return;
            }

            log.debug("2");
            if (!tokenAuthorizationResult.isValidated()) {
                log.error("Token invalid: \"" + tokenAuthorizationResult.getMessage() + "\"");
                sendErrorReply(400, String.format("Error: \" %s \"", tokenAuthorizationResult.getMessage()));
                return;
            }

            //checking access by verification of fields needed for that operation
            if (!validateAccess(request)) return;
            log.debug("3");

            if (request.getBody() != null) {

                log.debug("4");
                log.info("Message to Semantic Manager Sent. Request: " + request.getBody());
                //contact with Semantic Manager accordingly to Type of object Description received
                //sending JSON content to Semantic Manager and passing responsibility to another consumer

                log.debug("5");
                rabbitManager.sendSspResourceJsonTranslationRpcMessage(this, properties, envelope,
                        message,
                        request.getSdevId(),
                        request.getSspId(),
                        RegistryOperationType.CREATION,
                        request.getFilteringPolicies(),
                        request.getBody()
                );

            } else {
                sendErrorReply(400, "Message body is null!");
            }

        } catch (Exception e) {
            sendErrorReply(500, "Consumer critical error: " + e);
        }
    }

    private boolean validateAccess(CoreSspResourceRegistryRequest request) throws IOException {

        log.debug("Validating access for the request");
        try {
            ValidationUtils.checkIfResourcesDoesNotHaveIds(request);
            //// TODO: 09.07.2018 check!!

        } catch (IllegalArgumentException e) {
            sendErrorReply(HttpStatus.SC_BAD_REQUEST, "One of the resources has ID or list with resources is invalid. Resources not created!");
            return false;
        }

        try {
            log.debug("Validating SSP resource");
            ValidationUtils.validateSspResource(request, repositoryManager);
        } catch (IllegalArgumentException e) {
            sendErrorReply(HttpStatus.SC_BAD_REQUEST, e.getMessage());
            return false;
        } catch (Exception e) {
            sendErrorReply(HttpStatus.SC_INTERNAL_SERVER_ERROR, e.getMessage());
            return false;
        }

        log.debug("Validate Access success, returning true");
        return true;
    }


    /**
     * Sets status and massage in Registry Response for this Consumer and triggers sending this response in JSON format.
     *
     * @param status
     * @param message
     * @throws IOException
     */
    private void sendErrorReply(int status, String message) throws IOException {
        log.error(message);
        registryResponse.setStatus(status);
        registryResponse.setMessage(message);
        rabbitManager.sendRPCReplyMessage(this, this.properties, this.envelope, mapper.writeValueAsString(registryResponse));
    }

}
