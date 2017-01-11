package eu.h2020.symbiote;

import com.google.gson.Gson;
import eu.h2020.symbiote.model.Platform;
import eu.h2020.symbiote.utils.JsonConverter;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

/**
 * Created by mateuszl on 11.01.2017.
 */
public class TestUtils {

    Gson gson;
    private Platform platformToSave;
    private String platformToSaveJson;


    @Before
    public void setUp() {
        platformToSave = Mockito.mock(Platform.class);

        gson = new Gson();
        platformToSaveJson = gson.toJson(platformToSave, Platform.class);
    }

    @Test
    public void testMockCreation() {
    }

    @Test
    public void testGettingPlatformFromJson() {
        assertNotNull(JsonConverter.getPlatformFromJson(platformToSaveJson));
        assertEquals(JsonConverter.getPlatformFromJson(platformToSaveJson), platformToSave);
    }
}
