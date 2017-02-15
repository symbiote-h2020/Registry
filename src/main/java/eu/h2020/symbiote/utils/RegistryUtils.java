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
        if (platform.getUrl() == null || platform.getName() == null || platform.getInformationModelId() == null) {
            log.info("Given platform has some null fields");
            b = false;
        } else if (platform.getUrl().isEmpty() || platform.getName().isEmpty()
                || platform.getInformationModelId().isEmpty()) {
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
    public static boolean validate(Resource resource) {
        boolean b;
        if (resource.getResourceURL() == null || resource.getLocation() == null || resource.getPlatformId() == null
                || resource.getName() == null || resource.getObservedProperties() == null
                || resource.getOwner() == null) {
            log.info("Given resource has some null fields");
            b = false;
        } else if (resource.getResourceURL().isEmpty() || resource.getPlatformId().isEmpty()
                || resource.getName().isEmpty() || resource.getObservedProperties().isEmpty()
                || resource.getOwner().isEmpty()) {
            log.info("Given resource has some empty fields");
            b = false;
        } else {
            b = true;
        }
        return b;
    }
}
