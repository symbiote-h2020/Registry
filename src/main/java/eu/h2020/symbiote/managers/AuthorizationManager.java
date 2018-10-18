package eu.h2020.symbiote.managers;

import com.fasterxml.jackson.core.JsonProcessingException;
import eu.h2020.symbiote.core.internal.CoreResource;
import eu.h2020.symbiote.model.cim.Resource;
import eu.h2020.symbiote.model.mim.InterworkingService;
import eu.h2020.symbiote.model.mim.Platform;
import eu.h2020.symbiote.model.persistenceResults.AuthorizationResult;
import eu.h2020.symbiote.repository.PlatformRepository;
import eu.h2020.symbiote.security.ComponentSecurityHandlerFactory;
import eu.h2020.symbiote.security.accesspolicies.IAccessPolicy;
import eu.h2020.symbiote.security.accesspolicies.common.singletoken.ComponentHomeTokenAccessPolicy;
import eu.h2020.symbiote.security.commons.exceptions.custom.InvalidArgumentsException;
import eu.h2020.symbiote.security.commons.exceptions.custom.SecurityHandlerException;
import eu.h2020.symbiote.security.communication.payloads.SecurityRequest;
import eu.h2020.symbiote.security.handler.IComponentSecurityHandler;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.*;


/**
 * Component responsible for dealing with Symbiote Tokens and checking access right for requests.
 * <p>
 * Created by mateuszl on 04.05.2017.
 */
@Component
public class AuthorizationManager {

    private static Log log = LogFactory.getLog(AuthorizationManager.class);
    private Boolean securityEnabled = true;

    private IComponentSecurityHandler componentSecurityHandler;
    private PlatformRepository platformRepository;

    // Fields for tests purposes
    private String aamAddress;
    private String clientId;
    private String keystoreName;
    private String keystorePass;
    private String componentOwnerName;
    private String componentOwnerPassword;
    // do not remove even if seems not needed

    @Autowired
    public AuthorizationManager(PlatformRepository platformRepository,
                                @Value("${aam.deployment.owner.username}") String componentOwnerName,
                                @Value("${aam.deployment.owner.password}") String componentOwnerPassword,
                                @Value("${aam.environment.aamAddress}") String aamAddress,
                                @Value("${aam.environment.clientId}") String clientId,
                                @Value("${aam.environment.keystoreName}") String keystoreName,
                                @Value("${aam.environment.keystorePass}") String keystorePass,
                                @Value("${registry.security.enabled}") Boolean securityEnabled) throws SecurityHandlerException {
        this.platformRepository = platformRepository;
        this.securityEnabled = securityEnabled;

        // Fields for tests purposes
        this.componentOwnerName = componentOwnerName;
        this.componentOwnerPassword = componentOwnerPassword;
        this.aamAddress = aamAddress;
        this.clientId = clientId;
        this.keystoreName = keystoreName;
        this.keystorePass = keystorePass;
        // do not remove even if seems not needed

        if (securityEnabled) {
            componentSecurityHandler = ComponentSecurityHandlerFactory.getComponentSecurityHandler(
                    this.keystoreName,
                    this.keystorePass,
                    this.clientId,
                    this.aamAddress,
                    this.componentOwnerName,
                    this.componentOwnerPassword);
        }
    }

    public AuthorizationResult checkSinglePlatformOperationAccess(SecurityRequest securityRequest, String platformId) {
        Set<String> ids = new HashSet<>();

        final String rhComponentId = "reghandler";

        if (platformId == null) {
            return new AuthorizationResult("Platform Id is null!", false);
        } else {
            ids.add(platformId);
            return checkOperationAccess(securityRequest, ids, rhComponentId);
        }
    }

    public AuthorizationResult checkSdevOperationAccess(SecurityRequest securityRequest, String sDevsPluginID) {
        Set<String> ids = new HashSet<>();

        final String componentId = "sspRegistry";

        if (StringUtils.isBlank(sDevsPluginID)) {
            return new AuthorizationResult("SdevsPluginId (SSP Id) is null!", false);                                   //Id of SSP given in Sdev (PluginId field)
        } else {
            ids.add(sDevsPluginID);
            return checkOperationAccess(securityRequest, ids, componentId);
        }
    }

    private AuthorizationResult checkOperationAccess(SecurityRequest securityRequest, Set<String> platformIds, String componentId) {
        if (securityEnabled) {

            log.info("Received SecurityRequest to verification: (" + securityRequest + ")");

            if (securityRequest == null) {
                return new AuthorizationResult("SecurityRequest is null", false);
            }
            if (platformIds == null || platformIds.isEmpty()) {
                return new AuthorizationResult("Platform Ids set is null or empty", false);
            }

            Set<String> checkedPolicies = checkPolicies(securityRequest, platformIds, componentId);

            if (!checkedPolicies.isEmpty()) {
                return new AuthorizationResult("ok", true);
            } else {
                return new AuthorizationResult("Provided Policies does not match with needed to perform operation.", false);
            }

        } else {
            //if security is disabled in properties
            return new AuthorizationResult("security disabled", true);
        }
    }

    private Set<String> checkPolicies(SecurityRequest securityRequest, Set<String> platformIds, String componentId) {

        Map<String, IAccessPolicy> accessPoliciesMap = new HashMap<>();
        Set<String> satisfiedPoliciesIdentifiers;


        for (String platformId : platformIds) {

            try {
                log.debug("Setting up component home token access policy for: platformId: " + platformId + " componentId: " + componentId);
                ComponentHomeTokenAccessPolicy componentHomeTokenAccessPolicy = new ComponentHomeTokenAccessPolicy(platformId, componentId, null);
                accessPoliciesMap.put(platformId, componentHomeTokenAccessPolicy);
            } catch (InvalidArgumentsException e) {
                log.error("Component Home Token Access Policy error: " + e);
            }
        }

        printSecurityRequest(securityRequest);
        satisfiedPoliciesIdentifiers = componentSecurityHandler.getSatisfiedPoliciesIdentifiers(accessPoliciesMap, securityRequest);


        return satisfiedPoliciesIdentifiers;
    }

    private void printSecurityRequest(SecurityRequest securityRequest) {
        log.debug("SecurityRequest:");
        log.debug("header params:");
        try {
            log.debug(securityRequest.getSecurityRequestHeaderParams());
        } catch (JsonProcessingException e) {
            e.printStackTrace();
        }
        securityRequest.getSecurityCredentials().stream().forEach(sc -> {
            log.debug("Token: " + sc.getToken());
            log.debug("Challenge: " + sc.getAuthenticationChallenge());
            log.debug("ClientCert: " + sc.getClientCertificate());
            log.debug("ClientCertSignAAMCert: " + sc.getClientCertificateSigningAAMCertificate());
            log.debug("ForeignTokenIssuing: " + sc.getForeignTokenIssuingAAMCertificate());
        });
    }

    public String generateServiceResponse() {
        String serviceResponse = "";
        try {
            if (securityEnabled) {
                serviceResponse = componentSecurityHandler.generateServiceResponse();
            }
        } catch (Exception e) {
            log.error(e);
        }
        return serviceResponse;
    }

    public AuthorizationResult checkIfResourcesBelongToPlatform(Map<String, Resource> resources, String platformId) {
        Platform registryPlatform = platformRepository.findOne(platformId);

        if (registryPlatform == null) {
            log.error("Given platform does not exists in database. Platform ID: " + platformId);
            return new AuthorizationResult("Given platform does not exists in database. Platform ID: ", false);
        }

        List<InterworkingService> interworkingServices = registryPlatform.getInterworkingServices();

        if (interworkingServices == null || interworkingServices.isEmpty()) {
            log.error("Interworking services list in given platform is null or empty");
            return new AuthorizationResult("Interworking services list in given platform is null or empty", false);
        }

        List<String> platformInterworkingServicesUrls = new ArrayList<>();
        interworkingServices.stream()
                .map(InterworkingService::getUrl).forEach(serviceUrl -> platformInterworkingServicesUrls.add(serviceUrl.endsWith("/") ? serviceUrl : serviceUrl + "/"));
        //stream adds slash ("/") at the end of url if there was not one already

        if (resources != null) {
            for (String key : resources.keySet()) {
                String resourceInterworkingServiceUrl = resources.get(key).getInterworkingServiceURL();

                if (resourceInterworkingServiceUrl == null) {
                    log.error("Resource has a null Interworking service URL!");
                    return new AuthorizationResult("Resource has a null Interworking service URL!", false);
                }

                if (!resourceInterworkingServiceUrl.endsWith("/")) {
                    resourceInterworkingServiceUrl += "/";
                }
                if (!platformInterworkingServicesUrls.contains(resourceInterworkingServiceUrl)) {
                    log.error("Resource does not match with any Interworking Service in given platform!");
                    return new AuthorizationResult("Resource does not match with any Interworking Service in given platform!", false);
                }
            }
        } else {
            log.error("Resources Map is null");
            return new AuthorizationResult("Resources Map is null", false);
        }

        log.info("Interworking services check succeed!");
        return new AuthorizationResult("Interworking services check succeed!", true);
    }

    public AuthorizationResult checkIfCoreResourcesBelongToSdev(Map<String, CoreResource> coreResources, String sDevId) {
        return new AuthorizationResult("MOCKED", true); //// TODO: 01.06.2018 MOCKED!!!
    }
}
