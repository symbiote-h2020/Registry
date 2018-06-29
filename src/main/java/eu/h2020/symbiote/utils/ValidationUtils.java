package eu.h2020.symbiote.utils;

import eu.h2020.symbiote.model.mim.InformationModel;
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
}
