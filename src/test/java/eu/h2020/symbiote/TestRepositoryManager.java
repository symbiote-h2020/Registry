package eu.h2020.symbiote;

import eu.h2020.symbiote.messaging.RabbitManager;
import eu.h2020.symbiote.model.Platform;
import eu.h2020.symbiote.repository.PlatformRepository;
import eu.h2020.symbiote.repository.RepositoryManager;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;

import java.net.MalformedURLException;

import static org.junit.Assert.assertNotNull;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Created by mateuszl on 10.01.2017.
 */
public class TestRepositoryManager {

    private RepositoryManager repositoryManager;
    private PlatformRepository mockedPlatformRepo;
    private Platform platformToSave;
    private RabbitManager rabbitManager;

    public TestRepositoryManager() throws MalformedURLException {
    }

    @Before
    public void setUp() {
        mockedPlatformRepo = Mockito.mock(PlatformRepository.class);
        platformToSave = Mockito.mock(Platform.class);
        rabbitManager = Mockito.mock(RabbitManager.class);

        repositoryManager = new RepositoryManager(mockedPlatformRepo, rabbitManager);
    }

    @Test
    public void testMockCreation() {
        assertNotNull(mockedPlatformRepo);
        assertNotNull(platformToSave);
        assertNotNull(rabbitManager);
    }

    //checks if repositoryManager.savePlatform() triggers mockedPlatformRepo.save()
    @Test
    public void testSavePlatformTriggersRepo() {
        when(mockedPlatformRepo.save(platformToSave)).thenReturn(platformToSave);
        repositoryManager.savePlatform(platformToSave);
        verify(mockedPlatformRepo).save(platformToSave);
    }

    @After
    public void tearDown() {
    }
}
