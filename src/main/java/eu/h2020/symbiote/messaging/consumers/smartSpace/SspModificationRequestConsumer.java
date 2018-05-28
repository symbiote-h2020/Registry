package eu.h2020.symbiote.messaging.consumers.smartSpace;

import com.rabbitmq.client.Channel;
import com.rabbitmq.client.DefaultConsumer;
import eu.h2020.symbiote.managers.RabbitManager;
import eu.h2020.symbiote.managers.RepositoryManager;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * Created by mateuszl on 25.05.2018.
 */
public class SspModificationRequestConsumer extends DefaultConsumer {

    private Log log = LogFactory.getLog(this.getClass());
    private RepositoryManager repositoryManager;
    private RabbitManager rabbitManager;


    /**
     * Constructs a new instance and records its association to the passed-in channel.
     *
     * @param channel the channel to which this consumer is attached
     */
    public SspModificationRequestConsumer(Channel channel,
                                          RepositoryManager repositoryManager,
                                          RabbitManager rabbitManager) {
        super(channel);
        this.repositoryManager = repositoryManager;
        this.rabbitManager = rabbitManager;
    }
}
