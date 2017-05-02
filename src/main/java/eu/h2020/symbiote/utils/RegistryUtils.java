package eu.h2020.symbiote.utils;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import eu.h2020.symbiote.commons.security.SecurityHandler;
import eu.h2020.symbiote.commons.security.exception.DisabledException;
import eu.h2020.symbiote.commons.security.token.SymbIoTeToken;
import eu.h2020.symbiote.commons.security.token.TokenVerificationException;
import eu.h2020.symbiote.core.internal.CoreResourceRegistryResponse;
import eu.h2020.symbiote.core.model.InterworkingService;
import eu.h2020.symbiote.core.model.internal.CoreResource;
import eu.h2020.symbiote.core.model.resources.Resource;
import eu.h2020.symbiote.model.InformationModel;
import eu.h2020.symbiote.model.Platform;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Utils for Registry project.
 * <p>
 * Created by mateuszl on 14.02.2017.
 */
public class RegistryUtils {

    private static Log log = LogFactory.getLog(RegistryUtils.class);

    private RegistryUtils() {
        throw new IllegalAccessError("Utility class");
    }

    /**
     * Checks if given platform has all of the needed fields (besides the id field) and that neither is empty.
     *
     * @param platform platform to check
     * @return true if it has all the fields and neither is empty
     */
    public static boolean validateFields(Platform platform) {
        //todo for next release extend validation to all fields
        boolean b;

        for (InterworkingService interworkingService : platform.getInterworkingServices()) {
            if (interworkingService.getUrl().trim().charAt(interworkingService.getUrl().length() - 1)
                    != "/".charAt(0)) {
                interworkingService.setUrl(interworkingService.getUrl().trim() + "/");
            }
        }

        if (platform.getBody() == null || platform.getLabels() == null || platform.getRdfFormat() == null) {
            log.info("Given platform has some null fields");
            b = false;
        } else if (platform.getBody().isEmpty() || platform.getLabels().isEmpty()
                || platform.getRdfFormat().isEmpty()) {
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
    public static boolean validateFields(InformationModel informationModel) {
        //todo for next release extend validation to all fields
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

    /**
     * Converts given list of Core Resources to Resources
     *
     * @param coreResources
     * @return
     */
    public static List<Resource> convertCoreResourcesToResources(List<CoreResource> coreResources) {
        List<Resource> resources = new ArrayList<>();
        for (CoreResource coreResource : coreResources) {
            Resource resource = convertCoreResourceToResource(coreResource);
            resources.add(resource);
        }
        return resources;
    }

    /**
     * Converts given Core Resource to Resource
     *
     * @param coreResource
     * @return
     */
    public static Resource convertCoreResourceToResource(CoreResource coreResource) {
        Resource resource = new Resource();
        resource.setId(coreResource.getId());
        resource.setComments(coreResource.getComments());
        resource.setLabels(coreResource.getLabels());
        resource.setInterworkingServiceURL(coreResource.getInterworkingServiceURL());
        return resource;
    }

    /**
     * Converts given Resource to Core Resource
     *
     * @param resource
     * @return
     */
    public static CoreResource convertResourceToCoreResource(Resource resource) {
        CoreResource coreResource = new CoreResource();
        coreResource.setId(resource.getId());
        coreResource.setComments(resource.getComments());
        coreResource.setLabels(resource.getLabels());
        coreResource.setInterworkingServiceURL(resource.getInterworkingServiceURL());
        return coreResource;
    }

    /**
     * Converts Platform (from Symbiote Libraries) to Platform (used in Registry Service)
     *
     * @param requestPlatform
     * @return
     */
    public static Platform convertRequestPlatformToRegistryPlatform
    (eu.h2020.symbiote.core.model.Platform requestPlatform) {
        Platform platform = new Platform();

        platform.setLabels(Arrays.asList(requestPlatform.getName()));

        platform.setComments(Arrays.asList(requestPlatform.getDescription()));

        InterworkingService interworkingService = new InterworkingService();
        interworkingService.setInformationModelId(requestPlatform.getInformationModelId());
        interworkingService.setUrl(requestPlatform.getUrl());
        platform.setInterworkingServices(Arrays.asList(interworkingService));

        platform.setBody("not null body");
        platform.setRdfFormat("not null rdf");

        return platform;
    }

    /**
     * Converts Platform (used in Registry Service) to Platform (from Symbiote Libraries)
     *
     * @param registryPlatform
     * @return
     */
    public static eu.h2020.symbiote.core.model.Platform convertRegistryPlatformToRequestPlatform
    (Platform registryPlatform) {
        eu.h2020.symbiote.core.model.Platform platform = new eu.h2020.symbiote.core.model.Platform();

        platform.setPlatformId(registryPlatform.getId());
        platform.setName(registryPlatform.getLabels().get(0));
        platform.setDescription(registryPlatform.getComments().get(0));
        platform.setInformationModelId(registryPlatform.getInterworkingServices().get(0).getInformationModelId());
        platform.setUrl(registryPlatform.getInterworkingServices().get(0).getUrl());

        return platform;
    }


    //todo MOCKED!! waiting for cooperation with SemanticManager
////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

    public static InformationModel getRdfBodyForObject(InformationModel informationModel) {
        if (informationModel.getBody() == null) informationModel.setBody("mocked body");
        if (informationModel.getFormat() == null)
            informationModel.setFormat("mocked format"); //todo get properties from Sem. Man.
        return informationModel;
    }

    public static CoreResourceRegistryResponse getInformationModelFromRdf(String body) throws JsonProcessingException {
        log.error("not implemented method triggered!" + body);
        ObjectMapper mapper = new ObjectMapper();
        CoreResourceRegistryResponse registryResponse = new CoreResourceRegistryResponse();
        InformationModel im = new InformationModel();
        im.setUri("http://test_uri.com/");
        im.setBody("Test body");
        im.setFormat("Test format");
        registryResponse.setBody(mapper.writeValueAsString(im));
        registryResponse.setStatus(200);
        registryResponse.setMessage("OK");
        return registryResponse;
    }

////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
/////////////// TODO: MOCKED - waiting for Security implementation


    /**
     * Checks if given token have rights to access Core resources.
     *
     * @param tokenString
     * @return
     */
    public static boolean checkToken(String tokenString, SecurityHandler securityHandler) {
        log.info("Token to verification: " + tokenString);

        try {
            SymbIoTeToken token = securityHandler.verifyCoreToken(tokenString);
            log.info("Token " + token + " was verified");
        } catch (TokenVerificationException e) {
            log.error("Token could not be verified", e);
            return false;
        } catch (DisabledException e) {
            log.error(e);
            return true;
        }

        return true;
    }
////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
}
