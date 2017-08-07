package eu.h2020.symbiote.messaging;

import com.rabbitmq.client.AMQP;
import com.rabbitmq.client.Channel;
import com.rabbitmq.client.DefaultConsumer;
import com.rabbitmq.client.Envelope;

import java.io.IOException;

/**
 * Created by mateuszl on 07.08.2017.
 */
public class ListPIMsConsumer extends DefaultConsumer {

    public ListPIMsConsumer(Channel channel) {
        super(channel);
    }

    @Override
    public void handleDelivery(String consumerTag, Envelope envelope, AMQP.BasicProperties properties, byte[] body) throws IOException {
        super.handleDelivery(consumerTag, envelope, properties, body);
        
        //// TODO: 07.08.2017 IMPLEMENT!
        
    }
}
