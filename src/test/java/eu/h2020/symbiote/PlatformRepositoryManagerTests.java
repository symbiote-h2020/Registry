package eu.h2020.symbiote;

import com.mongodb.MongoException;
import eu.h2020.symbiote.core.internal.CoreResource;
import eu.h2020.symbiote.managers.RepositoryManager;
import eu.h2020.symbiote.model.mim.Platform;
import eu.h2020.symbiote.repository.*;
import eu.h2020.symbiote.security.commons.exceptions.custom.InvalidArgumentsException;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import java.util.Arrays;

import static eu.h2020.symbiote.TestSetupConfig.*;
import static org.mockito.Mockito.*;

/**
 * Created by mateuszl
 */
@RunWith(MockitoJUnitRunner.class)
public class PlatformRepositoryManagerTests {

    @Mock
    PlatformRepository platformRepository;
    @Mock
    ResourceRepository resourceRepository;
    @Mock
    InformationModelRepository informationModelRepository;
    @Mock
    FederationRepository federationRepository;
    @Mock
    SspRepository sspRepository;
    @Mock
    CoreSspResourceRepository coreSspResourceRepository;
    @Mock
    SdevRepository sdevRepository;
    @InjectMocks
    RepositoryManager repositoryManager;

    @Before
    public void setup() {
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
    public void testModifyPlatformMongoError(){
        Platform platform = generatePlatformB();
        doThrow(new MongoException("FAKE MONGO Exception")).when(platformRepository).save(platform);
        Assert.assertNotEquals(200,repositoryManager.modifyPlatform(platform).getStatus());
    }

//    @Test
//    public void testRemovePlatformWithResourcesFail(){
//        Platform platform = generatePlatformB();
//        when(resourceRepository.findByInterworkingServiceURL(platform.)).thenReturn(Arrays.asList(new CoreResource()));
//        Assert.assertNotEquals(200,repositoryManager.removePlatform(platform).getStatus());
//    }

    @Test
    public void testRemovePlatformMongoError(){
        Platform platform = generatePlatformB();
        when(platformRepository.findOne(platform.getId())).thenReturn(platform);
        doThrow(new MongoException("FAKE MONGO Exception")).when(platformRepository).delete(platform.getId());
        Assert.assertNotEquals(200,repositoryManager.removePlatform(platform).getStatus());
    }

    @Test
    public void testGetResourcesForPlatform() throws InvalidArgumentsException {
        Platform platform = generatePlatformB();
        CoreResource coreResource = generateCoreResourceWithoutId();

        when(platformRepository.findOne(platform.getId())).thenReturn(platform);
        when(resourceRepository.findByInterworkingServiceURL(platform.getInterworkingServices().get(0).getUrl())).
                thenReturn(Arrays.asList(coreResource));

        Assert.assertEquals(repositoryManager.getResourcesForPlatform(platform.getId()),Arrays.asList(coreResource));
    }

    @Test
    public void testCopyingPlatformData() throws Exception {
        Platform foundPlatform = generatePlatformB();
        Platform requestPlatform = new Platform();
        requestPlatform.setId(foundPlatform.getId());
        when(platformRepository.save(any(Platform.class))).thenReturn(foundPlatform);
        when(platformRepository.findOne(requestPlatform.getId())).thenReturn(foundPlatform);

        repositoryManager.modifyPlatform(requestPlatform);
        try {
            Thread.sleep(1000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        ArgumentCaptor<Platform> platformArgumentCaptor = ArgumentCaptor.forClass(Platform.class);
        verify(platformRepository).save(platformArgumentCaptor.capture());

        Assert.assertTrue(platformArgumentCaptor.getValue().getId().equals(requestPlatform.getId()));
        Assert.assertTrue(platformArgumentCaptor.getValue().getDescription().get(0).equals(requestPlatform.getDescription().get(0)));
        Assert.assertTrue(platformArgumentCaptor.getValue().getName().equals(requestPlatform.getName()));
        Assert.assertTrue(platformArgumentCaptor.getValue().getInterworkingServices().get(0).getInformationModelId().
                equals(requestPlatform.getInterworkingServices().get(0).getInformationModelId()));
    }
}