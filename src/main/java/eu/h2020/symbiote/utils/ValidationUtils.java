package eu.h2020.symbiote.utils;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import eu.h2020.symbiote.cloud.model.ssp.SspRegInfo;
import eu.h2020.symbiote.core.internal.CoreResourceRegistryRequest;
import eu.h2020.symbiote.core.internal.CoreSdevRegistryRequest;
import eu.h2020.symbiote.core.internal.CoreSspResourceRegistryRequest;
import eu.h2020.symbiote.managers.RepositoryManager;
import eu.h2020.symbiote.model.CoreSspResource;
import eu.h2020.symbiote.model.cim.*;
import eu.h2020.symbiote.model.mim.*;
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
 * Created by mateuszl on 29.06.2018.
 */
public class ValidationUtils {

    private static Log log = LogFactory.getLog(ValidationUtils.class);

    private ValidationUtils() {
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


    public static void validateInformationModelForCreation(InformationModel informationModelReceived) {

        validateInformationModel(informationModelReceived);

        if (StringUtils.isNotBlank(informationModelReceived.getId())) {
            throw new IllegalArgumentException("Id is not blank! Request denied!");
        }
    }

    public static void validateInformationModelForModification(InformationModel informationModelReceived) {

        validateInformationModel(informationModelReceived);

        if (StringUtils.isBlank(informationModelReceived.getId())) {
            throw new IllegalArgumentException("Id is blank! Request denied!");
        }
    }

    public static void validateInformationModelForRemoval(InformationModel informationModelReceived) {

        if (informationModelReceived == null) {
            throw new NullPointerException("Received Information Model is null!");

        } else if (StringUtils.isBlank(informationModelReceived.getId())) {
            throw new IllegalArgumentException("Id is blank! Request denied!");
        }
    }

    private static void validateInformationModel(InformationModel informationModelReceived) {

        if (informationModelReceived == null) {
            throw new NullPointerException("Received Information Model is null!");

        } else if (informationModelReceived.getRdfFormat() == null) {
            throw new IllegalArgumentException("RDF Format is null! Request denied!");

        } else if (StringUtils.isBlank(informationModelReceived.getRdf())) {
            throw new IllegalArgumentException("RDF is blank! Request denied!");

        } else if (StringUtils.isBlank(informationModelReceived.getName())) {
            throw new IllegalArgumentException("Name is blank! Request denied!");

        } else if (StringUtils.isBlank(informationModelReceived.getOwner())) {
            throw new IllegalArgumentException("Owner is blank! Request denied!");

        } else if (StringUtils.isBlank(informationModelReceived.getUri())) {
            throw new IllegalArgumentException("URI is blank! Request denied!");
        }
    }


    /**
     * Validates if there is no id in given Sdev and if there is a match with SSP
     *
     * @param repositoryManager
     * @param request
     * @throws IllegalAccessException
     */
    public static void validateIfSdevMatchWithSspForCreation(RepositoryManager repositoryManager, CoreSdevRegistryRequest request)
            throws IllegalAccessException {

        SspRegInfo sDev = request.getBody();

        if (StringUtils.isNotBlank(sDev.getSymId()))
            throw new IllegalAccessException("The SymId of Sdev is not blank!");

        validateIfMatchWithSsp(repositoryManager, request);
    }

    /**
     * Validates if there is an id in given Sdev and if there is a match with SSP
     *
     * @param repositoryManager
     * @param request
     * @throws IllegalAccessException
     */
    public static void validateIfSdevMatchWithSspForModification(RepositoryManager repositoryManager, CoreSdevRegistryRequest request)
            throws IllegalAccessException {

        SspRegInfo sDev = request.getBody();


        if (StringUtils.isBlank(sDev.getSymId()))
            throw new IllegalAccessException("The SymId of Sdev is blank!");

        if (repositoryManager.getSdevById(sDev.getSymId()) == null)
            throw new IllegalAccessException("There is no sDev in database with given SymId !");

        validateIfMatchWithSsp(repositoryManager, request);
    }

    /**
     * Validates if there is a match with SSP in given request
     *
     * @param repositoryManager
     * @param request
     * @throws IllegalAccessException
     */
    private static void validateIfMatchWithSsp(RepositoryManager repositoryManager, CoreSdevRegistryRequest request)
            throws IllegalAccessException {

        SspRegInfo sDev = request.getBody();

        //check if given sspId exists in DB
        if (!repositoryManager.checkIfSspExists(sDev.getPluginId())) {                                                  //Id of SSP given in Sdev (PluginId field)
            throw new IllegalAccessException("Ssp with given SspId does not exist in database!");
        }

        //check if given Sdevs pluginURL match to any of Ssps InterworkingInterfaceURL
        else if (!repositoryManager.getSspById(request.getSspId()).getInterworkingServices().stream()
                .map(InterworkingService::getUrl)
                .anyMatch(url -> url.equals(sDev.getPluginURL()))) {
            throw new IllegalAccessException("Given Sdevs pluginURL does not match to any of Ssps InterworkingServiceURL!");
        }
    }

    public static void validateSspResource(CoreSspResourceRegistryRequest request, RepositoryManager repositoryManager) {

        log.debug("Getting ssp by id : " + request.getSspId());
        SmartSpace sspById = repositoryManager.getSspById(request.getSspId());

        if (sspById == null) {
            log.error("There is no such ssp in db!");
            throw new IllegalArgumentException("There is no such ssp in db!");
        }

        log.debug("Getting sdev by id : " + request.getSdevId());
        SspRegInfo sdevById = repositoryManager.getSdevById(request.getSdevId());

        if (sdevById == null) {
            log.error("There is no such sdev in DB!");
            throw new IllegalArgumentException("There is no such sdev in DB!");
        } else {
            log.debug("Sdev by id found");
        }

        if (sdevById.getPluginId() == null) {
            log.error("Sdev " + sdevById.getSymId() + " from the database has null pluginId ");
            throw new IllegalArgumentException("Sdev " + sdevById.getSymId() + " from the database has null pluginId ");
        }

        if (request.getBody().keySet() == null) {
            log.error("keysetisnull");
            throw new IllegalArgumentException("Keyset is null");
        }

        //checks if there exists a matching IS URL in SSP to one given in Resource
        for (String k : request.getBody().keySet()) {
            log.debug("Checking for resource " + k);
            String interworkingServiceURL = request.getBody().get(k).getInterworkingServiceURL();
            if (interworkingServiceURL == null) {
                log.error("Resource " + k + " has null interworking service url");
                throw new IllegalArgumentException("Resource " + k + " has null interworking service url");
            } else {
                log.debug("Found iiservice: " + interworkingServiceURL);
            }
            if (sspById.getInterworkingServices() == null) {
                log.error("SSP " + request.getSspId() + " have a null list of interworking services");
                throw new IllegalArgumentException("SSP " + request.getSspId() + " have a null list of interworking services");
            } else {
                log.debug("Ssp has a list of interworking services");
            }


            if (
                    sspById.getInterworkingServices().stream()
                            .map(InterworkingService::getUrl)
                            .noneMatch(url -> url.equalsIgnoreCase(interworkingServiceURL))) {
                log.error("there does not exist a matching InterworkingService URL in SSP to one given in Resource!");
                throw new IllegalArgumentException("there does not exist a matching InterworkingService URL in SSP to one given in Resource!");
            }
        }

        //checks if SSP ID in request matches with SSP ID in given SDEV
        if (!sdevById.getPluginId().equals(request.getSspId())) {
            throw new IllegalArgumentException("SSP ID in request does not match with SSP ID in given SDEV");
        }
    }


    public static boolean validateFields(Federation federation) {
        //// TODO: should i check some more information?
        if (federation.getId() == null || federation.getId().isEmpty()) return false;
        if (federation.getName() == null || federation.getName().isEmpty()) return false;
        return true;
    }

    public static boolean validateFields(SmartSpace smartSpace) {
        //// TODO: should i check some more information?
        if (smartSpace == null) return false;
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
        try {
            checkIfResourcesDoesNotHaveIds(resources);
        } catch (IllegalArgumentException e) {
            return false;
        }
        return true;
    }

    /**
     * Checks if given request consists of resources, which does not have any content in ID field.
     *
     * @param request
     * @return true if given resources don't have an ID.
     */
    public static boolean checkIfResourcesDoesNotHaveIds(CoreSspResourceRegistryRequest request) {
        Map<String, Resource> resourceMap = request.getBody();
        List<Resource> resources = resourceMap.values().stream().collect(Collectors.toList());
        try {
            checkIfResourcesDoesNotHaveIds(resources);
        } catch (IllegalArgumentException e) {
            return false;
        }
        return true;
    }

    private static void checkIfResourcesDoesNotHaveIds(List<Resource> resources) {

        for (Resource resource : resources) {
            if (!checkIfResourceDoesNotHaveAnId(resource))
                throw new IllegalArgumentException("One of the resources has an ID or list with resources is invalid. Resources not created!");
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
                    if (!checkIfResourceDoesNotHaveAnId(service))
                        throw new IllegalArgumentException("One of the services has an ID or list with resources is invalid. Resources not created!");
                }
            }
        }
    }

    /**
     * Checks if given request consists of resources, which does not have any content in ID field.
     *
     * @param request
     * @return true if given resources don't have an ID.
     */
    public static void checkIfResourcesHaveNullOrEmptyId(CoreSspResourceRegistryRequest request) {
        List<Resource> resources = request.getBody().values().stream().collect(Collectors.toList());

        for (Resource resource : resources) {
            if (!resourceHasId(resource))
                throw new IllegalArgumentException("One of the resources has ID or list with resources is invalid. Resources not modified!");

            List<Service> services = new ArrayList<>();
            try {
                if (resource instanceof Device) services = ((Device) resource).getServices();
            } catch (Exception e) {
                log.error(e);
                throw new IllegalArgumentException("Exception occured when casting Resource type");
            }

            if (services != null && !services.isEmpty()) {
                for (Service service : services) {
                    if (!resourceHasId(service))
                        throw new IllegalArgumentException("One of the services has ID or list with resources is invalid. Resources not modified!");
                }
            }
        }
    }

    private static boolean resourceHasId(Resource resource) {
        if (StringUtils.isBlank(resource.getId())) {
            log.error("One of the resources (or actuating services) does not have an ID!");
            return false;
        }
        return true;
    }


    private static boolean checkIfResourceDoesNotHaveAnId(Resource resource) {
        if (StringUtils.isNotBlank(resource.getId())) {
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

    public static void checkIfDK1IsNotBlank(SspRegInfo receivedSdev) throws IllegalAccessException {
        if (receivedSdev.getRoaming() && StringUtils.isBlank(receivedSdev.getDerivedKey1()))
            throw new IllegalAccessException("DerivedKey1 can not be blank!");
    }
}
