package eu.h2020.symbiote;

import eu.h2020.symbiote.core.internal.CoreResource;
import eu.h2020.symbiote.core.internal.CoreResourceType;
import eu.h2020.symbiote.model.cim.*;
import eu.h2020.symbiote.model.mim.Platform;
import eu.h2020.symbiote.security.commons.exceptions.custom.InvalidArgumentsException;
import eu.h2020.symbiote.utils.RegistryUtils;
import eu.h2020.symbiote.utils.ValidationUtils;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import java.util.ArrayList;
import java.util.List;

import static eu.h2020.symbiote.TestSetupConfig.*;

/**
 * Created by mateuszl
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
        Assert.assertTrue(coreResource.getName().equals(resource.getName()));
        Assert.assertTrue(coreResource.getDescription().equals(resource.getDescription()));
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
            Assert.assertTrue(coreResources.get(i).getName().equals(resources.get(i).getName()));
            Assert.assertTrue(coreResources.get(i).getDescription().equals(resources.get(i).getDescription()));
            Assert.assertTrue(coreResources.get(i).getInterworkingServiceURL().equals(resources.get(i).getInterworkingServiceURL()));
        }
    }

    @Test
    public void testResourceToCoreResourceConverter() {
        Resource resource = generateCoreResourceSensorWithoutId();
        addIdToResource(resource);
        CoreResource coreResource = RegistryUtils.convertResourceToCoreResource(resource);

        Assert.assertTrue(coreResource.getId().equals(resource.getId()));
        Assert.assertTrue(coreResource.getName().equals(resource.getName()));
        Assert.assertTrue(coreResource.getDescription().equals(resource.getDescription()));
        Assert.assertTrue(coreResource.getInterworkingServiceURL().equals(resource.getInterworkingServiceURL()));
    }

    @Test
    public void testPlatformFieldsValidation() {
        Platform platform = generatePlatformB();
        Assert.assertTrue(ValidationUtils.validateFields(platform));
    }

    @Test
    public void testPlatformFieldsValidationFail() {
        Platform platform = null;
        Assert.assertFalse(ValidationUtils.validateFields(platform));
        platform = new Platform();
        Assert.assertFalse(ValidationUtils.validateFields(platform));

        platform.setDescription(new ArrayList<>());
        Assert.assertFalse(ValidationUtils.validateFields(platform));
        platform.setName("");
        Assert.assertFalse(ValidationUtils.validateFields(platform));
        platform.setInterworkingServices(new ArrayList<>());
        Assert.assertFalse(ValidationUtils.validateFields(platform));

        platform.getDescription().add(null);
        Assert.assertFalse(ValidationUtils.validateFields(platform));
        platform.getInterworkingServices().add(null);
        Assert.assertFalse(ValidationUtils.validateFields(platform));
    }

    @Test
    public void testResourceFieldsValidation() {
        Resource resource = generateCoreResourceSensorWithoutId();
        Assert.assertTrue(ValidationUtils.validateFields(resource));
    }

    @Test
    public void testResourceFieldsValidationFail() {
        Resource resource = null;
        Assert.assertFalse(ValidationUtils.validateFields(resource));
        resource = new Resource();
        Assert.assertFalse(ValidationUtils.validateFields(resource));
        resource.setInterworkingServiceURL("");
        Assert.assertFalse(ValidationUtils.validateFields(resource));
        resource.setId("");
        Assert.assertFalse(ValidationUtils.validateFields(resource));
        resource.setName("");
        Assert.assertFalse(ValidationUtils.validateFields(resource));
        resource.setDescription(new ArrayList<>());
        Assert.assertFalse(ValidationUtils.validateFields(resource));
    }

    @Test
    public void testResourceTypeChecker(){
        Resource resource = new Actuator();
        Assert.assertEquals(CoreResourceType.ACTUATOR, RegistryUtils.getTypeForResource(resource));
        resource = new Service();
        Assert.assertEquals(CoreResourceType.SERVICE, RegistryUtils.getTypeForResource(resource));
        resource = new Device();
        Assert.assertEquals(CoreResourceType.DEVICE, RegistryUtils.getTypeForResource(resource));
        Assert.assertNotEquals(CoreResourceType.MOBILE_SENSOR, RegistryUtils.getTypeForResource(resource));
    }
}