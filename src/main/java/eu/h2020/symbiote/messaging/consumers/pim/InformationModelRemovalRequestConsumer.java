package eu.h2020.symbiote.messaging.consumers.pim;

import com.rabbitmq.client.AMQP;
import com.rabbitmq.client.Channel;
import com.rabbitmq.client.DefaultConsumer;
import com.rabbitmq.client.Envelope;
import eu.h2020.symbiote.managers.AuthorizationManager;
import eu.h2020.symbiote.managers.RepositoryManager;
import eu.h2020.symbiote.messaging.RabbitManager;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import java.io.IOException;

/**
 * Created by mateuszl on 08.08.2017.
 */
public class InformationModelRemovalRequestConsumer extends DefaultConsumer {

    private static Log log = LogFactory.getLog(InformationModelRemovalRequestConsumer.class);
    private AuthorizationManager authorizationManager;
    private RabbitManager rabbitManager;
    private RepositoryManager repositoryManager;

    public InformationModelRemovalRequestConsumer(Channel channel,
                                                  RabbitManager rabbitManager,
                                                  AuthorizationManager authorizationManager,
                                                  RepositoryManager repositoryManager) {
        super(channel);
        this.rabbitManager = rabbitManager;
        this.authorizationManager = authorizationManager;
        this.repositoryManager = repositoryManager;
    }


    @Override
    public void handleDelivery(String consumerTag, Envelope envelope, AMQP.BasicProperties properties, byte[] body) throws IOException {
        super.handleDelivery(consumerTag, envelope, properties, body);

        //// TODO: 07.08.2017 IMPLEMENT!

    }
}