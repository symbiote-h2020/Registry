package eu.h2020.symbiote.repository;

import eu.h2020.symbiote.model.*;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.http.HttpStatus;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * Class managing persistence actions for Platforms, Resources and Locations using MongoDB repositories.
 *
 * Created by mateuszl
 */
@Component
public class RepositoryManager {

    private static Log log = LogFactory.getLog(RepositoryManager.class);
    private PlatformRepository platformRepository;
    private ResourceRepository resourceRepository;
    private LocationRepository locationRepository;

    @Autowired
    public RepositoryManager(PlatformRepository platformRepository,
                             ResourceRepository resourceRepository,
                             LocationRepository locationRepository) {
        this.platformRepository = platformRepository;
        this.resourceRepository = resourceRepository;
        this.locationRepository = locationRepository;
    }

    /**
     * Saves given Platform in MongoDB. It triggers save action in Platform Repository and if it ends successfully
     * it returns http status '200' and Platform object with generated ID field.
     * If given platform URL noe ends with "/", method appends it.
     * If given platform is null or it already has an id the method will return 'bad request' status.
     * If saving in DB goes wrong it returns 'internal server error' status.
     * Url of given platform is appended with "/" if it does not end with it.
     *
     * @param platform Platform to save - in JSON format
     * @return PlatformResponse with status and Platform object with unique "id" (generated in MongoDB)
     */
    public PlatformResponse savePlatform(Platform platform) {
        PlatformResponse platformResponse = new PlatformResponse();

        if (platform.getUrl().trim().charAt(platform.getUrl().length() - 1) != "/".charAt(0)) {
            platform.setUrl(platform.getUrl().trim() + "/");
        }

        if (platform.getPlatformId() == null) {
            log.error("Given platform has null PlatformId!");
            platformResponse.setStatus(HttpStatus.SC_BAD_REQUEST);
        } else {
            try {
                log.info("Saving platform: " + platform.getName());
                //todo check if provided platform already exists - somehow

                if (platform.getUrl().trim().charAt(platform.getUrl().length() - 1) != "/".charAt(0)) {
                    platform.setUrl(platform.getUrl().trim() + "/");
                }

                Platform savedPlatform = platformRepository.save(platform);
                log.info("Platform with id: " + savedPlatform.getPlatformId() + " saved !");

                platformResponse.setStatus(HttpStatus.SC_OK);
                platformResponse.setPlatform(savedPlatform);
            } catch (Exception e) {
                log.error("Error occurred during Platform saving to db", e);
                platformResponse.setStatus(HttpStatus.SC_INTERNAL_SERVER_ERROR);
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
     * @return PlatformResponse with status and removed Platform object
     */
    public PlatformResponse removePlatform(Platform platform) {
        PlatformResponse platformResponse = new PlatformResponse();

        if (platform == null || platform.getPlatformId().isEmpty() || platform.getPlatformId() == null) {
            log.error("Given platform is null or has empty PlatformId!");
            platformResponse.setStatus(HttpStatus.SC_BAD_REQUEST);
        } else if (resourceRepository.findByPlatformId(platform.getPlatformId()) != null
                && resourceRepository.findByPlatformId(platform.getPlatformId()).size() > 0) {
            log.error("Given Platform has registered resources. Take care of resources first.");
            platformResponse.setStatus(HttpStatus.SC_CONFLICT);
        } else {
            try {
                //todo do something with resources corresponding to removed platform
                platformRepository.delete(platform.getPlatformId());
                log.info("Platform with id: " + platform.getPlatformId() + " removed !");

                platformResponse.setStatus(HttpStatus.SC_OK);
                platformResponse.setPlatform(platform);
            } catch (Exception e) {
                log.error("Error occurred during Platform removing from db", e);
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
     *
     * If given platform is null or it has no id or has an empty 'id' field the method will return 'bad request' status.
     * If there is no Platform in database with ID same as given one, it returns 'bad request' status.
     * If saving in DB goes wrong it returns 'internal server error' status.
     *
     * @param platform Platform to remove - in JSON format
     * @return PlatformResponse with status and removed Platform object
     */
    public PlatformResponse modifyPlatform(Platform platform) {
        PlatformResponse platformResponse = new PlatformResponse();

        if (platform.getUrl().trim().charAt(platform.getUrl().length() - 1) != "/".charAt(0)) {
            platform.setUrl(platform.getUrl().trim() + "/");
        }

        Platform foundPlatform = null;
        if (platform.getPlatformId().isEmpty() || platform.getPlatformId() == null) {
            log.error("Given platform has empty PlatformId!");
            platformResponse.setStatus(HttpStatus.SC_BAD_REQUEST);
        } else {
            foundPlatform = platformRepository.findOne(platform.getPlatformId());
        }

        if (foundPlatform == null) {
            log.error("Given platform does not exist in database!");
            platformResponse.setStatus(HttpStatus.SC_BAD_REQUEST);
        } else {
            try {
                //fulfilment of empty Platform fields before saving
                if (platform.getDescription() == null && foundPlatform.getDescription() != null)
                    platform.setDescription(foundPlatform.getDescription());
                if (platform.getInformationModelId() == null && foundPlatform.getInformationModelId() != null)
                    platform.setInformationModelId(foundPlatform.getInformationModelId());
                if (platform.getName() == null && foundPlatform.getName() != null)
                    platform.setName(foundPlatform.getName());
                if (platform.getUrl() == null && foundPlatform.getUrl() != null)
                    platform.setUrl(foundPlatform.getUrl());

                platformRepository.save(platform);
                log.info("Platform with id: " + platform.getPlatformId() + " modified !");

                platformResponse.setStatus(HttpStatus.SC_OK);
                platformResponse.setPlatform(platform);
            } catch (Exception e) {
                e.printStackTrace();
                log.error("Error occurred during Platform modifying in db", e);
                platformResponse.setStatus(HttpStatus.SC_INTERNAL_SERVER_ERROR);
            }
        }
        return platformResponse;
    }

    /**
     * Saves resource in MongoDB. Checks if URL in given resource ends with "/" and if not, appends it.
     *
     * @param resource Resource with given properties in JSON format
     * @return Resource with added 'Id' (generated in MongoDB), in JSON format
     */
    public ResourceResponse saveResource(Resource resource) {
        ResourceResponse resourceResponse = new ResourceResponse();

        if (resource.getResourceURL().trim().charAt(resource.getResourceURL().length() - 1) != "/".charAt(0)) {
            resource.setResourceURL(resource.getResourceURL().trim() + "/");
        }

        if (platformRepository.findOne(resource.getPlatformId()) == null) {
            log.error("Given PlatformId does not exist in database");
            resourceResponse.setStatus(HttpStatus.SC_BAD_REQUEST);
        } else if (resource.getId() != null ||
                !platformRepository.findOne(resource.getPlatformId()).getUrl().equals(resource.getResourceURL())) {
            log.error("Platform with given resourceURL does not exist in database");
            resourceResponse.setStatus(HttpStatus.SC_BAD_REQUEST);
        } else {
            try {
                log.info("Saving Resource: " + resource.getName());
                //todo check if provided resource already exists - somehow (URL?)

                Resource savedResource = resourceRepository.save(resource);
                log.info("Resource with id: " + savedResource.getId() + " saved !");

                resourceResponse.setStatus(HttpStatus.SC_OK);
                resourceResponse.setResource(savedResource);

            } catch (Exception e) {
                log.error("Error occurred during Resource saving in db", e);
                resourceResponse.setStatus(HttpStatus.SC_INTERNAL_SERVER_ERROR);
            }
        }
        return resourceResponse;
    }

    /**
     * Deletes resource from MongoDB
     *
     * @param resource Resource with given properties in JSON format
     * @return Deleted Resource, in JSON format
     */
    public ResourceResponse removeResource(Resource resource) {
        ResourceResponse resourceResponse = new ResourceResponse();

        if (resource == null || resource.getId().isEmpty() || resource.getId() == null) {
            log.error("Given resource has empty or null ID!");
            resourceResponse.setStatus(HttpStatus.SC_BAD_REQUEST);
        } else {
            try {
                Resource foundResource = resourceRepository.findOne(resource.getId());
                if (foundResource != null) {
                    if (foundResource.getLocation() != null) {
                        Location loc = foundResource.getLocation();
                        removeLocation(loc);
                        log.info("Location with id: " + loc.getId() + " removed !");
                    }
                    resourceRepository.delete(resource.getId());
                    resourceResponse.setStatus(HttpStatus.SC_OK);
                    resourceResponse.setResource(resource);
                    log.info("Resource with id: " + resource.getId() + " removed !");
                } else {
                    log.error("Given resource does not exist in database");
                    resourceResponse.setStatus(HttpStatus.SC_BAD_REQUEST);
                }
            } catch (Exception e) {
                log.error("Error occurred during Resource deleting from db", e);
                resourceResponse.setStatus(HttpStatus.SC_INTERNAL_SERVER_ERROR);
            }
        }
        return resourceResponse;
    }

    /**
     * Modifies given resource in MongoDB
     *
     * @param resource Resource with given properties in JSON format
     * @return Modified Resource, in JSON format
     */
    public ResourceResponse modifyResource(Resource resource) {
        ResourceResponse resourceResponse = new ResourceResponse();

        if (resource.getResourceURL().trim().charAt(resource.getResourceURL().length() - 1) != "/".charAt(0)) {
            resource.setResourceURL(resource.getResourceURL().trim() + "/");
        }

        Resource foundResource = null;

        if (resource.getPlatformId().isEmpty() || resource.getPlatformId() == null) {
            log.error("Given resource has empty or null PlatformID!");
            resourceResponse.setStatus(HttpStatus.SC_BAD_REQUEST);
        } else {
            foundResource = resourceRepository.findOne(resource.getId());
        }

        if (foundResource == null) {
            log.error("Given resource does not exist in database!");
            resourceResponse.setStatus(HttpStatus.SC_BAD_REQUEST);
        } else {
            try {
                //fulfilment of empty Resource fields before saving
                if (resource.getDescription() == null && foundResource.getDescription() != null)
                    resource.setDescription(foundResource.getDescription());
                if (resource.getFeatureOfInterest() == null && foundResource.getFeatureOfInterest() != null)
                    resource.setFeatureOfInterest(foundResource.getFeatureOfInterest());
                if (resource.getName() == null && foundResource.getName() != null)
                    resource.setName(foundResource.getName());
                if (resource.getOwner() == null && foundResource.getOwner() != null)
                    resource.setOwner(foundResource.getOwner());
                if (resource.getResourceURL() == null && foundResource.getResourceURL() != null)
                    resource.setResourceURL(foundResource.getResourceURL());
                if (resource.getId() == null && foundResource.getId() != null)
                    resource.setId(foundResource.getId());
                if (resource.getObservedProperties() == null && foundResource.getObservedProperties() != null)
                    resource.setObservedProperties(foundResource.getObservedProperties());
                if (resource.getLocation() == null && foundResource.getLocation() != null)
                    resource.setLocation(foundResource.getLocation());

                resourceRepository.save(resource);
                log.info("Resource with id: " + resource.getId() + " modified !");

                resourceResponse.setStatus(HttpStatus.SC_OK);
                resourceResponse.setResource(resource);
            } catch (Exception e) {
                log.error("Error occurred during Resource modifying in db", e);
                resourceResponse.setStatus(HttpStatus.SC_INTERNAL_SERVER_ERROR);
            }
        }
        return resourceResponse;
    }

    /**
     * Removes from mongoDB given Location.
     *
     * @param location Location object to delete.
     */
    private void removeLocation(Location location) {
        try {
            locationRepository.delete(location.getId());
        } catch (Exception e) {
            log.error("Error occurred during Location deleting from db", e);
        }
    }

    /**
     * Saves in MongoDB given Location.
     *
     * @param location Location object to save.
     * @return saved Location with ID field fulfilled.
     */
    public Location saveLocation(Location location) {
        Location savedLocation = null;
        log.info("Adding Location to db");
        if (location == null) {
            return null;
        } else {
            try {
                savedLocation = locationRepository.save(location);
                log.info("Location with id: " + savedLocation.getId() + " saved !");
            } catch (Exception e) {
                log.error("Error occurred during Location saving in db", e);
            }
        }
        return savedLocation;
    }
}
