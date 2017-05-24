package eu.h2020.symbiote;

import eu.h2020.symbiote.core.model.internal.CoreResource;
import eu.h2020.symbiote.model.RegistryPlatform;
import eu.h2020.symbiote.repository.RegistryPlatformRepository;
import eu.h2020.symbiote.repository.RepositoryManager;
import eu.h2020.symbiote.repository.ResourceRepository;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
import org.mockito.runners.MockitoJUnitRunner;

import static eu.h2020.symbiote.TestSetupConfig.PLATFORM_B_ID;
import static eu.h2020.symbiote.TestSetupConfig.generateCoreResource;
import static eu.h2020.symbiote.TestSetupConfig.generateRegistryPlatformB;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Created by mateuszl on 17.05.2017.
 */
@RunWith(MockitoJUnitRunner.class)
public class RepositoryManagerTests {

    RepositoryManager repositoryManager;
    RegistryPlatformRepository registryPlatformRepository;
    ResourceRepository resourceRepository;

    @Before
    public void setup() {
        registryPlatformRepository = Mockito.mock(RegistryPlatformRepository.class);
        resourceRepository = Mockito.mock(ResourceRepository.class);
        repositoryManager = new RepositoryManager(registryPlatformRepository, resourceRepository);
    }

    @After
    public void teardown() {
    }

    @Test
    public void testSaveResourceTriggersRepository() {
        CoreResource resource = generateCoreResource();
        when(resourceRepository.save(resource)).thenReturn(resource);

        repositoryManager.saveResource(resource);
        try {
            Thread.sleep(1000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        verify(resourceRepository).save(resource);
    }

    @Test
    public void testModifyResourceTriggersRepository() {
        CoreResource resource = generateCoreResource();
        when(resourceRepository.save(resource)).thenReturn(resource);
        when(resourceRepository.findOne("101")).thenReturn(resource);

        repositoryManager.modifyResource(resource);
        try {
            Thread.sleep(1000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        verify(resourceRepository).save(resource);
    }

    @Test
    public void testRemoveResourceTriggersRepository() {
        CoreResource resource = generateCoreResource();
        when(resourceRepository.findOne("101")).thenReturn(resource);
        repositoryManager.removeResource(resource);
        try {
            Thread.sleep(1000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        verify(resourceRepository).delete("101");
    }

    @Test
    public void testSavePlatformTriggersRepository() {
        RegistryPlatform platform = generateRegistryPlatformB();
        when(registryPlatformRepository.save(platform)).thenReturn(platform);

        repositoryManager.savePlatform(platform);
        try {
            Thread.sleep(1000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        verify(registryPlatformRepository).save(platform);
    }

    @Test
    public void testModifyPlatformTriggersRepository() {
        RegistryPlatform platform = generateRegistryPlatformB();
        when(registryPlatformRepository.save(platform)).thenReturn(platform);
        when(registryPlatformRepository.findOne(PLATFORM_B_ID)).thenReturn(platform);

        repositoryManager.modifyPlatform(platform);
        try {
            Thread.sleep(1000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        verify(registryPlatformRepository).save(platform);
    }

    @Test
    public void testRemovePlatformTriggersRepository() {
        RegistryPlatform platform = generateRegistryPlatformB();
        when(registryPlatformRepository.findOne(PLATFORM_B_ID)).thenReturn(platform);

        repositoryManager.removePlatform(platform);
        try {
            Thread.sleep(1000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        verify(registryPlatformRepository).delete(PLATFORM_B_ID);
    }

    @Test
    public void testSavePlatformWithWrongId() throws Exception {
        RegistryPlatform platform = new RegistryPlatform();


    }
}

