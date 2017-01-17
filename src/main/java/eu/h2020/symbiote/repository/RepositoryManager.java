package eu.h2020.symbiote.repository;

import eu.h2020.symbiote.messaging.RabbitManager;
import eu.h2020.symbiote.model.Platform;
import eu.h2020.symbiote.model.PlatformCreationResponse;
import eu.h2020.symbiote.model.Resource;
import eu.h2020.symbiote.model.ResourceCreationResponse;
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

    @Autowired
    public RepositoryManager(PlatformRepository platformRepository,
                             RabbitManager rabbitManager,
                             ResourceRepository resourceRepository) {
        this.platformRepository = platformRepository;
        this.rabbitManager = rabbitManager;
        this.resourceRepository = resourceRepository;
    }

    /**
     * Saves platform in MongoDB
     *
     * @param platform Platform with given properties in JSON format
     * @return Platform with added 'Id' (generated in MongoDB), in JSON format
     */
    public PlatformCreationResponse savePlatform(Platform platform) {
        PlatformCreationResponse platformCreationResponse = new PlatformCreationResponse();

        //todo make sure that given platform has empty ID field
        log.debug("Adding Platform");
        if (platform == null) {
            platformCreationResponse.setStatus(HttpStatus.SC_BAD_REQUEST);
        } else {
            try {
                //todo check if provided platform already exists
                Platform savedPlatform = platformRepository.save(platform);
                log.info("Platform with id: " + savedPlatform.getPlatformId() + " saved !");

                platformCreationResponse.setStatus(HttpStatus.SC_OK);
                platformCreationResponse.setPlatform(savedPlatform);

                rabbitManager.sendPlatformCreatedMessage(savedPlatform);

            } catch (Exception e) {
                log.error("Error occured during Platform saving to db", e);
                platformCreationResponse.setStatus(HttpStatus.SC_INTERNAL_SERVER_ERROR);
            }
        }
        return platformCreationResponse;
    }

    /**
     * Saves resource in MongoDB
     *
     * @param resource Resource with given properties in JSON format
     * @return Resource with added 'Id' (generated in MongoDB), in JSON format
     */
    public ResourceCreationResponse saveResource(Resource resource) {
        ResourceCreationResponse resourceCreationResponse = new ResourceCreationResponse();

        //todo make sure that given resource has empty ID field
        log.debug("Adding Platform");
        if (resource == null) {
            resourceCreationResponse.setStatus(HttpStatus.SC_BAD_REQUEST);
        } else {
            try {
                //todo check if provided platform already exists
                Resource savedResource = resourceRepository.save(resource);
                log.info("Resource with id: " + savedResource.getId() + " saved !");

                resourceCreationResponse.setStatus(HttpStatus.SC_OK);
                resourceCreationResponse.setResource(savedResource);

                rabbitManager.sendResourceCreatedMessage(savedResource);

            } catch (Exception e) {
                log.error("Error occured during Platform saving to db", e);
                resourceCreationResponse.setStatus(HttpStatus.SC_INTERNAL_SERVER_ERROR);
            }
        }
        return resourceCreationResponse;
    }
}
