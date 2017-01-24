package eu.h2020.symbiote.repository;

import eu.h2020.symbiote.messaging.RabbitManager;
import eu.h2020.symbiote.model.*;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.http.HttpStatus;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * Created by mateuszl on 09.01.2017.
 */
@Component
public class RepositoryManager {

    public static Log log = LogFactory.getLog(RepositoryManager.class);

    private RabbitManager rabbitManager;
    private PlatformRepository platformRepository;
    private ResourceRepository resourceRepository;
    private LocationRepository locationRepository;

    @Autowired
    public RepositoryManager(PlatformRepository platformRepository,
                             RabbitManager rabbitManager,
                             ResourceRepository resourceRepository,
                             LocationRepository locationRepository) {
        this.platformRepository = platformRepository;
        this.rabbitManager = rabbitManager;
        this.resourceRepository = resourceRepository;
        this.locationRepository = locationRepository;
    }

    /**
     * Saves platform in MongoDB
     *
     * @param platform Platform with given properties in JSON format
     * @return Platform with added 'Id' (generated in MongoDB), in JSON format
     */
    public PlatformResponse savePlatform(Platform platform) {
        PlatformResponse platformResponse = new PlatformResponse();

        log.debug("Adding Platform");
        if (platform == null || platform.getPlatformId() != null) {
            platformResponse.setStatus(HttpStatus.SC_BAD_REQUEST);
        } else {
            try {
                //todo check if provided platform already exists
                Platform savedPlatform = platformRepository.save(platform);
                log.info("Platform with id: " + savedPlatform.getPlatformId() + " saved !");

                platformResponse.setStatus(HttpStatus.SC_OK);
                platformResponse.setPlatform(savedPlatform);

                rabbitManager.sendPlatformCreatedMessage(savedPlatform);

            } catch (Exception e) {
                log.error("Error occured during Platform saving to db", e);
                platformResponse.setStatus(HttpStatus.SC_INTERNAL_SERVER_ERROR);
            }
        }
        return platformResponse;
    }

    public PlatformResponse removePlatform(Platform platform) {
        PlatformResponse platformResponse = new PlatformResponse();

        if (platform == null || platform.getPlatformId().isEmpty() || platform.getPlatformId() == null) {
            platformResponse.setStatus(HttpStatus.SC_BAD_REQUEST);
        } else {
            try {
                //todo do something with resources corresponding to removed platform
                platformRepository.delete(platform.getPlatformId());

                platformResponse.setStatus(HttpStatus.SC_OK);
                platformResponse.setPlatform(platform);

                rabbitManager.sendPlatformRemovedMessage(platform);
            } catch (Exception e) {
                e.printStackTrace();
                platformResponse.setStatus(HttpStatus.SC_INTERNAL_SERVER_ERROR);
            }
        }
        return platformResponse;
    }

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

                platformResponse.setStatus(HttpStatus.SC_OK);
                platformResponse.setPlatform(platform);

                rabbitManager.sendPlatformModifiedMessage(platform);
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
                //todo check if provided resource already exists
                Resource savedResource = resourceRepository.save(resource);
                log.info("Resource with id: " + savedResource.getId() + " saved !");

                resourceResponse.setStatus(HttpStatus.SC_OK);
                resourceResponse.setResource(savedResource);

                rabbitManager.sendResourceCreatedMessage(savedResource);

            } catch (Exception e) {
                log.error("Error occured during Platform saving to db", e);
                resourceResponse.setStatus(HttpStatus.SC_INTERNAL_SERVER_ERROR);
            }
        }
        return resourceResponse;
    }

    public ResourceResponse removeResource(Resource resource) {
        ResourceResponse resourceResponse = new ResourceResponse();

        if (resource == null || resource.getId().isEmpty() || resource.getId() == null) {
            resourceResponse.setStatus(HttpStatus.SC_BAD_REQUEST);
        } else {
            try {
                if (resource.getLocation() != null) {
                    removeLocation(resource.getLocation());
                }
                resourceRepository.delete(resource.getId());
                resourceResponse.setStatus(HttpStatus.SC_OK);
                resourceResponse.setResource(resource);

                rabbitManager.sendResourceRemovedMessage(resource);
            } catch (Exception e) {
                e.printStackTrace();
                resourceResponse.setStatus(HttpStatus.SC_INTERNAL_SERVER_ERROR);
            }
        }
        return resourceResponse;
    }

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

                resourceResponse.setStatus(HttpStatus.SC_OK);
                resourceResponse.setResource(resource);

                rabbitManager.sendResourceModifiedMessage(resource);
            } catch (Exception e) {
                e.printStackTrace();
                resourceResponse.setStatus(HttpStatus.SC_INTERNAL_SERVER_ERROR);
            }
        }


        return resourceResponse;
    }


    private void removeLocation(Location location) {
        try {
            locationRepository.delete(location.getId());
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public ResourceResponse updateResource(Resource resource) {
        ResourceResponse resourceResponse = new ResourceResponse();
        //todo something with Location objects

        //todo implement
        return resourceResponse;
    }

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
