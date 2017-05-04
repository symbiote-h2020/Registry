package eu.h2020.symbiote;

import eu.h2020.symbiote.security.SecurityHandler;
import eu.h2020.symbiote.messaging.RabbitManager;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
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

    @Value("${symbiote.coreaam.url}")
    private String coreAAMUrl;

    @Value("${security.enabled}")
    private boolean securityEnabled;

    @Value("${rabbit.host}")
    private String rabbitHost;

    public static void main(String[] args) {
        SpringApplication.run(RegistryApplication.class, args);
    }

    private static Log log = LogFactory.getLog(RegistryApplication.class);

    @Bean
    public AlwaysSampler defaultSampler() {
        return new AlwaysSampler();
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
//
            //message retrieval - start rabbit exchange and consumers
            this.rabbitManager.init();
            log.info("CLR run() and Rabbit Manager init()");
        }
    }

    @Bean
    public SecurityHandler securityHandler() {
        SecurityHandler securityHandler = new SecurityHandler(coreAAMUrl, rabbitHost, securityEnabled);
        return securityHandler;
    }
}