package eu.h2020.symbiote;

import eu.h2020.symbiote.managers.RabbitManager;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.stereotype.Component;


/**
 * Created by mateuszl on 22.09.2016.
 */
@EnableDiscoveryClient
@SpringBootApplication
public class RegistryApplication {

    private static Log log = LogFactory.getLog(RegistryApplication.class);

    public static void main(String[] args) {
        SpringApplication.run(RegistryApplication.class, args);
    }

    @Component
    public static class CLR implements CommandLineRunner {

        private final RabbitManager rabbitManager;

        @Autowired
        public CLR(RabbitManager rabbitManager) {
            this.rabbitManager = rabbitManager;
        }

        @Override
        public void run(String... args) throws Exception {
// // TODO: 04.09.2017 If not exists, create BIM
            //message retrieval - start rabbit exchange and consumers
            this.rabbitManager.init();
            this.rabbitManager.startConsumers();
            log.info("CLR run() and Rabbit Manager init()");
        }
    }
}