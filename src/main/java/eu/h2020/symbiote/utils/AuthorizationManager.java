package eu.h2020.symbiote.utils;

import eu.h2020.symbiote.core.model.InterworkingService;
import eu.h2020.symbiote.core.model.resources.Resource;
import eu.h2020.symbiote.model.Platform;
import eu.h2020.symbiote.repository.PlatformRepository;
import eu.h2020.symbiote.repository.ResourceRepository;
import eu.h2020.symbiote.security.SecurityHandler;
import eu.h2020.symbiote.security.enums.CoreAttributes;
import eu.h2020.symbiote.security.enums.IssuingAuthorityType;
import eu.h2020.symbiote.security.enums.UserRole;
import eu.h2020.symbiote.security.exceptions.aam.MalformedJWTException;
import eu.h2020.symbiote.security.exceptions.aam.TokenValidationException;
import eu.h2020.symbiote.security.exceptions.sh.SecurityHandlerDisabledException;
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

/**
 * Component responsible for dealing with Symbiote Tokens and checking access right for requests.
 * <p>
 * Created by mateuszl on 04.05.2017.
 */
@Component
public class AuthorizationManager {

    private static Log log = LogFactory.getLog(AuthorizationManager.class);
    private SecurityHandler securityHandler;
    private PlatformRepository platformRepository;
    private ResourceRepository resourceRepository;

    @Autowired
    public AuthorizationManager(SecurityHandler securityHandler, PlatformRepository platformRepository,
                                ResourceRepository resourceRepository) {
        this.securityHandler = securityHandler;
        this.platformRepository = platformRepository;
        this.resourceRepository = resourceRepository;
    }

    public boolean checkResourceOperationAccess(String tokenString, String platformId) {
        log.info("Received Token to verification: " + tokenString);

        JWTClaims claims;

        if (platformRepository.findOne(platformId) == null) {
            log.error("Given platform does not exist");
            return false;
        }

        try {
            Token token = securityHandler.verifyCoreToken(tokenString);
            log.info("Token " + token + " was verified");
        } catch (TokenValidationException e) {
            log.error("Token could not be verified", e);
            return false;
        } catch (SecurityHandlerDisabledException e) {
            log.info("Security Handler is disabled", e);
            return true;
        } catch (Exception e) {
            log.error(e);
        }

        try {
            claims = JWTEngine.getClaimsFromToken(tokenString);
        } catch (MalformedJWTException e) {
            log.error(e);
            return false;
        }

        // verify if there is a right token issuer in claims
        if (!IssuingAuthorityType.CORE.equals(IssuingAuthorityType.valueOf(claims.getTtyp()))) {
            log.error("Presented token was not issued by CoreAAM!");
            return false;
        }

        // verify that this JWT contains attributes relevant for platform owner
        Map<String, String> attributes = claims.getAtt();

        // PO role
        if (!UserRole.PLATFORM_OWNER.toString().equals(attributes.get(CoreAttributes.ROLE.toString()))) {
            log.error("Wrong role claim in token!");
            return false;
        }

        // owned platform identifier
        if (!platformId.equals(attributes.get(CoreAttributes.OWNED_PLATFORM.toString()))) {
            log.error("Platform owner does not match with requested operation!");
            return false;
        }
        return true;
    }

    public boolean checkIfResourcesBelongToPlatform(List<Resource> resources, String platformId) {
        Platform platform = platformRepository.findOne(platformId);

        if (platform == null) {
            log.error("Given platform does not exists in database");
            return false;
        }

        List<InterworkingService> interworkingServices = platform.getInterworkingServices();

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
