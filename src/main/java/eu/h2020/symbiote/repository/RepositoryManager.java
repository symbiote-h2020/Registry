package eu.h2020.symbiote.repository;

import eu.h2020.symbiote.core.model.InterworkingService;
import eu.h2020.symbiote.core.model.internal.CoreResource;
import eu.h2020.symbiote.core.model.resources.Resource;
import eu.h2020.symbiote.model.PlatformResponse;
import eu.h2020.symbiote.model.RegistryPersistenceResult;
import eu.h2020.symbiote.model.RegistryPlatform;
import eu.h2020.symbiote.utils.RegistryUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.http.HttpStatus;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

/**
 * Class managing persistence actions for Platforms, Resources and Locations using MongoDB repositories.
 * <p>
 * Created by mateuszl
 */
@Component
public class RepositoryManager {

    private static final String RESOURCE_HAS_NULL_OR_EMPTY_ID = "Resource has null or empty ID!";
    private static final String GIVEN_PLATFORM_DOES_NOT_EXIST_IN_DATABASE = "Given platform does not exist in database!";
    private static Log log = LogFactory.getLog(RepositoryManager.class);
    private RegistryPlatformRepository registryPlatformRepository;
    private ResourceRepository resourceRepository;

    @Autowired
    public RepositoryManager(RegistryPlatformRepository registryPlatformRepository, ResourceRepository resourceRepository) {
        this.registryPlatformRepository = registryPlatformRepository;
        this.resourceRepository = resourceRepository;
    }

    /**
     * Saves given Platform in MongoDB. It triggers save action in Platform Repository and if it ends successfully
     * it returns http status '200' and Platform object with generated ID field.
     * If given platform is null or it already has an id the method will return 'bad request' status.
     * If saving in DB goes wrong it returns 'internal server error' status.
     * Url of given platform is appended with "/" if it does not end with it.
     *
     * @param registryPlatform Platform to save - in JSON format
     * @return PlatformResponse with Http status code and Platform object with unique "id" (generated in MongoDB)
     */
    public PlatformResponse savePlatform(RegistryPlatform registryPlatform) {
        PlatformResponse platformResponse = new PlatformResponse();
        RegistryPlatform savedRegistryPlatform = null;
        platformResponse.setPlatform(RegistryUtils.convertRegistryPlatformToRequestPlatform(registryPlatform));

        log.info("Received platform to save: " + registryPlatform);

        if (registryPlatform.getId() == null || registryPlatform.getId().isEmpty()) {
            log.error("Given platform has null or empty id!");
            platformResponse.setMessage("Given platform has null or empty id!");
            platformResponse.setStatus(HttpStatus.SC_BAD_REQUEST);
        } else {
            if (platformResponse.getStatus() != HttpStatus.SC_BAD_REQUEST) {
                try {
                    log.info("Saving platform: " + registryPlatform.getId());

                    savedRegistryPlatform = registryPlatformRepository.save(registryPlatform);
                    log.info("Platform \"" + savedRegistryPlatform + "\" saved !");
                    platformResponse.setStatus(HttpStatus.SC_OK);
                    platformResponse.setMessage("OK");
                    platformResponse.setPlatform(RegistryUtils.convertRegistryPlatformToRequestPlatform(savedRegistryPlatform));
                } catch (Exception e) {
                    log.error("Error occurred during Platform saving to db", e);
                    platformResponse.setMessage("Error occurred during Platform saving to db");
                    platformResponse.setStatus(HttpStatus.SC_INTERNAL_SERVER_ERROR);
                }
            }
        }
        return platformResponse;
    }

    /**
     * Removes given Platform from MongoDB. It triggers delete action in Platform Repository and if it ends successfully
     * it returns http status '200' and removed Platform object.
     * If given platform is null or it has no id or has an empty 'id' field the method will return 'bad request' status.
     * If saving in DB goes wrong it returns 'internal server error' status.
     *
     * @param registryPlatform Platform to remove - in JSON format
     * @return PlatformResponse with Http status code and removed Platform object - in JSON format
     */
    public PlatformResponse removePlatform(RegistryPlatform registryPlatform) {
        PlatformResponse platformResponse = new PlatformResponse();

        if (registryPlatform == null || registryPlatform.getId() == null || registryPlatform.getId().isEmpty()) {
            log.error("Given platform is null or has empty PlatformId!");
            platformResponse.setMessage("Given platform is null or has empty PlatformId!");
            platformResponse.setStatus(HttpStatus.SC_BAD_REQUEST);
        } else if (resourceRepository.findByInterworkingServiceURL(registryPlatform.getId()) != null
                && !resourceRepository.findByInterworkingServiceURL(registryPlatform.getId()).isEmpty()) {
            log.error("Given Platform has registered resources. Take care of resources first.");
            platformResponse.setMessage("Given Platform has registered resources. Take care of resources first.");
            platformResponse.setStatus(HttpStatus.SC_CONFLICT);
        } else {
            try {
                registryPlatformRepository.delete(registryPlatform.getId());
                log.info("Platform with id: " + registryPlatform.getId() + " removed !");

                platformResponse.setStatus(HttpStatus.SC_OK);
                platformResponse.setMessage("OK");
                platformResponse.setPlatform(RegistryUtils.convertRegistryPlatformToRequestPlatform(registryPlatform));
            } catch (Exception e) {
                log.error("Error occurred during Platform removing from db", e);
                platformResponse.setMessage("Error occurred during Platform removing from db");
                platformResponse.setStatus(HttpStatus.SC_INTERNAL_SERVER_ERROR);
            }
        }
        return platformResponse;
    }

    /**
     * Modifies (existing in mongodb) Platform accordingly to fields in given Platform.
     * It triggers delete and save actions in Platform Repository and if it ends successfully,
     * it returns http status '200' and new modified Platform object.
     * Url of given platform is appended with "/" if it does not end with it.
     * If given platform has any null field, it is retrieved from DB and fulfilled.
     * If given platform has no ID or has an empty 'id' field the method will return 'bad request' status.
     * If there is no Platform in database with ID same as given one, it returns 'bad request' status.
     * If saving in DB goes wrong it returns 'internal server error' status.
     *
     * @param registryPlatform Platform to remove - in JSON format
     * @return PlatformResponse with Http status code and modified Platform object - in JSON format
     */
    public PlatformResponse modifyPlatform(RegistryPlatform registryPlatform) {
        PlatformResponse platformResponse = new PlatformResponse();
        platformResponse.setPlatform(RegistryUtils.convertRegistryPlatformToRequestPlatform(registryPlatform));

        normalizePlatfromsIinterworkingServicesUrls(registryPlatform);

        RegistryPlatform foundRegistryPlatform = null;
        if (registryPlatform.getId() == null || registryPlatform.getId().isEmpty()) {
            log.error("Given platform has empty PlatformId!");
            platformResponse.setMessage("Given platform has empty PlatformId!");
            platformResponse.setStatus(HttpStatus.SC_BAD_REQUEST);
        } else {
            foundRegistryPlatform = registryPlatformRepository.findOne(registryPlatform.getId());
        }

        if (foundRegistryPlatform == null) {
            log.error(GIVEN_PLATFORM_DOES_NOT_EXIST_IN_DATABASE);
            platformResponse.setMessage(GIVEN_PLATFORM_DOES_NOT_EXIST_IN_DATABASE);
            platformResponse.setStatus(HttpStatus.SC_BAD_REQUEST);
        } else {
            try {
                //fulfilment of empty Platform fields before saving
                RegistryPlatform modifiedRegistryPlatform = copyExistingPlatformData(registryPlatform, foundRegistryPlatform);

                registryPlatformRepository.save(modifiedRegistryPlatform);
                log.info("Platform with id: " + modifiedRegistryPlatform.getId() + " modified !");

                platformResponse.setStatus(HttpStatus.SC_OK);
                platformResponse.setMessage("OK");
                platformResponse.setPlatform(RegistryUtils.convertRegistryPlatformToRequestPlatform(modifiedRegistryPlatform));
            } catch (Exception e) {
                log.error("Error occurred during Platform modifying in db", e);
                platformResponse.setMessage("Error occurred during Platform modifying in db");
                platformResponse.setStatus(HttpStatus.SC_INTERNAL_SERVER_ERROR);
            }
        }
        return platformResponse;
    }

    private RegistryPlatform copyExistingPlatformData(RegistryPlatform registryPlatform, RegistryPlatform foundRegistryPlatform) {
        if ((registryPlatform.getComments() == null || registryPlatform.getComments().isEmpty() || registryPlatform.getComments().get(0) == null)
                && foundRegistryPlatform.getComments() != null)
            registryPlatform.setComments(foundRegistryPlatform.getComments());
        if (registryPlatform.getRdfFormat() == null && foundRegistryPlatform.getRdfFormat() != null)
            registryPlatform.setRdfFormat(foundRegistryPlatform.getRdfFormat());
        if ((registryPlatform.getLabels() == null || registryPlatform.getLabels().isEmpty() || registryPlatform.getLabels().get(0) == null)
                && foundRegistryPlatform.getLabels() != null)
            registryPlatform.setLabels(foundRegistryPlatform.getLabels());
        if (registryPlatform.getBody() == null && foundRegistryPlatform.getBody() != null)
            registryPlatform.setBody(foundRegistryPlatform.getBody());
        if ((registryPlatform.getInterworkingServices() == null || registryPlatform.getInterworkingServices().isEmpty() ||
                registryPlatform.getInterworkingServices().get(0).getUrl() == null) && foundRegistryPlatform.getInterworkingServices() != null)
            registryPlatform.setInterworkingServices(foundRegistryPlatform.getInterworkingServices());

        return registryPlatform;
    }

    /**
     * Saves resource in MongoDB. Checks if URL in given resource ends with "/" and if not, appends it.
     * If in database there is no Platform with given PlatformId field, the method will return 'bad request' status.
     * If given resource has not null Id field, the method will return 'bad request' status.
     * If in database there is no Platform with given URL same as in given Resource, the method will return 'bad request'.
     * If saving in DB goes wrong it returns 'internal server error' status.
     *
     * @param resource Resource with given properties in JSON format
     * @return ResourceSavingResult containing Http status code and Resource with added 'Id' (generated in MongoDB),
     * in JSON format
     */
    public RegistryPersistenceResult saveResource(CoreResource resource) {
        RegistryPersistenceResult resourceSavingResult = new RegistryPersistenceResult();
        resourceSavingResult.setResource(resource);

        if (resource.getId() == null || resource.getId().isEmpty()) {
            log.error(RESOURCE_HAS_NULL_OR_EMPTY_ID);
            resourceSavingResult.setMessage(RESOURCE_HAS_NULL_OR_EMPTY_ID);
            resourceSavingResult.setStatus(HttpStatus.SC_BAD_REQUEST);
        } else {
            try {
                log.info("Saving Resource: " + resource.toString());

                CoreResource savedResource = resourceRepository.save(resource);
                log.info("Resource with id: " + savedResource.getId() + " saved !");

                resourceSavingResult.setStatus(HttpStatus.SC_OK);
                resourceSavingResult.setMessage("OK");
                resourceSavingResult.setResource(savedResource);
            } catch (Exception e) {
                log.error("Error occurred during Resource saving in db", e);
                resourceSavingResult.setMessage("Error occurred during Resource saving in db");
                resourceSavingResult.setStatus(HttpStatus.SC_INTERNAL_SERVER_ERROR);
            }
        }
        return resourceSavingResult;
    }

    /**
     * Deletes resource from MongoDB
     *
     * @param resource Resource with given properties in JSON format
     * @return ResourceSavingResult containing Http status code and Deleted Resource, in JSON format
     */
    public RegistryPersistenceResult removeResource(Resource resource) {
        RegistryPersistenceResult resourceRemovalResult = new RegistryPersistenceResult();

        if (resource == null || resource.getId() == null || resource.getId().isEmpty()) {
            log.error("Given resource is null or it has null or empty ID!");
            resourceRemovalResult.setMessage("Given resource is null or it has null or empty ID!");
            resourceRemovalResult.setStatus(HttpStatus.SC_BAD_REQUEST);
        } else {
            resourceRemovalResult.setResource(RegistryUtils.convertResourceToCoreResource(resource));
            try {
                CoreResource foundResource = resourceRepository.findOne(resource.getId());
                if (foundResource != null) {
                    resourceRepository.delete(resource.getId());
                    resourceRemovalResult.setStatus(HttpStatus.SC_OK);
                    resourceRemovalResult.setMessage("OK");
                    log.info("Resource with id: " + resource.getId() + " removed !");
                } else {
                    log.error("Given resource does not exist in database");
                    resourceRemovalResult.setMessage("Given resource does not exist in database");
                    resourceRemovalResult.setStatus(HttpStatus.SC_BAD_REQUEST);
                }
            } catch (Exception e) {
                log.error("Error occurred during Resource deleting from db", e);
                resourceRemovalResult.setMessage("Error occurred during Resource deleting from db");
                resourceRemovalResult.setStatus(HttpStatus.SC_INTERNAL_SERVER_ERROR);
            }
        }
        return resourceRemovalResult;
    }

    /**
     * Modifies given resource in MongoDB. If given resource does not consist some of the fields, empty ones are
     * fulfilled with data from the database.
     *
     * @param resource Resource with given properties in JSON format
     * @return ResourceSavingResult containing Http status code and Modified Resource, in JSON format
     */
    public RegistryPersistenceResult modifyResource(CoreResource resource) {
        RegistryPersistenceResult resourceSavingResult = new RegistryPersistenceResult();
        CoreResource foundResource;
        resourceSavingResult.setResource(resource);

        //todo

        if (resource.getId() == null || resource.getId().isEmpty()) {
            log.error(RESOURCE_HAS_NULL_OR_EMPTY_ID);
            resourceSavingResult.setMessage(RESOURCE_HAS_NULL_OR_EMPTY_ID);
            resourceSavingResult.setStatus(HttpStatus.SC_BAD_REQUEST);
            return resourceSavingResult;
        }

        if (resource.getInterworkingServiceURL() == null || resource.getInterworkingServiceURL().isEmpty()) {
            log.error("Given resource has null or empty Interworking service URL!");
            resourceSavingResult.setMessage("Given resource has null or empty Interworking service URL!");
            resourceSavingResult.setStatus(HttpStatus.SC_BAD_REQUEST);
            return resourceSavingResult;
        }

        normalizeResourceInterworkingServiceUrl(resource);

        foundResource = resourceRepository.findOne(resource.getId());


        if (foundResource == null) {
            log.error("Given resource does not exist in database!");
            resourceSavingResult.setMessage("Given resource does not exist in database!");
            resourceSavingResult.setStatus(HttpStatus.SC_BAD_REQUEST);
            return resourceSavingResult;
        }

        try {
            CoreResource savedResource = resourceRepository.save(resource);
            log.info("Resource with id: " + resource.getId() + " modified !");

            resourceSavingResult.setStatus(HttpStatus.SC_OK);
            resourceSavingResult.setMessage("OK");
            resourceSavingResult.setResource(savedResource);
        } catch (Exception e) {
            log.error("Error occurred during Resource modifying in db", e);
            resourceSavingResult.setMessage("Error occurred during Resource modifying in db");
            resourceSavingResult.setStatus(HttpStatus.SC_INTERNAL_SERVER_ERROR);
        }
        return resourceSavingResult;
    }

    //todo test method!
    public List<CoreResource> getResourcesForPlatform(String platformId) {
        RegistryPlatform platform = registryPlatformRepository.findOne(platformId);
        List<CoreResource> coreResources = new ArrayList<>();
        for (InterworkingService interworkingService : platform.getInterworkingServices()) {
            coreResources.addAll(resourceRepository.findByInterworkingServiceURL(interworkingService.getUrl()));
        }
        return coreResources;
    }


    private void normalizeResourceInterworkingServiceUrl(CoreResource resource) {
        if (resource.getInterworkingServiceURL().trim().charAt(resource.getInterworkingServiceURL().length() - 1)
                != "/".charAt(0)) {
            resource.setInterworkingServiceURL(resource.getInterworkingServiceURL().trim() + "/");
        }
    }

    private void normalizePlatfromsIinterworkingServicesUrls(RegistryPlatform platform) {
        if (platform.getInterworkingServices() != null && !platform.getInterworkingServices().isEmpty()) {
            for (InterworkingService service : platform.getInterworkingServices()) {
                if (service.getUrl().trim().charAt(service.getUrl().length() - 1) != "/".charAt(0)) {
                    service.setUrl(service.getUrl().trim() + "/");
                }
            }
        }
    }
}
