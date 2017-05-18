package eu.h2020.symbiote;

import eu.h2020.symbiote.core.model.Platform;
import eu.h2020.symbiote.core.model.RDFFormat;
import eu.h2020.symbiote.core.model.internal.CoreResource;
import eu.h2020.symbiote.core.model.resources.Resource;
import eu.h2020.symbiote.model.RegistryPlatform;

import java.util.Arrays;

/**
 * Created by Mael on 23/01/2017.
 */
public class TestSetupConfig {

    public static final String MOCKED_TOKEN = "eyJhbGciOiJFUzI1NiJ9.eyJTWU1CSU9URV9Pd25lZFBsYXRmb3JtIjoidGVzdDFQbGF0IiwiU1lNQklPVEVfUm9sZSI6IlBMQVRGT1JNX09XTkVSIiwidHR5cCI6IkNPUkUiLCJzdWIiOiJUZXN0MSIsImlwayI6Ik1Ga3dFd1lIS29aSXpqMENBUVlJS29aSXpqMERBUWNEUWdBRXI2OXZEV0pzT3duYW9CM0FDRVJPdnRETWtmNjh5aUd6c3lmR1duOWZnSnJHT2ZoTGJkM2Q5NEMxay9TUW1hRWdTakVOUWI4ZEljME9FYWRSRzFtWGR3PT0iLCJpc3MiOiJTeW1iSW9UZV9Db3JlX0FBTSIsImV4cCI6MTQ5NTExNTExMiwiaWF0IjoxNDk1MTExNTEyLCJqdGkiOiI0ODY1MTQ2NTIiLCJzcGsiOiJNRmt3RXdZSEtvWkl6ajBDQVFZSUtvWkl6ajBEQVFjRFFnQUV5RVJnYXhnQUUzSmUwand6RDBMdjluby9wQVQyUjV0Njc0MzJrcTQxaHNGTWZRSXdyQ212RVRZbW9lekgxYWU2WSsyV0ZCQVdiMHhiMGVUd1ozeWZZdz09In0.M7fIyUsr0GfUN7IyaKMG9T41dabvkFS_UNGeq3RCzzyzA-ttEdHnDNV3oImX7eMS_vvp-prBlnSsVl0dhL131Q";

    public static final String PLATFORM_EXCHANGE_NAME = "symbIoTe.platform";
    public static final String PLATFORM_CREATION_REQUESTED = "symbIoTe.platform.creationRequested";
    public static final String PLATFORM_MODIFICATION_REQUESTED = "symbIoTe.platform.modificationRequested";
    public static final String PLATFORM_REMOVAL_REQUESTED = "symbIoTe.platform.removalRequested";
    public static final String RESOURCE_EXCHANGE_NAME = "symbIoTe.resource";
    public static final String RESOURCE_CREATION_REQUESTED = "symbIoTe.resource.creationRequested";
    public static final String RESOURCE_MODIFICATION_REQUESTED = "symbIoTe.resource.modificationRequested";
    public static final String RESOURCE_REMOVAL_REQUESTED = "symbIoTe.resource.removalRequested";

    public static final String PLATFORM_CREATED_ROUTING_KEY = "symbIoTe.platform.created";
    public static final String PLATFORM_MODIFIED_ROUTING_KEY = "symbIoTe.platform.modified";
    public static final String PLATFORM_REMOVED_ROUTING_KEY = "symbIoTe.platform.removed";
    public static final String RESOURCE_CREATED_ROUTING_KEY = "symbIoTe.resource.created";
    public static final String RESOURCE_REMOVED_ROUTING_KEY = "symbIoTe.resource.removed";
    public static final String RESOURCE_MODIFIED_ROUTING_KEY = "symbIoTe.resource.modified";

    public static final String PLATFORM_REMOVAL_REQUESTED_QUEUE = "symbIoTe-Registry-platformRemovalRequestedQueue";
    public static final String RESOURCE_CREATION_REQUESTED_QUEUE = "symbIoTe-Registry-resourceCreationRequestedQueue";
    public static final String RESOURCE_MODIFICATION_REQUESTED_QUEUE = "symbIoTe-Registry-resourceModificationRequestedQueue";
    public static final String PLATFORM_CREATION_REQUESTED_QUEUE = "symbIoTe-Registry-platformCreationRequestedQueue";
    public static final String PLATFORM_MODIFICATION_REQUESTED_QUEUE = "symbIoTe-Registry-platformModificationRequestedQueue";
    public static final String RESOURCE_REMOVAL_REQUESTED_QUEUE = "symbIoTe-Registry-resourceRemovalRequestedQueue";

    public static final String PLATFORM_A_ID = "1";
    public static final String PLATFORM_A_NAME = "Platform1";
    public static final String PLATFORM_A_MODEL_ID = "11";
    public static final String PLATFORM_A_DESCRIPTION = "11desc";
    public static final String PLATFORM_A_FILENAME = "/platformA.ttl";
    //RDF URIs
    public static final String PLATFORM_A_URI = "http://www.symbiote-h2020.eu/ontology/platforms/1";
    public static final String PLATFORM_A_SERVICE_URI = "http://www.symbiote-h2020.eu/ontology/platforms/1/service/somehost1.com/resourceAccessProxy";

    //LINK to Interworking Service
    public static final String PLATFORM_A_URL = "http://somehost1.com/resourceAccessProxy";

    public static final String PLATFORM_A_NAME_UPDATED = "Platform1Updated";
    public static final String PLATFORM_A_MODEL_ID_UPDATED = "11Updated";
    public static final String PLATFORM_A_DESCRIPTION_UPDATED = "11descUpdated";
    public static final String PLATFORM_A_URL_UPDATED = "http://somehost1.com/resourceAccessProxyUpdated";

    public static final String PLATFORM_B_ID = "2";
    public static final String PLATFORM_B_NAME = "Platform2";
    public static final String PLATFORM_B_MODEL_ID = "21";
    public static final String PLATFORM_B_DESCRIPTION = "21desc";
    public static final String PLATFORM_B_FILENAME = "/platformB.ttl";
    public static final String PLATFORM_B_URI = "http://www.symbiote-h2020.eu/ontology/platforms/2";
    public static final String PLATFORM_B_SERVICE_URI = "http://www.symbiote-h2020.eu/ontology/platforms/2/service/somehost2.com/resourceAccessProxy";

    public static final String RESOURCE_PREDICATE = "http://www.symbiote-h2020.eu/ontology/resources/";

    public static final String RESOURCE_101_FILENAME = "/resource101.ttl";
    public static final String RESOURCE_101_URI = RESOURCE_PREDICATE + "101";
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


    public static Platform generatePlatformA() {
        Platform platform = new Platform();
        platform.setPlatformId(PLATFORM_A_ID);
        platform.setInformationModelId(PLATFORM_A_MODEL_ID);
        platform.setDescription(PLATFORM_A_DESCRIPTION);
        platform.setName(PLATFORM_A_NAME);
        platform.setUrl(PLATFORM_A_URL);
        return platform;
    }

    public static Platform generatePlatformAUpdate() {
        Platform registryPlatform = new Platform();
        registryPlatform.setPlatformId(PLATFORM_A_ID);
        registryPlatform.setInformationModelId(PLATFORM_A_MODEL_ID_UPDATED);
        registryPlatform.setDescription(PLATFORM_A_DESCRIPTION_UPDATED);
        registryPlatform.setName(PLATFORM_A_NAME_UPDATED);
        registryPlatform.setUrl(PLATFORM_A_URL_UPDATED);
        return registryPlatform;
    }

    public static RegistryPlatform genereteRegistryPlatformB(){
        RegistryPlatform platform = new RegistryPlatform ();
        platform.setId(PLATFORM_B_ID);
        platform.setLabels(Arrays.asList(PLATFORM_B_NAME));
        platform.setComments(Arrays.asList(PLATFORM_B_DESCRIPTION));
        platform.setBody("http://www.symbIoTe.com/");
        platform.setRdfFormat("some RDF Format");
        return platform;
    }

    public static CoreResource generateCoreResource() {
        return generateSensor(RESOURCE_101_LABEL, RESOURCE_101_COMMENT, RESOURCE_101_ID, PLATFORM_A_URL,
                RESOURCE_STATIONARY_FILENAME, RDFFormat.JSONLD);
    }

    public static Resource generateResource(){
        return generateSensor(RESOURCE_101_LABEL, RESOURCE_101_COMMENT, RESOURCE_101_ID, PLATFORM_A_URL);
    }

    public static CoreResource generateStationarySensor() {
        return generateSensor(RESOURCE_STATIONARY_LABEL, RESOURCE_STATIONARY_COMMENT, RESOURCE_STATIONARY_ID,
                PLATFORM_A_URL, RESOURCE_STATIONARY_FILENAME, RDFFormat.JSONLD);
    }

    public static CoreResource generateModifiedStationarySensor() {
        return generateSensor(RESOURCE_STATIONARY_LABEL_MODIFIED, RESOURCE_STATIONARY_COMMENT, RESOURCE_STATIONARY_ID,
                PLATFORM_A_URL, RESOURCE_STATIONARY_FILENAME_MODIFIED, RDFFormat.JSONLD);
    }

    public static CoreResource generateSensor(String label, String comment, String id, String serviceUrl,
                                              String rdfFilename, RDFFormat format) {
        CoreResource res = new CoreResource();
        res.setComments(Arrays.asList(comment));
        res.setLabels(Arrays.asList(label));
        res.setId(id);
        res.setInterworkingServiceURL(serviceUrl);
        res.setRdf(rdfFilename);
        res.setRdfFormat(format);
        return res;
    }

    public static Resource generateSensor(String label, String comment, String id, String serviceUrl) {
        CoreResource res = new CoreResource();
        res.setComments(Arrays.asList(comment));
        res.setLabels(Arrays.asList(label));
        res.setId(id);
        res.setInterworkingServiceURL(serviceUrl);
        return res;
    }

}
