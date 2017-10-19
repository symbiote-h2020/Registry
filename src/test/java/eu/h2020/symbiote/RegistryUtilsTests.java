package eu.h2020.symbiote;

import eu.h2020.symbiote.core.model.Platform;
import eu.h2020.symbiote.core.model.internal.CoreResource;
import eu.h2020.symbiote.core.model.internal.CoreResourceType;
import eu.h2020.symbiote.core.model.resources.*;
import eu.h2020.symbiote.security.commons.exceptions.custom.InvalidArgumentsException;
import eu.h2020.symbiote.utils.RegistryUtils;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import java.util.ArrayList;
import java.util.List;

import static eu.h2020.symbiote.TestSetupConfig.*;

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
    public void testCoreResourceToResourceConverter() throws InvalidArgumentsException {

        CoreResource coreResource = generateCoreResourceWithoutId();
        addIdToCoreResource(coreResource);

        Resource resource = RegistryUtils.convertCoreResourceToResource(coreResource);

        Assert.assertTrue(coreResource.getId().equals(resource.getId()));
        Assert.assertTrue(coreResource.getLabels().equals(resource.getLabels()));
        Assert.assertTrue(coreResource.getComments().equals(resource.getComments()));
        Assert.assertTrue(coreResource.getInterworkingServiceURL().equals(resource.getInterworkingServiceURL()));
    }

    @Test
    public void testCoreResourcesToResourcesConverter() throws InvalidArgumentsException {

        CoreResource coreResource1 = generateCoreResourceWithoutId();
        addIdToCoreResource(coreResource1);
        CoreResource coreResource2 = generateCoreResourceWithoutId();
        addIdToCoreResource(coreResource2);

        List<CoreResource> coreResources = new ArrayList<>();
        coreResources.add(coreResource1);
        coreResources.add(coreResource2);

        List<Resource> resources = RegistryUtils.convertCoreResourcesToResourcesList(coreResources);

        for (int i = 0; i < coreResources.size(); i++) {
            Assert.assertTrue(coreResources.get(i).getId().equals(resources.get(i).getId()));
            Assert.assertTrue(coreResources.get(i).getLabels().equals(resources.get(i).getLabels()));
            Assert.assertTrue(coreResources.get(i).getComments().equals(resources.get(i).getComments()));
            Assert.assertTrue(coreResources.get(i).getInterworkingServiceURL().equals(resources.get(i).getInterworkingServiceURL()));
        }
    }

    @Test
    public void testResourceToCoreResourceConverter() {
        Resource resource = generateResourceWithoutId();
        addIdToResource(resource);
        CoreResource coreResource = RegistryUtils.convertResourceToCoreResource(resource);

        Assert.assertTrue(coreResource.getId().equals(resource.getId()));
        Assert.assertTrue(coreResource.getLabels().equals(resource.getLabels()));
        Assert.assertTrue(coreResource.getComments().equals(resource.getComments()));
        Assert.assertTrue(coreResource.getInterworkingServiceURL().equals(resource.getInterworkingServiceURL()));
    }

    @Test
    public void testPlatformFieldsValidation() {
        Platform platform = generatePlatformB();
        Assert.assertTrue(RegistryUtils.validateFields(platform));
    }

    @Test
    public void testPlatformFieldsValidationFail() {
        Platform platform = null;
        Assert.assertTrue(!RegistryUtils.validateFields(platform));
        platform = new Platform();
        Assert.assertTrue(!RegistryUtils.validateFields(platform));

        platform.setComments(new ArrayList<>());
        Assert.assertTrue(!RegistryUtils.validateFields(platform));
        platform.setLabels(new ArrayList<>());
        Assert.assertTrue(!RegistryUtils.validateFields(platform));
        platform.setInterworkingServices(new ArrayList<>());
        Assert.assertTrue(!RegistryUtils.validateFields(platform));

        platform.getComments().add(null);
        Assert.assertTrue(!RegistryUtils.validateFields(platform));
        platform.getLabels().add(null);
        Assert.assertTrue(!RegistryUtils.validateFields(platform));
        platform.getInterworkingServices().add(null);
        Assert.assertFalse(RegistryUtils.validateFields(platform));
    }

    @Test
    public void testResourceFieldsValidation() {
        Resource resource = generateResourceWithoutId();
        Assert.assertTrue(RegistryUtils.validateFields(resource));
    }

    @Test
    public void testResourceFieldsValidationFail() {
        Resource resource = null;
        Assert.assertFalse(RegistryUtils.validateFields(resource));
        resource = new Resource();
        Assert.assertFalse(RegistryUtils.validateFields(resource));
        resource.setInterworkingServiceURL("");
        Assert.assertFalse(RegistryUtils.validateFields(resource));
        resource.setId("");
        Assert.assertFalse(RegistryUtils.validateFields(resource));
        resource.setLabels(new ArrayList<>());
        Assert.assertFalse(RegistryUtils.validateFields(resource));
        resource.setComments(new ArrayList<>());
        Assert.assertFalse(RegistryUtils.validateFields(resource));
    }

    @Test
    public void testResourceTypeChecker(){
        Resource resource = new Actuator();
        Assert.assertEquals(CoreResourceType.ACTUATOR, RegistryUtils.getTypeForResource(resource));
        resource = new Service();
        Assert.assertEquals(CoreResourceType.SERVICE, RegistryUtils.getTypeForResource(resource));
        resource = new Device();
        Assert.assertEquals(CoreResourceType.DEVICE, RegistryUtils.getTypeForResource(resource));
//        Resource resource1 = new MobileSensor(); //todo update
//        Assert.assertEquals(CoreResourceType.MOBILE_SENSOR, RegistryUtils.getTypeForResource(resource1));
        resource = new StationarySensor();
        Assert.assertEquals(CoreResourceType.STATIONARY_SENSOR, RegistryUtils.getTypeForResource(resource));

        Assert.assertNotEquals(CoreResourceType.MOBILE_SENSOR, RegistryUtils.getTypeForResource(resource));
    }
}