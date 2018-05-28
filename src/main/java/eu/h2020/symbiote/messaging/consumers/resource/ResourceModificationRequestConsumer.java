package eu.h2020.symbiote.messaging.consumers.resource;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.gson.JsonSyntaxException;
import com.rabbitmq.client.AMQP;
import com.rabbitmq.client.Channel;
import com.rabbitmq.client.DefaultConsumer;
import com.rabbitmq.client.Envelope;
import eu.h2020.symbiote.core.internal.CoreResourceRegistryRequest;
import eu.h2020.symbiote.core.internal.CoreResourceRegistryResponse;
import eu.h2020.symbiote.managers.AuthorizationManager;
import eu.h2020.symbiote.managers.RabbitManager;
import eu.h2020.symbiote.managers.RepositoryManager;
import eu.h2020.symbiote.model.persistenceResults.AuthorizationResult;
import eu.h2020.symbiote.model.RegistryOperationType;
import eu.h2020.symbiote.model.cim.Device;
import eu.h2020.symbiote.model.cim.Resource;
import eu.h2020.symbiote.model.cim.Service;
import eu.h2020.symbiote.security.accesspolicies.common.IAccessPolicySpecifier;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.http.HttpStatus;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * RabbitMQ Consumer implementation used for Resource Modification actions
 * <p>
 * Created by mateuszl
 */
public class ResourceModificationRequestConsumer extends DefaultConsumer {

    private static Log log = LogFactory.getLog(ResourceModificationRequestConsumer.class);
    private AuthorizationManager authorizationManager;
    private RabbitManager rabbitManager;
    private RepositoryManager repositoryManager;
    private ObjectMapper mapper;
    private Map<String, IAccessPolicySpecifier> policiesMap;

    /**
     * Constructs a new instance and records its association to the passed-in channel.
     * Managers beans passed as parameters because of lack of possibility to inject it to consumer.
     *
     * @param channel       the channel to which this consumer is attached
     * @param rabbitManager rabbit manager bean passed for access to messages manager
     */
    public ResourceModificationRequestConsumer(Channel channel,
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
        CoreResourceRegistryResponse response = new CoreResourceRegistryResponse();
        String message = new String(body, "UTF-8");

        log.info(" [x] Received resources to modify (CoreResourceRegistryRequest)");

        try {
            try {
                //request from CCI received and deserialized
                request = mapper.readValue(message, CoreResourceRegistryRequest.class);
            } catch (JsonSyntaxException | JsonMappingException e) {
                log.error("Unable to get CoreResourceRegistryRequest from Message body!", e);
                response.setStatus(HttpStatus.SC_BAD_REQUEST);
                response.setMessage("Content invalid. Could not deserialize. Resources not modified!");
                response.setServiceResponse(authorizationManager.generateServiceResponse());
                rabbitManager.sendRPCReplyMessage(this, properties, envelope, mapper.writeValueAsString(response));
                return;
            }

            if (request != null) {
                this.policiesMap = request.getFilteringPolicies();
                AuthorizationResult tokenAuthorizationResult = authorizationManager.checkSinglePlatformOperationAccess(request.getSecurityRequest(), request.getPlatformId());
                if (!tokenAuthorizationResult.isValidated()) {
                    log.error("Token invalid: \"" + tokenAuthorizationResult.getMessage() + "\"");
                    response.setStatus(400);
                    response.setMessage("Token invalid: \"" + tokenAuthorizationResult.getMessage() + "\"");
                    response.setServiceResponse(authorizationManager.generateServiceResponse());
                    rabbitManager.sendRPCReplyMessage(this, properties, envelope,
                            mapper.writeValueAsString(response));
                    return;
                }
            } else {
                log.error("Request is null!");
                response.setStatus(400);
                response.setMessage("Request is null!");
                response.setServiceResponse(authorizationManager.generateServiceResponse());
                rabbitManager.sendRPCReplyMessage(this, properties, envelope, mapper.writeValueAsString(response));
                return;
            }

            //contact with Semantic Manager accordingly to Type of object Description received
            switch (request.getDescriptionType()) {
                case RDF:
                    log.info("Message to Semantic Manager Sent. Content Type : RDF. Request: " + request.getBody());    //sending RDF content to Semantic Manager and passing responsibility to another consumer
                    rabbitManager.sendResourceRdfValidationRpcMessage(this, properties, envelope,
                            message,
                            request.getPlatformId(),
                            RegistryOperationType.MODIFICATION,
                            authorizationManager,
                            this.policiesMap);
                    break;
                case BASIC:
                    if (checkIfEveryResourceHasId(request)) {                                                           //if all of the resources have an Id, request is passed do SM
                        log.info("Message to Semantic Manager Sent. Content Type : BASIC. Request: " + request.getBody());
                        //sending JSON content to Semantic Manager and passing responsibility to another consumer
                        rabbitManager.sendResourceJsonTranslationRpcMessage(this, properties, envelope,
                                message,
                                request.getPlatformId(),
                                RegistryOperationType.MODIFICATION,
                                authorizationManager,
                                this.policiesMap,
                                request.getBody());
                    } else {                                                                                            //if any of the resources does not have an ID, request is rejected.
                        log.info(request.getBody());
                        log.error("One of the resources has no ID or list with resources is invalid. Resources not modified!");
                        response.setStatus(HttpStatus.SC_BAD_REQUEST);
                        response.setMessage("One of the resources has no ID or list with resources is invalid. Resources not modified!");
                        rabbitManager.sendRPCReplyMessage(this, properties, envelope,
                                mapper.writeValueAsString(response));
                    }
                    break;
            }
        } catch (Exception e) {
            log.error(e);
            response.setStatus(HttpStatus.SC_INTERNAL_SERVER_ERROR);
            response.setMessage("Consumer critical exception!");
            rabbitManager.sendRPCReplyMessage(this, properties, envelope,
                    mapper.writeValueAsString(response));
        }
    }

    /**
     * @param request with resources to check
     * @return True if every of resources in list has an id. False if any of resources does not have an id.
     */
    private boolean checkIfEveryResourceHasId(CoreResourceRegistryRequest request) {
        List<Resource> resources = retrieveResourcesListFromRequest(request);
        try {
            for (Resource resource : resources) {

                if (!checkIfResourceHasId(resource)) {                                                                  //if given resource does not have an id, false will be returned
                    log.error("One of resources does not have an id!");
                    return false;
                }

                if (resource instanceof Device) {
                    List<Service> services = ((Device) resource).getServices();                                                       //if given Resource is a subtype of Device, it can have services inside

                    if (services != null && !services.isEmpty()) {
                        for (Service service : services) {
                            if (!checkIfResourceHasId(service)) {                                                           //if given service does not have an id, false will be returned
                                log.error("One of services does not have an id!");
                                return false;
                            }
                        }
                    }
                }

            }
        } catch (Exception e) {
            log.error(e);
            return false;
        }
        return true;
    }

    /**
     * @param resource
     * @return True if resource has an ID. False if it has not an ID or ID is empty.
     */
    private boolean checkIfResourceHasId(Resource resource) {
        if (resource.getId() == null || resource.getId().isEmpty()) {
            return false;
        } else {
            log.error("Resource (or actuating service) has an ID!");
            return true;
        }
    }

    /**
     * Extracts Resource List from the request
     *
     * @param request
     * @return list with Resource objects
     */
    private List<Resource> retrieveResourcesListFromRequest(CoreResourceRegistryRequest request) {
        Map<String, Resource> resourceMap = new HashMap<>();
        try {
            resourceMap = mapper.readValue(request.getBody(), new TypeReference<Map<String, Resource>>() {
            });
        } catch (IOException e) {
            log.error("Could not deserialize content of request!" + e);
        }
        return resourceMap.values().stream().collect(Collectors.toList());
    }

}
