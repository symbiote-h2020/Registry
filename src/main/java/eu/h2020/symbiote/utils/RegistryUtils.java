package eu.h2020.symbiote.utils;

import eu.h2020.symbiote.model.Platform;
import eu.h2020.symbiote.model.Resource;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * Utils for manipulating POJOs in Registry project.
 *
 * Created by mateuszl on 14.02.2017.
 */
public class RegistryUtils {


    private static Log log = LogFactory.getLog(RegistryUtils.class);

    /**
     * Checks if given platform has all of the needed fields and that neither is empty.
     *
     * @param platform platform to check
     * @return true if it has all the fields and neither is empty
     */
    public static boolean validate(Platform platform) {
        boolean b;
        if (platform.getBody() == null || platform.getLabels() == null || platform.getFormat() == null) {
            log.info("Given platform has some null fields");
            b = false;
        } else if (platform.getBody().isEmpty() || platform.getLabels().isEmpty()
                || platform.getFormat().isEmpty()) {
            log.info("Given platform has some empty fields");
            b = false;
        } else {
            b = true;
        }
        return b;
    }

    /**
     * Checks if given resource has all of the needed fields and that neither is empty.
     *
     * @param resource resource to check
     * @return true if it has all the fields and neither is empty.
     */
    public static boolean validate(Resource resource) { //todo extend to all fields
        boolean b;
        if (resource.getBody() == null|| resource.getFormat() == null || resource.getLabels() == null) {
            log.info("Given resource has some null fields");
            b = false;
        } else if (resource.getBody().isEmpty() || resource.getFormat().isEmpty() || resource.getLabels().isEmpty()) {
            log.info("Given resource has some empty fields");
            b = false;
        } else {
            b = true;
        }
        return b;
    }

    //todo cooperation with SemanticManager

    public static Resource getRdfBodyFromObject(Resource resource){
        return resource;
    }

    public static Resource getObjectFromRdf(Resource resource){
        return resource;
    }

    public static Platform getRdfBodyFromObject(Platform platform){
        return platform;
    }

    public static Platform getObjectFromRdf(Platform platform){
        return platform;
    }

}
