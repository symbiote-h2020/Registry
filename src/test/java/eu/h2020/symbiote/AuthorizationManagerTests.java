package eu.h2020.symbiote;

import eu.h2020.symbiote.model.RegistryPlatform;
import eu.h2020.symbiote.repository.RegistryPlatformRepository;
import eu.h2020.symbiote.security.InternalSecurityHandler;
import eu.h2020.symbiote.security.enums.ValidationStatus;
import eu.h2020.symbiote.security.exceptions.aam.TokenValidationException;
import eu.h2020.symbiote.security.token.Token;
import eu.h2020.symbiote.utils.AuthorizationManager;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;

import java.io.IOException;
import java.util.concurrent.TimeoutException;

import static eu.h2020.symbiote.TestSetupConfig.MOCKED_TOKEN;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.when;

/**
 * Created by mateuszl on 18.05.2017.
 */
public class AuthorizationManagerTests {

    AuthorizationManager authorizationManager;
    RegistryPlatformRepository mockedRegistryPlatformRepository;
    InternalSecurityHandler mockedSecurityHandler;

    @Before
    public void setup() throws IOException, TimeoutException {
        mockedSecurityHandler = Mockito.mock(InternalSecurityHandler.class);
        mockedRegistryPlatformRepository = Mockito.mock(RegistryPlatformRepository.class);
        authorizationManager = new AuthorizationManager(mockedSecurityHandler, mockedRegistryPlatformRepository);
    }

    @After
    public void teardown() {

    }

    @Test
    public void testResouceOperationAccess() {
        Token token = null;
        when(mockedRegistryPlatformRepository.findOne("test1Plat")).thenReturn(new RegistryPlatform());
        try {
            token = new Token(MOCKED_TOKEN);

        } catch (TokenValidationException e) {
            e.printStackTrace();
        }
        when(mockedSecurityHandler.verifyHomeToken(any())).thenReturn(ValidationStatus.VALID);

        Assert.assertTrue("FALSE", authorizationManager.checkResourceOperationAccess(MOCKED_TOKEN, "test1Plat"));
    }
}
