package eu.h2020.symbiote.utils;

import eu.h2020.symbiote.cloud.model.ssp.SspRegInfo;
import eu.h2020.symbiote.core.internal.CoreSdevRegistryRequest;
import eu.h2020.symbiote.managers.RepositoryManager;
import eu.h2020.symbiote.model.mim.InformationModel;
import eu.h2020.symbiote.model.mim.InterworkingService;
import org.apache.commons.lang.StringUtils;

/**
 * Created by mateuszl on 29.06.2018.
 */
public class ValidationUtils {

    public static void validateInformationModelForCreation(InformationModel informationModelReceived) {

        validateInformationModel(informationModelReceived);

        if (StringUtils.isNotBlank(informationModelReceived.getId())) {
            throw new IllegalArgumentException("Id is not blank! Request denied!");
        }
    }

    public static void validateInformationModelForModification(InformationModel informationModelReceived) {

        validateInformationModel(informationModelReceived);

        if (StringUtils.isBlank(informationModelReceived.getId())) {
            throw new IllegalArgumentException("Id is blank! Request denied!");
        }
    }

    public static void validateInformationModelForRemoval(InformationModel informationModelReceived) {

        if (informationModelReceived == null) {
            throw new NullPointerException("Received Information Model is null!");

        } else if (StringUtils.isBlank(informationModelReceived.getId())) {
            throw new IllegalArgumentException("Id is blank! Request denied!");
        }
    }

    private static void validateInformationModel(InformationModel informationModelReceived) {

        if (informationModelReceived == null) {
            throw new NullPointerException("Received Information Model is null!");

        } else if (informationModelReceived.getRdfFormat() == null) {
            throw new IllegalArgumentException("RDF Format is null! Request denied!");

        } else if (StringUtils.isBlank(informationModelReceived.getRdf())) {
            throw new IllegalArgumentException("RDF is blank! Request denied!");

        } else if (StringUtils.isBlank(informationModelReceived.getName())) {
            throw new IllegalArgumentException("Name is blank! Request denied!");

        } else if (StringUtils.isBlank(informationModelReceived.getOwner())) {
            throw new IllegalArgumentException("Owner is blank! Request denied!");

        } else if (StringUtils.isBlank(informationModelReceived.getUri())) {
            throw new IllegalArgumentException("URI is blank! Request denied!");
        }
    }

    public static void validateSdev(RepositoryManager repositoryManager, CoreSdevRegistryRequest request)
            throws IllegalAccessException {

        SspRegInfo sDev = request.getBody();

        //check if given sspId exists in DB
        if (!repositoryManager.checkIfSspExists(request.getSspId())) {
            throw new IllegalAccessException("Given Ssp does not exist in database!");
        }

        //check if given sdev has a match PluginId with given SspId
        else if (!request.getSspId().equals(sDev.getPluginId())) {
            throw new IllegalAccessException("Given Ssp ID does not match with sDev's Plugin ID!");
        }

        //check if given sdevs pluginURL match to any of Ssps InterworkingInterfaceURL
        else if (!repositoryManager.getSspById(request.getSspId()).getInterworkingServices().stream()
                .map(InterworkingService::getUrl)
                .anyMatch(url -> url.equals(sDev.getPluginURL()))) {
            throw new IllegalAccessException("Given sdevs pluginURL does not match to any of Ssps InterworkingInterfaceURL!");
        }
    }
}
