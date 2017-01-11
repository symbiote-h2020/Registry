package eu.h2020.symbiote;

import eu.h2020.symbiote.repository.PlatformRepository;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;

import static org.junit.Assert.assertNotNull;

/**
 * Created by mateuszl on 11.01.2017.
 */
public class TestPlatformRepository {
    PlatformRepository repo;

    @Before
    public void setUp(){
        repo = Mockito.mock(PlatformRepository.class);
    }

    @Test
    public void testRepo() {
        assertNotNull(repo);
    }
}
