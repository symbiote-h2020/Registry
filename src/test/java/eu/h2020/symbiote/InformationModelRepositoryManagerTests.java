package eu.h2020.symbiote;

import com.mongodb.MongoException;
import eu.h2020.symbiote.managers.RepositoryManager;
import eu.h2020.symbiote.model.mim.InformationModel;
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

import static eu.h2020.symbiote.TestSetupConfig.generateInformationModelA;
import static eu.h2020.symbiote.TestSetupConfig.generateInformationModelFull;
import static org.mockito.Mockito.*;

/**
 * Created by mateuszl
 */
@RunWith(MockitoJUnitRunner.class)
public class InformationModelRepositoryManagerTests {

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
    public void testSaveInformationModelTriggersRepository() {
        InformationModel informationModel = generateInformationModelFull();

        when(informationModelRepository.save(informationModel)).thenReturn(informationModel);

        repositoryManager.saveInformationModel(informationModel);
        try {
            Thread.sleep(1000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        verify(informationModelRepository).save(informationModel);
    }

    @Test
    public void testModifyInformationModelTriggersRepository() {
        InformationModel informationModel = generateInformationModelFull();
        when(informationModelRepository.save(informationModel)).thenReturn(informationModel);
        when(informationModelRepository.findOne(informationModel.getId())).thenReturn(informationModel);

        repositoryManager.modifyInformationModel(informationModel);
        try {
            Thread.sleep(1000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        verify(informationModelRepository).save(informationModel);
    }

    @Test
    public void testRemoveInformationModelTriggersRepository() {
        InformationModel informationModel = generateInformationModelA();
        when(informationModelRepository.findOne(informationModel.getId())).thenReturn(informationModel);

        repositoryManager.removeInformationModel(informationModel);
        try {
            Thread.sleep(1000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        verify(informationModelRepository).delete(informationModel);
    }

    @Test
    public void testSaveInformationModelReturnsStatus200() throws Exception {
        InformationModel informationModel = generateInformationModelFull();
        when(informationModelRepository.save(informationModel)).thenReturn(informationModel);

        Assert.assertEquals(200, repositoryManager.saveInformationModel(informationModel).getStatus());
    }

    @Test
    public void testSaveInformationModelWithWrongId() throws Exception {
        InformationModel informationModel = new InformationModel();
        Assert.assertNotEquals(200, repositoryManager.saveInformationModel(informationModel).getStatus());
    }

    @Test
    public void testSaveInformationModelMongoError() {
        InformationModel informationModel = generateInformationModelA();
        when(informationModelRepository.save(informationModel)).thenThrow(new MongoException("FAKE MONGO ERROR"));
        Assert.assertNotEquals(200, repositoryManager.saveInformationModel(informationModel).getStatus());
    }

    @Test
    public void testRemoveInformationModelWithWrongId() throws Exception {
        InformationModel informationModel = new InformationModel();
        Assert.assertNotEquals(200, repositoryManager.removeInformationModel(informationModel).getStatus());
    }

    @Test
    public void testModifyInformationModelWithWrongId() throws Exception {
        InformationModel informationModel = new InformationModel();
        Assert.assertNotEquals(200, repositoryManager.modifyInformationModel(informationModel).getStatus());
    }

    @Test
    public void testModifyInformationModelMongoError() {
        InformationModel informationModel = generateInformationModelA();
        doThrow(new MongoException("FAKE MONGO Exception")).when(informationModelRepository).save(informationModel);
        Assert.assertNotEquals(200, repositoryManager.modifyInformationModel(informationModel).getStatus());
    }

    @Test
    public void testRemoveInformationModelMongoError() {
        InformationModel informationModel = generateInformationModelA();
        doThrow(new MongoException("FAKE MONGO Exception")).when(informationModelRepository).delete(informationModel.getId());
        Assert.assertNotEquals(200, repositoryManager.removeInformationModel(informationModel).getStatus());
    }
}
