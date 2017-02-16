package eu.h2020.symbiote;

import com.google.gson.Gson;
import eu.h2020.symbiote.messaging.RabbitManager;
import eu.h2020.symbiote.model.Platform;
import eu.h2020.symbiote.repository.PlatformRepository;
import eu.h2020.symbiote.repository.ResourceRepository;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import java.io.IOException;
import java.util.Random;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import static org.junit.Assert.assertEquals;

/**
 * Created by mateuszl on 16.02.2017.
 */
@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(classes={RegistryApplication.class})
@SpringBootTest({"eureka.client.enabled=false"})
public class MessagesTests {

    private static Logger log = LoggerFactory.getLogger(MessagesTests.class);

    @Autowired
    private ResourceRepository resourceRepo;

    @Autowired
    private PlatformRepository platformRepo;

    @Autowired
    private RabbitManager rabbitManager;

    private Random rand;

    @Before
    public void setup() throws IOException, TimeoutException {
        rand = new Random();
    }

    @Test
    public void PlatformCreationTest() throws Exception {
        Platform platform = new Platform ();
        String platformId = Integer.toString(rand.nextInt(50));
        String name = "platform" + rand.nextInt(50000);

        platform.setPlatformId(platformId);
        platform.setName(name);
        platform.setDescription("platform_description");
        platform.setUrl("http://www.symbIoTe.com");
        platform.setInformationModelId("platform_info_model");

        Gson gson = new Gson();
        String message = gson.toJson(platform);

        String exchangeName = "symbIoTe.platform";
        String routingKey = exchangeName + ".creationRequested";

        rabbitManager.sendCustomMessage(exchangeName, routingKey, message);

        // Sleep to make sure that the platform has been saved to the repo before querying
        TimeUnit.SECONDS.sleep(1);

        Platform result = platformRepo.findOne(platformId);
        assertEquals(name, result.getName());

        platformRepo.delete(platformId);
    }

}
