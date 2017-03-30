package eu.h2020.symbiote.messaging;

import com.google.gson.Gson;
import com.google.gson.JsonSyntaxException;
import com.google.gson.reflect.TypeToken;
import com.rabbitmq.client.AMQP;
import com.rabbitmq.client.Channel;
import com.rabbitmq.client.DefaultConsumer;
import com.rabbitmq.client.Envelope;
import eu.h2020.symbiote.model.RegistryRequest;
import eu.h2020.symbiote.model.Platform;
import eu.h2020.symbiote.model.PlatformResponse;
import eu.h2020.symbiote.model.SemanticResponse;
import eu.h2020.symbiote.repository.RepositoryManager;
import eu.h2020.symbiote.utils.RegistryUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import java.io.IOException;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;

/**
 * RabbitMQ Consumer implementation used for Platform Modification actions
 * <p>
 * Created by mateuszl
 */
public class PlatformModificationRequestConsumer extends DefaultConsumer {

    private static Log log = LogFactory.getLog(PlatformModificationRequestConsumer.class);
    private RepositoryManager repositoryManager;
    private RabbitManager rabbitManager;

    /**
     * Constructs a new instance and records its association to the passed-in channel.
     * Managers beans passed as parameters because of lack of possibility to inject it to consumer.
     *
     * @param channel           the channel to which this consumer is attached
     * @param rabbitManager     rabbit manager bean passed for access to messages manager
     * @param repositoryManager repository manager bean passed for persistence actions
     */
    public PlatformModificationRequestConsumer(Channel channel,
                                               RepositoryManager repositoryManager,
                                               RabbitManager rabbitManager) {
        super(channel);
        this.repositoryManager = repositoryManager;
        this.rabbitManager = rabbitManager;
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
        Gson gson = new Gson();
        RegistryRequest request;
        SemanticResponse semanticResponse;
        String response;
        List<Platform> platforms = new ArrayList<>();
        PlatformResponse platformResponse = new PlatformResponse();
        List<PlatformResponse> platformResponseList = new ArrayList<>();
        String message = new String(body, "UTF-8");
        Type listType = new TypeToken<ArrayList<Platform>>() {
        }.getType();
        log.info(" [x] Received platform to modify: '" + message + "'");

        try {
            request = gson.fromJson(message, RegistryRequest.class);
            if (RegistryUtils.checkToken(request.getToken())) {
                switch (request.getType()) {
                    case REGISTRATION_RDF:
                        try {
                            semanticResponse = RegistryUtils.getPlatformsFromRdf(request.getBody());
                            if (semanticResponse.getStatus() == 200) {
                                platforms = gson.fromJson(semanticResponse.getBody(), listType);
                            } else {
                                log.error("Error occured during rdf verification. Semantic Manager info: "
                                        + semanticResponse.getMessage());
                                platformResponse.setStatus(400);
                                platformResponse.setMessage("Error occured during rdf verification. Semantic Manager info: "
                                        + semanticResponse.getMessage());
                                platformResponseList.add(platformResponse);
                            }
                        } catch (JsonSyntaxException e) {
                            log.error("Error occured during getting Platforms from Json received from Semantic Manager", e);
                            platformResponse.setStatus(400);
                            platformResponse.setMessage("Error occured during getting Platforms from Json");
                            platformResponseList.add(platformResponse);
                        }
                    case REGISTRATION_BASIC:

                        try {
                            platforms = gson.fromJson(message, listType);
                        } catch (JsonSyntaxException e) {
                            log.error("Error occured during getting Platforms from Json", e);
                            platformResponse.setStatus(400);
                            platformResponse.setMessage("Error occured during getting Platforms from Json");
                            platformResponseList.add(platformResponse);
                        }
                }
            } else {
                log.error("Token invalid");
                platformResponse.setStatus(400);
                platformResponse.setMessage("Token invalid");
                platformResponseList.add(platformResponse);
            }
        } catch (JsonSyntaxException e) {
            log.error("Unable to get RegistryRequest from Message body!");
            e.printStackTrace();
        }

        //todo platforms list can be empty
        for (Platform platform : platforms) {
            if (RegistryUtils.validateFields(platform)) {
                platform = RegistryUtils.getRdfBodyForObject(platform);
                platformResponse = this.repositoryManager.modifyPlatform(platform);
                if (platformResponse.getStatus() == 200) {
                    rabbitManager.sendPlatformModifiedMessage(platformResponse.getPlatform());
                }
            } else {
                log.error("Given Platform has some fields null or empty");
                platformResponse.setMessage("Given Platform has some fields null or empty");
                platformResponse.setStatus(400);
            }
            platformResponseList.add(platformResponse);
        }

        //if platforms List is empty, platformResponseList will still contain needed information
        response = gson.toJson(platformResponseList);
        rabbitManager.sendReplyMessage(this, properties, envelope, response);
    }
}
