package eu.h2020.symbiote;

import com.google.gson.Gson;
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

    Gson gson;
    private RepositoryManager repositoryManager;
    private PlatformRepository mockedPlatformRepo;
    private String savedPlatformId;
    private Platform platformToSave;

    public TestRepositoryManager() throws MalformedURLException {
    }

    @Before
    public void setUp() {
        mockedPlatformRepo = Mockito.mock(PlatformRepository.class);
        platformToSave = Mockito.mock(Platform.class);

        repositoryManager = new RepositoryManager(mockedPlatformRepo);
    }

    @Test
    public void testMockCreation() {
        assertNotNull(mockedPlatformRepo);
        assertNotNull(platformToSave);
    }

    //checks if repositoryManager.savePlatform() triggers mockedPlatformRepo.save()
    @Test
    public void testSavePlatformTriggersRepo() {
        when(mockedPlatformRepo.save(platformToSave)).thenReturn(platformToSave);
        when(platformToSave.getId()).thenReturn("0");
        repositoryManager.savePlatform(platformToSave);
        verify(mockedPlatformRepo).save(platformToSave);
    }

    @After
    public void tearDown() {
    }
}
