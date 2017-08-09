package eu.h2020.symbiote.messaging.consumers.pim;

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.gson.JsonSyntaxException;
import com.rabbitmq.client.AMQP;
import com.rabbitmq.client.Channel;
import com.rabbitmq.client.DefaultConsumer;
import com.rabbitmq.client.Envelope;
import eu.h2020.symbiote.core.cci.PlatformRegistryRequest;
import eu.h2020.symbiote.core.internal.PlatformListResponse;
import eu.h2020.symbiote.core.model.Platform;
import eu.h2020.symbiote.managers.AuthorizationManager;
import eu.h2020.symbiote.managers.RepositoryManager;
import eu.h2020.symbiote.messaging.RabbitManager;
import eu.h2020.symbiote.model.AuthorizationResult;
import org.apache.commons.lang.NullArgumentException;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.http.HttpStatus;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by mateuszl on 07.08.2017.
 */
public class InformationModelsRequestConsumer extends DefaultConsumer {

    private static Log log = LogFactory.getLog(InformationModelsRequestConsumer.class);
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
    public InformationModelsRequestConsumer(Channel channel,
                                            RepositoryManager repositoryManager,
                                            RabbitManager rabbitManager,
                                            AuthorizationManager authorizationManager) {
        super(channel);
        this.rabbitManager = rabbitManager;
        this.repositoryManager = repositoryManager;
        this.authorizationManager = authorizationManager;
        this.mapper = new ObjectMapper();
    }
    @Override
    public void handleDelivery(String consumerTag, Envelope envelope, AMQP.BasicProperties properties, byte[] body) throws IOException {
        super.handleDelivery(consumerTag, envelope, properties, body);

        PlatformRegistryRequest request;
        PlatformListResponse platformListResponse = new PlatformListResponse();
        platformListResponse.setPlatforms(new ArrayList<>());
        platformListResponse.setStatus(400);
        List<Platform> platforms;
        AuthorizationResult authorizationResult = null;
        String message = new String(body, "UTF-8");
        log.info(" [x] Received request to retrieve resources for platform: '" + message + "'");

        try {
            request = mapper.readValue(message, PlatformRegistryRequest.class);
        } catch (JsonSyntaxException | JsonMappingException | JsonParseException e) {
            log.error("Error occured during getting Request from Json", e);
            platformListResponse.setMessage("Error occured during getting Request from Json");
            sendRpcReplyMessage(envelope, properties, platformListResponse);
            return;
        }

        if (request != null) {
            try {
                authorizationResult = authorizationManager.checkToken(request.getToken());
            } catch (NullArgumentException e) {
                log.error(e);
                platformListResponse.setMessage("Request invalid!");
                sendRpcReplyMessage(envelope, properties, platformListResponse);
                return;
            }
        } else {
            log.error("Request is null!");
            platformListResponse.setMessage("Request is null!");
            sendRpcReplyMessage(envelope, properties, platformListResponse);
            return;
        }

        if (!authorizationResult.isValidated()) {
            log.error("Token invalid! " + authorizationResult.getMessage());
            platformListResponse.setMessage(authorizationResult.getMessage());
            sendRpcReplyMessage(envelope, properties, platformListResponse);
            return;
        }

        platforms = repositoryManager.getAllPlatforms();
        platformListResponse.setStatus(HttpStatus.SC_OK);
        platformListResponse.setMessage("OK. " + platforms.size() + " platforms found!");
        platformListResponse.setPlatforms(platforms);
        sendRpcReplyMessage(envelope, properties, platformListResponse);
    }

    private void sendRpcReplyMessage(Envelope envelope, AMQP.BasicProperties properties,
                                     PlatformListResponse platformListResponse) throws IOException {
        rabbitManager.sendRPCReplyMessage(this, properties, envelope, mapper.writeValueAsString(platformListResponse));
    }
}
