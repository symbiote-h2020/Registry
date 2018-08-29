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
import eu.h2020.symbiote.security.helpers.SDevHelper;
import eu.h2020.symbiote.utils.ValidationUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.http.HttpStatus;

import java.io.IOException;
import java.security.NoSuchAlgorithmException;

/**
 * Created by mateuszl on 11.06.2018.
 */
public class SspSdevModificationRequestConsumer extends DefaultConsumer {

    private static Log log = LogFactory.getLog(SspSdevModificationRequestConsumer.class);
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
    public SspSdevModificationRequestConsumer(Channel channel,
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

        log.info(" [x] Received Sdev (SspRegInfo) to modify");
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

        /////////////////// checking access by token verification

        AuthorizationResult tokenAuthorizationResult = authorizationManager.checkSdevOperationAccess(
                request.getSecurityRequest(),
                request.getSspId()); //todo partially MOCKED

        if (!tokenAuthorizationResult.isValidated()) {
            log.error("Token invalid: \"" + tokenAuthorizationResult.getMessage() + "\"");
            prepareAndSendErrorResponse(400, String.format("Error: \" %s \"", tokenAuthorizationResult.getMessage()));
            return;
        }

        /////////////////// access and migration verification

        try {
            //check if given ids have a match needed
            validateAccess(request);

            //checks if derivedKey1 in given sDev is not empty
            checkIfDK1IsNotBlank(request.getBody());

            //check if hashes are equal
            checkIfHashfieldsAreEqual(request);

        } catch (NoSuchAlgorithmException | IllegalAccessException e) {
            prepareAndSendErrorResponse(HttpStatus.SC_BAD_REQUEST, e.getMessage());
            return;
        } catch (Exception e) {
            prepareAndSendErrorResponse(HttpStatus.SC_INTERNAL_SERVER_ERROR, e.getMessage());
            return;
        }

        //todo if we want to check if there is a migration, we have to compare SSP ID from request and Sdev PluginId

        SspRegInfo sDev = request.getBody();
        response.setBody(sDev);

        if (ValidationUtils.validateFields(sDev)) {

            SdevPersistenceResult sdevPersistenceResult = this.repositoryManager.modifySdev(sDev);

            response.setStatus(sdevPersistenceResult.getStatus());
            response.setMessage(sdevPersistenceResult.getMessage());
            response.setBody(sdevPersistenceResult.getSdev());

            if (sdevPersistenceResult.getStatus() == 200) {
                rabbitManager.sendSdevOperationMessage(sdevPersistenceResult.getSdev(),
                        RegistryOperationType.MODIFICATION);
            } else {
                prepareAndSendErrorResponse(HttpStatus.SC_BAD_REQUEST, "Error occurred during Sdev (SspRegInfo) saving in db, due to: " +
                        sdevPersistenceResult.getMessage());
            }
        } else {
            prepareAndSendErrorResponse(HttpStatus.SC_BAD_REQUEST, "Given Sdev (SspRegInfo) has some fields null or empty");
        }
        rabbitManager.sendRPCReplyMessage(this, properties, envelope, mapper.writeValueAsString(response));

    }

    private void checkIfDK1IsNotBlank(SspRegInfo receivedSdev) throws IllegalAccessException {
        if (StringUtils.isBlank(receivedSdev.getDerivedKey1()))
            throw new IllegalAccessException("DerivedKey1 can not be blank!");
    }


    private void checkIfHashfieldsAreEqual(CoreSdevRegistryRequest request) throws NoSuchAlgorithmException, IllegalAccessException {

        SspRegInfo receivedSdev = request.getBody();

        String receivedSdevId = receivedSdev.getSymId();

        String receivedSdevHashField = receivedSdev.getHashField();


        SspRegInfo sDevFromDbById = repositoryManager.getSdevById(receivedSdevId);

        //Only do hashchecks for roaming devices
        if (sDevFromDbById.getRoaming()) {


            String previousDK1 = sDevFromDbById.getDerivedKey1();

            String newHash = calculateHash(receivedSdevId, previousDK1);

            if (!newHash.equals(receivedSdevHashField)) {

                // if new hash field is different than old hashfield -> throw illegal access exception

                String msg = String.format("Sdev Hash comparing failed! Received Sdev Hash: %s . Calculated hash: %s", receivedSdevHashField, newHash);

                log.error(msg);
                throw new IllegalAccessException(msg);
            }

        } else {
            log.debug("Sdev is not roaming - skipping hash comparison");
        }
        //if new hash field is the same as old one -> just update fields in sdev
    }


    private String calculateHash(String symId, String previousDK1) throws NoSuchAlgorithmException {
        String hash;

        String s = String.format("%s%s", symId, previousDK1);

        hash = SDevHelper.hashSHA1(s);

        log.info(hash);
        return hash;
    }

    private void validateAccess(CoreSdevRegistryRequest request) throws IllegalAccessException {
        ValidationUtils.validateIfSdevMatchWithSspForModification(repositoryManager, request);
    }

    private void prepareAndSendErrorResponse(int status, String message) throws IOException {
        log.error(message);
        this.response.setStatus(status);
        this.response.setMessage(message);
        rabbitManager.sendRPCReplyMessage(this, properties, envelope, mapper.writeValueAsString(response));
    }
}