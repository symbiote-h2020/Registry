package eu.h2020.symbiote;

import com.mongodb.MongoException;
import eu.h2020.symbiote.managers.RepositoryManager;
import eu.h2020.symbiote.model.mim.Federation;
import eu.h2020.symbiote.repository.*;
import org.apache.http.HttpStatus;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import static eu.h2020.symbiote.TestSetupConfig.generateFederationA;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Created by mateuszl
 */
@RunWith(MockitoJUnitRunner.class)
public class FederationRepositoryManagerTests {

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
    public void testSaveFederationTriggersRepository() {
        Federation federation = generateFederationA();
        when(federationRepository.save(federation)).thenReturn(federation);

        repositoryManager.saveFederation(federation);
        try {
            Thread.sleep(1000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        verify(federationRepository).save(federation);
    }

    @Test
    public void testModifyFederationTriggersRepository() {
        Federation federation = generateFederationA();
        when(federationRepository.save(federation)).thenReturn(federation);
        when(federationRepository.findOne(federation.getId())).thenReturn(federation);

        repositoryManager.modifyFederation(federation);
        try {
            Thread.sleep(1000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        verify(federationRepository).save(federation);
    }

    @Test
    public void testRemoveFederationTriggersRepository() {
        Federation federation = generateFederationA();
        when(federationRepository.findOne(federation.getId())).thenReturn(federation);
        repositoryManager.removeFederation(federation);
        try {
            Thread.sleep(1000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        verify(federationRepository).delete(federation);
    }


    @Test
    public void testSaveFederationReturnsStatus200() throws Exception {
        Federation federation = generateFederationA();
        when(federationRepository.save(federation)).thenReturn(federation);
        Assert.assertEquals(200, repositoryManager.saveFederation(federation).getStatus());
    }

    @Test
    public void testSaveFederationWithWrongId() throws Exception {
        Federation federation = generateFederationA();
        federation.setId(null);
        Assert.assertEquals(HttpStatus.SC_BAD_REQUEST, repositoryManager.saveFederation(federation).getStatus());
    }

    @Test
    public void testSaveFederationMongoError() {
        Federation federation = generateFederationA();
        when(federationRepository.save(federation)).thenThrow(new MongoException("FAKE MONGO ERROR"));
        Assert.assertNotEquals(200, repositoryManager.saveFederation(federation).getStatus());
    }

    @Test
    public void testModifyFederationWithWrongId() throws Exception {
        Federation federation = generateFederationA();
        federation.setId(null);
        Assert.assertEquals(HttpStatus.SC_BAD_REQUEST, repositoryManager.modifyFederation(federation).getStatus());
    }

    @Test
    public void testModifyFederationWithEmptySlaDefinition() throws Exception {
        Federation federation = generateFederationA();
        //FIXME set null for SLA in order to be able to build
        federation.setSlaConstraints(null);
        Assert.assertEquals(HttpStatus.SC_BAD_REQUEST, repositoryManager.modifyFederation(federation).getStatus());
    }

    @Test
    public void testModifyFederationWithSlaDefinition() throws Exception {
        Federation federation = generateFederationA();
        //FIXME set null for SLA in order to be able to build
        federation.setSlaConstraints(null);
        Assert.assertEquals(HttpStatus.SC_BAD_REQUEST, repositoryManager.modifyFederation(federation).getStatus());
    }

    @Test
    public void testModifyFederationThatDoesNotExistInDb() throws Exception {
        Federation federation = generateFederationA();
        when(federationRepository.findOne(federation.getId())).thenReturn(null);
        Assert.assertEquals(HttpStatus.SC_BAD_REQUEST, repositoryManager.modifyFederation(federation).getStatus());
    }

    @Test
    public void testModifyFederationFailedWhenSaving() throws Exception {
        Federation federation = generateFederationA();
        when(federationRepository.findOne(federation.getId())).thenReturn(federation);
        when(federationRepository.save(federation)).thenThrow(new MongoException("FAKE ERROR during saving"));
        Assert.assertEquals(HttpStatus.SC_INTERNAL_SERVER_ERROR, repositoryManager.modifyFederation(federation).getStatus());
    }

    @Test
    public void testRemoveFederationWithoutId() throws Exception {
        Federation federation = generateFederationA();
        federation.setId(null);
        Assert.assertEquals(HttpStatus.SC_BAD_REQUEST, repositoryManager.removeFederation(federation).getStatus());
    }

    @Test
    public void testRemoveFederationWithWrongId() throws Exception {
        Federation federation = generateFederationA();
        federation.setId("");
        Assert.assertEquals(HttpStatus.SC_BAD_REQUEST, repositoryManager.removeFederation(federation).getStatus());
    }

    @Test
    public void testRemoveFederationThatDoesNotExist() throws Exception {
        Federation federation = generateFederationA();
        Assert.assertEquals(HttpStatus.SC_BAD_REQUEST, repositoryManager.removeFederation(federation).getStatus());
    }
}