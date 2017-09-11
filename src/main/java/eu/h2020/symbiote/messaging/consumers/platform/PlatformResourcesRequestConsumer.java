package eu.h2020.symbiote.messaging.consumers.platform;

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.gson.JsonSyntaxException;
import com.rabbitmq.client.AMQP;
import com.rabbitmq.client.Channel;
import com.rabbitmq.client.DefaultConsumer;
import com.rabbitmq.client.Envelope;
import eu.h2020.symbiote.core.internal.CoreResourceRegistryRequest;
import eu.h2020.symbiote.core.internal.ResourceListResponse;
import eu.h2020.symbiote.core.model.internal.CoreResource;
import eu.h2020.symbiote.managers.RabbitManager;
import eu.h2020.symbiote.model.AuthorizationResult;
import eu.h2020.symbiote.managers.RepositoryManager;
import eu.h2020.symbiote.managers.AuthorizationManager;
import eu.h2020.symbiote.utils.RegistryUtils;
import org.apache.commons.lang.NullArgumentException;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.http.HttpStatus;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by mateuszl on 12.06.2017.
 */
public class PlatformResourcesRequestConsumer extends DefaultConsumer {

    private static Log log = LogFactory.getLog(PlatformResourcesRequestConsumer.class);
    private ObjectMapper mapper;
    private RabbitManager rabbitManager;
    private AuthorizationManager authorizationManager;
    private RepositoryManager repositoryManager;

    /**
     * Constructs a new instance and records its association to the passed-in channel.
     * Managers beans passed as parameters because of lack of possibility to inject it to consumer.
     *
     * @param channel       the channel to which this consumer is attached
     * @param rabbitManager rabbit manager bean passed for access to messages manager
     */
    public PlatformResourcesRequestConsumer(Channel channel,
                                            RepositoryManager repositoryManager,
                                            RabbitManager rabbitManager,
                                            AuthorizationManager authorizationManager) {
        super(channel);
        this.rabbitManager = rabbitManager;
        this.repositoryManager = repositoryManager;
        this.authorizationManager = authorizationManager;
        this.mapper = new ObjectMapper();
    }


    /**
     * Called when a <code><b>basic.deliver</b></code> is received for this consumer.
     * Waiting for message containing CoreResourceRegistryRequest with Token and Platform Id fields only.
     * RPC reply body: List of Resources (mapped to JSON). It can be an empty list.
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
        CoreResourceRegistryRequest request;
        ResourceListResponse resourceRegistryResponse = new ResourceListResponse();
        resourceRegistryResponse.setBody(new ArrayList<>());
        List<CoreResource> coreResources;
        AuthorizationResult authorizationResult;
        String message = new String(body, "UTF-8");
        log.info(" [x] Received request to retrieve resources for platform: '" + message + "'");

        try {
            request = mapper.readValue(message, CoreResourceRegistryRequest.class);
        } catch (JsonSyntaxException | JsonMappingException | JsonParseException e) {
            log.error("Error occurred during getting Request from Json", e);
            resourceRegistryResponse.setMessage("Error occurred during getting Request from Json");
            rabbitManager.sendRPCReplyMessage(this, properties, envelope, mapper.writeValueAsString(resourceRegistryResponse));
            return;
        }

        if (request != null) {
            try {
                authorizationResult = authorizationManager.checkSinglePlatformOperationAccess(request.getSecurityRequest(), request.getPlatformId());
            } catch (NullArgumentException e) {
                log.error(e);
                resourceRegistryResponse.setMessage("Request invalid!");
                rabbitManager.sendRPCReplyMessage(this, properties, envelope, mapper.writeValueAsString(resourceRegistryResponse));
                return;
            }
        } else {
            log.error("Request is null!");
            resourceRegistryResponse.setMessage("Request is null!");
            rabbitManager.sendRPCReplyMessage(this, properties, envelope, mapper.writeValueAsString(resourceRegistryResponse));
            return;
        }

        if (!authorizationResult.isValidated()) {
            log.error("Token invalid! " + authorizationResult.getMessage());
            resourceRegistryResponse.setMessage(authorizationResult.getMessage());
            rabbitManager.sendRPCReplyMessage(this, properties, envelope, mapper.writeValueAsString(resourceRegistryResponse));
            return;
        }

        coreResources = repositoryManager.getResourcesForPlatform(request.getPlatformId());
        resourceRegistryResponse.setStatus(HttpStatus.SC_OK);
        resourceRegistryResponse.setMessage("OK. " + coreResources.size() + " resources found!");
        resourceRegistryResponse.setBody(RegistryUtils.convertCoreResourcesToResourcesList(coreResources));
        rabbitManager.sendRPCReplyMessage(this, properties, envelope, mapper.writeValueAsString(resourceRegistryResponse));
    }
}