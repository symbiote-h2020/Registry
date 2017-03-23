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

    //// TODO: 17.03.2017 methods major update !!

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
        platformResponse.setPlatform(platform);
//
//        if (platform.getBody().trim().charAt(platform.getBody().length() - 1) != "/".charAt(0)) {
//            platform.setBody(platform.getBody().trim() + "/");
//        }

        if (platform.getId() != null) {
            log.error("Given platform has not null id!");
            platformResponse.setMessage("Given platform has not null id!");
            platformResponse.setStatus(HttpStatus.SC_BAD_REQUEST);
        } else {
            try {
                log.info("Saving platform: " + platform.getId());
                //todo check if provided platform already exists - somehow

                //todo Interworking service !!

                savedPlatform = platformRepository.save(platform);

            } catch (Exception e) {
                log.error("Error occurred during Platform saving to db", e);
                platformResponse.setMessage("Error occurred during Platform saving to db");
                platformResponse.setStatus(HttpStatus.SC_INTERNAL_SERVER_ERROR);
            }
                log.info("Platform \"" + savedPlatform + "\" saved !");
                platformResponse.setStatus(HttpStatus.SC_OK);
                platformResponse.setMessage("OK");
                platformResponse.setPlatform(savedPlatform);
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
        } else if (resourceRepository.findByPlatformId(platform.getId()) != null
                && resourceRepository.findByPlatformId(platform.getId()).size() > 0) {
            log.error("Given Platform has registered resources. Take care of resources first.");
            platformResponse.setMessage("Given Platform has registered resources. Take care of resources first.");
            platformResponse.setStatus(HttpStatus.SC_CONFLICT);
        } else {
            try {
                platformRepository.delete(platform.getId());
                log.info("Platform with id: " + platform.getId() + " removed !");

                platformResponse.setStatus(HttpStatus.SC_OK);
                platformResponse.setMessage("OK");
                platformResponse.setPlatform(platform);
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
        platformResponse.setPlatform(platform);

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
            log.error("Given platform does not exist in database!");
            platformResponse.setMessage("Given platform does not exist in database!");
            platformResponse.setStatus(HttpStatus.SC_BAD_REQUEST);
        } else {
            try {
                //fulfilment of empty Platform fields before saving
                if (platform.getComments() == null && foundPlatform.getComments() != null)
                    platform.setComments(foundPlatform.getComments());
                if (platform.getFormat() == null && foundPlatform.getFormat() != null)
                    platform.setFormat(foundPlatform.getFormat());
                if (platform.getLabels() == null && foundPlatform.getLabels() != null)
                    platform.setLabels(foundPlatform.getLabels());
                if (platform.getBody() == null && foundPlatform.getBody() != null)
                    platform.setBody(foundPlatform.getBody());

                platformRepository.save(platform);
                log.info("Platform with id: " + platform.getId() + " modified !");

                platformResponse.setStatus(HttpStatus.SC_OK);
                platformResponse.setMessage("OK");
                platformResponse.setPlatform(platform);
            } catch (Exception e) {
                e.printStackTrace();
                log.error("Error occurred during Platform modifying in db", e);
                platformResponse.setMessage("Error occurred during Platform modifying in db");
                platformResponse.setStatus(HttpStatus.SC_INTERNAL_SERVER_ERROR);
            }
        }
        return platformResponse;
    }

    /**
     * Saves resource in MongoDB. Checks if URL in given resource ends with "/" and if not, appends it.
     * If in database there is no Platform with given PlatformId field, the method will return 'bad request' status.
     * If given resource has not null Id field, the method will return 'bad request' status.
     * If in database there is no Platform with given URL same as in given Resource, the method will return 'bad request'.
     * If saving in DB goes wrong it returns 'internal server error' status.
     *
     * @param resource Resource with given properties in JSON format
     * @return ResourceResponse containing Http status code and Resource with added 'Id' (generated in MongoDB),
     * in JSON format
     */
    public ResourceResponse saveResource(Resource resource) {
        ResourceResponse resourceResponse = new ResourceResponse();

        if (resource.getBody().trim().charAt(resource.getBody().length() - 1) != "/".charAt(0)) {
            resource.setBody(resource.getBody().trim() + "/");
        }

        if (platformRepository.findOne(resource.getFormat()) == null) {
            log.error("Given PlatformId does not exist in database");
            resourceResponse.setMessage("Given PlatformId does not exist in database");
            resourceResponse.setStatus(HttpStatus.SC_BAD_REQUEST);
        } else if (resource.getId() != null){
            log.error("Resource has not null ID!");
            resourceResponse.setMessage("Resource has not null ID!");
            resourceResponse.setStatus(HttpStatus.SC_BAD_REQUEST);
        } else if (!platformRepository.findOne(resource.getFormat()).getBody().equals(resource.getBody())) {
            log.error("Platform with given resourceURL does not exist in database");
            resourceResponse.setMessage("Platform with given resourceURL does not exist in database");
            resourceResponse.setStatus(HttpStatus.SC_BAD_REQUEST);
        } else {
            try {
                log.info("Saving Resource: " + resource.getLabels());
                //todo check if provided resource already exists - somehow (URL?)

                Resource savedResource = resourceRepository.save(resource);
                log.info("Resource with id: " + savedResource.getId() + " saved !");

                resourceResponse.setStatus(HttpStatus.SC_OK);
                resourceResponse.setMessage("OK");
                resourceResponse.setResource(savedResource);
            } catch (Exception e) {
                log.error("Error occurred during Resource saving in db", e);
                resourceResponse.setMessage("Error occurred during Resource saving in db");
                resourceResponse.setStatus(HttpStatus.SC_INTERNAL_SERVER_ERROR);
            }
        }
        return resourceResponse;
    }

    /**
     * Deletes resource from MongoDB
     *
     * @param resource Resource with given properties in JSON format
     * @return ResourceResponse containing Http status code and Deleted Resource, in JSON format
     */
    public ResourceResponse removeResource(Resource resource) {
        ResourceResponse resourceResponse = new ResourceResponse();

        if (resource == null || resource.getId().isEmpty() || resource.getId() == null) {
            log.error("Given resource has empty or null ID!");
            resourceResponse.setMessage("Given resource has empty or null ID!");
            resourceResponse.setStatus(HttpStatus.SC_BAD_REQUEST);
        } else {
            try {
                Resource foundResource = resourceRepository.findOne(resource.getId());
                if (foundResource != null) {
//                    if (foundResource.getLocation() != null) {
//                        Location loc = foundResource.getLocation();
//                        removeLocation(loc);
//                        log.info("Location with id: " + loc.getId() + " removed !");
//                    }
                    resourceRepository.delete(resource.getId());
                    resourceResponse.setStatus(HttpStatus.SC_OK);
                    resourceResponse.setMessage("OK");
                    resourceResponse.setResource(resource);
                    log.info("Resource with id: " + resource.getId() + " removed !");
                } else {
                    log.error("Given resource does not exist in database");
                    resourceResponse.setMessage("Given resource does not exist in database");
                    resourceResponse.setStatus(HttpStatus.SC_BAD_REQUEST);
                }
            } catch (Exception e) {
                log.error("Error occurred during Resource deleting from db", e);
                resourceResponse.setMessage("Error occurred during Resource deleting from db");
                resourceResponse.setStatus(HttpStatus.SC_INTERNAL_SERVER_ERROR);
            }
        }
        return resourceResponse;
    }

    /**
     * Modifies given resource in MongoDB. If given resource does not consist some of the fields, empty ones are
     * fulfilled with data from the database.
     *
     * @param resource Resource with given properties in JSON format
     * @return ResourceResponse containing Http status code and Modified Resource, in JSON format
     */
    public ResourceResponse modifyResource(Resource resource) {
        ResourceResponse resourceResponse = new ResourceResponse();

        if (resource.getBody().trim().charAt(resource.getBody().length() - 1) != "/".charAt(0)) {
            resource.setBody(resource.getBody().trim() + "/");
        }

        Resource foundResource = null;

        if (resource.getFormat().isEmpty() || resource.getFormat() == null) {
            log.error("Given resource has empty or null PlatformID!");
            resourceResponse.setMessage("Given resource has empty or null PlatformID!");
            resourceResponse.setStatus(HttpStatus.SC_BAD_REQUEST);
        } else {
            foundResource = resourceRepository.findOne(resource.getId());
        }

        if (foundResource == null) {
            log.error("Given resource does not exist in database!");
            resourceResponse.setMessage("Given resource does not exist in database!");
            resourceResponse.setStatus(HttpStatus.SC_BAD_REQUEST);
        } else {
            try {
                //fulfilment of empty Resource fields before saving
                if (resource.getComments() == null && foundResource.getComments() != null)
                    resource.setComments(foundResource.getComments());
//                if (resource.getFeatureOfInterest() == null && foundResource.getFeatureOfInterest() != null)
//                    resource.setFeatureOfInterest(foundResource.getFeatureOfInterest());
//                if (resource.getLabels() == null && foundResource.getLabels() != null)
//                    resource.setLabels(foundResource.getLabels());
//                if (resource.getOwner() == null && foundResource.getOwner() != null)
//                    resource.setOwner(foundResource.getOwner());
//                if (resource.getBody() == null && foundResource.getBody() != null)
//                    resource.setBody(foundResource.getBody());
//                if (resource.getId() == null && foundResource.getId() != null)
//                    resource.setId(foundResource.getId());
//                if (resource.getObservedProperties() == null && foundResource.getObservedProperties() != null)
//                    resource.setObservedProperties(foundResource.getObservedProperties());
//                if (resource.getLocation() == null && foundResource.getLocation() != null)
//                    resource.setLocation(foundResource.getLocation());

                resourceRepository.save(resource);
                log.info("Resource with id: " + resource.getId() + " modified !");

                resourceResponse.setStatus(HttpStatus.SC_OK);
                resourceResponse.setMessage("OK");
                resourceResponse.setResource(resource);
            } catch (Exception e) {
                log.error("Error occurred during Resource modifying in db", e);
                resourceResponse.setMessage("Error occurred during Resource modifying in db");
                resourceResponse.setStatus(HttpStatus.SC_INTERNAL_SERVER_ERROR);
            }
        }
        return resourceResponse;
    }

//    /**
//     * Removes from mongoDB given Location.
//     *
//     * @param location Location object to delete.
//     */
//    private void removeLocation(Location location) {
//        try {
//            locationRepository.delete(location.getId());
//        } catch (Exception e) {
//            log.error("Error occurred during Location deleting from db", e);
//        }
//    }

//    /**
//     * Saves in MongoDB given Location.
//     *
//     * @param location Location object to save.
//     * @return saved Location with ID field fulfilled.
//     */
//    public Location saveLocation(Location location) {
//        Location savedLocation = null;
//        log.info("Adding Location to db");
//        if (location == null) {
//            return null;
//        } else {
//            try {
//                savedLocation = locationRepository.save(location);
//                log.info("Location with id: " + savedLocation.getId() + " saved !");
//            } catch (Exception e) {
//                log.error("Error occurred during Location saving in db", e);
//            }
//        }
//        return savedLocation;
//    }
}
