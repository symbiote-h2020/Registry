package eu.h2020.symbiote.repository;

import eu.h2020.symbiote.messaging.RabbitManager;
import eu.h2020.symbiote.model.Platform;
import eu.h2020.symbiote.model.PlatformCreationResponse;
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
    @Autowired
    RabbitManager rabbitManager;

    private PlatformRepository platformRepository;

    @Autowired
    public RepositoryManager(PlatformRepository platformRepository) {
        this.platformRepository = platformRepository;
    }

    /**
     * Saves platform in MongoDB
     *
     * @param platform Platform with given properties in JSON format
     * @return One field 'Id' of added platform in JSON format (generated ine MongoDB)
     */
    public PlatformCreationResponse savePlatform(Platform platform) {
        PlatformCreationResponse platformCreationResponse = new PlatformCreationResponse();

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

}
