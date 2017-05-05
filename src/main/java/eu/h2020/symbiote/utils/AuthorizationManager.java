package eu.h2020.symbiote.utils;

import eu.h2020.symbiote.repository.PlatformRepository;
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

import java.util.Map;


/**
 * Created by mateuszl on 04.05.2017.
 */
@Component
public class AuthorizationManager {

    private static Log log = LogFactory.getLog(AuthorizationManager.class);
    private SecurityHandler securityHandler;
    private PlatformRepository platformRepository;

    @Autowired
    public AuthorizationManager(SecurityHandler securityHandler, PlatformRepository platformRepository) {
        this.securityHandler = securityHandler;
        this.platformRepository = platformRepository;
    }

    public boolean checkAccess(String tokenString, String platformId) {
        log.info("Received Token to verification: " + tokenString);
        JWTClaims claims;

        if (platformRepository.findOne(platformId) == null) return false;

        try {
            claims = JWTEngine.getClaimsFromToken(tokenString);
        } catch (MalformedJWTException e) {
            log.error(e);
            return false;
        }

        if (!IssuingAuthorityType.CORE.equals(IssuingAuthorityType.valueOf(claims.getTtyp()))) return false;

        // verify that this JWT contains attributes relevant for platform owner
        Map<String, String> attributes = claims.getAtt();
        // PO role
        if (!UserRole.PLATFORM_OWNER.toString().equals(attributes.get(CoreAttributes.ROLE.toString()))) return false;
        // owned platform identifier
        if (!platformId.equals(attributes.get(CoreAttributes.OWNED_PLATFORM.toString()))) return false;

        try {
            Token token = securityHandler.verifyCoreToken(tokenString);
            log.info("Token " + token + " was verified");
        } catch (TokenValidationException e) {
            log.error("Token could not be verified", e);
            return false;
        } catch (SecurityHandlerDisabledException e) {
            log.error("Security Handler is disabled", e);
            return true;
        }
        return true;
    }
}
