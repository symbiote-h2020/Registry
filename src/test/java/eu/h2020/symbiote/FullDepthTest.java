package eu.h2020.symbiote;

import eu.h2020.symbiote.messaging.RabbitManager;
import eu.h2020.symbiote.model.Platform;
import eu.h2020.symbiote.repository.LocationRepository;
import eu.h2020.symbiote.repository.PlatformRepository;
import eu.h2020.symbiote.repository.RepositoryManager;
import eu.h2020.symbiote.repository.ResourceRepository;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mockito;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.test.util.ReflectionTestUtils;

import java.io.IOException;
import java.util.concurrent.TimeoutException;

import static org.junit.Assert.assertNotNull;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Tests using autowired RabbitManager Bean
 */
@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(classes={RegistryApplication.class})
@SpringBootTest({"eureka.client.enabled=false"})
public class FullDepthTest {
    private RepositoryManager repositoryManager;
    private PlatformRepository mockedPlatformRepo;
    private ResourceRepository mockedResourceRepo;
    private LocationRepository mockedLocationRepo;
    private Platform platformToSave;

    @InjectMocks
    private RabbitManager rabbitManager;

    @Before
    public void setUp() {
        mockedPlatformRepo = Mockito.mock(PlatformRepository.class);
        mockedResourceRepo = Mockito.mock(ResourceRepository.class);
        mockedLocationRepo = Mockito.mock(LocationRepository.class);

        platformToSave = new Platform();
        platformToSave.setName("testPlatform");
        platformToSave.setUrl("http://test.url/");
        platformToSave.setDescription("testDescription");

        repositoryManager = new RepositoryManager(mockedPlatformRepo, mockedResourceRepo, mockedLocationRepo);
    }

    @Test
    public void testMockCreation() throws IOException, TimeoutException {
        assertNotNull(mockedPlatformRepo);
        assertNotNull(mockedResourceRepo);
        assertNotNull(mockedLocationRepo);
        assertNotNull(platformToSave);
        assertNotNull(rabbitManager);
        assertNotNull(rabbitManager.getConnection());
    }

    @Test
    public void messageTriggersRepositoryManager(){

    }

    @Test
    public void repositoryManagerTriggersRepo() {
        when(mockedPlatformRepo.save(platformToSave)).thenReturn(platformToSave);

        repositoryManager.savePlatform(platformToSave);

        verify(mockedPlatformRepo).save(platformToSave);
    }

    @After
    public void teardown() {
        ReflectionTestUtils.invokeMethod(rabbitManager, "cleanup");
    }

}
