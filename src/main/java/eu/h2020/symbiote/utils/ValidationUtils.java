package eu.h2020.symbiote.utils;

import eu.h2020.symbiote.cloud.model.ssp.SspRegInfo;
import eu.h2020.symbiote.core.internal.CoreSdevRegistryRequest;
import eu.h2020.symbiote.core.internal.CoreSspResourceRegistryRequest;
import eu.h2020.symbiote.managers.RepositoryManager;
import eu.h2020.symbiote.model.mim.InformationModel;
import eu.h2020.symbiote.model.mim.InterworkingService;
import eu.h2020.symbiote.model.mim.SmartSpace;
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

        if (repositoryManager.getSdevById(sDev.getSymId()) == null)
            throw new IllegalAccessException("Wrong Sdev Id!");


        //check if given sspId exists in DB
        if (!repositoryManager.checkIfSspExists(request.getSspId())) {
            throw new IllegalAccessException("Given Ssp does not exist in database!");
        }

        //check if given sdevs pluginURL match to any of Ssps InterworkingInterfaceURL
        else if (!repositoryManager.getSspById(request.getSspId()).getInterworkingServices().stream()
                .map(InterworkingService::getUrl)
                .anyMatch(url -> url.equals(sDev.getPluginURL()))) {
            throw new IllegalAccessException("Given sdevs pluginURL does not match to any of Ssps InterworkingInterfaceURL!");
        }
    }


    public static void validateSspResource(CoreSspResourceRegistryRequest request, RepositoryManager repositoryManager) {

        SmartSpace sspById = repositoryManager.getSspById(request.getSspId());

        if (sspById == null) {
            throw new IllegalArgumentException("There is no such ssp in db!");
        }

        SspRegInfo sdevById = repositoryManager.getSdevById(request.getSdevId());

        if (sdevById == null) {
            throw new IllegalArgumentException("There is no such sdev in DB!");
        }

        //checks if there exists a matching IS URL in SSP to one given in Resource
        for (String k : request.getBody().keySet()) {
            String interworkingServiceURL = request.getBody().get(k).getInterworkingServiceURL();
            if (
                    sspById.getInterworkingServices().stream()
                            .map(InterworkingService::getUrl)
                            .noneMatch(url -> url.equalsIgnoreCase(interworkingServiceURL))) {

                throw new IllegalArgumentException("there does not exist a matching InterworkingService URL in SSP to one given in Resource!");
            }
        }

        //checks if SSP ID in request matches with SSP ID in given SDEV
        if (!sdevById.getSspId().equals(request.getSspId())) {
            throw new IllegalArgumentException("SSP ID in request does not match with SSP ID in given SDEV");
        }
    }
}
