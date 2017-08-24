package eu.h2020.symbiote.messaging.consumers.federation;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.rabbitmq.client.AMQP;
import com.rabbitmq.client.Channel;
import com.rabbitmq.client.DefaultConsumer;
import com.rabbitmq.client.Envelope;
import eu.h2020.symbiote.core.internal.FederationListResponse;
import eu.h2020.symbiote.core.model.Federation;
import eu.h2020.symbiote.managers.AuthorizationManager;
import eu.h2020.symbiote.managers.RepositoryManager;
import eu.h2020.symbiote.messaging.RabbitManager;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.http.HttpStatus;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by mateuszl on 12.06.2017.
 */
public class ListAllFederationsRequestConsumer extends DefaultConsumer {

    //// TODO: 22.08.2017 MOCKED, CHANGE!!


    private static Log log = LogFactory.getLog(ListAllFederationsRequestConsumer.class);
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
    public ListAllFederationsRequestConsumer(Channel channel,
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
     * Replies with List of Resources. It can be an empty list.
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
        FederationListResponse federationResponse = new FederationListResponse();
        federationResponse.setFederations(new ArrayList<>());
        List<Federation> federations;
        //String message = new String(body, "UTF-8"); //unnecessary here
        log.info(" [x] Received request to retrieve all federations");

        federations = repositoryManager.getAllFederations();
        federationResponse.setStatus(HttpStatus.SC_OK);
        federationResponse.setMessage("OK. " + federations.size() + " federations found!");
        federationResponse.setFederations(federations);
        rabbitManager.sendRPCReplyMessage(this, properties, envelope, mapper.writeValueAsString(federationResponse));
    }
}