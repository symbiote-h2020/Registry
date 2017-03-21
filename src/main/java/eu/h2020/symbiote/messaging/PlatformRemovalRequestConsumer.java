package eu.h2020.symbiote.messaging;

import com.google.gson.Gson;
import com.google.gson.JsonSyntaxException;
import com.google.gson.reflect.TypeToken;
import com.rabbitmq.client.AMQP;
import com.rabbitmq.client.Channel;
import com.rabbitmq.client.DefaultConsumer;
import com.rabbitmq.client.Envelope;
import eu.h2020.symbiote.model.OperationRequest;
import eu.h2020.symbiote.model.Platform;
import eu.h2020.symbiote.model.PlatformResponse;
import eu.h2020.symbiote.repository.RepositoryManager;
import eu.h2020.symbiote.utils.RegistryUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import java.io.IOException;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;

/**
 * RabbitMQ Consumer implementation used for Platform Removal actions
 * <p>
 * Created by mateuszl
 */
public class PlatformRemovalRequestConsumer extends DefaultConsumer {

    private static Log log = LogFactory.getLog(PlatformRemovalRequestConsumer.class);
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
    public PlatformRemovalRequestConsumer(Channel channel,
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

        //only BASIC type of objects accepted for remove !!

        Gson gson = new Gson();
        OperationRequest request = null;
        String response;
        List<Platform> platforms = new ArrayList<>();
        PlatformResponse platformResponse = new PlatformResponse();
        List<PlatformResponse> platformResponseList = new ArrayList<>();
        String message = new String(body, "UTF-8");
        Type listType = new TypeToken<ArrayList<Platform>>() {
        }.getType();

        log.info(" [x] Received platforms to remove: '" + message + "'");

        try {
            request = gson.fromJson(message, listType);
        } catch (JsonSyntaxException e) {
            log.error("Error occured during getting Operation Request from Json", e);
            platformResponse.setStatus(400);
            platformResponse.setMessage("Error occured during getting Operation Request from Json");
            platformResponseList.add(platformResponse);
        }

        if (request != null) {
            if (RegistryUtils.checkToken(request.getToken())) {
                try {
                    platforms = gson.fromJson(request.getBody(), listType);
                } catch (JsonSyntaxException e) {
                    log.error("Error occured during getting Platforms from Json", e);
                    platformResponse.setStatus(400);
                    platformResponse.setMessage("Error occured during getting Platforms from Json");
                    platformResponseList.add(platformResponse);
                }
            } else {
                log.error("Token invalid");
                platformResponse.setStatus(400);
                platformResponse.setMessage("Token invalid");
                platformResponseList.add(platformResponse);
            }
        }

        for (Platform platform : platforms) {
            if (platform.getId() != null || !platform.getId().isEmpty()) {
                platform = RegistryUtils.getRdfBodyForObject(platform); //fixme needed? or not completed object is fine?
                platformResponse = this.repositoryManager.removePlatform(platform);
                if (platformResponse.getStatus() == 200) {
                    rabbitManager.sendPlatformRemovedMessage(platformResponse.getPlatform());
                }
            } else {
                log.error("Given Platform has ID null or empty");
                platformResponse.setMessage("Given Platform has ID null or empty");
                platformResponse.setStatus(400);
            }
            platformResponseList.add(platformResponse);
        }
        response = gson.toJson(platformResponseList);
        rabbitManager.sendReplyMessage(this, properties, envelope, response);
    }
}
