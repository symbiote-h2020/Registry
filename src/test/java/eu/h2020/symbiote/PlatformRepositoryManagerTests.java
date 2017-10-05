package eu.h2020.symbiote;

import com.mongodb.MongoException;
import eu.h2020.symbiote.core.model.Platform;
import eu.h2020.symbiote.core.model.internal.CoreResource;
import eu.h2020.symbiote.managers.RepositoryManager;
import eu.h2020.symbiote.repository.FederationRepository;
import eu.h2020.symbiote.repository.InformationModelRepository;
import eu.h2020.symbiote.repository.PlatformRepository;
import eu.h2020.symbiote.repository.ResourceRepository;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
import org.mockito.runners.MockitoJUnitRunner;

import java.util.Arrays;

import static eu.h2020.symbiote.TestSetupConfig.PLATFORM_B_ID;
import static eu.h2020.symbiote.TestSetupConfig.generateCoreResourceWithoutId;
import static eu.h2020.symbiote.TestSetupConfig.generatePlatformB;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Created by mateuszl on 30.08.2017.
 */
@RunWith(MockitoJUnitRunner.class)
public class PlatformRepositoryManagerTests {

    RepositoryManager repositoryManager;
    PlatformRepository platformRepository;
    ResourceRepository resourceRepository;
    InformationModelRepository informationModelRepository;
    FederationRepository federationRepository;

    @Before
    public void setup() {
        platformRepository = Mockito.mock(PlatformRepository.class);
        resourceRepository = Mockito.mock(ResourceRepository.class);
        informationModelRepository = Mockito.mock(InformationModelRepository.class);
        federationRepository = Mockito.mock(FederationRepository.class);
        repositoryManager = new RepositoryManager(platformRepository, resourceRepository, informationModelRepository, federationRepository);
    }

    @After
    public void teardown() {
    }

    @Test
    public void testSavePlatformTriggersRepository() {
        Platform platform = generatePlatformB();
        when(platformRepository.save(platform)).thenReturn(platform);

        repositoryManager.savePlatform(platform);
        try {
            Thread.sleep(1000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        verify(platformRepository).save(platform);
    }

    @Test
    public void testModifyPlatformTriggersRepository() {
        Platform platform = generatePlatformB();
        when(platformRepository.save(platform)).thenReturn(platform);
        when(platformRepository.findOne(PLATFORM_B_ID)).thenReturn(platform);

        repositoryManager.modifyPlatform(platform);
        try {
            Thread.sleep(1000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        verify(platformRepository).save(platform);
    }

    @Test
    public void testRemovePlatformTriggersRepository() {
        Platform platform = generatePlatformB();
        when(platformRepository.findOne(PLATFORM_B_ID)).thenReturn(platform);

        repositoryManager.removePlatform(platform);
        try {
            Thread.sleep(1000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        verify(platformRepository).delete(PLATFORM_B_ID);
    }

    @Test
    public void testSavePlatformReturnsStatus200() throws Exception {
        Platform platform = generatePlatformB();
        when(platformRepository.save(platform)).thenReturn(platform);

        Assert.assertEquals(200,repositoryManager.savePlatform(platform).getStatus());
    }

    @Test
    public void testSavePlatformWithWrongId() throws Exception {
        Platform platform = new Platform();
        Assert.assertNotEquals(200,repositoryManager.savePlatform(platform).getStatus());
    }

    @Test
    public void testSavePlatformMongoError(){
        Platform platform = generatePlatformB();
        when(platformRepository.save(platform)).thenThrow(new MongoException("FAKE MONGO ERROR"));
        Assert.assertNotEquals(200,repositoryManager.savePlatform(platform).getStatus());
    }

    @Test
    public void testRemovePlatformWithWrongId() throws Exception {
        Platform platform = new Platform();
        Assert.assertNotEquals(200,repositoryManager.removePlatform(platform).getStatus());
    }

    @Test
    public void testModifyPlatformWithWrongId() throws Exception {
        Platform platform = new Platform();
        Assert.assertNotEquals(200,repositoryManager.modifyPlatform(platform).getStatus());
    }

    @Test
    public void testmodifyPlatformMongoError(){
        Platform platform = generatePlatformB();
        doThrow(new MongoException("FAKE MONGO Exception")).when(platformRepository).save(platform);
        Assert.assertNotEquals(200,repositoryManager.modifyPlatform(platform).getStatus());
    }
    @Test
    public void testRemovePlatformWithResourcesFail(){
        Platform platform = generatePlatformB();
        when(resourceRepository.findByInterworkingServiceURL(platform.getId())).thenReturn(Arrays.asList(new CoreResource()));
        Assert.assertNotEquals(200,repositoryManager.removePlatform(platform).getStatus());
    }

    @Test
    public void testRemovePlatformMongoError(){
        Platform platform = generatePlatformB();
        doThrow(new MongoException("FAKE MONGO Exception")).when(platformRepository).delete(platform.getId());
        Assert.assertNotEquals(200,repositoryManager.removePlatform(platform).getStatus());
    }

    @Test
    public void testGetResourcesForPlatform(){
        Platform platform = generatePlatformB();
        CoreResource coreResource = generateCoreResourceWithoutId();

        when(platformRepository.findOne(platform.getId())).thenReturn(platform);
        when(resourceRepository.findByInterworkingServiceURL(platform.getInterworkingServices().get(0).getUrl())).
                thenReturn(Arrays.asList(coreResource));

        Assert.assertEquals(repositoryManager.getResourcesForPlatform(platform.getId()),Arrays.asList(coreResource));
    }
}