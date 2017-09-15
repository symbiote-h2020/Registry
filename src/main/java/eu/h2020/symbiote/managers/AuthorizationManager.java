package eu.h2020.symbiote.managers;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import eu.h2020.symbiote.core.model.InterworkingService;
import eu.h2020.symbiote.core.model.Platform;
import eu.h2020.symbiote.core.model.resources.Resource;
import eu.h2020.symbiote.model.AuthorizationResult;
import eu.h2020.symbiote.repository.PlatformRepository;
import eu.h2020.symbiote.security.ComponentSecurityHandlerFactory;
import eu.h2020.symbiote.security.accesspolicies.IAccessPolicy;
import eu.h2020.symbiote.security.accesspolicies.common.singletoken.SingleLocalHomeTokenIdentityBasedAccessPolicy;
import eu.h2020.symbiote.security.commons.SecurityConstants;
import eu.h2020.symbiote.security.commons.exceptions.custom.InvalidArgumentsException;
import eu.h2020.symbiote.security.commons.exceptions.custom.SecurityHandlerException;
import eu.h2020.symbiote.security.communication.payloads.Credentials;
import eu.h2020.symbiote.security.communication.payloads.GetPlatformOwnersRequest;
import eu.h2020.symbiote.security.communication.payloads.GetPlatformOwnersResponse;
import eu.h2020.symbiote.security.communication.payloads.SecurityRequest;
import eu.h2020.symbiote.security.handler.IComponentSecurityHandler;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;


/**
 * Component responsible for dealing with Symbiote Tokens and checking access right for requests.
 * <p>
 * Created by mateuszl on 04.05.2017.
 */
@Component
public class AuthorizationManager {

    private static Log log = LogFactory.getLog(AuthorizationManager.class);
    ObjectMapper mapper = new ObjectMapper();
    private String aamAddress;
    private String clientId;
    private String keystoreName;
    private String keystorePass;
    private String componentOwnerName;
    private String componentOwnerPassword;
    private Boolean securityEnabled;

    private IComponentSecurityHandler componentSecurityHandler;
    private PlatformRepository platformRepository;
    private RabbitManager rabbitManager;

    @Autowired
    public AuthorizationManager(PlatformRepository platformRepository,
                                RabbitManager rabbitManager,
                                @Value("${aam.deployment.owner.username}") String componentOwnerName,
                                @Value("${aam.deployment.owner.password}") String componentOwnerPassword,
                                @Value("${aam.environment.aamAddress}") String aamAddress,
                                @Value("${aam.environment.clientId}") String clientId,
                                @Value("${aam.environment.keystoreName}") String keystoreName,
                                @Value("${aam.environment.keystorePass}") String keystorePass,
                                @Value("${registry.security.enabled}") Boolean securityEnabled) throws SecurityHandlerException {
        this.rabbitManager = rabbitManager;
        this.platformRepository = platformRepository;
        this.componentOwnerName = componentOwnerName;
        this.componentOwnerPassword = componentOwnerPassword;
        this.aamAddress = aamAddress;
        this.clientId = clientId;
        this.keystoreName = keystoreName;
        this.keystorePass = keystorePass;
        this.securityEnabled = securityEnabled;

        if (securityEnabled) {
            componentSecurityHandler = ComponentSecurityHandlerFactory.getComponentSecurityHandler(aamAddress,
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

    public AuthorizationResult checkSMultiplePlatformOperationAccess(SecurityRequest securityRequest, List<String> platformIds) {
        Set<String> ids = new HashSet<>();
        ids.addAll(platformIds);
        return checkOperationAccess(securityRequest, ids);
    }

    public AuthorizationResult checkOperationAccess(SecurityRequest securityRequest, Set<String> platformIds) {
        if (securityEnabled) {
            log.info("Received SecurityRequest to verification: (" + securityRequest + ")");

            if (securityRequest == null) {
                return new AuthorizationResult("SecurityRequest is null", false);
            }
            if (platformIds == null) {
                return new AuthorizationResult("Platform Ids is null", false);
            }

            Set<String> checkedPolicies = checkPolicies(securityRequest, platformIds);

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

    public Set<String> checkPolicies(SecurityRequest securityRequest, Set<String> platformIds) {

        Map<String, IAccessPolicy> accessPoliciesMap = new HashMap<>();

        Map<String, String> platformsAndOwnersMap = getOwnersOfPlatformsFromAAM(platformIds);

        if (platformsAndOwnersMap != null) {
            for (String platformId : platformsAndOwnersMap.keySet()) {
                try {
                    accessPoliciesMap.put(
                            platformId,
                            new SingleLocalHomeTokenIdentityBasedAccessPolicy(
                                    SecurityConstants.CORE_AAM_INSTANCE_ID,
                                    platformsAndOwnersMap.get(platformId),
                                    null));
                } catch (InvalidArgumentsException e) {
                    log.error(e);
                }
            }
            return componentSecurityHandler.getSatisfiedPoliciesIdentifiers(accessPoliciesMap, securityRequest);
        } else {
            return new HashSet<>();
        }
    }

    private Map<String, String> getOwnersOfPlatformsFromAAM(Set<String> platformIds) {
        try {
            Credentials credentials = new Credentials(componentOwnerName, componentOwnerPassword);
            GetPlatformOwnersRequest request = new GetPlatformOwnersRequest(credentials, platformIds);

            String ownersOfPlatformsFromAAM = rabbitManager.getOwnersOfPlatformsFromAAM(mapper.writeValueAsString(request));
            GetPlatformOwnersResponse response = mapper.readValue(ownersOfPlatformsFromAAM, GetPlatformOwnersResponse.class);
            if (!response.getHttpStatus().is2xxSuccessful()) {
                log.error("Getting platform owners for aam failed: " + response.getHttpStatus());
                return new HashMap<>();
            } else {
                return response.getplatformsOwners();
            }
        } catch (JsonProcessingException e) {
            log.error(e);
            return null;
        } catch (IOException e) {
            log.error(e);
            return null;
        }
    }

    public String generateServiceResponse() {
        String serviceResponse = "";
        try {
            serviceResponse = componentSecurityHandler.generateServiceResponse();
        } catch (SecurityHandlerException e) {
            log.error(e);
        }
        return serviceResponse;
    }

    public SecurityRequest generateSecurityRequest() {
        SecurityRequest securityRequest = null;
        try {
            securityRequest = componentSecurityHandler.generateSecurityRequestUsingCoreCredentials();
        } catch (Exception e) {
            log.error(e);
        }
        return securityRequest;
    }

    public AuthorizationResult checkIfResourcesBelongToPlatform(Map<String, Resource> resources, String platformId) {
        Platform registryPlatform = platformRepository.findOne(platformId);

        if (registryPlatform == null) {
            log.error("Given platform does not exists in database");
            return new AuthorizationResult("Given platform does not exists in database", false);
        }

        List<InterworkingService> interworkingServices = registryPlatform.getInterworkingServices();

        if (interworkingServices == null) {
            log.error("Interworking services list in given platform is null");
            return new AuthorizationResult("Interworking services list in given platform is null", false);
        }

        List<String> platformInterworkingServicesUrls = interworkingServices.stream()
                .map(InterworkingService::getUrl)
                .collect(Collectors.toList());

        for (String key : resources.keySet()) {
            if (!platformInterworkingServicesUrls.contains(resources.get(key).getInterworkingServiceURL())) {
                log.error("Resource does not match with any Interworking Service in given platform! " + resources.get(key));
                return new AuthorizationResult("Resource does not match with any Interworking Service in given platform!", false);
            }
        }

        log.info("Interworking services check succeed!");
        return new AuthorizationResult("Interworking services check succeed!", true);
    }
}
