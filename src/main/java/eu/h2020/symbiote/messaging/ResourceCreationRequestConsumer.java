package eu.h2020.symbiote.messaging;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.gson.JsonSyntaxException;
import com.rabbitmq.client.AMQP;
import com.rabbitmq.client.Channel;
import com.rabbitmq.client.DefaultConsumer;
import com.rabbitmq.client.Envelope;
import eu.h2020.symbiote.core.internal.CoreResourceRegistryRequest;
import eu.h2020.symbiote.core.internal.CoreResourceRegistryResponse;
import eu.h2020.symbiote.core.model.resources.*;
import eu.h2020.symbiote.model.RegistryOperationType;
import eu.h2020.symbiote.utils.AuthorizationManager;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.http.HttpStatus;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

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

    /**
     * Constructs a new instance and records its association to the passed-in channel.
     * Managers beans passed as parameters because of lack of possibility to inject it to consumer.
     *
     * @param channel       the channel to which this consumer is attached
     * @param rabbitManager rabbit manager bean passed for access to messages manager
     */
    public ResourceCreationRequestConsumer(Channel channel,
                                           RabbitManager rabbitManager,
                                           AuthorizationManager authorizationManager) {
        super(channel);
        this.rabbitManager = rabbitManager;
        this.authorizationManager = authorizationManager;
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
        CoreResourceRegistryResponse registryResponse = new CoreResourceRegistryResponse();
        String message = new String(body, "UTF-8");
        log.info(" [x] Received resources to create (CoreResourceRegistryRequest): '" + message + "'");

        try {
            //request from CCI received and deserialized
            request = mapper.readValue(message, CoreResourceRegistryRequest.class);
        } catch (JsonSyntaxException e) {
            log.error("Unable to get CoreResourceRegistryRequest from Message body!", e);
            registryResponse.setStatus(HttpStatus.SC_BAD_REQUEST);
            registryResponse.setMessage("Content invalid. Could not deserialize. Resources not created!");
            rabbitManager.sendRPCReplyMessage(this, properties, envelope, mapper.writeValueAsString(registryResponse));
        }

        if (request != null) {
            //checking access by token verification
            if (!authorizationManager.checkResourceOperationAccess(request.getToken(), request.getPlatformId())) {
                log.error("Token invalid");
                registryResponse.setStatus(400);
                registryResponse.setMessage("Token invalid");
                rabbitManager.sendRPCReplyMessage(this, properties, envelope,
                        mapper.writeValueAsString(registryResponse));
                return;
            }

            if (request.getBody() != null) {
                //contact with Semantic Manager accordingly to Type of object Description received
                switch (request.getDescriptionType()) {
                    case RDF:
                        log.info("Message to Semantic Manager Sent. Request: " + request.getBody());
                        //sending RDF content to Semantic Manager and passing responsibility to another consumer
                        rabbitManager.sendResourceRdfValidationRpcMessage(this, properties, envelope,
                                message, request.getPlatformId(), RegistryOperationType.CREATION);
                        break;
                    case BASIC:
                        if (checkIfResourcesHaveNullOrEmptyId(request)) {
                            log.info("Message to Semantic Manager Sent. Request: " + request.getBody());
                            //sending JSON content to Semantic Manager and passing responsibility to another consumer
                            rabbitManager.sendResourceJsonTranslationRpcMessage(this, properties, envelope,
                                    message, request.getPlatformId(), RegistryOperationType.CREATION);
                        } else {
                            log.error("One of the resources has ID or list with resources is invalid. Resources not created!");
                            registryResponse.setStatus(HttpStatus.SC_BAD_REQUEST);
                            registryResponse.setMessage("One of the resources has ID or list with resources is invalid. Resources not created!");
                            rabbitManager.sendRPCReplyMessage(this, properties, envelope,
                                    mapper.writeValueAsString(registryResponse));
                        }
                        break;
                }
            } else {
                log.error("Message body is null!");
                registryResponse.setStatus(400);
                registryResponse.setMessage("Message body is null!");
                rabbitManager.sendRPCReplyMessage(this, properties, envelope,
                        mapper.writeValueAsString(registryResponse));
            }

        }
    }

    /**
     * Checks if given request consists of resources, which does not have any content in ID field.
     *
     * @param request
     * @return
     */
    private boolean checkIfResourcesHaveNullOrEmptyId(CoreResourceRegistryRequest request) {
        List<Resource> resources = new ArrayList<>();
        try {
            resources = mapper.readValue(request.getBody(), new TypeReference<List<Resource>>() {
            });
        } catch (IOException e) {
            log.error("Could not deserialize content of request!" + e);
        }
        return checkIds(resources);
    }

    private boolean checkIds(List<Resource> resources) {
        try {
            for (Resource resource : resources) {
                if (!checkId(resource)) return false;
                List<ActuatingService> actuatingServices = new ArrayList<>();
                if (resource instanceof Actuator) {
                    actuatingServices = ((Actuator) resource).getCapabilities();
                } else if (resource instanceof MobileDevice) {
                    actuatingServices = ((MobileDevice) resource).getCapabilities();
                } else if (resource instanceof StationaryDevice) {
                    actuatingServices = ((StationaryDevice) resource).getCapabilities();
                }
                if (!actuatingServices.isEmpty()) {
                    for (ActuatingService actuatingService : actuatingServices) {
                        if (!checkId(actuatingService)) return checkId(actuatingService);
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
