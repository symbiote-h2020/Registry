package eu.h2020.symbiote.utils;

import eu.h2020.symbiote.core.model.InterworkingService;
import eu.h2020.symbiote.core.model.internal.CoreResource;
import eu.h2020.symbiote.core.model.resources.Resource;
import eu.h2020.symbiote.model.RegistryPlatform;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Utils for Registry project.
 * <p>
 * Created by mateuszl on 14.02.2017.
 */
public class RegistryUtils {

    private static Log log = LogFactory.getLog(RegistryUtils.class);

    private RegistryUtils() {
        throw new IllegalAccessError("Utility class");
    }

    /**
     * Checks if given platform has all of the needed fields (besides the id field) and that neither is empty.
     *
     * @param registryPlatform platform to check
     * @return true if it has all the fields and neither is empty
     */
    public static boolean validateFields(RegistryPlatform registryPlatform) {
        //todo extend validation to all fields
        boolean b;

        for (InterworkingService interworkingService : registryPlatform.getInterworkingServices()) {
            if (interworkingService.getUrl().trim().charAt(interworkingService.getUrl().length() - 1)
                    != "/".charAt(0)) {
                interworkingService.setUrl(interworkingService.getUrl().trim() + "/");
            }
        }

        if (registryPlatform.getBody() == null || registryPlatform.getLabels() == null || registryPlatform.getRdfFormat() == null) {
            log.info("Given platform has some null fields");
            b = false;
        } else if (registryPlatform.getBody().isEmpty() || registryPlatform.getLabels().isEmpty()
                || registryPlatform.getRdfFormat().isEmpty()) {
            log.info("Given platform has some empty fields");
            b = false;
        } else {
            b = true;
        }
        return b;
    }

    /**
     * Checks if given resource has all of the needed fields (besides the id field) and that neither is empty.
     *
     * @param resource resource to check
     * @return true if it has all the fields and neither is empty.
     */
    public static boolean validateFields(Resource resource) {
        //todo extend validation to all fields
        boolean b;
        if (resource.getInterworkingServiceURL() == null
                || resource.getComments() == null
                || resource.getLabels() == null) {
            log.info("Given resource has some null fields");
            b = false;
        } else if (resource.getInterworkingServiceURL().isEmpty()
                || resource.getComments().isEmpty()
                || resource.getLabels().isEmpty()) {
            log.info("Given resource has some empty fields");
            b = false;
        } else {
            b = true;
        }
        return b;
    }

    /**
     * Converts given list of Core Resources to Resources
     *
     * @param coreResources
     * @return
     */
    public static List<Resource> convertCoreResourcesToResources(List<CoreResource> coreResources) {
        List<Resource> resources = new ArrayList<>();
        for (CoreResource coreResource : coreResources) {
            Resource resource = convertCoreResourceToResource(coreResource);
            resources.add(resource);
        }
        return resources;
    }

    /**
     * Converts given Core Resource to Resource
     *
     * @param coreResource
     * @return
     */
    public static Resource convertCoreResourceToResource(CoreResource coreResource) {
        Resource resource = new Resource();
        if (coreResource.getId() != null) resource.setId(coreResource.getId());
        resource.setComments(coreResource.getComments());
        resource.setLabels(coreResource.getLabels());
        resource.setInterworkingServiceURL(coreResource.getInterworkingServiceURL());
        return resource;
    }

    /**
     * Converts given Resource to Core Resource
     *
     * @param resource
     * @return
     */
    public static CoreResource convertResourceToCoreResource(Resource resource) {
        CoreResource coreResource = new CoreResource();
        if (resource.getId() != null) coreResource.setId(resource.getId());
        coreResource.setComments(resource.getComments());
        coreResource.setLabels(resource.getLabels());
        coreResource.setInterworkingServiceURL(resource.getInterworkingServiceURL());
        return coreResource;
    }

    /**
     * Converts Platform (from Symbiote Libraries) to Platform (used in Registry Service)
     *
     * @param requestPlatform
     * @return
     */
    public static RegistryPlatform convertRequestPlatformToRegistryPlatform
    (eu.h2020.symbiote.core.model.Platform requestPlatform) {
        RegistryPlatform registryPlatform = new RegistryPlatform();

        registryPlatform.setId(requestPlatform.getPlatformId());

        registryPlatform.setLabels(Arrays.asList(requestPlatform.getName()));

        registryPlatform.setComments(Arrays.asList(requestPlatform.getDescription()));

        InterworkingService interworkingService = new InterworkingService();
        interworkingService.setInformationModelId(requestPlatform.getInformationModelId());
        interworkingService.setUrl(requestPlatform.getUrl());
        registryPlatform.setInterworkingServices(Arrays.asList(interworkingService));

        //// TODO: 10.05.2017  
        registryPlatform.setBody("not null body");
        registryPlatform.setRdfFormat("not null rdf");

        return registryPlatform;
    }

    /**
     * Converts Platform (used in Registry Service) to Platform (from Symbiote Libraries)
     *
     * @param registryRegistryPlatform
     * @return
     */
    public static eu.h2020.symbiote.core.model.Platform convertRegistryPlatformToRequestPlatform
    (RegistryPlatform registryRegistryPlatform) {
        eu.h2020.symbiote.core.model.Platform platform = new eu.h2020.symbiote.core.model.Platform();

        platform.setPlatformId(registryRegistryPlatform.getId());
        platform.setName(registryRegistryPlatform.getLabels().get(0));
        platform.setDescription(registryRegistryPlatform.getComments().get(0));
        platform.setInformationModelId(registryRegistryPlatform.getInterworkingServices().get(0).getInformationModelId());
        platform.setUrl(registryRegistryPlatform.getInterworkingServices().get(0).getUrl());

        return platform;
    }
}
