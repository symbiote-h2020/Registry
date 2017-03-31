package eu.h2020.symbiote.utils;

import com.google.gson.Gson;
import eu.h2020.symbiote.model.InformationModel;
import eu.h2020.symbiote.model.Platform;
import eu.h2020.symbiote.model.Resource;
import eu.h2020.symbiote.model.SemanticResponse;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.http.HttpStatus;

import java.util.ArrayList;
import java.util.List;

/**
 * Utils for manipulating POJOs in Registry project.
 *
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

    //todo MOCKED!! waiting for cooperation with SemanticManager
////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

    public static Resource getRdfBodyForObject(Resource resource){
        if (resource.getBody()==null) resource.setBody("mocked body");
        if (resource.getFormat()==null) resource.setFormat("mocked format"); //todo get properties from Sem. Man.
        return resource;
    }

    public static Platform getRdfBodyForObject(Platform platform){
        if (platform.getBody()==null) platform.setBody("mocked body");
        if (platform.getFormat()==null) platform.setFormat("mocked format"); //todo get properties from Sem. Man.
        return platform;
    }

    public static InformationModel getRdfBodyForObject(InformationModel informationModel){
        if (informationModel.getBody()==null) informationModel.setBody("mocked body");
        if (informationModel.getFormat()==null) informationModel.setFormat("mocked format"); //todo get properties from Sem. Man.
        return informationModel;
    }

    public static SemanticResponse getPlatformsFromRdf(String rdf) {
        Gson gson = new Gson();
        SemanticResponse semanticResponse = new SemanticResponse();
        Platform p1 = new Platform();
        p1.getLabels().add("p1");
        Platform p2 = new Platform();
        p1.getLabels().add("p2");
        List<Platform> platforms = new ArrayList<>();
        platforms.add(p1);
        platforms.add(p2);
        semanticResponse.setStatus(HttpStatus.SC_OK);
        semanticResponse.setMessage("OK");
        semanticResponse.setBody(gson.toJson(platforms));
        return semanticResponse;
    }

    public static SemanticResponse getResourcesFromRdf(String rdf){
        Gson gson = new Gson();
        SemanticResponse semanticResponse = new SemanticResponse();
        Resource r1 = new Resource();
        r1.getLabels().add("r1");
        Resource r2 = new Resource();
        r2.getLabels().add("r2");
        List<Resource> resources = new ArrayList<>();
        resources.add(r1);
        resources.add(r2);
        semanticResponse.setStatus(HttpStatus.SC_OK);
        semanticResponse.setMessage("OK");
        semanticResponse.setBody(gson.toJson(resources));
        return semanticResponse;
    }


    public static SemanticResponse getInformationModelFromRdf(String body) {
        Gson gson = new Gson();
        SemanticResponse semanticResponse = new SemanticResponse();
        InformationModel im = new InformationModel();
        im.setUri("http://test_uri.com/");
        im.setBody("Test body");
        im.setFormat("Test format");
        semanticResponse.setBody(gson.toJson(im));
        semanticResponse.setStatus(200);
        semanticResponse.setMessage("OK");
        return semanticResponse;
    }

////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

    public static boolean checkToken(String tokenString){

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
