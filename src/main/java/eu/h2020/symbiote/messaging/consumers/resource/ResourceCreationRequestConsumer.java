package eu.h2020.symbiote.messaging.consumers.resource;

import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.gson.JsonSyntaxException;
import com.rabbitmq.client.AMQP;
import com.rabbitmq.client.Channel;
import com.rabbitmq.client.DefaultConsumer;
import com.rabbitmq.client.Envelope;
import eu.h2020.symbiote.core.cci.RDFResourceRegistryRequest;
import eu.h2020.symbiote.core.internal.CoreResourceRegistryRequest;
import eu.h2020.symbiote.core.internal.CoreResourceRegistryResponse;
import eu.h2020.symbiote.core.internal.DescriptionType;
import eu.h2020.symbiote.core.internal.ResourceInstanceValidationRequest;
import eu.h2020.symbiote.managers.AuthorizationManager;
import eu.h2020.symbiote.managers.RabbitManager;
import eu.h2020.symbiote.managers.RepositoryManager;
import eu.h2020.symbiote.model.RegistryOperationType;
import eu.h2020.symbiote.model.persistenceResults.AuthorizationResult;
import eu.h2020.symbiote.security.accesspolicies.common.IAccessPolicySpecifier;
import eu.h2020.symbiote.utils.ValidationUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.http.HttpStatus;

import java.io.IOException;
import java.util.Map;

/**
 * RabbitMQ Consumer implementation used for Resource Creation actions
 * <p>
 * Created by mateuszl
 */
public class ResourceCreationRequestConsumer extends DefaultConsumer {

    private static Log log = LogFactory.getLog(ResourceCreationRequestConsumer.class);
    private ObjectMapper mapper;
    private RabbitManager rabbitManager;
    private AuthorizationManager authorizationManager;
    private RepositoryManager repositoryManager;
    private Map<String, IAccessPolicySpecifier> policiesMap;
    private CoreResourceRegistryResponse response;
    private Envelope envelope;
    private AMQP.BasicProperties properties;

    /**
     * Constructs a new instance and records its association to the passed-in channel.
     * Managers beans passed as parameters because of lack of possibility to inject it to consumer.
     *
     * @param channel       the channel to which this consumer is attached
     * @param rabbitManager rabbit manager bean passed for access to messages manager
     */
    public ResourceCreationRequestConsumer(Channel channel,
                                           RabbitManager rabbitManager,
                                           AuthorizationManager authorizationManager,
                                           RepositoryManager repositoryManager) {
        super(channel);
        this.rabbitManager = rabbitManager;
        this.authorizationManager = authorizationManager;
        this.repositoryManager = repositoryManager;
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
        CoreResourceRegistryRequest request = null;
        response = new CoreResourceRegistryResponse();
        String message = new String(body, "UTF-8");
        log.info(" [x] Received resources to create (CoreResourceRegistryRequest)");
        log.info("Content: " + message);
        this.envelope = envelope;
        this.properties = properties;

        try {
            try {
                //request from CCI received and deserialized
                request = mapper.readValue(message, CoreResourceRegistryRequest.class);
            } catch (JsonSyntaxException | JsonMappingException e) {
                prepareAndSendErrorResponse(HttpStatus.SC_BAD_REQUEST, "Unable to get CoreResourceRegistryRequest from Message body! Resources not modified! " + e);
            }

            if (request != null) {
                //checking access by token verification
                AuthorizationResult tokenAuthorizationResult = authorizationManager.checkSinglePlatformOperationAccess(request.getSecurityRequest(), request.getPlatformId());
                if (!tokenAuthorizationResult.isValidated()) {
                    prepareAndSendErrorResponse(400, "Token invalid: \"" + tokenAuthorizationResult.getMessage() + "\"");
                    return;
                }

                if (request.getBody() != null) {

                    this.policiesMap = request.getFilteringPolicies();

                    //contact with Semantic Manager accordingly to Type of object Description received
                    switch (request.getDescriptionType()) {
                        case RDF:
                            log.info("Message to Semantic Manager Sent. Request: " + request.getBody());

                            createAndSendValidationRequest(envelope, properties, request);

                            break;
                        case BASIC:
                            if (ValidationUtils.checkIfResourcesDoesNotHaveIds(request)) {
                                log.info("Message to Semantic Manager Sent. Request: " + request.getBody());
                                //sending JSON content to Semantic Manager and passing responsibility to another consumer
                                rabbitManager.sendResourceJsonTranslationRpcMessage(this, properties, envelope,
                                        message,
                                        request.getPlatformId(),
                                        RegistryOperationType.CREATION,
                                        authorizationManager,
                                        this.policiesMap,
                                        request.getBody());
                            } else {
                                prepareAndSendErrorResponse(HttpStatus.SC_BAD_REQUEST, "One of the resources has ID or list with resources is invalid. Resources not created!");
                            }
                            break;
                    }
                } else {
                    prepareAndSendErrorResponse(400, "Message body is null!");
                }
            }
        } catch (Exception e) {
            prepareAndSendErrorResponse(500, "Consumer critical error!" + e);
        }
    }

    private void createAndSendValidationRequest(Envelope envelope, AMQP.BasicProperties properties,
                                                CoreResourceRegistryRequest request) throws IOException {
        RDFResourceRegistryRequest rdfResourceRegistryRequest = mapper.readValue(request.getBody(), RDFResourceRegistryRequest.class);

        String requestedInterworkingServiceUrl = rdfResourceRegistryRequest.getInterworkingServiceUrl();

        String informationModelIdByInterworkingServiceUrl =
                repositoryManager.getInformationModelIdByInterworkingServiceUrl(request.getPlatformId(), requestedInterworkingServiceUrl);

        if (informationModelIdByInterworkingServiceUrl == null) {
            prepareAndSendErrorResponse(400, "Requested Interworking Service Url does not exist for given platform! Resource not accepted.");
        } else {
            ResourceInstanceValidationRequest resourceInstanceValidationRequest = new ResourceInstanceValidationRequest();
            resourceInstanceValidationRequest.setRdf(rdfResourceRegistryRequest.getBody().getRdf());
            resourceInstanceValidationRequest.setRdfFormat(rdfResourceRegistryRequest.getBody().getRdfFormat());
            resourceInstanceValidationRequest.setInformationModelId(informationModelIdByInterworkingServiceUrl);
            resourceInstanceValidationRequest.setInterworkingServiceURL(requestedInterworkingServiceUrl);

            //sending RDF content to Semantic Manager and passing responsibility to another consumer
            rabbitManager.sendResourceRdfValidationRpcMessage(this, properties, envelope,
                    mapper.writeValueAsString(resourceInstanceValidationRequest),
                    request.getPlatformId(), RegistryOperationType.CREATION, authorizationManager, this.policiesMap);
        }
    }

    private void prepareAndSendErrorResponse(int status, String message) throws IOException {
        log.error(message);
        this.response.setStatus(status);
        this.response.setMessage(message);
        this.response.setDescriptionType(DescriptionType.BASIC);
        this.response.setServiceResponse(authorizationManager.generateServiceResponse());
        rabbitManager.sendRPCReplyMessage(this, properties, envelope, mapper.writeValueAsString(response));
    }
}
