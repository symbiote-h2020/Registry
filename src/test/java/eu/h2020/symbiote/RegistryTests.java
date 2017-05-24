package eu.h2020.symbiote;

import eu.h2020.symbiote.messaging.RabbitManager;
import eu.h2020.symbiote.security.InternalSecurityHandler;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringRunner;

/**
 * Created by mateuszl on 23.05.2017.
 */
@RunWith(SpringRunner.class)
@ContextConfiguration
@SpringBootTest
@DirtiesContext
public class RegistryTests {

    @Autowired
    protected RabbitTemplate rabbitTemplate;

    @Autowired
    protected RabbitManager rabbitManager;

    @Autowired
    protected InternalSecurityHandler securityHandler;

    @Before
    public void setUp() throws Exception {

    }

    @After
    public void tearDown() throws Exception {

    }

    @Test
    public void startupTest() {
        Assert.assertNotNull(rabbitManager);
        Assert.assertNotNull(securityHandler);
        Assert.assertNotNull(rabbitTemplate);
    }

    @Configuration
    @ComponentScan(basePackages = {"eu.h2020.symbiote"})
    static class ContextConfiguration {
    }

}
