package eu.h2020.symbiote.utils;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import eu.h2020.symbiote.cloud.model.ssp.SspRegInfo;
import eu.h2020.symbiote.core.internal.CoreResource;
import eu.h2020.symbiote.core.internal.CoreResourceRegistryRequest;
import eu.h2020.symbiote.core.internal.CoreResourceType;
import eu.h2020.symbiote.core.internal.CoreSspResourceRegistryRequest;
import eu.h2020.symbiote.model.CoreSspResource;
import eu.h2020.symbiote.model.cim.*;
import eu.h2020.symbiote.model.mim.Federation;
import eu.h2020.symbiote.model.mim.InformationModel;
import eu.h2020.symbiote.model.mim.Platform;
import eu.h2020.symbiote.model.mim.SmartSpace;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

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
        if (StringUtils.isBlank(smartSpace.getId())) return false;
        if (StringUtils.isBlank(smartSpace.getName())) return false;
        return true;
    }

    public static boolean validateFields(SspRegInfo sDev) {
        //// TODO: should i check some more information?
        if (StringUtils.isBlank(sDev.getPluginId())) return false;
        if (StringUtils.isBlank(sDev.getPluginURL())) return false;
        return true;
    }

    public static boolean validateFields(CoreSspResource coreSspResource) {
        //// TODO: should i check some more information?
        if (StringUtils.isBlank(coreSspResource.getId())) return false;
        if (StringUtils.isBlank(coreSspResource.getSdevId())) return false;
        return true;
    }

    public static Map<String, Resource> getMapFromRequestBody(CoreResourceRegistryRequest request) throws IOException {
        ObjectMapper mapper = new ObjectMapper();
        return mapper.readValue(request.getBody(), new TypeReference<Map<String, Resource>>() {
        });
    }

    /**
     * Checks if given request consists of resources, which does not have any content in ID field.
     *
     * @param request
     * @return true if given resources don't have an ID.
     */
    public static boolean checkIfResourcesDoesNotHaveIds(CoreResourceRegistryRequest request) {
        Map<String, Resource> resourceMap = new HashMap<>();
        ObjectMapper mapper = new ObjectMapper();
        try {
            resourceMap = mapper.readValue(request.getBody(), new TypeReference<Map<String, Resource>>() {
            });
        } catch (IOException e) {
            log.error("Could not deserialize content of request!" + e);
        }
        List<Resource> resources = resourceMap.values().stream().collect(Collectors.toList());
        return checkIfResourcesDoesNotHaveIds(resources);
    }

    /**
     * Checks if given request consists of resources, which does not have any content in ID field.
     *
     * @param request
     * @return true if given resources don't have an ID.
     */
    public static boolean checkIfResourcesDoesNotHaveIds(CoreSspResourceRegistryRequest request) {
        Map<String, Resource> resourceMap = request.getBody();
        ObjectMapper mapper = new ObjectMapper();
        List<Resource> resources = resourceMap.values().stream().collect(Collectors.toList());
        return checkIfResourcesDoesNotHaveIds(resources);
    }

    private static boolean checkIfResourcesDoesNotHaveIds(List<Resource> resources) {

        try {
            for (Resource resource : resources) {
                if (!checkIfResourceDoesNotHaveAnId(resource)) return false;
                List<Service> services = new ArrayList<>();
                if (resource instanceof Device) {
                    services = ((Device) resource).getServices();
                } else if (resource instanceof MobileSensor) {
                    services = ((MobileSensor) resource).getServices();
                } else if (resource instanceof Actuator) {
                    services = ((Actuator) resource).getServices();
                }
                if (services != null && !services.isEmpty()) {
                    for (Service service : services) {
                        if (!checkIfResourceDoesNotHaveAnId(service)) return false;
                    }
                }
            }
        } catch (Exception e) {
            log.error(e);
            return false;
        }

        return true;
    }

    private static boolean checkIfResourceDoesNotHaveAnId(Resource resource) {
        if (resource.getId() != null && !resource.getId().isEmpty()) {
            log.error("One of the resources (or actuating services) has an ID!");
            return false;
        }
        return true;
    }

    /**
     * @param request with resources to check
     * @return True if every of resources in list has an id. False if any of resources does not have an id.
     */
    public static boolean checkIfEveryResourceHasId(CoreResourceRegistryRequest request) {
        List<Resource> resources = retrieveResourcesListFromRequest(request);
        try {
            for (Resource resource : resources) {

                if (!checkIfResourceHasId(resource)) {                                                                  //if given resource does not have an id, false will be returned
                    log.error("One of resources does not have an id!");
                    return false;
                }

                if (resource instanceof Device) {
                    List<Service> services = ((Device) resource).getServices();                                         //if given Resource is a subtype of Device, it can have services inside

                    if (services != null && !services.isEmpty()) {
                        for (Service service : services) {
                            if (!checkIfResourceHasId(service)) {                                                       //if given service does not have an id, false will be returned
                                log.error("One of services does not have an id!");
                                return false;
                            }
                        }
                    }
                }

            }
        } catch (Exception e) {
            log.error(e);
            return false;
        }
        return true;
    }

    /**
     * Extracts Resource List from the request
     *
     * @param request
     * @return list with Resource objects
     */
    private static List<Resource> retrieveResourcesListFromRequest(CoreResourceRegistryRequest request) {
        Map<String, Resource> resourceMap = new HashMap<>();
        ObjectMapper mapper = new ObjectMapper();
        try {
            resourceMap = mapper.readValue(request.getBody(), new TypeReference<Map<String, Resource>>() {
            });
        } catch (IOException e) {
            log.error("Could not deserialize content of request!" + e);
        }
        return resourceMap.values().stream().collect(Collectors.toList());
    }

    /**
     * @param resource
     * @return True if resource has an ID. False if it has not an ID or ID is empty.
     */
    private static boolean checkIfResourceHasId(Resource resource) {
        if (resource.getId() == null || resource.getId().isEmpty()) {
            return false;
        } else {
            log.error("Resource (or actuating service) has an ID!");
            return true;
        }
    }
}
