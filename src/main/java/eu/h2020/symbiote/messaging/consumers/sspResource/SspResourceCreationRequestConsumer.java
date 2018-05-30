package eu.h2020.symbiote.messaging.consumers.sspResource;

import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.gson.JsonSyntaxException;
import com.rabbitmq.client.AMQP;
import com.rabbitmq.client.Channel;
import com.rabbitmq.client.DefaultConsumer;
import com.rabbitmq.client.Envelope;
import eu.h2020.symbiote.core.cci.RDFResourceRegistryRequest;
import eu.h2020.symbiote.core.internal.*;
import eu.h2020.symbiote.managers.AuthorizationManager;
import eu.h2020.symbiote.managers.RabbitManager;
import eu.h2020.symbiote.managers.RepositoryManager;
import eu.h2020.symbiote.messaging.consumers.resource.ResourceCreationRequestConsumer;
import eu.h2020.symbiote.model.RegistryOperationType;
import eu.h2020.symbiote.model.cim.*;
import eu.h2020.symbiote.model.persistenceResults.AuthorizationResult;
import eu.h2020.symbiote.security.accesspolicies.common.IAccessPolicySpecifier;
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
public class SspResourceCreationRequestConsumer extends DefaultConsumer {

    private static Log log = LogFactory.getLog(ResourceCreationRequestConsumer.class);
    private ObjectMapper mapper;
    private RabbitManager rabbitManager;
    private AuthorizationManager authorizationManager;
    private RepositoryManager repositoryManager;
    private Map<String, IAccessPolicySpecifier> policiesMap;

    /**
     * Constructs a new instance and records its association to the passed-in channel.
     * Managers beans passed as parameters because of lack of possibility to inject it to consumer.
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
        CoreSspResourceRegistryRequest request = null;
        CoreSspResourceRegistryResponse registryResponse = new CoreSspResourceRegistryResponse();
        String message = new String(body, "UTF-8");
        log.info(" [x] Received Ssp resources to create (CoreSspResourceRegistryRequest): \n" + message);

        try {
            try {
                //request from CCI received and deserialized
                request = mapper.readValue(message, CoreSspResourceRegistryRequest.class);
            } catch (JsonSyntaxException | JsonMappingException e) {
                log.error("Unable to get CoreSspResourceRegistryRequest from Message body!", e);
                registryResponse.setStatus(HttpStatus.SC_BAD_REQUEST);
                registryResponse.setMessage("Content invalid. Could not deserialize. Resources not created!");
                rabbitManager.sendRPCReplyMessage(this, properties, envelope, mapper.writeValueAsString(registryResponse));
            }

            if (request != null) {
                //checking access by token verification
                AuthorizationResult tokenAuthorizationResult = authorizationManager.checkSdevOperationAccess(request.getSecurityRequest(), request.getSdevId());
                if (!tokenAuthorizationResult.isValidated()) {
                    log.error("Token invalid: \"" + tokenAuthorizationResult.getMessage() + "\"");
                    registryResponse.setStatus(400);
                    registryResponse.setMessage("Error: \"" + tokenAuthorizationResult.getMessage() + "\"");
                    rabbitManager.sendRPCReplyMessage(this, properties, envelope,
                            mapper.writeValueAsString(registryResponse));
                    return;
                }

                if (request.getBody() != null) {

                    this.policiesMap = request.getFilteringPolicies();

                    //contact with Semantic Manager accordingly to Type of object Description received
                    if (checkIfResourcesHaveNullOrEmptyId(request)) {
                        log.info("Message to Semantic Manager Sent. Request: " + request.getBody());
                        //sending JSON content to Semantic Manager and passing responsibility to another consumer

                        String requestBodyAsString = mapper.writeValueAsString(request.getBody());

                        //// TODO: 30.05.2018 todo!
                        rabbitManager.sendSspResourceJsonTranslationRpcMessage(this, properties, envelope,
                                message,
                                request.getSdevId(),
                                RegistryOperationType.CREATION,
                                authorizationManager,
                                this.policiesMap,
                                requestBodyAsString
                        );
                    } else {
                        log.error("One of the resources has ID or list with resources is invalid. Resources not created!");
                        registryResponse.setStatus(HttpStatus.SC_BAD_REQUEST);
                        registryResponse.setMessage("One of the resources has ID or list with resources is invalid. Resources not created!");
                        rabbitManager.sendRPCReplyMessage(this, properties, envelope,
                                mapper.writeValueAsString(registryResponse));
                    }
                } else {
                    log.error("Message body is null!");
                    registryResponse.setStatus(400);
                    registryResponse.setMessage("Message body is null!");
                    rabbitManager.sendRPCReplyMessage(this, properties, envelope,
                            mapper.writeValueAsString(registryResponse));
                }
            }
        } catch (Exception e) {
            log.error(e);
            registryResponse.setStatus(500);
            registryResponse.setMessage("Consumer critical error!");
            rabbitManager.sendRPCReplyMessage(this, properties, envelope,
                    mapper.writeValueAsString(registryResponse));
        }
    }

    private void createAndSendValidationRequest(Envelope envelope, AMQP.BasicProperties properties,
                                                CoreResourceRegistryRequest request,
                                                CoreResourceRegistryResponse registryResponse) throws IOException {
        RDFResourceRegistryRequest rdfResourceRegistryRequest = mapper.readValue(request.getBody(), RDFResourceRegistryRequest.class);

        String requestedInterworkingServiceUrl = rdfResourceRegistryRequest.getInterworkingServiceUrl();

        String informationModelIdByInterworkingServiceUrl =
                repositoryManager.getInformationModelIdByInterworkingServiceUrl(request.getPlatformId(), requestedInterworkingServiceUrl);

        if (informationModelIdByInterworkingServiceUrl == null) {
            log.error("Requested Interworking Service Url does not exist for given platform! Resource not accepted.");
            registryResponse.setStatus(400);
            registryResponse.setMessage("Requested Interworking Service Url does not exist for given platform! Resource not accepted.");
            registryResponse.setServiceResponse(authorizationManager.generateServiceResponse());
            rabbitManager.sendRPCReplyMessage(this, properties, envelope,
                    mapper.writeValueAsString(registryResponse));
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

    /**
     * Checks if given request consists of resources, which does not have any content in ID field.
     *
     * @param request
     * @return true if given resources don't have an ID.
     */
    private boolean checkIfResourcesHaveNullOrEmptyId(CoreSspResourceRegistryRequest request) {
        List<Resource> resources = request.getBody().values().stream().collect(Collectors.toList());
        return checkIds(resources);
    }

    private boolean checkIds(List<Resource> resources) {

        //// TODO: 30.05.2018 todo!

        try {
            for (Resource resource : resources) {
                if (!checkId(resource)) return false;
                List<Service> services = new ArrayList<>();
                if (resource instanceof Device) {
                    services = ((Device) resource).getServices();
                } else if (resource instanceof MobileSensor) {
                    services = ((MobileSensor) resource).getServices();
                } else if (resource instanceof Actuator) {
                    services = ((Actuator) resource).getServices();
                }
                if (services != null && !services.isEmpty()) {
                    for (Service service : services) {
                        if (!checkId(service)) return false;
                    }
                }
            }
        } catch (Exception e) {
            log.error(e);
            return false;
        }

        return true;
    }

    private boolean checkId(Resource resource) {
        if (resource.getId() != null && !resource.getId().isEmpty()) {
            log.error("One of the resources (or actuating services) has an ID!");
            return false;
        }
        return true;
    }

}
