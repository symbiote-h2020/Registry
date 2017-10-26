package eu.h2020.symbiote;

import eu.h2020.symbiote.managers.AuthorizationManager;
import eu.h2020.symbiote.managers.RabbitManager;
import eu.h2020.symbiote.model.cim.Resource;
import eu.h2020.symbiote.model.mim.InterworkingService;
import eu.h2020.symbiote.model.mim.Platform;
import eu.h2020.symbiote.repository.PlatformRepository;
import eu.h2020.symbiote.security.commons.exceptions.custom.SecurityHandlerException;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;
import org.springframework.test.util.ReflectionTestUtils;

import java.io.IOException;
import java.util.Arrays;
import java.util.HashMap;
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
    public void setup() throws IOException, TimeoutException, SecurityHandlerException {
        mockedPlatformRepository = Mockito.mock(PlatformRepository.class);
        RabbitManager rabbitManager = Mockito.mock(RabbitManager.class);

        authorizationManager = new AuthorizationManager(mockedPlatformRepository, null, null, null, null, null, null, SECURITY_ENABLED); //security enabled = false stands for DISABLING security
        ReflectionTestUtils.setField(authorizationManager, "aamAddress", AAM_ADDRESS);
        ReflectionTestUtils.setField(authorizationManager, "clientId", AAM_CLIENT_ID);
        ReflectionTestUtils.setField(authorizationManager, "keystoreName", AAM_KEYSTORE_NAME);
        ReflectionTestUtils.setField(authorizationManager, "keystorePass", AAM_KEYSTORE_PASS);
        ReflectionTestUtils.setField(authorizationManager, "componentOwnerName", AAM_COMP_OWNER_NAME);
        ReflectionTestUtils.setField(authorizationManager, "componentOwnerPassword", AAM_COMP_OWNER_PASS);
    }

    @After
    public void teardown() {

    }

    @Test
    public void operationAccessWithSecurityDisabledTest(){
        Assert.assertTrue(authorizationManager.checkSinglePlatformOperationAccess(SECURITY_REQUEST, "").isValidated());
        //method should return true because of security is disabled
    }


    @Test(expected = NullPointerException.class)
    public void nullPointerSecurityHandlerExceptionTest() throws Exception {

        AuthorizationManager authorizationManager1 = null;
        try {
            authorizationManager1 = new AuthorizationManager(mockedPlatformRepository, null, null, null, null, null, null, true);
            //security enabled = true stands for ENABLING security
        } catch (Exception e){
            System.out.println(e);
        }
        authorizationManager1.checkSinglePlatformOperationAccess(SECURITY_REQUEST, "id");
        //method should return throw a null pointer because of enabled security
    }

    @Test
    public void checkIfResourceBelongToPlatformPassTest() throws Exception {
        Platform platformB = generatePlatformB();
        when(mockedPlatformRepository.findOne(platformB.getId())).thenReturn(platformB);

        HashMap<String, Resource> resources = new HashMap<>();
        resources.put("1", generateResourceWithoutId());

        Assert.assertTrue(authorizationManager.checkIfResourcesBelongToPlatform(resources, platformB.getId()).isValidated());
    }

    @Test
    public void checkIfResourceBelongToPlatformWitchDoesNotExistInDbTest() throws Exception {
        Platform platformB = generatePlatformB();
        when(mockedPlatformRepository.findOne(platformB.getId())).thenReturn(null);

        Assert.assertFalse(authorizationManager.checkIfResourcesBelongToPlatform(new HashMap<>(), platformB.getId()).isValidated());
        //method should return false because repository did not found given platform id
    }

    @Test
    public void checkIfResourceBelongToPlatformWithNullISTest() throws Exception {
        Platform platformB = generatePlatformB();
        platformB.setInterworkingServices(null);
        when(mockedPlatformRepository.findOne(platformB.getId())).thenReturn(platformB);

        Assert.assertFalse(authorizationManager.checkIfResourcesBelongToPlatform(new HashMap<>(), platformB.getId()).isValidated());
        //method should return false because platform has a null Interworking Seervices list
    }

    @Test
    public void checkIfResourceBelongToPlatformWithNullResourcesMapTest() throws Exception {
        Platform platformB = generatePlatformB();
        when(mockedPlatformRepository.findOne(platformB.getId())).thenReturn(platformB);

        Assert.assertFalse(authorizationManager.checkIfResourcesBelongToPlatform(null, platformB.getId()).isValidated());
        //method should return false because platform has a null Interworking Seervices list
    }

    @Test
    public void checkIfResourceDoNotBelongToPlatformTest() throws Exception {
        Platform platformB = generatePlatformB();
        InterworkingService interworkingService = new InterworkingService();
        interworkingService.setUrl("some url");
        interworkingService.setInformationModelId("some id");
        platformB.setInterworkingServices(Arrays.asList(interworkingService));
        when(mockedPlatformRepository.findOne(platformB.getId())).thenReturn(platformB);

        HashMap<String, Resource> resources = new HashMap<>();
        resources.put("1", generateResourceWithoutId());

        Assert.assertFalse(authorizationManager.checkIfResourcesBelongToPlatform(resources, platformB.getId()).isValidated());
    }
}