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

        //todo make sure that given platform has empty ID field
        log.debug("Adding Platform");
        if (platform == null) {
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

    /**
     * Saves resource in MongoDB
     *
     * @param resource Resource with given properties in JSON format
     * @return Resource with added 'Id' (generated in MongoDB), in JSON format
     */
    public ResourceResponse saveResource(Resource resource) {
        ResourceResponse resourceResponse = new ResourceResponse();

        //todo make sure that given resource has empty ID field
        log.debug("Adding Platform");
        if (resource == null) {
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
                platformRepository.delete(resource.getId());

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
