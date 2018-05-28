package eu.h2020.symbiote.utils;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import eu.h2020.symbiote.core.internal.CoreResource;
import eu.h2020.symbiote.core.internal.CoreResourceRegistryRequest;
import eu.h2020.symbiote.core.internal.CoreResourceType;
import eu.h2020.symbiote.model.cim.*;
import eu.h2020.symbiote.model.mim.Federation;
import eu.h2020.symbiote.model.mim.InformationModel;
import eu.h2020.symbiote.model.mim.Platform;
import eu.h2020.symbiote.model.mim.SmartSpace;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import java.io.IOException;
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
     * Checks if given platform has all of the needed fields (besides the id field) and that neither is empty.
     *
     * @param platform platform to check
     * @return true if it has all the fields and neither is empty
     */
    public static boolean validateFields(Platform platform) {
        //todo extend validation to all fields?

        if (platform == null) {
            log.info("Given Platform is null");
            return false;
        } else {
            if (platform.getName() == null || platform.getInterworkingServices() == null || platform.getDescription() == null) {
                log.info("Given platform has some null fields");
                return false;
            } else if (platform.getInterworkingServices().isEmpty() || platform.getName().isEmpty()
                    || platform.getDescription().isEmpty()) {
                log.info("Given platform has some empty fields");
                return false;
            } else if (platform.getInterworkingServices().contains(null) || platform.getDescription().contains(null)) {
                log.info("Given platform has some lists with null objects");
                return false;
            }
            return true;
        }
    }

    /**
     * Checks if given resource has all of the needed fields (besides the id field) and that neither is empty.
     *
     * @param resource resource to check
     * @return true if it has all the fields and neither is empty.
     */
    public static boolean validateFields(Resource resource) {
        //todo extend validation to all fields?
        boolean b;
        if (resource == null) {
            log.info("Given resource is null");
            b = false;
        } else {
            if (resource.getInterworkingServiceURL() == null
                    || resource.getDescription() == null
                    || resource.getName() == null) {
                log.info("Given resource has some null fields");
                b = false;
            } else if (resource.getInterworkingServiceURL().isEmpty()
                    || resource.getDescription().isEmpty()
                    || resource.getName().isEmpty()) {
                log.info("Given resource has some empty fields");
                b = false;
            } else {
                b = true;
            }
        }
        return b;
    }

    /**
     * Checks if given informationModel has all of the needed fields (besides the id field) and that neither is empty.
     *
     * @param informationModel informationModel to check
     * @return true if it has all the fields and neither is empty.
     */
    public static boolean validateFields(InformationModel informationModel) {
        //todo extend validation to all fields?
        boolean b;
        if (informationModel == null) {
            log.info("Given informationModel is null");
            b = false;

        } else {
            if (informationModel.getName() == null
                    || informationModel.getOwner() == null
                    || informationModel.getUri() == null) {
                log.info("Given informationModel has some null fields");
                b = false;
            } else if (informationModel.getName().isEmpty()
                    || informationModel.getOwner().isEmpty()
                    || informationModel.getUri().isEmpty()) {
                log.info("Given informationModel has some empty fields");
                b = false;
            } else {
                b = true;
            }
        }
        return b;
    }

    public static boolean validateNullOrEmptyId(InformationModel informationModel) {
        if (informationModel.getId() == null || informationModel.getId().isEmpty()) return true;
        return false;
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

    public static boolean validateFields(Federation federation) {
        //// TODO: should i check some more information?
        if (federation.getId() == null || federation.getId().isEmpty()) return false;
        if (federation.getName() == null || federation.getName().isEmpty()) return false;
        return true;
    }

    public static boolean validateFields(SmartSpace smartSpace) {
        //// TODO: should i check some more information?
        if (smartSpace.getId() == null || smartSpace.getId().isEmpty()) return false;
        if (smartSpace.getName() == null || smartSpace.getName().isEmpty()) return false;
        return true;
    }

    public static Map<String, Resource> getMapFromRequestBody(CoreResourceRegistryRequest request) throws IOException {
        ObjectMapper mapper = new ObjectMapper();
        return mapper.readValue(request.getBody(), new TypeReference<Map<String, Resource>>() {
        });
    }

    /* Deprecated ////////////////////
    /**
     * Converts Platform (from Symbiote Libraries) to Platform (used in Registry Service)
     *
     * @param requestPlatform
     * @return
     *
    public static Platform convertRequestPlatformToPlatform(Platform requestPlatform) {
        Platform convertedPlatform = new Platform();
        if (requestPlatform.getId() != null) {
            convertedPlatform.setId(requestPlatform.getId());
        }
        if (requestPlatform.getName() != null) {
            convertedPlatform.setName(Arrays.asList(requestPlatform.get()));
        }
        if (requestPlatform.getDescription() != null) {
            convertedPlatform.setDescription(Arrays.asList(requestPlatform.getDescription()));
        }
        if (requestPlatform.getInformationModelId() != null) {
            InterworkingService interworkingService = new InterworkingService();
            interworkingService.setInformationModelId(requestPlatform.getInformationModelId());
            interworkingService.setUrl(requestPlatform.getUrl());
            convertedPlatform.setInterworkingServices(Arrays.asList(interworkingService));
        }
        if (convertedPlatform.getBody() == null) {
            convertedPlatform.setBody("not null body MOCKED");
        }
        if (convertedPlatform.getRdfFormat() == null) {
            convertedPlatform.setRdfFormat(RDFFormat.JSONLD.toString());
        }
        return convertedPlatform;
    }
    /**
     * Converts Platform (used in Registry Service) to Platform (from Symbiote Libraries)
     *
     * @param registryPlatform
     * @return

    public static Platform convertPlatformToRequestPlatform
    (Platform registryPlatform) {
        Platform platform = new Platform();

        if (registryPlatform.getId() != null && !registryPlatform.getId().isEmpty())
            platform.setPlatformId(registryPlatform.getId());
        if (registryPlatform.getName() != null && !registryPlatform.getName().isEmpty()) {
            if (registryPlatform.getName().get(0) != null)
                platform.setName(registryPlatform.getName().get(0));
        }
        if (registryPlatform.getDescription() != null && !registryPlatform.getDescription().isEmpty()) {
            if (registryPlatform.getDescription().get(0) != null)
                platform.setDescription(registryPlatform.getDescription().get(0));
        }
        if (registryPlatform.getInterworkingServices() != null && !registryPlatform.getInterworkingServices().isEmpty()) {
            if (registryPlatform.getInterworkingServices().get(0).getInformationModelId() != null)
                platform.setInformationModelId(registryPlatform.getInterworkingServices().get(0).getInformationModelId());
            if (registryPlatform.getInterworkingServices().get(0).getUrl() != null)
                platform.setUrl(registryPlatform.getInterworkingServices().get(0).getUrl());
        }
        return platform;
    }
*/
}
