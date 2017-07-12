package eu.h2020.symbiote;

import eu.h2020.symbiote.core.model.Platform;
import eu.h2020.symbiote.core.model.internal.CoreResource;
import eu.h2020.symbiote.core.model.resources.Resource;
import eu.h2020.symbiote.model.RegistryPlatform;
import eu.h2020.symbiote.utils.RegistryUtils;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.springframework.util.Assert;

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

        Resource resource = RegistryUtils.convertCoreResourceToResource(coreResource);

        Assert.isTrue(coreResource.getId().equals(resource.getId()), "Different Ids!");
        Assert.isTrue(coreResource.getLabels().equals(resource.getLabels()), "Different labels!");
        Assert.isTrue(coreResource.getComments().equals(resource.getComments()), "Different comments!");
        Assert.isTrue(coreResource.getInterworkingServiceURL().equals(resource.getInterworkingServiceURL()), "Different interworkingServiceUrls!");
    }

    @Test
    public void testCoreResourcesToResourcesConverter() {

        CoreResource coreResource1 = generateCoreResource();
        CoreResource coreResource2 = generateCoreResource();

        List<CoreResource> coreResources = new ArrayList<>();
        coreResources.add(coreResource1);
        coreResources.add(coreResource2);

        List<Resource> resources = RegistryUtils.convertCoreResourcesToResources(coreResources);

        for (int i = 0; i < coreResources.size(); i++) {
            Assert.isTrue(coreResources.get(i).getId().equals(resources.get(i).getId()), "Different Ids!");
            Assert.isTrue(coreResources.get(i).getLabels().equals(resources.get(i).getLabels()), "Different labels!");
            Assert.isTrue(coreResources.get(i).getComments().equals(resources.get(i).getComments()), "Different comments!");
            Assert.isTrue(coreResources.get(i).getInterworkingServiceURL().equals(resources.get(i).getInterworkingServiceURL()), "Different interworkingServiceUrls!");
        }
    }

    @Test
    public void testResourceToCoreResourceConverter() {
        Resource resource = generateResource();
        CoreResource coreResource = RegistryUtils.convertResourceToCoreResource(resource);

        Assert.isTrue(coreResource.getId().equals(resource.getId()), "Different Ids!");
        Assert.isTrue(coreResource.getLabels().equals(resource.getLabels()), "Different labels!");
        Assert.isTrue(coreResource.getComments().equals(resource.getComments()), "Different comments!");
        Assert.isTrue(coreResource.getInterworkingServiceURL().equals(resource.getInterworkingServiceURL()), "Different interworkingServiceUrls!");
    }

    @Test
    public void testConverterRequestPlatformToRegistryPlatform() {
        Platform requestPlatform = generateSymbiotePlatformA();
        RegistryPlatform registryPlatform = RegistryUtils.convertRequestPlatformToRegistryPlatform(requestPlatform);

        Assert.isTrue(registryPlatform.getComments().get(0).equals(requestPlatform.getDescription()), "Different name!");
        Assert.isTrue(registryPlatform.getLabels().get(0).equals(requestPlatform.getName()), "Different labels!");
        Assert.isTrue(registryPlatform.getInterworkingServices().get(0).getUrl().equals(requestPlatform.getUrl()), "Different labels!");
        Assert.isTrue(registryPlatform.getInterworkingServices().get(0).getInformationModelId().
                equals(requestPlatform.getInformationModelId()), "Different Information Model Id!");
        Assert.hasText(registryPlatform.getBody(), "No body!");
        Assert.hasText(registryPlatform.getRdfFormat(), "No rdf format!");
    }

    @Test
    public void testConverterRegistryPlatformToRequestPlatform() {
        RegistryPlatform registryPlatform = generateRegistryPlatformB();
        Platform requestPlatform = RegistryUtils.convertRegistryPlatformToRequestPlatform(registryPlatform);

        Assert.isTrue(registryPlatform.getComments().get(0).equals(requestPlatform.getDescription()), "Different name!");
        Assert.isTrue(registryPlatform.getLabels().get(0).equals(requestPlatform.getName()), "Different labels!");
        Assert.isTrue(registryPlatform.getInterworkingServices().get(0).getUrl().equals(requestPlatform.getUrl()), "Different labels!");
        Assert.isTrue(registryPlatform.getInterworkingServices().get(0).getInformationModelId().
                equals(requestPlatform.getInformationModelId()), "Different Information Model Id!");
    }

    @Test
    public void testPlatformFieldsValidation() {
        RegistryPlatform platform = generateRegistryPlatformB();
        Assert.isTrue(RegistryUtils.validateFields(platform), "Wrong platform!");
    }

    @Test
    public void testPlatformFieldsValidationFail() {
        RegistryPlatform platform = null;
        Assert.isTrue(!RegistryUtils.validateFields(platform), "Wrong platform!");
        platform = new RegistryPlatform();
        Assert.isTrue(!RegistryUtils.validateFields(platform), "Wrong platform!");

        platform.setComments(new ArrayList<>());
        platform.setLabels(new ArrayList<>());
        platform.setInterworkingServices(new ArrayList<>());
        Assert.isTrue(!RegistryUtils.validateFields(platform), "Wrong platform!");

        platform.getComments().add(null);
        platform.getLabels().add(null);
        platform.getInterworkingServices().add(null);
        Assert.isTrue(!RegistryUtils.validateFields(platform), "Wrong platform!");
    }

    @Test
    public void testResourceFieldsValidation() {
        Resource resource = generateResource();
        Assert.isTrue(RegistryUtils.validateFields(resource), "Wrong resource!");
    }

    @Test
    public void testResourceFieldsValidationFail() {
        Resource resource = null;
        Assert.isTrue(!RegistryUtils.validateFields(resource), "Wrong resource!");
        resource = new Resource();
        Assert.isTrue(!RegistryUtils.validateFields(resource), "Wrong resource!");

        resource.setInterworkingServiceURL(null);
        Assert.isTrue(!RegistryUtils.validateFields(resource), "Wrong resource!");
    }
}
