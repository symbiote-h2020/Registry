package eu.h2020.symbiote.repository;

import eu.h2020.symbiote.core.model.InterworkingService;
import eu.h2020.symbiote.core.model.internal.CoreResource;
import eu.h2020.symbiote.core.model.resources.Resource;
import eu.h2020.symbiote.model.*;
import eu.h2020.symbiote.utils.RegistryUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.http.HttpStatus;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

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
    private PlatformRepository platformRepository;
    private ResourceRepository resourceRepository;
    private InformationModelRepository modelRepository;

    @Autowired
    public RepositoryManager(PlatformRepository platformRepository, ResourceRepository resourceRepository,
                             InformationModelRepository modelRepository) {
        this.platformRepository = platformRepository;
        this.resourceRepository = resourceRepository;
        this.modelRepository = modelRepository;
    }

    /**
     * Saves given Platform in MongoDB. It triggers save action in Platform Repository and if it ends successfully
     * it returns http status '200' and Platform object with generated ID field.
     * If given platform is null or it already has an id the method will return 'bad request' status.
     * If saving in DB goes wrong it returns 'internal server error' status.
     * Url of given platform is appended with "/" if it does not end with it.
     *
     * @param platform Platform to save - in JSON format
     * @return PlatformResponse with Http status code and Platform object with unique "id" (generated in MongoDB)
     */
    public PlatformResponse savePlatform(Platform platform) {
        PlatformResponse platformResponse = new PlatformResponse();
        Platform savedPlatform = null;
        platformResponse.setPlatform(RegistryUtils.convertRegistryPlatformToRequestPlatform(platform));

        log.info("Received platform to save: " + platform);

        if (platform.getId() == null && platform.getId().isEmpty()) {
            log.error("Given platform has null or empty id!");
            platformResponse.setMessage("Given platform has null or empty id!");
            platformResponse.setStatus(HttpStatus.SC_BAD_REQUEST);
        } else {
            if (platformResponse.getStatus() != HttpStatus.SC_BAD_REQUEST) {
                try {
                    log.info("Saving platform: " + platform.getId());

                    savedPlatform = platformRepository.save(platform);
                    log.info("Platform \"" + savedPlatform + "\" saved !");
                    platformResponse.setStatus(HttpStatus.SC_OK);
                    platformResponse.setMessage("OK");
                    platformResponse.setPlatform(RegistryUtils.convertRegistryPlatformToRequestPlatform(savedPlatform));
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
     * @param platform Platform to remove - in JSON format
     * @return PlatformResponse with Http status code and removed Platform object - in JSON format
     */
    public PlatformResponse removePlatform(Platform platform) {
        PlatformResponse platformResponse = new PlatformResponse();

        if (platform == null || platform.getId().isEmpty() || platform.getId() == null) {
            log.error("Given platform is null or has empty PlatformId!");
            platformResponse.setMessage("Given platform is null or has empty PlatformId!");
            platformResponse.setStatus(HttpStatus.SC_BAD_REQUEST);
        } else if (resourceRepository.findByInterworkingServiceURL(platform.getId()) != null
                && !resourceRepository.findByInterworkingServiceURL(platform.getId()).isEmpty()) {
            log.error("Given Platform has registered resources. Take care of resources first.");
            platformResponse.setMessage("Given Platform has registered resources. Take care of resources first.");
            platformResponse.setStatus(HttpStatus.SC_CONFLICT);
        } else {
            try {
                platformRepository.delete(platform.getId());
                log.info("Platform with id: " + platform.getId() + " removed !");

                platformResponse.setStatus(HttpStatus.SC_OK);
                platformResponse.setMessage("OK");
                platformResponse.setPlatform(RegistryUtils.convertRegistryPlatformToRequestPlatform(platform));
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
     * @param platform Platform to remove - in JSON format
     * @return PlatformResponse with Http status code and modified Platform object - in JSON format
     */
    public PlatformResponse modifyPlatform(Platform platform) {
        PlatformResponse platformResponse = new PlatformResponse();
        platformResponse.setPlatform(RegistryUtils.convertRegistryPlatformToRequestPlatform(platform));

        if (platform.getBody().trim().charAt(platform.getBody().length() - 1) != "/".charAt(0)) {
            platform.setBody(platform.getBody().trim() + "/");
        }

        Platform foundPlatform = null;
        if (platform.getId().isEmpty() || platform.getId() == null) {
            log.error("Given platform has empty PlatformId!");
            platformResponse.setMessage("Given platform has empty PlatformId!");
            platformResponse.setStatus(HttpStatus.SC_BAD_REQUEST);
        } else {
            foundPlatform = platformRepository.findOne(platform.getId());
        }

        if (foundPlatform == null) {
            log.error(GIVEN_PLATFORM_DOES_NOT_EXIST_IN_DATABASE);
            platformResponse.setMessage(GIVEN_PLATFORM_DOES_NOT_EXIST_IN_DATABASE);
            platformResponse.setStatus(HttpStatus.SC_BAD_REQUEST);
        } else {
            try {
                //fulfilment of empty Platform fields before saving
                copyExistingPlatformData(platform, foundPlatform);

                platformRepository.save(platform);
                log.info("Platform with id: " + platform.getId() + " modified !");

                platformResponse.setStatus(HttpStatus.SC_OK);
                platformResponse.setMessage("OK");
                platformResponse.setPlatform(RegistryUtils.convertRegistryPlatformToRequestPlatform(platform));
            } catch (Exception e) {
                log.error("Error occurred during Platform modifying in db", e);
                platformResponse.setMessage("Error occurred during Platform modifying in db");
                platformResponse.setStatus(HttpStatus.SC_INTERNAL_SERVER_ERROR);
            }
        }
        return platformResponse;
    }

    private void copyExistingPlatformData(Platform platform, Platform foundPlatform) {
        if (platform.getComments() == null && foundPlatform.getComments() != null)
            platform.setComments(foundPlatform.getComments());
        if (platform.getRdfFormat() == null && foundPlatform.getRdfFormat() != null)
            platform.setRdfFormat(foundPlatform.getRdfFormat());
        if (platform.getLabels() == null && foundPlatform.getLabels() != null)
            platform.setLabels(foundPlatform.getLabels());
        if (platform.getBody() == null && foundPlatform.getBody() != null)
            platform.setBody(foundPlatform.getBody());
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

        if (resource == null || resource.getId().isEmpty() || resource.getId() == null) {
            log.error("Given resource has empty or null ID!");
            resourceRemovalResult.setMessage("Given resource has empty or null ID!");
            resourceRemovalResult.setStatus(HttpStatus.SC_BAD_REQUEST);
        } else {
            resourceRemovalResult.setResource(RegistryUtils.convertResourceToCoreResource(resource));
            try {
                CoreResource foundResource = resourceRepository.findOne(resource.getId());
                if (foundResource != null) {
//                    if (foundResource.getLocation() != null) {
//                        Location loc = foundResource.getLocation();
//                        removeLocation(loc);
//                        log.info("Location with id: " + loc.getId() + " removed !");
//                    }
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

        if (resource.getInterworkingServiceURL().isEmpty() || resource.getInterworkingServiceURL() == null) {
            log.error("Given resource has empty or null Interworking service URL!");
            resourceSavingResult.setMessage("Given resource has empty or null Interworking service URL!");
            resourceSavingResult.setStatus(HttpStatus.SC_BAD_REQUEST);
            return resourceSavingResult;
        }

        normalizeUrl(resource);

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

    private void normalizeUrl(CoreResource resource) {
        if (resource.getInterworkingServiceURL().trim().charAt(resource.getInterworkingServiceURL().length() - 1)
                != "/".charAt(0)) {
            resource.setInterworkingServiceURL(resource.getInterworkingServiceURL().trim() + "/");
        }
    }

    /**
     * todo for future release
     *
     * @param informationModel
     * @return
     */
    public InformationModelResponse saveInformationModel(InformationModel informationModel) {
        InformationModelResponse response = new InformationModelResponse();
        response.setInformationModel(informationModel);

        if (informationModel.getUri().trim().charAt(informationModel.getUri().length() - 1) != "/".charAt(0)) {
            informationModel.setUri(informationModel.getUri().trim() + "/");
        }

        try {
            modelRepository.save(informationModel);
            log.info("Information Model \"" + informationModel + "\" saved !");
            response.setStatus(HttpStatus.SC_OK);
            response.setMessage("OK");
            response.setInformationModel(informationModel);
        } catch (Exception e) {
            log.error("Error occurred during Information Model saving to db", e);
            response.setMessage("Error occurred during Information Model saving to db");
            response.setStatus(HttpStatus.SC_INTERNAL_SERVER_ERROR);
        }

        return response;
    }

    public boolean checkIfPlatformExistsAndHasInterworkingServiceUrl(String platformId, String interworkingServiceUrl) {
        Platform platform = platformRepository.findOne(platformId);
        log.debug("Checking Platform: " + platform);

        if (platform != null) {
            for (InterworkingService service : platform.getInterworkingServices()) {
                if (service.getUrl().equals(interworkingServiceUrl)) {
                    return true;
                } else {
                    log.error("There is a mismatch of interworking service urls. Platform Int. Service url: "
                            + service.getUrl() + ". Interworking Service url " + interworkingServiceUrl);
                }
            }
        } else {
            log.error(GIVEN_PLATFORM_DOES_NOT_EXIST_IN_DATABASE);
        }
        return false;
    }

}
