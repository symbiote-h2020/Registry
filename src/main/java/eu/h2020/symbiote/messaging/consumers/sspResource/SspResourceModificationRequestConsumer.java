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
import eu.h2020.symbiote.model.cim.Device;
import eu.h2020.symbiote.model.cim.Resource;
import eu.h2020.symbiote.model.cim.Service;
import eu.h2020.symbiote.model.persistenceResults.AuthorizationResult;
import eu.h2020.symbiote.security.accesspolicies.common.IAccessPolicySpecifier;
import eu.h2020.symbiote.utils.ValidationUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.http.HttpStatus;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Created by mateuszl on 30.05.2018.
 */
public class SspResourceModificationRequestConsumer extends DefaultConsumer {

    //// TODO: 01.06.2018 change all heavy String concatenations to String.format !!

    private static Log log = LogFactory.getLog(SspResourceModificationRequestConsumer.class);
    private ObjectMapper mapper;
    private RabbitManager rabbitManager;
    private AuthorizationManager authorizationManager;
    private RepositoryManager repositoryManager;
    private Map<String, IAccessPolicySpecifier> policiesMap;
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
    public SspResourceModificationRequestConsumer(Channel channel,
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

        log.info(" [x] Received Ssp resources to modify (CoreSspResourceRegistryRequest): \n" + message);

        try {
            try {
                //request from CCI received and deserialized
                request = mapper.readValue(message, CoreSspResourceRegistryRequest.class);
            } catch (JsonSyntaxException | JsonMappingException e) {
                log.error("Unable to get CoreSspResourceRegistryRequest from Message body!", e);
                sendErrorReply(HttpStatus.SC_BAD_REQUEST, "Content invalid. Could not deserialize. Resources not modified!");
                return;
            }

            //checking access by token verification
            AuthorizationResult tokenAuthorizationResult = authorizationManager.checkSdevOperationAccess(
                    request.getSecurityRequest(),
                    request.getSdevId()); //todo MOCKED

            if (!tokenAuthorizationResult.isValidated()) {
                log.error("Token invalid: \"" + tokenAuthorizationResult.getMessage() + "\"");
                sendErrorReply(400, String.format("Error: \" %s \"", tokenAuthorizationResult.getMessage()));
                return;
            }

            //checking access by verification of fields needed for that operation
            validateAccess(request);

            //// TODO: 05.06.2018 check if the resource is bounded to existing SSP

            if (request.getBody() != null) {
                //contact with Semantic Manager accordingly to Type of object Description received
                if (checkIfResourcesHaveNullOrEmptyId(request)) {
                    //// TODO: 04.07.2018 MOVE to ValidationUtils and refactor
                    //resources should not have empty or null ids
                    log.error("One of the resources has ID or list with resources is invalid. Resources not modified!");
                    sendErrorReply(HttpStatus.SC_BAD_REQUEST, "One of the resources has ID or list with resources is invalid. Resources not modified!");
                } else {
                    log.info("Message to Semantic Manager Sent. Request: " + request.getBody());
                    //sending JSON content to Semantic Manager and passing responsibility to another consumer

                    this.policiesMap = request.getFilteringPolicies();

                    rabbitManager.sendSspResourceJsonTranslationRpcMessage(this, properties, envelope,
                            message,
                            request.getSdevId(),
                            request.getSspId(),
                            RegistryOperationType.MODIFICATION,
                            this.policiesMap,
                            request.getBody()
                    );
                }
            } else {
                log.error("Message body is null!");
                sendErrorReply(400, "Message body is null!");
            }

        } catch (Exception e) {
            log.error(e);
            sendErrorReply(500, "Consumer critical error");
        }
    }

    private void validateAccess(CoreSspResourceRegistryRequest request) throws IOException {
        try {
            ValidationUtils.validateSspResource(request, repositoryManager);
        } catch (IllegalArgumentException e) {
            sendErrorReply(HttpStatus.SC_BAD_REQUEST, e.getMessage());
        } catch (Exception e) {
            sendErrorReply(HttpStatus.SC_INTERNAL_SERVER_ERROR, e.getMessage());
        }
    }

    /**
     * Sets status and massage in Registry Response for this Consumer and triggers sending this response in JSON format.
     *
     * @param status
     * @param message
     * @throws IOException
     */
    private void sendErrorReply(int status, String message) throws IOException {
        registryResponse.setStatus(status);
        registryResponse.setMessage(message);
        rabbitManager.sendRPCReplyMessage(this, this.properties, this.envelope, mapper.writeValueAsString(registryResponse));
    }

    /**
     * Checks if given request consists of resources, which does not have any content in ID field.
     *
     * @param request
     * @return true if given resources don't have an ID.
     */
    private boolean checkIfResourcesHaveNullOrEmptyId(CoreSspResourceRegistryRequest request) {
        List<Resource> resources = request.getBody().values().stream().collect(Collectors.toList());

        for (Resource resource : resources) {
            if (!resourceHasId(resource)) return false;

            List<Service> services = new ArrayList<>();
            try {
                if (resource instanceof Device) services = ((Device) resource).getServices();
            } catch (Exception e) {
                log.error(e);
                return false;
            }

            if (services != null && !services.isEmpty()) {
                for (Service service : services) {
                    if (!resourceHasId(service)) return false;
                }
            }
        }

        return true;
    }

    private boolean resourceHasId(Resource resource) {
        if (StringUtils.isBlank(resource.getId())) {
            log.error("One of the resources (or actuating services) does not have an ID!");
            return false;
        }
        return true;
    }

}
