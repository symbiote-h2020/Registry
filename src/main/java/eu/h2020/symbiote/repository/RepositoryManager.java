package eu.h2020.symbiote.repository;

import eu.h2020.symbiote.model.*;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.http.HttpStatus;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * Class managing persistence actions for Platforms, Resources and Locations using MongoDB repositories.
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
     * If given platform is null or it already has an id the method will return 'bad request' status.
     * If saving in DB goes wrong it returns 'internal server error' status.
     *
     * @param platform Platform to save - in JSON format
     * @return PlatformResponse with status and Platform object with unique "id" (generated in MongoDB)
     */
    public PlatformResponse savePlatform(Platform platform) {
        PlatformResponse platformResponse = new PlatformResponse();
        log.debug("Adding Platform");
        if (platform == null || platform.getPlatformId() != null) {
            platformResponse.setStatus(HttpStatus.SC_BAD_REQUEST);
        } else {
            try {
                //todo check if provided platform already exists - somehow
                Platform savedPlatform = platformRepository.save(platform);
                log.info("Platform with id: " + savedPlatform.getPlatformId() + " saved !");

                platformResponse.setStatus(HttpStatus.SC_OK);
                platformResponse.setPlatform(savedPlatform);
            } catch (Exception e) {
                log.error("Error occured during Platform saving to db", e);
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
            platformResponse.setStatus(HttpStatus.SC_BAD_REQUEST);
        } else {
            try {
                //todo do something with resources corresponding to removed platform
                platformRepository.delete(platform.getPlatformId());
                log.info("Platform with id: " + platform.getPlatformId() + " removed !");

                platformResponse.setStatus(HttpStatus.SC_OK);
                platformResponse.setPlatform(platform);
            } catch (Exception e) {
                e.printStackTrace();
                platformResponse.setStatus(HttpStatus.SC_INTERNAL_SERVER_ERROR);
            }
        }
        return platformResponse;
    }

    /**
     * Modifies (existing in mongodb) Platform accordingly to fields in given Platform.
     * It triggers delete and save actions in Platform Repository and if it ends successfully
     * it returns http status '200' and new modified Platform object.
     * //todo from here
     * If given platform is null or it has no id or has an empty 'id' field the method will return 'bad request' status.
     * If there is no Platform in database with ID same as given one, it returns 'bad request' status.
     * If saving in DB goes wrong it returns 'internal server error' status.
     *
     * @param platform Platform to remove - in JSON format
     * @return PlatformResponse with status and removed Platform object
     */
    public PlatformResponse modifyPlatform(Platform platform) {
        PlatformResponse platformResponse = new PlatformResponse();
        Platform foundPlatform = null;

        if (platform.getPlatformId().isEmpty() || platform.getPlatformId() == null) {
            platformResponse.setStatus(HttpStatus.SC_BAD_REQUEST);
        } else {
            foundPlatform = platformRepository.findOne(platform.getPlatformId());
        }

        if (foundPlatform == null) {
            platformResponse.setStatus(HttpStatus.SC_BAD_REQUEST);
        } else {
            try {
                //todo do something with resources corresponding to removed platform

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
                platformResponse.setStatus(HttpStatus.SC_INTERNAL_SERVER_ERROR);
            }
        }
        return platformResponse;
    }

    /**
     * Saves resource in MongoDB
     *
     * @param resource Resource with given properties in JSON format
     * @return Resource with added 'Id' (generated in MongoDB), in JSON format
     */
    public ResourceResponse saveResource(Resource resource) {
        ResourceResponse resourceResponse = new ResourceResponse();

        log.debug("Adding Platform");
        if (resource == null || resource.getId() != null) {
            resourceResponse.setStatus(HttpStatus.SC_BAD_REQUEST);
        } else {
            try {
                //todo check Platform ID in given Resource
                //todo check if provided resource already exists - somehow (URL?)
                Resource savedResource = resourceRepository.save(resource);
                log.info("Resource with id: " + savedResource.getId() + " saved !");

                resourceResponse.setStatus(HttpStatus.SC_OK);
                resourceResponse.setResource(savedResource);

            } catch (Exception e) {
                log.error("Error occured during Platform saving to db", e);
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
                    resourceResponse.setStatus(HttpStatus.SC_BAD_REQUEST);
                }
            } catch (Exception e) {
                e.printStackTrace();
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

        Resource foundResource = null;

        if (resource.getPlatformId().isEmpty() || resource.getPlatformId() == null) {
            resourceResponse.setStatus(HttpStatus.SC_BAD_REQUEST);
        } else {
            foundResource = resourceRepository.findOne(resource.getPlatformId());
        }

        if (foundResource == null) {
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
                e.printStackTrace();
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
            e.printStackTrace();
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
        log.debug("Adding Location");
        if (location == null) {
            return null;
        } else {
            try {
                savedLocation = locationRepository.save(location);
                log.info("Location with id: " + savedLocation.getId() + " saved !");
            } catch (Exception e) {
                log.error(e);
            }
        }
        return savedLocation;
    }
}
