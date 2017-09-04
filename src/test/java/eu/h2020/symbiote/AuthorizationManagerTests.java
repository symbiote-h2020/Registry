package eu.h2020.symbiote;

import eu.h2020.symbiote.core.model.Platform;
import eu.h2020.symbiote.core.model.resources.Resource;
import eu.h2020.symbiote.managers.AuthorizationManager;
import eu.h2020.symbiote.repository.PlatformRepository;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeoutException;

import static eu.h2020.symbiote.TestSetupConfig.*;
import static org.mockito.Mockito.when;

/**
 * Created by mateuszl on 18.05.2017.
 */
public class AuthorizationManagerTests {

    AuthorizationManager authorizationManager;
    PlatformRepository mockedPlatformRepository;

    @Before
    public void setup() throws IOException, TimeoutException {
        mockedPlatformRepository = Mockito.mock(PlatformRepository.class);
        authorizationManager = new AuthorizationManager(mockedPlatformRepository);
    }

    @After
    public void teardown() {

    }

    //// TODO: 13.07.2017
//    @Test
//    public void testResourceOperationAccessPass() {
//        when(mockedPlatformRepository.findOne("test1Plat")).thenReturn(generatePlatformB());
//        when(mockedSecurityHandler.verifyHomeToken(any())).thenReturn(ValidationStatus.VALID);
//        Assert.assertTrue(authorizationManager.checkOperationAccess(SECURITY_REQUEST, "test1Plat").isValidated());
//    }

//    @Test
//    public void testCheckToken(){
//        when(mockedSecurityHandler.verifyHomeToken(any())).thenReturn(ValidationStatus.VALID);
//        Assert.assertTrue(authorizationManager.checkToken(SECURITY_REQUEST).isValidated());
//    }

//    @Test
//    public void testCheckTokenFail(){
//        when(mockedSecurityHandler.verifyHomeToken(any())).thenReturn(ValidationStatus.VALID);
//        Assert.assertFalse(authorizationManager.checkToken("wrong_token").isValidated());
//    }
//
//    @Test
//    public void testResourceOperationAccessFailWithInvalidStatus() {
//        when(mockedPlatformRepository.findOne("test1Plat")).thenReturn(generatePlatformB());
//        when(mockedSecurityHandler.verifyHomeToken(any())).thenReturn(ValidationStatus.INVALID);
//        Assert.assertFalse("Access test passed!", authorizationManager.checkSinglePlatformOperationAccess(SECURITY_REQUEST, "test1Plat").isValidated());
//    }
//
//    @Test
//    public void testResourceOperationAccessFailWithWrongPlatformOwner() {
//        when(mockedPlatformRepository.findOne("wrongOwner")).thenReturn(generatePlatformB());
//        when(mockedSecurityHandler.verifyHomeToken(any())).thenReturn(ValidationStatus.VALID);
//        Assert.assertFalse("Access test passed!", authorizationManager.checkSinglePlatformOperationAccess(SECURITY_REQUEST, "wrongOwner").isValidated());
//    }
//
//    @Test
//    public void testResourceOperationAccessFailWithWrongPlatformId() {
//        when(mockedSecurityHandler.verifyHomeToken(any())).thenReturn(ValidationStatus.VALID);
//        Assert.assertFalse("Access test passed!", authorizationManager.checkSinglePlatformOperationAccess(SECURITY_REQUEST, "wrongPlatformId").isValidated());
//    }
//
//    @Test
//    public void testResourceOperationAccessFailWithWrongIssuer() {
//        when(mockedSecurityHandler.verifyHomeToken(any())).thenReturn(ValidationStatus.VALID);
//        Assert.assertFalse("Access test passed!", authorizationManager.checkSinglePlatformOperationAccess(SECURITY_REQUEST, "wrongIssuer").isValidated());
//    }

    @Test
    public void testIfResourceBelongsToPlatform(){
        Resource resource = generateResource();
        Platform platform = generatePlatformB();
        when(mockedPlatformRepository.findOne(PLATFORM_B_ID)).thenReturn(platform);
        Map<String, Resource> resources = new HashMap<>();
        resources.put("3", resource);

        Assert.assertTrue(authorizationManager.checkIfResourcesBelongToPlatform(resources, PLATFORM_B_ID).isValidated());
    }

    @Test
    public void testIfResourceDoesNotBelongToPlatform(){
        Resource resource = generateResource();
        resource.setInterworkingServiceURL("http://other_url.com/");
        Platform platform = generatePlatformB();
        when(mockedPlatformRepository.findOne(PLATFORM_B_ID)).thenReturn(platform);

        Map<String, Resource> resources = new HashMap<>();
        resources.put("3", resource);

        Assert.assertFalse(authorizationManager.checkIfResourcesBelongToPlatform(resources, PLATFORM_B_ID).isValidated());
    }
}