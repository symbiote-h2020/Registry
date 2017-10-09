package eu.h2020.symbiote;

import eu.h2020.symbiote.core.model.InformationModel;
import eu.h2020.symbiote.core.model.RDFFormat;
import eu.h2020.symbiote.managers.RabbitManager;
import eu.h2020.symbiote.repository.InformationModelRepository;
import org.apache.commons.io.IOUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.stereotype.Component;

import java.nio.charset.Charset;


/**
 * Created by mateuszl on 22.09.2016.
 */
@EnableDiscoveryClient
@SpringBootApplication
public class RegistryApplication {

    private static Log log = LogFactory.getLog(RegistryApplication.class);

    public static final String BIM_MODEL_ID = "BIM";
    public static final String BIM_MODEL_URI = "http://www.symbiote-h2020.eu/ontology/informationModel/BIM";
    public static final String BIM_MODEL_OWNER = "BIM";

    public static void main(String[] args) {
        SpringApplication.run(RegistryApplication.class, args);
    }

    @Component
    public static class CLR implements CommandLineRunner {

        @Value("${reload.bim}")
        private boolean shouldReloadBim;
        @Value("${bim.location}")
        private String bimLocation;

        private final RabbitManager rabbitManager;

        private final InformationModelRepository informationModelRepository;

        @Autowired
        public CLR(RabbitManager rabbitManager, InformationModelRepository informationModelRepository) {
            this.rabbitManager = rabbitManager;
            this.informationModelRepository = informationModelRepository;
        }

        @Override
        public void run(String... args) throws Exception {
//
            //message retrieval - start rabbit exchange and consumers
            this.rabbitManager.init();
            this.rabbitManager.startConsumers();
            log.info("CLR run() and Rabbit Manager init()");

            log.info("Checking if BIM model exist");
            if (!this.informationModelRepository.exists(BIM_MODEL_ID) || shouldReloadBim) {
                String bimRdf = IOUtils.toString(RegistryApplication.class
                        .getResourceAsStream(bimLocation), Charset.defaultCharset());
                InformationModel bim = getBIM(bimRdf);
                informationModelRepository.save(bim);
            }

        }

        private InformationModel getBIM( String bimRdf) {
            InformationModel informationModel = new InformationModel();
            informationModel.setId(BIM_MODEL_ID);
            informationModel.setRdf(bimRdf);
            informationModel.setRdfFormat(RDFFormat.Turtle);
            informationModel.setName(BIM_MODEL_ID);
            informationModel.setOwner(BIM_MODEL_OWNER);
            informationModel.setUri(BIM_MODEL_URI);
            return informationModel;
        }
    }
}