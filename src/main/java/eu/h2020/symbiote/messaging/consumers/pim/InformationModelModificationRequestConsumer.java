package eu.h2020.symbiote.messaging.consumers.pim;

import com.rabbitmq.client.AMQP;
import com.rabbitmq.client.Channel;
import com.rabbitmq.client.DefaultConsumer;
import com.rabbitmq.client.Envelope;
import eu.h2020.symbiote.managers.AuthorizationManager;
import eu.h2020.symbiote.messaging.RabbitManager;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import java.io.IOException;

/**
 * Created by mateuszl on 08.08.2017.
 */
public class InformationModelModificationRequestConsumer extends DefaultConsumer {

    private static Log log = LogFactory.getLog(InformationModelModificationRequestConsumer.class);
    private AuthorizationManager authorizationManager;
    private RabbitManager rabbitManager;

    public InformationModelModificationRequestConsumer(Channel channel,
                                                       RabbitManager rabbitManager,
                                                       AuthorizationManager authorizationManager) {
        super(channel);
        this.rabbitManager = rabbitManager;
        this.authorizationManager = authorizationManager;
    }

    @Override
    public void handleDelivery(String consumerTag, Envelope envelope, AMQP.BasicProperties properties, byte[] body) throws IOException {
        super.handleDelivery(consumerTag, envelope, properties, body);

        //// TODO: 07.08.2017 IMPLEMENT!

    }
}