package eu.h2020.symbiote;

import eu.h2020.symbiote.repository.PlatformRepository;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import static org.junit.Assert.assertNotNull;

/**
 * Created by mateuszl on 11.01.2017.
 */
@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(classes={RegistryApplication.class})
@SpringBootTest({"eureka.client.enabled=false"})
public class TestPlatformRepository {

    @Autowired
    private PlatformRepository repo;

    @Test
    public void testRepo() {
        assertNotNull(repo);
    }
}
