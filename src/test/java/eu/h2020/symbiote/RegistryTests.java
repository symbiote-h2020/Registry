package eu.h2020.symbiote;

import eu.h2020.symbiote.managers.AuthorizationManager;
import eu.h2020.symbiote.managers.RabbitManager;
import eu.h2020.symbiote.managers.RepositoryManager;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit4.SpringRunner;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Created by mateuszl
 */
@RunWith(SpringRunner.class)
@SpringBootTest
@DirtiesContext
@TestPropertySource(
        locations = {"classpath:test.properties"},
        properties = {"key=value"})
public class RegistryTests {

    @Autowired
    RabbitManager rabbitManager;

    @Autowired
    AuthorizationManager authorizationManager;

    @Autowired
    RepositoryManager repositoryManager;

    @Before
    public void setUp() throws Exception {
    }

    @After
    public void tearDown() throws Exception {

    }


    @Test
    public void contextLoads() throws Exception {
        assertThat(rabbitManager).isNotNull();
        assertThat(repositoryManager).isNotNull();
        assertThat(authorizationManager).isNotNull();
    }

}