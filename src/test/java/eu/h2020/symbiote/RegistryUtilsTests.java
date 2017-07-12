package eu.h2020.symbiote;

import eu.h2020.symbiote.core.model.Platform;
import eu.h2020.symbiote.core.model.internal.CoreResource;
import eu.h2020.symbiote.core.model.resources.Resource;
import eu.h2020.symbiote.model.RegistryPlatform;
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
    public void testCoreResourceToResourceConverter() {

        CoreResource coreResource = generateCoreResource();
        addIdToCoreResource(coreResource);

        Resource resource = RegistryUtils.convertCoreResourceToResource(coreResource);

        Assert.assertTrue(coreResource.getId().equals(resource.getId()));
        Assert.assertTrue(coreResource.getLabels().equals(resource.getLabels()));
        Assert.assertTrue(coreResource.getComments().equals(resource.getComments()));
        Assert.assertTrue(coreResource.getInterworkingServiceURL().equals(resource.getInterworkingServiceURL()));
    }

    @Test
    public void testCoreResourcesToResourcesConverter() {

        CoreResource coreResource1 = generateCoreResource();
        addIdToCoreResource(coreResource1);
        CoreResource coreResource2 = generateCoreResource();
        addIdToCoreResource(coreResource2);

        List<CoreResource> coreResources = new ArrayList<>();
        coreResources.add(coreResource1);
        coreResources.add(coreResource2);

        List<Resource> resources = RegistryUtils.convertCoreResourcesToResources(coreResources);

        for (int i = 0; i < coreResources.size(); i++) {
            Assert.assertTrue(coreResources.get(i).getId().equals(resources.get(i).getId()));
            Assert.assertTrue(coreResources.get(i).getLabels().equals(resources.get(i).getLabels()));
            Assert.assertTrue(coreResources.get(i).getComments().equals(resources.get(i).getComments()));
            Assert.assertTrue(coreResources.get(i).getInterworkingServiceURL().equals(resources.get(i).getInterworkingServiceURL()));
        }
    }

    @Test
    public void testResourceToCoreResourceConverter() {
        Resource resource = generateResource();
        addIdToResource(resource);
        CoreResource coreResource = RegistryUtils.convertResourceToCoreResource(resource);

        Assert.assertTrue(coreResource.getId().equals(resource.getId()));
        Assert.assertTrue(coreResource.getLabels().equals(resource.getLabels()));
        Assert.assertTrue(coreResource.getComments().equals(resource.getComments()));
        Assert.assertTrue(coreResource.getInterworkingServiceURL().equals(resource.getInterworkingServiceURL()));
    }

    @Test
    public void testConverterRequestPlatformToRegistryPlatform() {
        Platform requestPlatform = generateSymbiotePlatformA();
        RegistryPlatform registryPlatform = RegistryUtils.convertRequestPlatformToRegistryPlatform(requestPlatform);

        Assert.assertTrue(registryPlatform.getComments().get(0).equals(requestPlatform.getDescription()));
        Assert.assertTrue(registryPlatform.getLabels().get(0).equals(requestPlatform.getName()));
        Assert.assertTrue(registryPlatform.getInterworkingServices().get(0).getUrl().equals(requestPlatform.getUrl()));
        Assert.assertTrue(registryPlatform.getInterworkingServices().get(0).getInformationModelId().
                equals(requestPlatform.getInformationModelId()));
        Assert.assertNotNull(registryPlatform.getBody());
        Assert.assertNotNull(registryPlatform.getRdfFormat());
    }

    @Test
    public void testConverterRegistryPlatformToRequestPlatform() {
        RegistryPlatform registryPlatform = generateRegistryPlatformB();
        Platform requestPlatform = RegistryUtils.convertRegistryPlatformToRequestPlatform(registryPlatform);

        Assert.assertTrue(registryPlatform.getComments().get(0).equals(requestPlatform.getDescription()));
        Assert.assertTrue(registryPlatform.getLabels().get(0).equals(requestPlatform.getName()));
        Assert.assertTrue(registryPlatform.getInterworkingServices().get(0).getUrl().equals(requestPlatform.getUrl()));
        Assert.assertTrue(registryPlatform.getInterworkingServices().get(0).getInformationModelId().
                equals(requestPlatform.getInformationModelId()));
    }

    @Test
    public void testPlatformFieldsValidation() {
        RegistryPlatform platform = generateRegistryPlatformB();
        Assert.assertTrue(RegistryUtils.validateFields(platform));
    }

    @Test
    public void testPlatformFieldsValidationFail() {
        RegistryPlatform platform = null;
        Assert.assertTrue(!RegistryUtils.validateFields(platform));
        platform = new RegistryPlatform();
        Assert.assertTrue(!RegistryUtils.validateFields(platform));

        platform.setComments(new ArrayList<>());
        platform.setLabels(new ArrayList<>());
        platform.setInterworkingServices(new ArrayList<>());
        Assert.assertTrue(!RegistryUtils.validateFields(platform));

        platform.getComments().add(null);
        platform.getLabels().add(null);
        platform.getInterworkingServices().add(null);
        Assert.assertFalse(RegistryUtils.validateFields(platform));
    }

    @Test
    public void testResourceFieldsValidation() {
        Resource resource = generateResource();
        Assert.assertTrue(RegistryUtils.validateFields(resource));
    }

    @Test
    public void testResourceFieldsValidationFail() {
        Resource resource = null;
        Assert.assertTrue(!RegistryUtils.validateFields(resource));
        resource = new Resource();
        Assert.assertTrue(!RegistryUtils.validateFields(resource));

        resource.setInterworkingServiceURL(null);
        Assert.assertTrue(!RegistryUtils.validateFields(resource));
    }
}
