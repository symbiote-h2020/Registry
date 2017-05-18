package eu.h2020.symbiote;

import eu.h2020.symbiote.core.model.internal.CoreResource;
import eu.h2020.symbiote.core.model.resources.Resource;
import eu.h2020.symbiote.utils.RegistryUtils;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.springframework.util.Assert;

import static eu.h2020.symbiote.TestSetupConfig.generateCoreResource;
import static eu.h2020.symbiote.TestSetupConfig.generateResource;

/**
 * Created by mateuszl on 17.05.2017.
 */
public class RegistryUtilsTests {


    @Before
    public void setup() {
    }

    @After
    public void teardown() {
    }

    @Test
    public void testCorePlatformToPlatformConverter() {

    }

    @Test
    public void testCoreResourceToResourceConverter(){

        CoreResource coreResource = generateCoreResource();

        Resource resource = RegistryUtils.convertCoreResourceToResource(coreResource);

        Assert.isTrue(coreResource.getId().equals(resource.getId()), "Different Ids!");
        Assert.isTrue(coreResource.getLabels().equals(resource.getLabels()), "Different labels!");
        Assert.isTrue(coreResource.getComments().equals(resource.getComments()), "Different comments!");
        Assert.isTrue(coreResource.getInterworkingServiceURL().equals(resource.getInterworkingServiceURL()), "Different interworkingServiceUrls!");
    }

    @Test
    public void testResourceToCoreResourceConverter(){
        Resource resource = generateResource();

        CoreResource coreResource = RegistryUtils.convertResourceToCoreResource(resource);

        Assert.isTrue(coreResource.getId().equals(resource.getId()), "Different Ids!");
        Assert.isTrue(coreResource.getLabels().equals(resource.getLabels()), "Different labels!");
        Assert.isTrue(coreResource.getComments().equals(resource.getComments()), "Different comments!");
        Assert.isTrue(coreResource.getInterworkingServiceURL().equals(resource.getInterworkingServiceURL()), "Different interworkingServiceUrls!");
    }

    @Test
    public void testRequestPlatformToRegistryPlatformConverter(){

    }

    @Test
    public void testRegistryPlatformToRequestPlatformConverter(){

    }
}
