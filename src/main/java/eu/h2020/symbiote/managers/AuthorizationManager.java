package eu.h2020.symbiote.managers;

import eu.h2020.symbiote.core.model.InterworkingService;
import eu.h2020.symbiote.core.model.Platform;
import eu.h2020.symbiote.core.model.resources.Resource;
import eu.h2020.symbiote.model.AuthorizationResult;
import eu.h2020.symbiote.repository.PlatformRepository;
import eu.h2020.symbiote.security.ComponentSecurityHandlerFactory;
import eu.h2020.symbiote.security.accesspolicies.IAccessPolicy;
import eu.h2020.symbiote.security.accesspolicies.SingleLocalHomeTokenIdentityBasedTokenAccessPolicy;
import eu.h2020.symbiote.security.commons.SecurityConstants;
import eu.h2020.symbiote.security.commons.exceptions.custom.InvalidArgumentsException;
import eu.h2020.symbiote.security.commons.exceptions.custom.SecurityHandlerException;
import eu.h2020.symbiote.security.communication.payloads.SecurityRequest;
import eu.h2020.symbiote.security.handler.IComponentSecurityHandler;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;


/**
 * Component responsible for dealing with Symbiote Tokens and checking access right for requests.
 * <p>
 * Created by mateuszl on 04.05.2017.
 */
@Component
public class AuthorizationManager {

    private static Log log = LogFactory.getLog(AuthorizationManager.class);
    private static IComponentSecurityHandler componentSecurityHandler;
    private PlatformRepository platformRepository;

    @Autowired
    public AuthorizationManager(PlatformRepository platformRepository) {
        this.platformRepository = platformRepository;
        try {
            componentSecurityHandler = ComponentSecurityHandlerFactory.getComponentSecurityHandler("", "", "", "ID", "", false, "user", "pass");
        } catch (SecurityHandlerException e) {
            log.error(e);
        }
    }

    public AuthorizationResult checkResourceOperationAccess(SecurityRequest securityRequest, String platformId) {
        log.info("Received SecurityRequest to verification: (" + securityRequest + ")");

        if (platformRepository.findOne(platformId) == null) {
            return new AuthorizationResult("Given platform does not exist in database", false);
        }

        if (securityRequest == null) {
            return new AuthorizationResult("SecurityRequest is null", false);
        }
        if (platformId == null) {
            return new AuthorizationResult("Platform Id is null", false);
        }

        return checkSecurityRequest(securityRequest);
    }

    public AuthorizationResult checkSecurityRequest(SecurityRequest securityRequest) {
        AuthorizationResult authorizationResult = new AuthorizationResult("MOCKED", true);

        //// TODO: 31.08.2017 implement!


        return authorizationResult;
    }

    public AuthorizationResult checkResourceOperationAccess(SecurityRequest securityRequest, Set<String> platformIds) {

        //todo check and finish !!

        if (platformIds.size() == checkPolicies(securityRequest).size()) {
            return new AuthorizationResult("ok", true);
        } else {
            return new AuthorizationResult("not authorized", false);
        }

    }

    public Set<String> checkPolicies(SecurityRequest securityRequest) {

        //todo check and finish !!

        Map<String, IAccessPolicy> accessPoliciesMap = new HashMap<>();

        Map<String, String> platformsAndOwnersMap = getOwnersOfPlatformsFromAAM();

        for (String platformId : platformsAndOwnersMap.keySet()) {
            try {
                accessPoliciesMap.put(
                        platformId,
                        new SingleLocalHomeTokenIdentityBasedTokenAccessPolicy(
                                SecurityConstants.AAM_CORE_AAM_INSTANCE_ID,
                                platformsAndOwnersMap.get(platformId),
                                null));
            } catch (InvalidArgumentsException e) {
                log.error(e);
            }
        }


        Set<String> satisfiedPoliciesIdentifiers =
                componentSecurityHandler.getSatisfiedPoliciesIdentifiers(accessPoliciesMap, securityRequest);

        return satisfiedPoliciesIdentifiers;
    }

    private Map<String, String> getOwnersOfPlatformsFromAAM() {

        return null; //todo implement rabbit magic !!
        
    }

    public String getServiceResponse() {
        String serviceResponse = "";
        try {
            serviceResponse = componentSecurityHandler.generateServiceResponse();
        } catch (SecurityHandlerException e) {
            log.error(e);
        }
        return serviceResponse;
    }

    public SecurityRequest getSecurityRequest() {
        SecurityRequest securityRequest = null;
        try {
            securityRequest = componentSecurityHandler.generateSecurityRequestUsingCoreCredentials();
        } catch (Exception e) {
            log.error(e);
        }
        return securityRequest;
    }


    /*
    public AuthorizationResult checkToken(String tokenString) {
        Token token = getToken(tokenString);
        if (token == null) {
            return new AuthorizationResult("Token invalid! Token could not be verified", false);
        }

        if (validationStatus != VALID) {
            log.error("Token failed verification due to " + validationStatus);
            return new AuthorizationResult("Token failed verification due to " + validationStatus, false);
        }
        return new AuthorizationResult("", true);
    }

    private Token getToken(String tokenString) {
        Token token;
        try {
            token = new Token(tokenString);
        } catch (TokenValidationException e) {
            log.error("Token could not be verified", e);
            return null;
        }
        return token;
    }

    private AuthorizationResult getAuthorizationResult(String tokenString, String platformId) {
        JWTClaims claims;

        try {
            claims = JWTEngine.getClaimsFromToken(tokenString);
        } catch (MalformedJWTException e) {
            log.error("Could not get the claims for token!", e);
            return new AuthorizationResult("Token invalid! Could not get the claims for token!", false);
        }

        // verify if there is a right token issuer in claims
        if (!IssuingAuthorityType.CORE.equals(IssuingAuthorityType.valueOf(claims.getTtyp()))) {
            log.error("Presented token was not issued by CoreAAM!");
            return new AuthorizationResult("Token invalid! Presented token was not issued by CoreAAM!", false);
        }

        // verify that this JWT contains attributes relevant for platform owner
        Map<String, String> attributes = claims.getAtt();

        // PO role
        if (!UserRole.PLATFORM_OWNER.toString().equals(attributes.get(CoreAttributes.ROLE.toString()))) {
            log.error("Wrong role claim in token!");
            return new AuthorizationResult("Token invalid! Wrong role claim in token!", false);
        }

        // owned platform identifier
        if (!platformId.equals(attributes.get(CoreAttributes.OWNED_PLATFORM.toString()))) {
            log.error("Platform owner does not match with requested operation!");
            return new AuthorizationResult("Token invalid! Platform owner does not match with requested operation!", false);
        }
        return new AuthorizationResult("Authorization check successful!", true);
    }

    */

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
