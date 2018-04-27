package eu.h2020.symbiote;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import eu.h2020.symbiote.core.cci.RDFResourceRegistryRequest;
import eu.h2020.symbiote.core.internal.*;
import eu.h2020.symbiote.managers.RabbitManager;
import eu.h2020.symbiote.model.cim.Resource;
import eu.h2020.symbiote.model.cim.StationarySensor;
import eu.h2020.symbiote.model.mim.*;
import eu.h2020.symbiote.security.accesspolicies.common.AccessPolicyType;
import eu.h2020.symbiote.security.accesspolicies.common.IAccessPolicySpecifier;
import eu.h2020.symbiote.security.accesspolicies.common.singletoken.SingleTokenAccessPolicySpecifier;
import eu.h2020.symbiote.security.commons.exceptions.custom.InvalidArgumentsException;
import eu.h2020.symbiote.security.communication.payloads.SecurityRequest;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.util.ReflectionTestUtils;

import java.io.IOException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeoutException;

/**
 * Created by Mael and mateuszl
 */
@SpringBootTest
@ComponentScan
@TestPropertySource(
        locations = {"classpath:test.properties"},
        properties = {"key=value"})
public class TestSetupConfig {

    public static final SecurityRequest SECURITY_REQUEST = new SecurityRequest(null, new Long("5"));
    public static final String PLATFORM_EXCHANGE_NAME = "symbIoTe.platform";
    public static final String PLATFORM_CREATION_REQUESTED_RK = "symbIoTe.platform.creationRequested";
    public static final String PLATFORM_MODIFICATION_REQUESTED_RK = "symbIoTe.platform.modificationRequested";
    public static final String PLATFORM_REMOVAL_REQUESTED_RK = "symbIoTe.platform.removalRequested";
    public static final String RESOURCE_EXCHANGE_NAME = "symbIoTe.resource";
    public static final String RESOURCE_CREATION_REQUESTED_RK = "symbIoTe.resource.creationRequested";
    public static final String RESOURCE_MODIFICATION_REQUESTED_RK = "symbIoTe.resource.modificationRequested";
    public static final String RESOURCE_REMOVAL_REQUESTED_RK = "symbIoTe.resource.removalRequested";
    public static final String RESOURCE_CLEAR_DATA_REQUESTED_RK = "resource.clearDataRequested";
    public static final String FEDERATION_EXCHANGE_NAME = "symbIoTe.federation";
    public static final String INFORMATION_MODEL_EXCHANGE_NAME = "symbIoTe.platform";
    public static final String FEDERATION_CREATION_REQUESTED_RK = "symbIoTe.federation.creationRequested";
    public static final String FEDERATION_MODIFICATION_REQUESTED_RK = "symbIoTe.federation.modificationRequested";
    public static final String FEDERATION_REMOVAL_REQUESTED_RK = "symbIoTe.federation.removalRequested";
    public static final String INFORMATION_MODEL_CREATION_REQUESTED_RK = "symbIoTe.platform.model.creationRequested";
    public static final String INFORMATION_MODEL_MODIFICATION_REQUESTED_RK = "symbIoTe.platform.model.modificationRequested";
    public static final String INFORMATION_MODEL_REMOVAL_REQUESTED_RK = "symbIoTe.platform.model.removalRequested";
    public static final String INFORMATION_MODEL_VALIDATION_REQUESTED_RK = "symbIoTe.platform.model.validationRequested";
    public static final String GET_ALL_INFORMATION_MODELS_REQUESTED_RK = "symbIoTe.informationModel.allInformationModelsRequested";
    public static final String PLATFORM_CREATED_ROUTING_KEY = "symbIoTe.platform.created";
    public static final String PLATFORM_MODIFIED_ROUTING_KEY = "symbIoTe.platform.updated";
    public static final String PLATFORM_REMOVED_ROUTING_KEY = "symbIoTe.platform.deleted";
    public static final String RESOURCE_CREATED_ROUTING_KEY = "symbIoTe.resource.created";
    public static final String RESOURCE_REMOVED_ROUTING_KEY = "symbIoTe.resource.deleted";
    public static final String RESOURCE_MODIFIED_ROUTING_KEY = "symbIoTe.resource.updated";
    public static final String PLATFORM_REMOVAL_REQUESTED_QUEUE = "symbIoTe-Registry-platformRemovalRequestedQueue";
    public static final String RESOURCE_CREATION_REQUESTED_QUEUE = "symbIoTe-Registry-resourceCreationRequestedQueue";
    public static final String RESOURCE_MODIFICATION_REQUESTED_QUEUE = "symbIoTe-Registry-resourceModificationRequestedQueue";
    public static final String PLATFORM_CREATION_REQUESTED_QUEUE = "symbIoTe-Registry-platformCreationRequestedQueue";
    public static final String PLATFORM_MODIFICATION_REQUESTED_QUEUE = "symbIoTe-Registry-platformModificationRequestedQueue";
    public static final String RESOURCE_REMOVAL_REQUESTED_QUEUE = "symbIoTe-Registry-resourceRemovalRequestedQueue";
    public static final String PLATFORM_RESOURCES_REQUESTED_QUEUE = "symbIoTe-Registry-platformResourcesRequestedQueue";
    public static final String INFORMATION_MODEL_CREATION_REQUESTED_QUEUE = "symbIoTe-Registry-informationModelCreationRequestedQueue";
    public static final String INFORMATION_MODEL_MODIFICATION_REQUESTED_QUEUE = "symbIoTe-Registry-informationModelModificationRequestedQueue";
    public static final String INFORMATION_MODEL_REMOVAL_REQUESTED_QUEUE = "symbIoTe-Registry-informationModelRemovalRequestedQueue";
    public static final String RESOURCES_FOR_PLATFORM_REQUESTED_RK = "symbIoTe.platform.resourcesRequested";
    public static final String PLATFORM_DETAILS_REQUESTED_RK = "symbIoTe.platform.platformDetailsRequested";
    public static final String RESOURCE_TRANSLATION_REQUESTED_RK = "symbIoTe.resource.instance.translationRequested";
    public static final String RESOURCE_VALIDATION_REQUESTED_RK = "symbIoTe.resource.instance.validationRequested";
    public static final String GET_FEDERATION_FOR_PLATFORM_RK = "symbIoTe.federation.getForPlatform";
    public static final String GET_ALL_FEDERATIONS_RK = "symbIoTe.federation.getAll";
    public static final String AAM_EXCHANGE_NAME = "symbIoTe.AuthenticationAuthorizationManager";
    public static final String AAM_GET_PLATFORM_OWNERS_RK = "symbIoTe-AuthenticationAuthorizationManager.get_platform_owners_names";
    public static final String PLATFORM_A_ID = "1";
    public static final String PLATFORM_A_NAME = "Platform1";
    public static final String PLATFORM_A_DESCRIPTION = "11desc";
    public static final String PLATFORM_A_URI = "http://www.symbiote-h2020.eu/ontology/platforms/1";
    public static final String PLATFORM_A_SERVICE_URI = "http://www.symbiote-h2020.eu/ontology/platforms/1/service/somehost1.com/resourceAccessProxy";
    public static final String PLATFORM_A_URL = "http://somehost1.com/resourceAccessProxy";
    public static final String INTERWORKING_SERVICE_URL_A = "http://somehost1.com/platformA";
    public static final String INFORMATION_MODEL_ID_A = "IM_1";
    public static final String PLATFORM_A_NAME_UPDATED = "Platform1Updated";
    public static final String PLATFORM_A_MODEL_ID_UPDATED = "11Updated";
    public static final String PLATFORM_A_DESCRIPTION_UPDATED = "11descUpdated";
    public static final String PLATFORM_A_URL_UPDATED = "http://somehost1.com/resourceAccessProxyUpdated";
    public static final String PLATFORM_B_ID = "PlatB";
    public static final String PLATFORM_B_NAME = "PlatformB";
    public static final String PLATFORM_B_DESCRIPTION = "21desc";
    public static final String INTERWORKING_SERVICE_URL_B = "http://somehost1.com/platformB";
    public static final String INFORMATION_MODEL_ID_B = "IM_2";
    public static final String RESOURCE_PREDICATE = "http://www.symbiote-h2020.eu/ontology/resources/";
    public static final String RESOURCE_101_LABEL = "Resource 101";
    public static final String RESOURCE_101_COMMENT = "Resource 101 comment";
    public static final String RESOURCE_101_ID = "101";
    public static final String RESOURCE_STATIONARY_FILENAME = "/exampleStationarySensor.json";
    public static final String RESOURCE_STATIONARY_FILENAME_MODIFIED = "/exampleStationarySensorModified.json";
    public static final String RESOURCE_STATIONARY_LABEL = "Stationary 1";
    public static final String RESOURCE_STATIONARY_LABEL_MODIFIED = "New sensor 1";
    public static final String RESOURCE_STATIONARY_COMMENT = "This is stationary 1";
    public static final String RESOURCE_STATIONARY_URI = RESOURCE_PREDICATE + "stationary1";
    public static final String RESOURCE_STATIONARY_ID = "stationary1";
    public static final String AAM_ADDRESS = "";
    public static final String AAM_CLIENT_ID = "";
    public static final String AAM_KEYSTORE_NAME = "RegistyTestKeystrore.jks";
    public static final String AAM_KEYSTORE_PASS = "test_pass";
    public static final String AAM_COMP_OWNER_NAME = "";
    public static final String AAM_COMP_OWNER_PASS = "";
    public static final Boolean SECURITY_ENABLED = false;
    public static final String TEMP_QUEUE = "RPCqueue";

    public static void initializeRabbitManager(RabbitManager rm) {
        ReflectionTestUtils.setField(rm, "rabbitHost", "localhost");
        ReflectionTestUtils.setField(rm, "rabbitUsername", "guest");
        ReflectionTestUtils.setField(rm, "rabbitPassword", "guest");

        ReflectionTestUtils.setField(rm, "platformExchangeName", PLATFORM_EXCHANGE_NAME);
        ReflectionTestUtils.setField(rm, "platformExchangeType", "topic");
        ReflectionTestUtils.setField(rm, "plaftormExchangeDurable", true);
        ReflectionTestUtils.setField(rm, "platformExchangeAutodelete", false);
        ReflectionTestUtils.setField(rm, "platformExchangeInternal", false);

        ReflectionTestUtils.setField(rm, "resourceExchangeName", RESOURCE_EXCHANGE_NAME);
        ReflectionTestUtils.setField(rm, "resourceExchangeType", "topic");
        ReflectionTestUtils.setField(rm, "resourceExchangeDurable", true);
        ReflectionTestUtils.setField(rm, "resourceExchangeAutodelete", false);
        ReflectionTestUtils.setField(rm, "resourceExchangeInternal", false);

        ReflectionTestUtils.setField(rm, "federationExchangeName", FEDERATION_EXCHANGE_NAME);
        ReflectionTestUtils.setField(rm, "federationExchangeType", "topic");
        ReflectionTestUtils.setField(rm, "federationExchangeDurable", true);
        ReflectionTestUtils.setField(rm, "federationExchangeAutodelete", false);
        ReflectionTestUtils.setField(rm, "federationExchangeInternal", false);

        ReflectionTestUtils.setField(rm, "informationModelExchangeName", INFORMATION_MODEL_EXCHANGE_NAME);
        ReflectionTestUtils.setField(rm, "informationModelExchangeType", "topic");
        ReflectionTestUtils.setField(rm, "informationModelExchangeDurable", true);
        ReflectionTestUtils.setField(rm, "informationModelExchangeAutodelete", false);
        ReflectionTestUtils.setField(rm, "informationModelExchangeInternal", false);

        ReflectionTestUtils.setField(rm, "platformCreationRequestedRoutingKey", PLATFORM_CREATION_REQUESTED_RK);
        ReflectionTestUtils.setField(rm, "platformModificationRequestedRoutingKey", PLATFORM_MODIFICATION_REQUESTED_RK);
        ReflectionTestUtils.setField(rm, "platformRemovalRequestedRoutingKey", PLATFORM_REMOVAL_REQUESTED_RK);
        ReflectionTestUtils.setField(rm, "resourceCreationRequestedRoutingKey", RESOURCE_CREATION_REQUESTED_RK);
        ReflectionTestUtils.setField(rm, "resourceModificationRequestedRoutingKey", RESOURCE_MODIFICATION_REQUESTED_RK);
        ReflectionTestUtils.setField(rm, "resourceRemovalRequestedRoutingKey", RESOURCE_REMOVAL_REQUESTED_RK);

        ReflectionTestUtils.setField(rm, "federationCreationRequestedRoutingKey", FEDERATION_CREATION_REQUESTED_RK);
        ReflectionTestUtils.setField(rm, "federationModificationRequestedRoutingKey", FEDERATION_MODIFICATION_REQUESTED_RK);
        ReflectionTestUtils.setField(rm, "federationRemovalRequestedRoutingKey", FEDERATION_REMOVAL_REQUESTED_RK);

        ReflectionTestUtils.setField(rm, "informationModelCreationRequestedRoutingKey", INFORMATION_MODEL_CREATION_REQUESTED_RK);
        ReflectionTestUtils.setField(rm, "informationModelModificationRequestedRoutingKey", INFORMATION_MODEL_MODIFICATION_REQUESTED_RK);
        ReflectionTestUtils.setField(rm, "informationModelRemovalRequestedRoutingKey", INFORMATION_MODEL_REMOVAL_REQUESTED_RK);
        ReflectionTestUtils.setField(rm, "rdfInformationModelValidationRequestedRoutingKey", INFORMATION_MODEL_VALIDATION_REQUESTED_RK);
        ReflectionTestUtils.setField(rm, "informationModelRemovedRoutingKey", "not_important_RK");
        ReflectionTestUtils.setField(rm, "informationModelsRequestedRoutingKey", GET_ALL_INFORMATION_MODELS_REQUESTED_RK);

        ReflectionTestUtils.setField(rm, "platformCreatedRoutingKey", PLATFORM_CREATED_ROUTING_KEY);
        ReflectionTestUtils.setField(rm, "platformRemovedRoutingKey", PLATFORM_REMOVED_ROUTING_KEY);
        ReflectionTestUtils.setField(rm, "platformModifiedRoutingKey", PLATFORM_MODIFIED_ROUTING_KEY);
        ReflectionTestUtils.setField(rm, "resourceCreatedRoutingKey", RESOURCE_CREATED_ROUTING_KEY);
        ReflectionTestUtils.setField(rm, "resourceRemovedRoutingKey", RESOURCE_REMOVED_ROUTING_KEY);
        ReflectionTestUtils.setField(rm, "resourceModifiedRoutingKey", RESOURCE_MODIFIED_ROUTING_KEY);

        ReflectionTestUtils.setField(rm, "federationsRequestedRoutingKey", GET_ALL_FEDERATIONS_RK);
        ReflectionTestUtils.setField(rm, "federationRequestedRoutingKey", GET_FEDERATION_FOR_PLATFORM_RK);

        ReflectionTestUtils.setField(rm, "aamExchangeName", AAM_EXCHANGE_NAME);

        ReflectionTestUtils.setField(rm, "platformResourcesRequestedRoutingKey", RESOURCES_FOR_PLATFORM_REQUESTED_RK);
        ReflectionTestUtils.setField(rm, "platformDetailsRequestedRoutingKey", PLATFORM_DETAILS_REQUESTED_RK);

        ReflectionTestUtils.setField(rm, "jsonResourceTranslationRequestedRoutingKey", RESOURCE_TRANSLATION_REQUESTED_RK);
        ReflectionTestUtils.setField(rm, "rdfResourceValidationRequestedRoutingKey", RESOURCE_VALIDATION_REQUESTED_RK);
        ReflectionTestUtils.invokeMethod(rm, "init");
    }


    public static void deleteRabbitQueues(RabbitManager rm) {
        try {
            Connection conn = rm.getConnection();
            if (conn != null && conn.isOpen()) {
                deleteQueue(conn,
                        PLATFORM_CREATION_REQUESTED_RK, PLATFORM_MODIFICATION_REQUESTED_RK,
                        PLATFORM_REMOVAL_REQUESTED_RK, RESOURCE_CREATION_REQUESTED_RK,
                        RESOURCE_MODIFICATION_REQUESTED_RK, RESOURCE_REMOVAL_REQUESTED_RK,
                        RESOURCE_CREATION_REQUESTED_QUEUE, RESOURCE_MODIFICATION_REQUESTED_QUEUE,
                        RESOURCE_REMOVAL_REQUESTED_QUEUE, PLATFORM_CREATION_REQUESTED_QUEUE,
                        PLATFORM_MODIFICATION_REQUESTED_QUEUE, PLATFORM_REMOVAL_REQUESTED_QUEUE,
                        RESOURCES_FOR_PLATFORM_REQUESTED_RK, PLATFORM_RESOURCES_REQUESTED_QUEUE,
                        INFORMATION_MODEL_CREATION_REQUESTED_QUEUE, INFORMATION_MODEL_VALIDATION_REQUESTED_RK,
                        INFORMATION_MODEL_REMOVAL_REQUESTED_QUEUE, INFORMATION_MODEL_MODIFICATION_REQUESTED_QUEUE,
                        GET_ALL_INFORMATION_MODELS_REQUESTED_RK, TEMP_QUEUE);
                conn.close();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private static void deleteQueue(Connection conn, String... strings) throws IOException, TimeoutException {
        Channel cl = conn.createChannel();
        for (String s :
                strings) {
            cl.queueDelete(s);
        }
        cl.close();
    }

    public static Platform generateSymbiotePlatformA() {
        Platform platform = new Platform();
        platform.setId(PLATFORM_A_ID);
        platform.setDescription(Arrays.asList(PLATFORM_A_DESCRIPTION));
        platform.setName(PLATFORM_A_NAME);
        InterworkingService interworkingService = new InterworkingService();
        interworkingService.setInformationModelId(INFORMATION_MODEL_ID_A);
        interworkingService.setUrl(INTERWORKING_SERVICE_URL_A);
        platform.setInterworkingServices(Arrays.asList(interworkingService));
        platform.setRdf("http://www.symbIoTe.com/");
        platform.setRdfFormat(RDFFormat.JSONLD);
        return platform;
    }

    public static Platform generatePlatformB() {
        Platform platform = new Platform();
        platform.setId(PLATFORM_B_ID);
        platform.setDescription(Arrays.asList(PLATFORM_B_DESCRIPTION));
        platform.setName(PLATFORM_B_NAME);
        InterworkingService interworkingService = new InterworkingService();
        interworkingService.setInformationModelId(INFORMATION_MODEL_ID_B);
        interworkingService.setUrl(INTERWORKING_SERVICE_URL_B);
        platform.setInterworkingServices(Arrays.asList(interworkingService));
        platform.setRdf("http://www.symbIoTe.com/");
        platform.setRdfFormat(RDFFormat.JSONLD);
        return platform;
    }

    public static CoreResource generateCoreResourceWithoutId() throws InvalidArgumentsException {
        return generateCoreResourceSensor(RESOURCE_101_LABEL, RESOURCE_101_COMMENT, null, INTERWORKING_SERVICE_URL_B,
                RESOURCE_STATIONARY_FILENAME, RDFFormat.JSONLD,
                new SingleTokenAccessPolicySpecifier("mock", "mock"));
    }

    public static Resource generateCoreResourceSensorWithoutId() {
        return generateCoreResourceSensor(RESOURCE_101_LABEL, RESOURCE_101_COMMENT, null, INTERWORKING_SERVICE_URL_B);
    }

    public static CoreResource addIdToCoreResource(CoreResource coreResource) {
        coreResource.setId(RESOURCE_101_ID);
        return coreResource;
    }

    public static Resource addIdToResource(Resource resource) {
        resource.setId(RESOURCE_101_ID);
        return resource;
    }

    public static CoreResource generateStationarySensor() throws InvalidArgumentsException {
        return generateCoreResourceSensor(RESOURCE_STATIONARY_LABEL, RESOURCE_STATIONARY_COMMENT, null,
                PLATFORM_A_URL, RESOURCE_STATIONARY_FILENAME, RDFFormat.JSONLD,
                new SingleTokenAccessPolicySpecifier("mock", "mock"));
    }

    public static Resource generateStationaryResourceSensor() throws InvalidArgumentsException {
        return generateResourceSensor(RESOURCE_STATIONARY_LABEL, RESOURCE_STATIONARY_COMMENT, null,
                PLATFORM_A_URL);
    }

    public static CoreResource generateModifiedStationarySensor() throws InvalidArgumentsException {
        return generateCoreResourceSensor(RESOURCE_STATIONARY_LABEL_MODIFIED, RESOURCE_STATIONARY_COMMENT, RESOURCE_STATIONARY_ID,
                PLATFORM_A_URL, RESOURCE_STATIONARY_FILENAME_MODIFIED, RDFFormat.JSONLD,
                new SingleTokenAccessPolicySpecifier("mock", "mock"));
    }

    public static CoreResource generateCoreResourceSensor(String label, String comment, String id, String serviceUrl,
                                                          String rdfFilename, RDFFormat format, IAccessPolicySpecifier specifier) {
        CoreResource res = new CoreResource();
        res.setDescription(Arrays.asList(comment));
        res.setName(label);
        res.setId(id);
        res.setInterworkingServiceURL(serviceUrl);
        res.setRdf(rdfFilename);
        res.setRdfFormat(format);
        res.setType(CoreResourceType.STATIONARY_SENSOR);
        res.setPolicySpecifier(specifier);
        return res;
    }

    public static Resource generateResourceSensor(String label, String comment, String id, String serviceUrl) {
        Resource res = new StationarySensor();
        res.setDescription(Arrays.asList(comment));
        res.setName(label);
        res.setId(id);
        res.setInterworkingServiceURL(serviceUrl);
        return res;
    }

    public static Resource generateCoreResourceSensor(String label, String comment, String id, String interworkingServiceUrl) {
        CoreResource res = new CoreResource();
        res.setDescription(Arrays.asList(comment));
        res.setName(label);
        res.setId(id);
        res.setType(CoreResourceType.ACTUATOR);
        res.setInterworkingServiceURL(interworkingServiceUrl);
        return res;
    }

    public static CoreResourceRegistryRequest generateCoreResourceRegistryRequestBasicType(Resource resource1, Resource resource2)
            throws JsonProcessingException, InvalidArgumentsException {

        Map<String, Resource> resourceMap = new HashMap<>();
        resourceMap.put("1", resource1);
        resourceMap.put("2", resource2);

        ObjectMapper mapper = new ObjectMapper();
        String resources = mapper.writerFor(new TypeReference<Map<String, Resource>>() {
        }).writeValueAsString(resourceMap);

        CoreResourceRegistryRequest coreResourceRegistryRequest = new CoreResourceRegistryRequest();
        coreResourceRegistryRequest.setPlatformId(PLATFORM_B_ID);
        coreResourceRegistryRequest.setSecurityRequest(SECURITY_REQUEST);
        coreResourceRegistryRequest.setDescriptionType(DescriptionType.BASIC);
        coreResourceRegistryRequest.setBody(resources);

        Map<String, IAccessPolicySpecifier> filteringPolicies = new HashMap<>();
        Map<String, String> claims = new HashMap<>();
        claims.put("a", "a");
        filteringPolicies.put("1", new SingleTokenAccessPolicySpecifier(AccessPolicyType.PUBLIC, null));
        filteringPolicies.put("2", new SingleTokenAccessPolicySpecifier(AccessPolicyType.PUBLIC, null));

        coreResourceRegistryRequest.setFilteringPolicies(filteringPolicies);

        return coreResourceRegistryRequest;
    }

    public static CoreResourceRegistryRequest generateCoreResourceRegistryRequestRdfType(Resource resource1, Resource resource2)
            throws JsonProcessingException {
        RDFResourceRegistryRequest request = new RDFResourceRegistryRequest();
        RDFInfo rdfInfo = new RDFInfo();
        rdfInfo.setRdf("some rdf");
        rdfInfo.setRdfFormat(RDFFormat.JSONLD);
        request.setBody(rdfInfo);

        ObjectMapper mapper = new ObjectMapper();

        CoreResourceRegistryRequest coreResourceRegistryRequest = new CoreResourceRegistryRequest();
        coreResourceRegistryRequest.setPlatformId(PLATFORM_B_ID);
        coreResourceRegistryRequest.setSecurityRequest(SECURITY_REQUEST);
        coreResourceRegistryRequest.setDescriptionType(DescriptionType.RDF);
        coreResourceRegistryRequest.setBody(mapper.writeValueAsString(request));

        return coreResourceRegistryRequest;
    }

    private static FederationMember generateMemberA() {
        FederationMember federationMember = new FederationMember();
        federationMember.setPlatformId(PLATFORM_A_ID);
        federationMember.setInterworkingServiceURL(INTERWORKING_SERVICE_URL_A);
        return federationMember;
    }

    private static FederationMember generateMemberB() {
        FederationMember federationMember = new FederationMember();
        federationMember.setPlatformId(PLATFORM_B_ID);
        federationMember.setInterworkingServiceURL(INTERWORKING_SERVICE_URL_B);
        return federationMember;
    }

    public static Federation generateFederationA() {
        Federation federation = new Federation();

        federation.setName("FederationA");
        federation.setId("A");
        federation.setPublic(true);
        //FIXME set null for SLA in order to be able to build
        federation.setSlaConstraints(null);
        federation.setMembers(Arrays.asList(generateMemberA()));

        return federation;
    }

    public static Federation generateFederationB() {
        Federation federation = new Federation();
        federation.setName("FederationB");
        federation.setId("B");
        federation.setPublic(false);
        //FIXME set null for SLA in order to be able to build
        federation.setSlaConstraints(null);
        federation.setMembers(Arrays.asList(generateMemberB()));
        return federation;
    }

    public static InformationModel generateInformationModelWithoutID() {
        InformationModel informationModel = new InformationModel();
        informationModel.setName("IM mocked name");
        informationModel.setOwner("Some mocked owner");
        informationModel.setUri("Some Uri/");
        return informationModel;
    }

    public static InformationModel generateInformationModelFull() {
        InformationModel informationModel = new InformationModel();
        informationModel.setId("SOME ID");
        informationModel.setRdf("Some mocked RDF");
        informationModel.setName("IM mocked name");
        informationModel.setOwner("Some mocked owner");
        informationModel.setRdfFormat(RDFFormat.JSONLD);
        informationModel.setUri("Some Uri/");
        return informationModel;
    }
}