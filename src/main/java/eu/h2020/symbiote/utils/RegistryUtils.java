package eu.h2020.symbiote.utils;

import eu.h2020.symbiote.core.internal.CoreResource;
import eu.h2020.symbiote.core.internal.CoreResourceType;
import eu.h2020.symbiote.model.cim.*;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

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
     * Converts given Map of Core Resources to Resources
     *
     * @param coreResources
     * @return
     */
    public static Map<String, Resource> convertCoreResourcesToResourcesMap(Map<String, CoreResource> coreResources) {
        Map<String, Resource> resources = new HashMap<>();
        for (String key : coreResources.keySet()) {
            Resource resource = convertCoreResourceToResource(coreResources.get(key));
            resources.put(key, resource);
        }
        return resources;
    }

    /**
     * Converts given list of Core Resources to Resources
     *
     * @param coreResources
     * @return
     */
    public static List<Resource> convertCoreResourcesToResourcesList(List<CoreResource> coreResources) {
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
        if (coreResource.getDescription() != null) resource.setDescription(coreResource.getDescription());
        if (coreResource.getName() != null) resource.setName(coreResource.getName());
        if (coreResource.getInterworkingServiceURL() != null)
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
        if (resource.getDescription() != null) coreResource.setDescription(resource.getDescription());
        if (resource.getName() != null) coreResource.setName(resource.getName());
        if (resource.getInterworkingServiceURL() != null)
            coreResource.setInterworkingServiceURL(resource.getInterworkingServiceURL());
        coreResource.setType(getTypeForResource(resource));
        return coreResource;
    }


    public static CoreResourceType getTypeForResource(Resource resource) {
        CoreResourceType type = null;
        if (resource instanceof Actuator) {
            type = CoreResourceType.ACTUATOR;
        } else if (resource instanceof Service) {
            type = CoreResourceType.SERVICE;
        } else if (resource instanceof Device) {
            type = CoreResourceType.DEVICE;
        } else if (resource instanceof MobileSensor) {
            type = CoreResourceType.MOBILE_SENSOR;
        } else if (resource instanceof StationarySensor) {
            type = CoreResourceType.STATIONARY_SENSOR;
        } else {
            log.error("Unrecognized type of Resource retrieved!");
        }
        return type;
    }


}
