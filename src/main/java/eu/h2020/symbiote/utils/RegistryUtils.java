package eu.h2020.symbiote.utils;

import com.google.gson.Gson;
import eu.h2020.symbiote.core.model.internal.CoreResource;
import eu.h2020.symbiote.core.model.resources.Resource;
import eu.h2020.symbiote.model.InformationModel;
import eu.h2020.symbiote.model.Platform;
import eu.h2020.symbiote.model.RegistryResponse;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.http.HttpStatus;

import java.util.ArrayList;
import java.util.List;

/**
 * Utils for manipulating POJOs in Registry project.
 * <p>
 * Created by mateuszl on 14.02.2017.
 */
public class RegistryUtils {

    private static Log log = LogFactory.getLog(RegistryUtils.class);

    /**
     * Checks if given platform has all of the needed fields (besides the id field) and that neither is empty.
     *
     * @param platform platform to check
     * @return true if it has all the fields and neither is empty
     */
    public static boolean validateFields(Platform platform) { //todo extend validation to all fields
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
     * Checks if given resource has all of the needed fields (besides the id field) and that neither is empty.
     *
     * @param resource resource to check
     * @return true if it has all the fields and neither is empty.
     */
    public static boolean validateFields(Resource resource) { //todo extend validation to all fields
        boolean b;
        if (resource.getInterworkingServiceURL() == null
                || resource.getComments() == null
                || resource.getLabels() == null) {
            log.info("Given resource has some null fields");
            b = false;
        } else if (resource.getInterworkingServiceURL().isEmpty()
                || resource.getComments().isEmpty()
                || resource.getLabels().isEmpty()) {
            log.info("Given resource has some empty fields");
            b = false;
        } else {
            b = true;
        }
        return b;
    }

    /**
     * Checks if given informationModel has all of the needed fields and that neither is empty.
     *
     * @param informationModel informationModel to check
     * @return true if it has all the fields and neither is empty.
     */
    public static boolean validateFields(InformationModel informationModel) { //todo extend validation to all fields
        boolean b;
        if (informationModel.getBody() == null || informationModel.getFormat() == null ||
                informationModel.getUri() == null) {
            log.info("Given informationModel has some null fields");
            b = false;
        } else if (informationModel.getBody().isEmpty() || informationModel.getFormat().isEmpty() ||
                informationModel.getUri().isEmpty()) {
            log.info("Given informationModel has some empty fields");
            b = false;
        } else {
            b = true;
        }
        return b;
    }

    public static List<Resource> convertCoreResourcesToResources(List<CoreResource> coreResources) {
        List<Resource> resources = new ArrayList<>();
        for (CoreResource coreResource : coreResources) {
            Resource resource = new Resource();
            resource.setId(coreResource.getId());
            resource.setComments(coreResource.getComments());
            resource.setLabels(coreResource.getLabels());
            resource.setInterworkingServiceURL(coreResource.getInterworkingServiceURL());
            resources.add(resource);
        }
        return resources;
    }


    //todo MOCKED!! waiting for cooperation with SemanticManager
////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

    public static Platform getRdfBodyForObject(Platform platform) {
        if (platform.getBody() == null) platform.setBody("mocked body");
        if (platform.getFormat() == null) platform.setFormat("mocked format"); //todo get properties from Sem. Man.
        return platform;
    }

    public static InformationModel getRdfBodyForObject(InformationModel informationModel) {
        if (informationModel.getBody() == null) informationModel.setBody("mocked body");
        if (informationModel.getFormat() == null)
            informationModel.setFormat("mocked format"); //todo get properties from Sem. Man.
        return informationModel;
    }

    public static RegistryResponse getPlatformsFromRdf(String rdf) {
        Gson gson = new Gson();
        RegistryResponse RegistryResponse = new RegistryResponse();
        Platform p1 = new Platform();
        p1.getLabels().add("p1");
        Platform p2 = new Platform();
        p1.getLabels().add("p2");
        List<Platform> platforms = new ArrayList<>();
        platforms.add(p1);
        platforms.add(p2);
        RegistryResponse.setStatus(HttpStatus.SC_OK);
        RegistryResponse.setMessage("OK");
        RegistryResponse.setBody(gson.toJson(platforms));
        return RegistryResponse;
    }

    public static RegistryResponse getResourcesFromRdf(String rdf) {
        Gson gson = new Gson();
        RegistryResponse RegistryResponse = new RegistryResponse();
        Resource r1 = new Resource();
        r1.getLabels().add("r1");
        Resource r2 = new Resource();
        r2.getLabels().add("r2");
        List<Resource> resources = new ArrayList<>();
        resources.add(r1);
        resources.add(r2);
        RegistryResponse.setStatus(HttpStatus.SC_OK);
        RegistryResponse.setMessage("OK");
        RegistryResponse.setBody(gson.toJson(resources));
        return RegistryResponse;
    }


    public static RegistryResponse getInformationModelFromRdf(String body) {
        Gson gson = new Gson();
        RegistryResponse RegistryResponse = new RegistryResponse();
        InformationModel im = new InformationModel();
        im.setUri("http://test_uri.com/");
        im.setBody("Test body");
        im.setFormat("Test format");
        RegistryResponse.setBody(gson.toJson(im));
        RegistryResponse.setStatus(200);
        RegistryResponse.setMessage("OK");
        return RegistryResponse;
    }

////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
/////////////// TODO: MOCKED waiting for Security implementation


    public static boolean checkToken(String tokenString) {
/*
        SecurityHandler securityHandler = new SecurityHandler(); //TODO CIEKAWE CO TU NIBY MAM PRZEKZAć?!?!
        try {
            SymbIoTeToken token = securityHandler.verifyCoreToken(tokenString);
            log.info("Token " + token + " was verified");
        }
        catch (TokenVerificationException e) {
            log.error("Token could not be verified");
//            JSONObject error = new JSONObject();
//            error.put("error", "Token could not be verified");
//            return error;
            return false;
        }

*/
        return true;
    }
////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
}
