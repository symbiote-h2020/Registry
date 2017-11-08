package eu.h2020.symbiote.managers;

import eu.h2020.symbiote.model.AuthorizationResult;
import eu.h2020.symbiote.model.cim.Resource;
import eu.h2020.symbiote.model.mim.InterworkingService;
import eu.h2020.symbiote.model.mim.Platform;
import eu.h2020.symbiote.repository.PlatformRepository;
import eu.h2020.symbiote.security.ComponentSecurityHandlerFactory;
import eu.h2020.symbiote.security.accesspolicies.IAccessPolicy;
import eu.h2020.symbiote.security.accesspolicies.common.SingleTokenAccessPolicyFactory;
import eu.h2020.symbiote.security.accesspolicies.common.singletoken.SingleTokenAccessPolicySpecifier;
import eu.h2020.symbiote.security.commons.exceptions.custom.InvalidArgumentsException;
import eu.h2020.symbiote.security.commons.exceptions.custom.SecurityHandlerException;
import eu.h2020.symbiote.security.communication.payloads.SecurityRequest;
import eu.h2020.symbiote.security.handler.IComponentSecurityHandler;
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
    private String aamAddress;
    private String clientId;
    private String keystoreName;
    private String keystorePass;
    private String componentOwnerName;
    private String componentOwnerPassword;
    private Boolean securityEnabled = true;

    private IComponentSecurityHandler componentSecurityHandler;
    private PlatformRepository platformRepository;

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
        this.componentOwnerName = componentOwnerName;
        this.componentOwnerPassword = componentOwnerPassword;
        this.aamAddress = aamAddress;
        this.clientId = clientId;
        this.keystoreName = keystoreName;
        this.keystorePass = keystorePass;
        this.securityEnabled = securityEnabled;

        if (securityEnabled) {
            componentSecurityHandler = ComponentSecurityHandlerFactory.getComponentSecurityHandler(
                    this.aamAddress,
                    this.keystoreName,
                    this.keystorePass,
                    this.clientId,
                    this.aamAddress,
                    false,
                    this.componentOwnerName,
                    this.componentOwnerPassword);
        }
    }

    public AuthorizationResult checkSinglePlatformOperationAccess(SecurityRequest securityRequest, String platformId) {
        Set<String> ids = new HashSet<>();
        ids.add(platformId);
        return checkOperationAccess(securityRequest, ids);
    }

    private AuthorizationResult checkOperationAccess(SecurityRequest securityRequest, Set<String> platformIds) {
        if (securityEnabled) {
            log.info("Received SecurityRequest to verification: (" + securityRequest + ")");

            if (securityRequest == null) {
                return new AuthorizationResult("SecurityRequest is null", false);
            }
            if (platformIds == null) {
                return new AuthorizationResult("Platform Ids is null", false);
            }
            Set<String> checkedPolicies;
            try {
                checkedPolicies = checkPolicies(securityRequest, platformIds);
            } catch (Exception e) {
                log.error("Component Security Handler Exception " + e);
                return new AuthorizationResult("Component Security Handler Exception " + e, false);
            }
            if (platformIds.size() == checkedPolicies.size()) {
                return new AuthorizationResult("ok", true);
            } else {
                return new AuthorizationResult("Provided Policies does not match with needed to perform operation.", false);
            }
        } else {
            //if security is disabled in properties
            return new AuthorizationResult("security disabled", true);
        }
    }

    private Set<String> checkPolicies(SecurityRequest securityRequest, Set<String> platformIds) throws Exception {

        Map<String, IAccessPolicy> accessPoliciesMap = new HashMap<>();
        Set<String> satisfiedPoliciesIdentifiers;

        String rhComponentId = "reghandler";

        for (String platformId : platformIds) {
            try {
                SingleTokenAccessPolicySpecifier specifier = new SingleTokenAccessPolicySpecifier(rhComponentId, platformId);
                accessPoliciesMap.put(
                        platformId, SingleTokenAccessPolicyFactory.getSingleTokenAccessPolicy(specifier));

            } catch (InvalidArgumentsException e) {
                log.error(e);
            }
        }
        try {
            satisfiedPoliciesIdentifiers = componentSecurityHandler.getSatisfiedPoliciesIdentifiers(accessPoliciesMap, securityRequest);
        } catch (Exception e) {
            log.error("Component Security Handler Exception " + e);
            throw e;
        }

        return satisfiedPoliciesIdentifiers;
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
            log.error("Given platform does not exists in database");
            return new AuthorizationResult("Given platform does not exists in database", false);
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
}
