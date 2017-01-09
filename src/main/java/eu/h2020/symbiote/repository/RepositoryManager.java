package eu.h2020.symbiote.repository;

import com.google.gson.Gson;
import eu.h2020.symbiote.model.Platform;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * Created by mateuszl on 09.01.2017.
 */
@Component
public class RepositoryManager {

    public static Log log = LogFactory.getLog(RepositoryManager.class);

    private PlatformRepository platformRepository;

    @Autowired
    public RepositoryManager(PlatformRepository platformRepository) {
        this.platformRepository = platformRepository;
    }

    /**Saves platform in MongoDB
     * @param platformJson Platform with given properties in JSON format
     * @return One field 'Id' of added platform in JSON format (generated ine MongoDB)
     */
    public String savePlatform(String platformJson) {
        String response = "";
        log.debug("Adding Platform");
        if (platformJson.isEmpty()) {
            response = "Saving platform failure";
        } else {
            try {
                Gson gson = new Gson();
                Platform platform = gson.fromJson(platformJson, Platform.class);

                String savedPlatformId = "";

                if (platform != null) {
                    //todo check if provided platform already exists
                    Platform savedPlatform = platformRepository.save(platform);
                    savedPlatformId = savedPlatform.getId();
                    log.info("Platform with id: " + savedPlatformId + " saved !");

//                    log.info("Platform added! : " + savedPlatform + ". Sending message...");
//                    //Sending message
//                    publisher.sendPlatformCreatedMessage(savedPlatform);
//                    log.info("Response send with id: " + savedPlatformId);

                    response = "{\"id\": \"" + savedPlatformId + "\"}"; //id of platfrom in JSON
                    log.info("Platform with id: " + savedPlatformId + " saved !");
                }
            } catch (Exception e) {
                log.error("Error occured during Platform saving to db", e);
            }
        }
        return response;
    }
}
