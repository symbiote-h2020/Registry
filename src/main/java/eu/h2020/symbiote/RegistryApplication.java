package eu.h2020.symbiote;

import eu.h2020.symbiote.messaging.RabbitManager;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.cloud.sleuth.sampler.AlwaysSampler;
import org.springframework.context.annotation.Bean;
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

        private final RabbitManager manager;

        @Autowired
        public CLR( RabbitManager manager ) {
            this.manager = manager;
        }

        @Override
        public void run(String... args) throws Exception {
//
            //message retrieval - start consumer
            this.manager.receiveMessages(); //todo check
        }
    }

    @Bean
    public AlwaysSampler defaultSampler() {
        return new AlwaysSampler();
    }
}