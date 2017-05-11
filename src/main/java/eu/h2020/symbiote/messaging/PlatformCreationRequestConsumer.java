package eu.h2020.symbiote.messaging;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.gson.JsonSyntaxException;
import com.rabbitmq.client.AMQP;
import com.rabbitmq.client.Channel;
import com.rabbitmq.client.DefaultConsumer;
import com.rabbitmq.client.Envelope;
import eu.h2020.symbiote.model.Platform;
import eu.h2020.symbiote.model.PlatformResponse;
import eu.h2020.symbiote.model.RegistryOperationType;
import eu.h2020.symbiote.repository.RepositoryManager;
import eu.h2020.symbiote.utils.RegistryUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import java.io.IOException;

/**
 * RabbitMQ Consumer implementation used for Platform Creation actions
 * <p>
 * Created by mateuszl
 */
public class PlatformCreationRequestConsumer extends DefaultConsumer {

    private static Log log = LogFactory.getLog(PlatformCreationRequestConsumer.class);
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
    public PlatformCreationRequestConsumer(Channel channel,
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
        ObjectMapper mapper = new ObjectMapper();
        String response;
        String message = new String(body, "UTF-8");
        log.info(" [x] Received requestPlatform to create: '" + message + "'");

        eu.h2020.symbiote.core.model.Platform requestPlatform;
        Platform registryPlatform;

        PlatformResponse platformResponse = new PlatformResponse();
        try {
            requestPlatform = mapper.readValue(message, eu.h2020.symbiote.core.model.Platform.class);

            registryPlatform = RegistryUtils.convertRequestPlatformToRegistryPlatform(requestPlatform);

            log.info("Platform converted to RegistryPlatform: " + registryPlatform);

            if (RegistryUtils.validateFields(registryPlatform)) {
                platformResponse = this.repositoryManager.savePlatform(registryPlatform);
                if (platformResponse.getStatus() == 200) {
                    rabbitManager.sendPlatformOperationMessage(platformResponse.getPlatform(),
                            RegistryOperationType.CREATION);
                }
            } else {
                log.error("Given Platform has some fields null or empty");
                platformResponse.setStatus(400);
            }
        } catch (JsonSyntaxException e) {
            log.error("Error occured during Platform saving to db", e);
            platformResponse.setStatus(400);
        }
        response = mapper.writeValueAsString(platformResponse);

        if (properties.getReplyTo() != null || properties.getCorrelationId() != null) {

            AMQP.BasicProperties replyProps = new AMQP.BasicProperties
                    .Builder()
                    .correlationId(properties.getCorrelationId())
                    .build();

            this.getChannel().basicPublish("", properties.getReplyTo(), replyProps, response.getBytes());
            log.info("- Message with content: '" + platformResponse + "' sent back");
        } else {
            log.warn("Received RPC message without ReplyTo or CorrelationId props.");
        }
        this.getChannel().basicAck(envelope.getDeliveryTag(), false);
    }


    //todo FOR NEXT RELEASE
    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
    /*
    @Override
    public void handleDelivery(String consumerTag, Envelope envelope,
                               AMQP.BasicProperties properties, byte[] body)
            throws IOException {
        ObjectMapper mapper = new ObjectMapper();
        RegistryRequest request = null;
        String response;
        RegistryResponse registryResponse = new RegistryResponse();
        boolean bulkRequestSuccess = true;
        String responseBody;
        List<Platform> platforms = new ArrayList<>();
        PlatformResponse platformResponse = new PlatformResponse();
        List<PlatformResponse> platformResponseList = new ArrayList<>();
        String message = new String(body, "UTF-8");
        Type listType = new TypeToken<ArrayList<Platform>>() {
        }.getType();
        log.info(" [x] Received platforms to create: '" + message + "'");

        try {
            request = gson.fromJson(message, RegistryRequest.class);
        } catch (JsonSyntaxException e) {
            log.error("Error occured during getting Operation Request from Json", e);
            platformResponse.setStatus(HttpStatus.SC_BAD_REQUEST);
            platformResponse.setMessage("Error occured during getting Operation Request from Json");
            platformResponseList.add(platformResponse);
        }

        if (request != null) {
            if (authorizationManager.checkResourceOperationAccess(request.getToken())) {
                registryResponse.setStatus(HttpStatus.SC_BAD_REQUEST);
                switch (request.getType()) {
                    case RDF:
                        try {
                            registryResponse = RegistryUtils.getPlatformsFromRdf(request.getBody());
                        } catch (JsonSyntaxException e) {
                            log.error("Error occured during getting Platforms from Json received from Semantic Manager", e);
                            platformResponse.setStatus(HttpStatus.SC_BAD_REQUEST);
                            platformResponse.setMessage("Error occured during getting Platforms from Json");
                            platformResponseList.add(platformResponse);
                        }
                        if (registryResponse.getStatus() == 200) {
                            platforms = gson.fromJson(RegistryResponse.getBody(), listType);
                        } else {
                            log.error("Error occured during rdf verification. Semantic Manager info: "
                                    + RegistryResponse.getMessage());
                            platformResponse.setStatus(HttpStatus.SC_BAD_REQUEST);
                            platformResponse.setMessage("Error occured during rdf verification. Semantic Manager info: "
                                    + RegistryResponse.getMessage());
                            platformResponseList.add(platformResponse);
                        }
                    case BASIC:
                        try {
                            platforms = gson.fromJson(request.getBody(), listType);
                        } catch (JsonSyntaxException e) {
                            log.error("Error occured during getting Platforms from Json", e);
                            platformResponse.setStatus(HttpStatus.SC_BAD_REQUEST);
                            platformResponse.setMessage("Error occured during getting Platforms from Json");
                            platformResponseList.add(platformResponse);
                        }
                }
            } else {
                log.error("Token invalid");
                platformResponse.setStatus(HttpStatus.SC_UNAUTHORIZED);
                platformResponse.setMessage("Token invalid");
                platformResponseList.add(platformResponse);
            }
        } else {
            log.error("Request is null");
            platformResponse.setStatus(HttpStatus.SC_BAD_REQUEST);
            platformResponse.setMessage("Request is null");
            platformResponseList.add(platformResponse);
        }

        for (Platform platform : platforms) {
            if (RegistryUtils.validateFields(platform)) {
                if (platform.getBody()==null) platform = RegistryUtils.getRdfBodyForObject(platform);
                platformResponse = this.repositoryManager.savePlatform(platform);
            } else {
                log.error("Given Platform has some fields null or empty");
                platformResponse.setMessage("Given Platform has some fields null or empty");
                platformResponse.setStatus(HttpStatus.SC_BAD_REQUEST);
                platformResponse.setPlatform(platform);
            }
            platformResponseList.add(platformResponse);
        }

        registryResponse = new RegistryResponse();

        for (PlatformResponse platformResponse1 : platformResponseList) {
            if (platformResponse1.getStatus()!=200) {
                rollback(platformResponse1.getPlatform());
                bulkRequestSuccess = false;
                registryResponse.setStatus(500);
                registryResponse.setMessage("One of objects could not be registered. Check list of response " +
                        "objects for details.");
            }
        }

        if (bulkRequestSuccess) {
            for (PlatformResponse platformResponse2 : platformResponseList) {
                rabbitManager.sendPlatformCreatedMessage(platformResponse2.getPlatform());
            }
            registryResponse.setStatus(200);
            registryResponse.setMessage("Bulk registration successful!");
        }

        responseBody = gson.toJson(platformResponseList);
        registryResponse.setBody(responseBody);
        response = gson.toJson(registryResponse);
        rabbitManager.sendRPCReplyMessage(this, properties, envelope, response);
    }

     //Form of transaction rollback used for bulk registration, triggered for all succesfully saved objects when
     //any of given objects in list did not save successfully in database.

    private void rollback(Platform platform){
        repositoryManager.removePlatform(platform);
    }
    */

}