package eu.h2020.symbiote;

import com.mongodb.MongoException;
import eu.h2020.symbiote.core.internal.CoreResource;
import eu.h2020.symbiote.managers.RepositoryManager;
import eu.h2020.symbiote.model.cim.Resource;
import eu.h2020.symbiote.repository.*;
import eu.h2020.symbiote.security.commons.exceptions.custom.InvalidArgumentsException;
import org.apache.http.HttpStatus;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import static eu.h2020.symbiote.TestSetupConfig.*;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Created by mateuszl
 */
@RunWith(MockitoJUnitRunner.class)
public class ResourceRepositoryManagerTests {

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
    public void testSaveResourceTriggersRepository() throws InvalidArgumentsException {
        CoreResource resource = generateCoreResourceWithoutId();
        resource = addIdToCoreResource(resource);
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
    public void testModifyResourceTriggersRepository() throws InvalidArgumentsException {
        CoreResource resource = generateCoreResourceWithoutId();
        resource = addIdToCoreResource(resource);
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
    public void testRemoveResourceTriggersRepository() throws InvalidArgumentsException {
        CoreResource resource = generateCoreResourceWithoutId();
        addIdToCoreResource(resource);
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
    public void testSaveResourceReturnsStatus200() throws Exception {
        CoreResource coreResource = generateCoreResourceWithoutId();
        when(resourceRepository.save(coreResource)).thenReturn(addIdToCoreResource(coreResource));
        Assert.assertEquals(200,repositoryManager.saveResource(coreResource).getStatus());
    }

    @Test
    public void testSaveResourceWithWrongId() throws Exception {
        CoreResource coreResource = generateCoreResourceWithoutId();
        Assert.assertEquals(HttpStatus.SC_BAD_REQUEST,repositoryManager.saveResource(coreResource).getStatus());
    }

    @Test
    public void testSaveResourceMongoError() throws InvalidArgumentsException {
        CoreResource coreResource = generateCoreResourceWithoutId();
        coreResource = addIdToCoreResource(coreResource);
        when(resourceRepository.save(coreResource)).thenThrow(new MongoException("FAKE MONGO ERROR"));
        Assert.assertNotEquals(200,repositoryManager.saveResource(coreResource).getStatus());
    }

    @Test
    public void testModifyResourceWithWrongId() throws Exception {
        CoreResource coreResource = generateCoreResourceWithoutId();
        Assert.assertEquals(HttpStatus.SC_BAD_REQUEST,repositoryManager.modifyResource(coreResource).getStatus());
    }

    @Test
    public void testModifyResourceWithEmptyInterworkingService() throws Exception {
        CoreResource coreResource = generateCoreResourceWithoutId();
        coreResource = addIdToCoreResource(coreResource);
        coreResource.setInterworkingServiceURL("");
        Assert.assertEquals(HttpStatus.SC_BAD_REQUEST,repositoryManager.modifyResource(coreResource).getStatus());
    }

    @Test
    public void testModifyResourceWithNullInterworkingService() throws Exception {
        CoreResource coreResource = generateCoreResourceWithoutId();
        coreResource = addIdToCoreResource(coreResource);
        coreResource.setInterworkingServiceURL(null);
        Assert.assertEquals(HttpStatus.SC_BAD_REQUEST,repositoryManager.modifyResource(coreResource).getStatus());
    }

    @Test
    public void testModifyResourceThatDoesNotExistInDb() throws Exception {
        CoreResource coreResource = generateCoreResourceWithoutId();
        coreResource = addIdToCoreResource(coreResource);
        when(resourceRepository.findOne(coreResource.getId())).thenReturn(null);
        Assert.assertEquals(HttpStatus.SC_BAD_REQUEST,repositoryManager.modifyResource(coreResource).getStatus());
    }

    @Test
    public void testModifyResourceFailedWhenSaving() throws Exception {
        CoreResource coreResource = generateCoreResourceWithoutId();
        coreResource = addIdToCoreResource(coreResource);
        when(resourceRepository.findOne(coreResource.getId())).thenReturn(coreResource);
        when(resourceRepository.save(coreResource)).thenThrow(new MongoException("FAKE ERROR during saving"));
        Assert.assertEquals(HttpStatus.SC_INTERNAL_SERVER_ERROR,repositoryManager.modifyResource(coreResource).getStatus());
    }

    @Test
    public void testRemoveResourceWithoutId() throws Exception {
        Resource resource = generateCoreResourceSensorWithoutId();
        Assert.assertEquals(HttpStatus.SC_BAD_REQUEST,repositoryManager.removeResource(resource).getStatus());
    }

    @Test
    public void testRemoveResourceWithWrongId() throws Exception {
        Resource resource = generateCoreResourceSensorWithoutId();
        resource.setId("");
        Assert.assertEquals(HttpStatus.SC_BAD_REQUEST,repositoryManager.removeResource(resource).getStatus());
    }

    @Test
    public void testRemoveResourceThatDoesNotExist() throws Exception {
        Resource resource = generateCoreResourceSensorWithoutId();
        resource.setId("1234");
        when(resourceRepository.findOne(resource.getId())).thenReturn(null);
        Assert.assertEquals(HttpStatus.SC_BAD_REQUEST,repositoryManager.removeResource(resource).getStatus());
    }
}