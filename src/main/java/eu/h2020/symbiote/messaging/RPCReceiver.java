package eu.h2020.symbiote.messaging;

import eu.h2020.symbiote.repository.RepositoryManager;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;

/**
 * Created by mateuszl on 09.01.2017.
 */

@Component
public class RPCReceiver {

    public static Log log = LogFactory.getLog(RPCReceiver.class);

    private final RepositoryManager repositoryManager;

    @Autowired
    public RPCReceiver(RepositoryManager repositoryManager) {
        this.repositoryManager = repositoryManager;
    }

    @PostConstruct
    public void init(){

    }

}