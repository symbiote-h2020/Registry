package eu.h2020.symbiote.utils;

import eu.h2020.symbiote.core.model.InterworkingService;
import eu.h2020.symbiote.core.model.resources.Resource;
import eu.h2020.symbiote.model.AuthorizationResult;
import eu.h2020.symbiote.model.RegistryPlatform;
import eu.h2020.symbiote.repository.RegistryPlatformRepository;
import eu.h2020.symbiote.security.InternalSecurityHandler;
import eu.h2020.symbiote.security.enums.CoreAttributes;
import eu.h2020.symbiote.security.enums.IssuingAuthorityType;
import eu.h2020.symbiote.security.enums.UserRole;
import eu.h2020.symbiote.security.enums.ValidationStatus;
import eu.h2020.symbiote.security.exceptions.aam.MalformedJWTException;
import eu.h2020.symbiote.security.exceptions.aam.TokenValidationException;
import eu.h2020.symbiote.security.token.Token;
import eu.h2020.symbiote.security.token.jwt.JWTClaims;
import eu.h2020.symbiote.security.token.jwt.JWTEngine;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static eu.h2020.symbiote.security.enums.ValidationStatus.VALID;

/**
 * Component responsible for dealing with Symbiote Tokens and checking access right for requests.
 * <p>
 * Created by mateuszl on 04.05.2017.
 */
@Component
public class AuthorizationManager {

    private static Log log = LogFactory.getLog(AuthorizationManager.class);
    private InternalSecurityHandler securityHandler;
    private RegistryPlatformRepository registryPlatformRepository;

    @Autowired
    public AuthorizationManager(InternalSecurityHandler securityHandler, RegistryPlatformRepository registryPlatformRepository) {
        this.securityHandler = securityHandler;
        this.registryPlatformRepository = registryPlatformRepository;
    }

    public AuthorizationResult checkResourceOperationAccess(String tokenString, String platformId) {
        log.info("Received Token to verification: (" + tokenString + ")");

        JWTClaims claims;

        if (registryPlatformRepository.findOne(platformId) == null) {
            return new AuthorizationResult("Given platform does not exist", false);
        }

        AuthorizationResult authorizationResult = checkToken(tokenString);
        if (!authorizationResult.isValidated()) return authorizationResult;

        try {
            claims = JWTEngine.getClaimsFromToken(tokenString);
        } catch (MalformedJWTException e) {
            log.error("Could not get the claims for token!", e);
            return new AuthorizationResult("Token invalid!", false);
        }

        // verify if there is a right token issuer in claims
        if (!IssuingAuthorityType.CORE.equals(IssuingAuthorityType.valueOf(claims.getTtyp()))) {
            log.error("Presented token was not issued by CoreAAM!");
            return new AuthorizationResult("Token invalid!", false);
        }

        // verify that this JWT contains attributes relevant for platform owner
        Map<String, String> attributes = claims.getAtt();

        // PO role
        if (!UserRole.PLATFORM_OWNER.toString().equals(attributes.get(CoreAttributes.ROLE.toString()))) {
            log.error("Wrong role claim in token!");
            return new AuthorizationResult("Token invalid!", false);
        }

        // owned platform identifier
        if (!platformId.equals(attributes.get(CoreAttributes.OWNED_PLATFORM.toString()))) {
            log.error("Platform owner does not match with requested operation!");
            return new AuthorizationResult("Token invalid!", false);
        }
        return new AuthorizationResult("Authorization check successful!", true);
    }

    public AuthorizationResult checkToken(String tokenString) {
        Token token;
        try {
            token = new Token(tokenString);
        } catch (TokenValidationException e) {
            log.error("Token could not be verified", e);
            return new AuthorizationResult("Token invalid!", false);
        }
        ValidationStatus validationStatus = securityHandler.verifyHomeToken(token);

        if (validationStatus != VALID) {
            log.error("Token failed verification due to " + validationStatus);
            return new AuthorizationResult("Token failed verification due to " + validationStatus, false);
        }
        return new AuthorizationResult("", true);
    }

    public boolean checkIfResourcesBelongToPlatform(List<Resource> resources, String platformId) {
        RegistryPlatform registryPlatform = registryPlatformRepository.findOne(platformId);

        if (registryPlatform == null) {
            log.error("Given platform does not exists in database");
            return false;
        }

        List<InterworkingService> interworkingServices = registryPlatform.getInterworkingServices();

        if (interworkingServices == null) {
            log.error("Interworking services list in given platform is null");
            return false;
        }

        List<String> platformInterworkingServicesUrls = interworkingServices.stream()
                .map(InterworkingService::getUrl)
                .collect(Collectors.toList());

        for (Resource resource : resources) {
            if (!platformInterworkingServicesUrls.contains(resource.getInterworkingServiceURL())) {
                log.error("Resource does not match with any Interworking Service in given platform! " + resource);
                return false;
            }
        }

        log.info("Interworking services check succeed!");
        return true;
    }
}
