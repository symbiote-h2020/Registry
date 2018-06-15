package eu.h2020.symbiote;

import eu.h2020.symbiote.managers.RepositoryManager;
import eu.h2020.symbiote.repository.*;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.class)
public class SspRepositoryManagerTests {

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

    @Test
    public void testTest() {
        //// TODO: 15.06.2018
        Assert.assertTrue(true);
    }

}
