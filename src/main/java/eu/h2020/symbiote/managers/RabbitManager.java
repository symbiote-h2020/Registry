package eu.h2020.symbiote.managers;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.rabbitmq.client.*;
import eu.h2020.symbiote.cloud.model.ssp.SspRegInfo;
import eu.h2020.symbiote.core.internal.CoreResourceRegisteredOrModifiedEventPayload;
import eu.h2020.symbiote.core.internal.DescriptionType;
import eu.h2020.symbiote.messaging.consumers.federation.*;
import eu.h2020.symbiote.messaging.consumers.informationModel.*;
import eu.h2020.symbiote.messaging.consumers.platform.*;
import eu.h2020.symbiote.messaging.consumers.resource.*;
import eu.h2020.symbiote.messaging.consumers.sspResource.SspResourceCreationRequestConsumer;
import eu.h2020.symbiote.messaging.consumers.sspResource.SspResourceModificationRequestConsumer;
import eu.h2020.symbiote.messaging.consumers.sspResource.SspResourceRemovalRequestConsumer;
import eu.h2020.symbiote.messaging.consumers.sspResource.SspResourceTranslationResponseConsumer;
import eu.h2020.symbiote.messaging.consumers.sspSdev.SspSdevCreationRequestConsumer;
import eu.h2020.symbiote.messaging.consumers.sspSdev.SspSdevModificationRequestConsumer;
import eu.h2020.symbiote.messaging.consumers.sspSdev.SspSdevRemovalRequestConsumer;
import eu.h2020.symbiote.messaging.consumers.ssp_smartSpace.SspCreationRequestConsumer;
import eu.h2020.symbiote.messaging.consumers.ssp_smartSpace.SspModificationRequestConsumer;
import eu.h2020.symbiote.messaging.consumers.ssp_smartSpace.SspRemovalRequestConsumer;
import eu.h2020.symbiote.model.RegistryOperationType;
import eu.h2020.symbiote.model.cim.Resource;
import eu.h2020.symbiote.model.mim.Federation;
import eu.h2020.symbiote.model.mim.InformationModel;
import eu.h2020.symbiote.model.mim.Platform;
import eu.h2020.symbiote.model.mim.SmartSpace;
import eu.h2020.symbiote.security.accesspolicies.common.IAccessPolicySpecifier;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;

import javax.annotation.PreDestroy;
import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.TimeoutException;

import static eu.h2020.symbiote.core.internal.DescriptionType.BASIC;
import static eu.h2020.symbiote.core.internal.DescriptionType.RDF;

/**
 * Bean used to manage internal communication using RabbitMQ.
 * It is responsible for declaring exchanges and using routing keys from centralized config server.
 * <p>
 * Created by mateuszl
 */
@Component
public class RabbitManager {

    /* Queues names */
    private static final String RDF_RESOURCE_VALIDATION_REQUESTED_QUEUE = "rdfResourceValidationRequestedQueue";
    private static final String JSON_RESOURCE_TRANSLATION_REQUESTED_QUEUE = "jsonResourceTranslationRequestedQueue";
    private static final String RESOURCE_CREATION_REQUESTED_QUEUE = "symbIoTe-Registry-resourceCreationRequestedQueue";
    private static final String RESOURCE_MODIFICATION_REQUESTED_QUEUE = "symbIoTe-Registry-resourceModificationRequestedQueue";
    private static final String RESOURCE_REMOVAL_REQUESTED_QUEUE = "symbIoTe-Registry-resourceRemovalRequestedQueue";
    private static final String RESOURCE_CLEAR_DATA_REQUESTED_QUEUE = "symbIoTe-Registry-resourceClearDataRequestedQueue";
    private static final String PLATFORM_CREATION_REQUESTED_QUEUE = "symbIoTe-Registry-platformCreationRequestedQueue";
    private static final String PLATFORM_MODIFICATION_REQUESTED_QUEUE = "symbIoTe-Registry-platformModificationRequestedQueue";
    private static final String PLATFORM_REMOVAL_REQUESTED_QUEUE = "symbIoTe-Registry-platformRemovalRequestedQueue";
    private static final String INFORMATION_MODEL_CREATION_REQUESTED_QUEUE = "symbIoTe-Registry-informationModelCreationRequestedQueue";
    private static final String INFORMATION_MODEL_MODIFICATION_REQUESTED_QUEUE = "symbIoTe-Registry-informationModelModificationRequestedQueue";
    private static final String INFORMATION_MODEL_REMOVAL_REQUESTED_QUEUE = "symbIoTe-Registry-informationModelRemovalRequestedQueue";
    private static final String PLATFORM_RESOURCES_REQUESTED_QUEUE = "symbIoTe-Registry-platformResourcesRequestedQueue";
    private static final String INFORMATION_MODELS_REQUESTED_QUEUE = "symbIoTe-Registry-informationModelsRequestedQueue";
    private static final String FEDERATION_CREATION_REQUESTED_QUEUE = "symbIoTe-Registry-federationCreationRequestedQueue";
    private static final String FEDERATION_MODIFICATION_REQUESTED_QUEUE = "symbIoTe-Registry-federationModificationRequestedQueue";
    private static final String FEDERATION_REMOVAL_REQUESTED_QUEUE = "symbIoTe-Registry-federationRemovalRequestedQueue";
    private static final String FEDERATION_REQUESTED_QUEUE = "symbIoTe-Registry-federationRequestedQueue";
    private static final String FEDERATIONS_REQUESTED_QUEUE = "symbIoTe-Registry-federationsRequestedQueue";
    private static final String SSP_CREATION_REQUESTED_QUEUE = "symbIoTe-Registry-sspCreationRequestedQueue";
    private static final String SSP_MODIFICATION_REQUESTED_QUEUE = "symbIoTe-Registry-sspModificationRequestedQueue";
    private static final String SSP_REMOVAL_REQUESTED_QUEUE = "symbIoTe-Registry-sspRemovalRequestedQueue";
    private static final String SSP_SDEV_CREATION_REQUESTED_QUEUE = "symbIoTe-Registry-sspSdevCreationRequestedQueue";
    private static final String SSP_SDEV_MODIFICATION_REQUESTED_QUEUE = "symbIoTe-Registry-sspSdevModificationRequestedQueue";
    private static final String SSP_SDEV_REMOVAL_REQUESTED_QUEUE = "symbIoTe-Registry-sspSdevRemovalRequestedQueue";
    private static final String SSP_RESOURCE_CREATION_REQUESTED_QUEUE = "symbIoTe-Registry-sspSdevResourceCreationRequestedQueue";
    private static final String SSP_RESOURCE_MODIFICATION_REQUESTED_QUEUE = "symbIoTe-Registry-sspSdevResourceModificationRequestedQueue";
    private static final String SSP_RESOURCE_REMOVAL_REQUESTED_QUEUE = "symbIoTe-Registry-sspSdevResourceRemovalRequestedQueue";
    private static final String PLATFORM_DETAILS_REQUESTED_QUEUE = "symbIoTe-Registry-platformDetailsRequestedQueue";
    private static final String ERROR_OCCURRED_WHEN_PARSING_OBJECT_TO_JSON = "Error occurred when parsing Resource object JSON: ";
    /* Queues names */

    private static Log log = LogFactory.getLog(RabbitManager.class);
    private AuthorizationManager authorizationManager;
    private RepositoryManager repositoryManager;
    private Connection connection;
    private Channel rpcChannel;
    private Channel channel;

    /* Connection Params */
    @Value("${rabbit.host}")
    private String rabbitHost;
    @Value("${rabbit.username}")
    private String rabbitUsername;
    @Value("${rabbit.password}")
    private String rabbitPassword;
    /* Connection Params */

    /* Exchanges Params */
    @Value("${rabbit.exchange.platform.name}")
    private String platformExchangeName;
    @Value("${rabbit.exchange.platform.type}")
    private String platformExchangeType;
    @Value("${rabbit.exchange.platform.durable}")
    private boolean plaftormExchangeDurable;
    @Value("${rabbit.exchange.platform.autodelete}")
    private boolean platformExchangeAutodelete;
    @Value("${rabbit.exchange.platform.internal}")
    private boolean platformExchangeInternal;

    @Value("${rabbit.exchange.platform.name}")
    private String informationModelExchangeName;
    @Value("${rabbit.exchange.platform.type}")
    private String informationModelExchangeType;
    @Value("${rabbit.exchange.platform.durable}")
    private boolean informationModelExchangeDurable;
    @Value("${rabbit.exchange.platform.autodelete}")
    private boolean informationModelExchangeAutodelete;
    @Value("${rabbit.exchange.platform.internal}")
    private boolean informationModelExchangeInternal;

    @Value("${rabbit.exchange.federation.name}")
    private String federationExchangeName;
    @Value("${rabbit.exchange.federation.type}")
    private String federationExchangeType;
    @Value("${rabbit.exchange.federation.durable}")
    private boolean federationExchangeDurable;
    @Value("${rabbit.exchange.federation.autodelete}")
    private boolean federationExchangeAutodelete;
    @Value("${rabbit.exchange.federation.internal}")
    private boolean federationExchangeInternal;

    @Value("${rabbit.exchange.ssp.name}")
    private String sspExchangeName;
    @Value("${rabbit.exchange.ssp.type}")
    private String sspExchangeType;
    @Value("${rabbit.exchange.ssp.durable}")
    private boolean sspExchangeDurable;
    @Value("${rabbit.exchange.ssp.autodelete}")
    private boolean sspExchangeAutodelete;
    @Value("${rabbit.exchange.ssp.internal}")
    private boolean sspExchangeInternal;

    @Value("${rabbit.exchange.aam.name}")
    private String aamExchangeName;
    /* Exchanges Params */

    /* Platform messages Params */
    @Value("${rabbit.routingKey.platform.creationRequested}")
    private String platformCreationRequestedRoutingKey;
    @Value("${rabbit.routingKey.platform.created}")
    private String platformCreatedRoutingKey;
    @Value("${rabbit.routingKey.platform.removalRequested}")
    private String platformRemovalRequestedRoutingKey;
    @Value("${rabbit.routingKey.platform.removed}")
    private String platformRemovedRoutingKey;
    @Value("${rabbit.routingKey.platform.modificationRequested}")
    private String platformModificationRequestedRoutingKey;
    @Value("${rabbit.routingKey.platform.modified}")
    private String platformModifiedRoutingKey;
    @Value("${rabbit.routingKey.platform.resourcesRequested}")
    private String platformResourcesRequestedRoutingKey;
    @Value("${rabbit.routingKey.platform.platformDetailsRequested}")
    private String platformDetailsRequestedRoutingKey;
    /* Platform messages Params */

    /* Resource messages Params */
    @Value("${rabbit.exchange.resource.name}")
    private String resourceExchangeName;
    @Value("${rabbit.exchange.resource.type}")
    private String resourceExchangeType;
    @Value("${rabbit.exchange.resource.durable}")
    private boolean resourceExchangeDurable;
    @Value("${rabbit.exchange.resource.autodelete}")
    private boolean resourceExchangeAutodelete;
    @Value("${rabbit.exchange.resource.internal}")
    private boolean resourceExchangeInternal;
    @Value("${rabbit.routingKey.resource.creationRequested}")
    private String resourceCreationRequestedRoutingKey;
    @Value("${rabbit.routingKey.resource.created}")
    private String resourceCreatedRoutingKey;
    @Value("${rabbit.routingKey.resource.removalRequested}")
    private String resourceRemovalRequestedRoutingKey;
    @Value("${rabbit.routingKey.resource.clearDataRequested}")
    private String resourceClearDataRequestedRoutingKey;
    @Value("${rabbit.routingKey.resource.removed}")
    private String resourceRemovedRoutingKey;
    @Value("${rabbit.routingKey.resource.modificationRequested}")
    private String resourceModificationRequestedRoutingKey;
    @Value("${rabbit.routingKey.resource.modified}")
    private String resourceModifiedRoutingKey;
    /* Resource messages Params */

    /* Information Model messages Params */
    @Value("${rabbit.routingKey.platform.model.creationRequested}")
    private String informationModelCreationRequestedRoutingKey;
    @Value("${rabbit.routingKey.platform.model.created}")
    private String informationModelCreatedRoutingKey;
    @Value("${rabbit.routingKey.platform.model.removalRequested}")
    private String informationModelRemovalRequestedRoutingKey;
    @Value("${rabbit.routingKey.platform.model.removed}")
    private String informationModelRemovedRoutingKey;
    @Value("${rabbit.routingKey.platform.model.modificationRequested}")
    private String informationModelModificationRequestedRoutingKey;
    @Value("${rabbit.routingKey.platform.model.modified}")
    private String informationModelModifiedRoutingKey;
    @Value("${rabbit.routingKey.platform.model.allInformationModelsRequested}")
    private String informationModelsRequestedRoutingKey;
    /* Information Model messages Params */

    /* Federation messages Params */
    @Value("${rabbit.routingKey.federation.creationRequested}")
    private String federationCreationRequestedRoutingKey;
    @Value("${rabbit.routingKey.federation.created}")
    private String federationCreatedRoutingKey;
    @Value("${rabbit.routingKey.federation.modificationRequested}")
    private String federationModificationRequestedRoutingKey;
    @Value("${rabbit.routingKey.federation.modified}")
    private String federationModifiedRoutingKey;
    @Value("${rabbit.routingKey.federation.removalRequested}")
    private String federationRemovalRequestedRoutingKey;
    @Value("${rabbit.routingKey.federation.removed}")
    private String federationRemovedRoutingKey;
    @Value("${rabbit.routingKey.federation.getFederationForPlatform}")
    private String federationRequestedRoutingKey;
    @Value("${rabbit.routingKey.federation.getAllFederations}")
    private String federationsRequestedRoutingKey;
    /* Federation messages Params */

    /* Smart Space messages Params */
    @Value("${rabbit.routingKey.ssp.creationRequested}")
    private String sspCreationRequestedRoutingKey;
    @Value("${rabbit.routingKey.ssp.created}")
    private String sspCreatedRoutingKey;
    @Value("${rabbit.routingKey.ssp.modificationRequested}")
    private String sspModificationRequestedRoutingKey;
    @Value("${rabbit.routingKey.ssp.modified}")
    private String sspModifiedRoutingKey;
    @Value("${rabbit.routingKey.ssp.removalRequested}")
    private String sspRemovalRequestedRoutingKey;
    @Value("${rabbit.routingKey.ssp.removed}")
    private String sspRemovedRoutingKey;
    /* Smart Space messages Params */

    /* Smart Device messages Params */
    @Value("${rabbit.routingKey.ssp.sdev.creationRequested}")
    private String sspSdevCreationRequestedRoutingKey;
    @Value("${rabbit.routingKey.ssp.sdev.created}")
    private String sspSdevCreatedRoutingKey;
    @Value("${rabbit.routingKey.ssp.sdev.modificationRequested}")
    private String sspSdevModificationRequestedRoutingKey;
    @Value("${rabbit.routingKey.ssp.sdev.modified}")
    private String sspSdevModifiedRoutingKey;
    @Value("${rabbit.routingKey.ssp.sdev.removalRequested}")
    private String sspSdevRemovalRequestedRoutingKey;
    @Value("${rabbit.routingKey.ssp.sdev.removed}")
    private String sspSdevRemovedRoutingKey;
    /* Smart Device messages Params */

    /* Smart Space Resource messages Params */
    @Value("${rabbit.routingKey.ssp.sdev.resource.creationRequested}")
    private String sspSdevResourceCreationRequestedRoutingKey;
    @Value("${rabbit.routingKey.ssp.sdev.resource.created}")
    private String sspSdevResourceCreatedRoutingKey;
    @Value("${rabbit.routingKey.ssp.sdev.resource.modificationRequested}")
    private String sspResourceModificationRequestedRoutingKey;
    @Value("${rabbit.routingKey.ssp.sdev.resource.modified}")
    private String sspSdevResourceModifiedRoutingKey;
    @Value("${rabbit.routingKey.ssp.sdev.resource.removalRequested}")
    private String sspResourceRemovalRequestedRoutingKey;
    @Value("${rabbit.routingKey.ssp.sdev.resource.removed}")
    private String sspSdevResourceRemovedRoutingKey;
    /* Smart Space Resource messages Params */

    /* RDF translation/validation messages Params */
    @Value("${rabbit.routingKey.resource.instance.translationRequested}")
    private String jsonResourceTranslationRequestedRoutingKey; //dla JSONów
    @Value("${rabbit.routingKey.resource.instance.validationRequested}")
    private String rdfResourceValidationRequestedRoutingKey; //dla RDFów
    @Value("${rabbit.routingKey.platform.model.validationRequested}")
    private String rdfInformationModelValidationRequestedRoutingKey;
    /* RDF translation/validation messages Params */

    @Autowired
    public RabbitManager(RepositoryManager repositoryManager, @Lazy AuthorizationManager authorizationManager) {
        this.repositoryManager = repositoryManager;
        this.authorizationManager = authorizationManager;
    }

    private Channel getChannel() throws IOException {
        if (this.channel == null) {
            this.channel = this.connection.createChannel();
        }
        return channel;
    }

    /**
     * Initiates connection with Rabbit server using parameters from ConfigProperties
     *
     * @throws IOException
     * @throws TimeoutException
     */
    public Connection getConnection() throws TimeoutException {
        if (connection == null) {
            ConnectionFactory factory = new ConnectionFactory();
            factory.setHost(this.rabbitHost);
            factory.setUsername(this.rabbitUsername);
            factory.setPassword(this.rabbitPassword);
            try {
                this.connection = factory.newConnection();
            } catch (IOException e) {
                log.error(e);
            }
        }
        return this.connection;
    }

    /**
     * Method creates channel and declares Rabbit exchanges for Platform and Resources.
     * It triggers start of all consumers used in Registry communication.
     */
    public void init() {
        try {
            getConnection();
        } catch (TimeoutException e) {
            log.error(e);
        }

        if (this.connection != null) {
            try {
                getChannel();

                this.rpcChannel = connection.createChannel();

                this.channel.exchangeDeclare(this.platformExchangeName,
                        this.platformExchangeType,
                        this.plaftormExchangeDurable,
                        this.platformExchangeAutodelete,
                        this.platformExchangeInternal,
                        null);

                this.channel.exchangeDeclare(this.resourceExchangeName,
                        this.resourceExchangeType,
                        this.resourceExchangeDurable,
                        this.resourceExchangeAutodelete,
                        this.resourceExchangeInternal,
                        null);

                this.channel.exchangeDeclare(this.federationExchangeName,
                        this.federationExchangeType,
                        this.federationExchangeDurable,
                        this.federationExchangeAutodelete,
                        this.federationExchangeInternal,
                        null);
            } catch (IOException e) {
                log.error(e);
            } finally {
//                closeChannel(this.channel);
            }
        } else {
            log.error("Rabbit connection is null!");
        }
    }

    /**
     * Cleanup method for rabbit - set on pre destroy
     */
    @PreDestroy
    public void cleanup() {
        log.info("Rabbit cleaned!");
        try {
            Channel channel;
            if (this.connection != null && this.connection.isOpen()) {
                channel = connection.createChannel();
                channel.queueUnbind(PLATFORM_CREATION_REQUESTED_QUEUE, this.platformExchangeName,
                        this.platformCreationRequestedRoutingKey);
                channel.queueUnbind(PLATFORM_MODIFICATION_REQUESTED_QUEUE, this.platformExchangeName,
                        this.platformCreationRequestedRoutingKey);
                channel.queueUnbind(PLATFORM_REMOVAL_REQUESTED_QUEUE, this.platformExchangeName,
                        this.platformCreationRequestedRoutingKey);
                channel.queueUnbind(RESOURCE_CREATION_REQUESTED_QUEUE, this.resourceExchangeName,
                        this.resourceCreationRequestedRoutingKey);
                channel.queueUnbind(RESOURCE_MODIFICATION_REQUESTED_QUEUE, this.resourceExchangeName,
                        this.resourceCreationRequestedRoutingKey);
                channel.queueUnbind(RESOURCE_REMOVAL_REQUESTED_QUEUE, this.resourceExchangeName,
                        this.resourceCreationRequestedRoutingKey);
                channel.queueUnbind(RDF_RESOURCE_VALIDATION_REQUESTED_QUEUE, this.resourceExchangeName,
                        this.rdfResourceValidationRequestedRoutingKey);
                channel.queueUnbind(JSON_RESOURCE_TRANSLATION_REQUESTED_QUEUE, this.resourceExchangeName,
                        this.jsonResourceTranslationRequestedRoutingKey);
                channel.queueUnbind(PLATFORM_RESOURCES_REQUESTED_QUEUE, this.platformExchangeName,
                        this.platformResourcesRequestedRoutingKey);
                channel.queueDelete(PLATFORM_CREATION_REQUESTED_QUEUE);
                channel.queueDelete(PLATFORM_MODIFICATION_REQUESTED_QUEUE);
                channel.queueDelete(PLATFORM_REMOVAL_REQUESTED_QUEUE);
                channel.queueDelete(RESOURCE_CREATION_REQUESTED_QUEUE);
                channel.queueDelete(RESOURCE_MODIFICATION_REQUESTED_QUEUE);
                channel.queueDelete(RESOURCE_REMOVAL_REQUESTED_QUEUE);
                channel.queueDelete(INFORMATION_MODEL_CREATION_REQUESTED_QUEUE);
                channel.queueDelete(INFORMATION_MODEL_MODIFICATION_REQUESTED_QUEUE);
                channel.queueDelete(INFORMATION_MODEL_REMOVAL_REQUESTED_QUEUE);
                channel.queueDelete(INFORMATION_MODELS_REQUESTED_QUEUE);
                channel.queueDelete(RDF_RESOURCE_VALIDATION_REQUESTED_QUEUE);
                channel.queueDelete(JSON_RESOURCE_TRANSLATION_REQUESTED_QUEUE);
                channel.queueDelete(PLATFORM_RESOURCES_REQUESTED_QUEUE);
                channel.queueDelete(PLATFORM_DETAILS_REQUESTED_QUEUE);
                channel.queueDelete(FEDERATION_REQUESTED_QUEUE);
                channel.queueDelete(FEDERATIONS_REQUESTED_QUEUE);
                channel.queueDelete(FEDERATION_CREATION_REQUESTED_QUEUE);
                channel.queueDelete(FEDERATION_MODIFICATION_REQUESTED_QUEUE);
                channel.queueDelete(FEDERATION_REMOVAL_REQUESTED_QUEUE);
                closeChannel(channel);
                this.connection.close();
            }
        } catch (IOException e) {
            log.error(e);
        }
    }

    /**
     * Method gathers all of the rabbit consumer starter methods
     */
    public void startConsumers() {
        startConsumerOfResourceCreationMessages(this.authorizationManager);
        startConsumerOfResourceModificationMessages(this.authorizationManager);
        startConsumerOfResourceRemovalMessages(this.authorizationManager);
        startConsumerOfClearDataMessages(this.authorizationManager);

        startConsumerOfPlatformCreationMessages();
        startConsumerOfPlatformModificationMessages();
        startConsumerOfPlatformRemovalMessages();

        startConsumerOfInformationModelCreationMessages();
        startConsumerOfInformationModelModificationMessages();
        startConsumerOfInformationModelRemovalMessages();

        startConsumerOfFederationCreationMessages();
        startConsumerOfFederationModificationMessages();
        startConsumerOfFederationRemovalMessages();
        startConsumerOfGetFederationForPlatformMessages();
        startConsumerOfGetAllFederationsMessages();

        startConsumerOfPlatformResourcesRequestsMessages(this.authorizationManager);
        startConsumerOfGetAllInformationModelsRequestsMessages();
        startConsumerOfPlatformDetailsConsumer();

        startConsumerOfSspResourceCreationMessages(this.authorizationManager);
        startConsumerOfSspResourceModificationMessages(this.authorizationManager);
        startConsumerOfSspResourceRemovalMessages(this.authorizationManager);

        startConsumerOfSspCreationMessages();
        startConsumerOfSspModificationMessages();
        startConsumerOfSspRemovalMessages();

        startConsumerOfSspSdevCreationMessages();
        startConsumerOfSspSdevModificationMessages();
        startConsumerOfSspSdevRemovalMessages();
    }

    private void createQueueAndBeginConsuming(String queueName,
                                              String exchangeName,
                                              String routingKeyName,
                                              Consumer consumer) throws IOException {
        getChannel().queueDeclare(queueName, true, false, false, null);
        getChannel().queueBind(queueName, exchangeName, routingKeyName);
//            channel.basicQos(1); // to spread the load over multiple servers we set the prefetchCount setting
        getChannel().basicConsume(queueName, false, consumer);
    }

    /**
     * Method creates queue and binds it globally available exchange and adequate Routing Key.
     * It also creates a consumer for messages incoming to this queue, regarding to Resource creation requests.
     */
    public void startConsumerOfResourceCreationMessages(AuthorizationManager authorizationManager) {
        try {
            createQueueAndBeginConsuming(RESOURCE_CREATION_REQUESTED_QUEUE,
                    this.resourceExchangeName,
                    this.resourceCreationRequestedRoutingKey,
                    new ResourceCreationRequestConsumer(channel, this, authorizationManager, repositoryManager));
            log.info("Receiver waiting for Resource Creation messages....");
        } catch (IOException e) {
            log.error(e);
        }
    }

    /**
     * Method creates queue and binds it globally available exchange and adequate Routing Key.
     * It also creates a consumer for messages incoming to this queue, regarding to Resource modification requests.
     */
    public void startConsumerOfResourceModificationMessages(AuthorizationManager authorizationManager) {
        try {
            createQueueAndBeginConsuming(RESOURCE_MODIFICATION_REQUESTED_QUEUE,
                    this.resourceExchangeName,
                    this.resourceModificationRequestedRoutingKey,
                    new ResourceModificationRequestConsumer(getChannel(), this, authorizationManager, repositoryManager));
            log.info("Receiver waiting for Resource Modification messages....");
        } catch (IOException e) {
            log.error(e);
        }
    }

    /**
     * Method creates queue and binds it globally available exchange and adequate Routing Key.
     * It also creates a consumer for messages incoming to this queue, regarding to Resource removal requests.
     */
    public void startConsumerOfResourceRemovalMessages(AuthorizationManager authorizationManager) {
        try {
            createQueueAndBeginConsuming(RESOURCE_REMOVAL_REQUESTED_QUEUE,
                    this.resourceExchangeName,
                    this.resourceRemovalRequestedRoutingKey,
                    new ResourceRemovalRequestConsumer(getChannel(), repositoryManager, this, authorizationManager));
            log.info("Receiver waiting for Resource Removal messages....");
        } catch (IOException e) {
            log.error(e);
        }
    }

    /**
     * Method creates queue and binds it globally available exchange and adequate Routing Key.
     * It also creates a consumer for messages incoming to this queue, regarding to Resource removal requests.
     */
    public void startConsumerOfClearDataMessages(AuthorizationManager authorizationManager) {
        try {
            createQueueAndBeginConsuming(RESOURCE_CLEAR_DATA_REQUESTED_QUEUE,
                    this.resourceExchangeName,
                    this.resourceClearDataRequestedRoutingKey,
                    new ResourceClearDataRequestConsumer(channel, repositoryManager, this, authorizationManager));
            log.info("Receiver waiting for Clear Data messages....");
        } catch (IOException e) {
            log.error(e);
        }
    }

    /**
     * Method creates queue and binds it globally available exchange and adequate Routing Key.
     * It also creates a consumer for messages incoming to this queue, regarding to Platforms Resources requests.
     */
    public void startConsumerOfPlatformResourcesRequestsMessages(AuthorizationManager authorizationManager) {
        try {
            createQueueAndBeginConsuming(PLATFORM_RESOURCES_REQUESTED_QUEUE,
                    this.platformExchangeName,
                    this.platformResourcesRequestedRoutingKey,
                    new PlatformResourcesRequestConsumer(channel, repositoryManager, this, authorizationManager));
            log.info("Receiver waiting for Platform Resources Requests messages....");
        } catch (IOException e) {
            log.error(e);
        }
    }

    /**
     * Method creates queue and binds it globally available exchange and adequate Routing Key.
     * It also creates a consumer for messages incoming to this queue, regarding to Platform creation requests.
     */
    public void startConsumerOfPlatformCreationMessages() {
        try {
            createQueueAndBeginConsuming(PLATFORM_CREATION_REQUESTED_QUEUE,
                    this.platformExchangeName,
                    this.platformCreationRequestedRoutingKey,
                    new PlatformCreationRequestConsumer(channel, repositoryManager, this));
            log.info("Receiver waiting for Platform Creation messages....");
        } catch (IOException e) {
            log.error(e);
        }
    }

    /**
     * Method creates queue and binds it globally available exchange and adequate Routing Key.
     * It also creates a consumer for messages incoming to this queue, regarding to Platform modification requests.
     */
    public void startConsumerOfPlatformModificationMessages() {
        try {
            createQueueAndBeginConsuming(PLATFORM_MODIFICATION_REQUESTED_QUEUE,
                    this.platformExchangeName,
                    this.platformModificationRequestedRoutingKey,
                    new PlatformModificationRequestConsumer(channel, repositoryManager, this));
            log.info("Receiver waiting for Platform Modification messages....");
        } catch (IOException e) {
            log.error(e);
        }
    }

    /**
     * Method creates queue and binds it globally available exchange and adequate Routing Key.
     * It also creates a consumer for messages incoming to this queue, regarding to Platform removal requests.
     */
    public void startConsumerOfPlatformRemovalMessages() {
        try {
            createQueueAndBeginConsuming(PLATFORM_REMOVAL_REQUESTED_QUEUE,
                    this.platformExchangeName,
                    this.platformRemovalRequestedRoutingKey,
                    new PlatformRemovalRequestConsumer(channel, repositoryManager, this));
            log.info("Receiver waiting for Platform Removal messages....");
        } catch (IOException e) {
            log.error(e);
        }
    }

    /**
     * Method creates queue and binds it globally available exchange and adequate Routing Key.
     * It also creates a consumer for messages incoming to this queue, regarding to Platform Details requests.
     */
    public void startConsumerOfPlatformDetailsConsumer() {
        try {
            createQueueAndBeginConsuming(PLATFORM_DETAILS_REQUESTED_QUEUE,
                    this.platformExchangeName,
                    this.platformDetailsRequestedRoutingKey,
                    new PlatformDetailsRequestConsumer(channel, repositoryManager, this));
            log.info("Receiver waiting for Get Platform Details messages....");
        } catch (IOException e) {
            log.error(e);
        }
    }

    /**
     * Method creates queue and binds it globally available exchange and adequate Routing Key.
     * It also creates a consumer for messages incoming to this queue, regarding to Platform creation requests.
     */
    public void startConsumerOfInformationModelCreationMessages() {
        try {
            createQueueAndBeginConsuming(INFORMATION_MODEL_CREATION_REQUESTED_QUEUE,
                    this.informationModelExchangeName,
                    this.informationModelCreationRequestedRoutingKey,
                    new InformationModelCreationRequestConsumer(channel, this));
            log.info("Receiver waiting for Information Model Creation messages....");
        } catch (IOException e) {
            log.error(e);
        }
    }

    /**
     * Method creates queue and binds it globally available exchange and adequate Routing Key.
     * It also creates a consumer for messages incoming to this queue, regarding to Information Model modification requests.
     */
    public void startConsumerOfInformationModelModificationMessages() {
        try {
            createQueueAndBeginConsuming(INFORMATION_MODEL_MODIFICATION_REQUESTED_QUEUE,
                    this.informationModelExchangeName,
                    this.informationModelModificationRequestedRoutingKey,
                    new InformationModelModificationRequestConsumer(channel, this));
            log.info("Receiver waiting for Information Model Modification messages....");
        } catch (IOException e) {
            log.error(e);
        }
    }

    /**
     * Method creates queue and binds it globally available exchange and adequate Routing Key.
     * It also creates a consumer for messages incoming to this queue, regarding to Information Model removal requests.
     */
    public void startConsumerOfInformationModelRemovalMessages() {
        try {
            createQueueAndBeginConsuming(INFORMATION_MODEL_REMOVAL_REQUESTED_QUEUE,
                    this.informationModelExchangeName,
                    this.informationModelRemovalRequestedRoutingKey,
                    new InformationModelRemovalRequestConsumer(channel, this, repositoryManager));
            log.info("Receiver waiting for Information Model Removal messages....");
        } catch (IOException e) {
            log.error(e);
        }
    }

    public void startConsumerOfGetAllInformationModelsRequestsMessages() {
        try {
            createQueueAndBeginConsuming(INFORMATION_MODELS_REQUESTED_QUEUE,
                    this.informationModelExchangeName,
                    this.informationModelsRequestedRoutingKey,
                    new GetAllInformationModelsRequestConsumer(channel, repositoryManager, this));
            log.info("Receiver waiting for List All Information Models Requests messages....");
        } catch (IOException e) {
            log.error(e);
        }
    }

    private void startConsumerOfFederationCreationMessages() {
        try {
            createQueueAndBeginConsuming(FEDERATION_CREATION_REQUESTED_QUEUE,
                    this.federationExchangeName,
                    this.federationCreationRequestedRoutingKey,
                    new FederationCreationRequestConsumer(channel, repositoryManager, this));
            log.info("Receiver waiting for Federation Creation messages....");
        } catch (IOException e) {
            log.error(e);
        }
    }

    private void startConsumerOfFederationModificationMessages() {
        ;
        try {
            createQueueAndBeginConsuming(FEDERATION_MODIFICATION_REQUESTED_QUEUE,
                    this.federationExchangeName,
                    this.federationModificationRequestedRoutingKey,
                    new FederationModificationRequestConsumer(channel, repositoryManager, this));
            log.info("Receiver waiting for Federation Modification messages....");
        } catch (IOException e) {
            log.error(e);
        }
    }

    private void startConsumerOfFederationRemovalMessages() {
        try {
            createQueueAndBeginConsuming(FEDERATION_REMOVAL_REQUESTED_QUEUE,
                    this.federationExchangeName,
                    this.federationRemovalRequestedRoutingKey,
                    new FederationRemovalRequestConsumer(channel, repositoryManager, this));
            log.info("Receiver waiting for Federation Removal messages....");
        } catch (IOException e) {
            log.error(e);
        }
    }

    private void startConsumerOfGetAllFederationsMessages() {
        try {
            createQueueAndBeginConsuming(FEDERATIONS_REQUESTED_QUEUE,
                    this.federationExchangeName,
                    this.federationsRequestedRoutingKey,
                    new GetAllFederationsRequestConsumer(channel, repositoryManager, this));
            log.info("Receiver waiting for Get All Federations messages....");
        } catch (IOException e) {
            log.error(e);
        }
    }

    private void startConsumerOfGetFederationForPlatformMessages() {
        try {
            createQueueAndBeginConsuming(FEDERATION_REQUESTED_QUEUE,
                    this.federationExchangeName,
                    this.federationRequestedRoutingKey,
                    new GetFederationForPlatformRequestConsumer(channel, repositoryManager, this));
            log.info("Receiver waiting for Get Federation for Platform messages....");
        } catch (IOException e) {
            log.error(e);
        }
    }

    /**
     * Method creates queue and binds it globally available exchange and adequate Routing Key.
     * It also creates a consumer for messages incoming to this queue, regarding to SSP Resource creation requests.
     */
    public void startConsumerOfSspResourceCreationMessages(AuthorizationManager authorizationManager) {
        try {
            createQueueAndBeginConsuming(SSP_RESOURCE_CREATION_REQUESTED_QUEUE,
                    this.resourceExchangeName,
                    this.sspSdevResourceCreationRequestedRoutingKey,
                    new SspResourceCreationRequestConsumer(channel, this, authorizationManager, repositoryManager));
            log.info("Receiver waiting for SSP Resource Creation messages....");
        } catch (IOException e) {
            log.error(e);
        }
    }

    /**
     * Method creates queue and binds it globally available exchange and adequate Routing Key.
     * It also creates a consumer for messages incoming to this queue, regarding to SSP Resource modification requests.
     */
    public void startConsumerOfSspResourceModificationMessages(AuthorizationManager authorizationManager) {
        try {
            createQueueAndBeginConsuming(SSP_RESOURCE_MODIFICATION_REQUESTED_QUEUE,
                    this.resourceExchangeName,
                    this.sspResourceModificationRequestedRoutingKey,
                    new SspResourceModificationRequestConsumer(getChannel(), this, authorizationManager, repositoryManager));
            log.info("Receiver waiting for SSP Resource Modification messages....");
        } catch (IOException e) {
            log.error(e);
        }
    }

    /**
     * Method creates queue and binds it globally available exchange and adequate Routing Key.
     * It also creates a consumer for messages incoming to this queue, regarding to SSP Resource removal requests.
     */
    public void startConsumerOfSspResourceRemovalMessages(AuthorizationManager authorizationManager) {
        try {
            createQueueAndBeginConsuming(SSP_RESOURCE_REMOVAL_REQUESTED_QUEUE,
                    this.resourceExchangeName,
                    this.sspResourceRemovalRequestedRoutingKey,
                    new SspResourceRemovalRequestConsumer(getChannel(), repositoryManager, this, authorizationManager));
            log.info("Receiver waiting for SSP Resource Removal messages....");
        } catch (IOException e) {
            log.error(e);
        }
    }

    /**
     * Method creates queue and binds it globally available exchange and adequate Routing Key.
     * It also creates a consumer for messages incoming to this queue, regarding to SSP Resource creation requests.
     */
    public void startConsumerOfSspCreationMessages() {
        try {
            createQueueAndBeginConsuming(SSP_CREATION_REQUESTED_QUEUE,
                    this.sspExchangeName,
                    this.sspCreationRequestedRoutingKey,
                    new SspCreationRequestConsumer(channel, this, repositoryManager));
            log.info("Receiver waiting for SSP Creation messages....");
        } catch (IOException e) {
            log.error(e);
        }
    }

    /**
     * Method creates queue and binds it globally available exchange and adequate Routing Key.
     * It also creates a consumer for messages incoming to this queue, regarding to SSP Resource modification requests.
     */
    public void startConsumerOfSspModificationMessages() {
        try {
            createQueueAndBeginConsuming(SSP_MODIFICATION_REQUESTED_QUEUE,
                    this.sspExchangeName,
                    this.sspModificationRequestedRoutingKey,
                    new SspModificationRequestConsumer(getChannel(), this, repositoryManager));
            log.info("Receiver waiting for SSP Modification messages....");
        } catch (IOException e) {
            log.error(e);
        }
    }

    /**
     * Method creates queue and binds it globally available exchange and adequate Routing Key.
     * It also creates a consumer for messages incoming to this queue, regarding to SSP Resource removal requests.
     */
    public void startConsumerOfSspRemovalMessages() {
        try {
            createQueueAndBeginConsuming(SSP_REMOVAL_REQUESTED_QUEUE,
                    this.sspExchangeName,
                    this.sspRemovalRequestedRoutingKey,
                    new SspRemovalRequestConsumer(getChannel(), this, repositoryManager));
            log.info("Receiver waiting for SSP Removal messages....");
        } catch (IOException e) {
            log.error(e);
        }
    }

    /**
     * Method creates queue and binds it globally available exchange and adequate Routing Key.
     * It also creates a consumer for messages incoming to this queue, regarding to SSP Sdev creation requests.
     */
    public void startConsumerOfSspSdevCreationMessages() {
        try {
            createQueueAndBeginConsuming(SSP_SDEV_CREATION_REQUESTED_QUEUE,
                    this.sspExchangeName,
                    this.sspSdevCreationRequestedRoutingKey,
                    new SspSdevCreationRequestConsumer(getChannel(), this, repositoryManager));
            log.info("Receiver waiting for SSP Sdev Creation messages....");
        } catch (IOException e) {
            log.error(e);
        }
    }

    /**
     * Method creates queue and binds it globally available exchange and adequate Routing Key.
     * It also creates a consumer for messages incoming to this queue, regarding to SSP Sdev modification requests.
     */
    public void startConsumerOfSspSdevModificationMessages() {
        try {
            createQueueAndBeginConsuming(SSP_SDEV_MODIFICATION_REQUESTED_QUEUE,
                    this.sspExchangeName,
                    this.sspSdevModificationRequestedRoutingKey,
                    new SspSdevModificationRequestConsumer(getChannel(), this, repositoryManager));
            log.info("Receiver waiting for SSP Sdev Modification messages....");
        } catch (IOException e) {
            log.error(e);
        }
    }

    /**
     * Method creates queue and binds it globally available exchange and adequate Routing Key.
     * It also creates a consumer for messages incoming to this queue, regarding to SSP Sdev removal requests.
     */
    public void startConsumerOfSspSdevRemovalMessages() {
        try {
            createQueueAndBeginConsuming(SSP_SDEV_REMOVAL_REQUESTED_QUEUE,
                    this.sspExchangeName,
                    this.sspSdevRemovalRequestedRoutingKey,
                    new SspSdevRemovalRequestConsumer(getChannel(), this, repositoryManager));
            log.info("Receiver waiting for SSP Sdev Removal messages....");
        } catch (IOException e) {
            log.error(e);
        }
    }

    /**
     * Triggers sending message containing Platform accordingly to Operation Type.
     *
     * @param platform
     * @param operationType
     */
    public void sendPlatformOperationMessage(Platform platform, RegistryOperationType operationType) {
        try {
            ObjectMapper mapper = new ObjectMapper();
            String message = mapper.writeValueAsString(platform);

            switch (operationType) {
                case CREATION:
                    sendMessage(this.platformExchangeName, this.platformCreatedRoutingKey, message,
                            platform.getClass().getCanonicalName());
                    log.info("- platform created message sent");
                    break;
                case MODIFICATION:
                    sendMessage(this.platformExchangeName, this.platformModifiedRoutingKey, message,
                            platform.getClass().getCanonicalName());
                    log.info("- platform modified message sent");
                    break;
                case REMOVAL:
                    sendMessage(this.platformExchangeName, this.platformRemovedRoutingKey, message,
                            platform.getClass().getCanonicalName());
                    log.info("- platform removed message sent");
                    break;
            }

        } catch (JsonProcessingException e) {
            log.error(ERROR_OCCURRED_WHEN_PARSING_OBJECT_TO_JSON + platform, e);
        }
    }

    public void sendResourceOperationMessage(CoreResourceRegisteredOrModifiedEventPayload payload,
                                             RegistryOperationType operationType) {
        try {
            ObjectMapper mapper = new ObjectMapper();
            String message = mapper.writeValueAsString(payload);

            switch (operationType) {
                case CREATION:
                    sendMessage(this.resourceExchangeName, this.resourceCreatedRoutingKey, message,
                            payload.getClass().getCanonicalName());
                    break;
                case MODIFICATION:
                    sendMessage(this.resourceExchangeName, this.resourceModifiedRoutingKey, message,
                            payload.getClass().getCanonicalName());
                    break;
            }
            log.info("- resources operation (" + operationType + ") message sent (fanout).");
        } catch (JsonProcessingException e) {
            log.error(ERROR_OCCURRED_WHEN_PARSING_OBJECT_TO_JSON + payload, e);
        }
    }

    public void sendInformationModelOperationMessage(InformationModel informationModel,
                                                     RegistryOperationType operationType) {
        try {
            ObjectMapper mapper = new ObjectMapper();
            String message = mapper.writeValueAsString(informationModel);

            switch (operationType) {
                case CREATION:
                    sendMessage(this.informationModelExchangeName, this.informationModelCreatedRoutingKey, message,
                            informationModel.getClass().getCanonicalName());
                    break;
                case MODIFICATION:
                    sendMessage(this.informationModelExchangeName, this.informationModelModifiedRoutingKey, message,
                            informationModel.getClass().getCanonicalName());
                    break;
                case REMOVAL:
                    sendMessage(this.informationModelExchangeName, this.informationModelRemovedRoutingKey, message,
                            informationModel.getClass().getCanonicalName());
                    break;
            }
            log.info("- information model operation (" + operationType + ") message sent (fanout).");
        } catch (JsonProcessingException e) {
            log.error(ERROR_OCCURRED_WHEN_PARSING_OBJECT_TO_JSON + informationModel, e);
        }
    }

    public void sendResourcesRemovalMessage(List<String> resourcesIds) {
        ObjectMapper mapper = new ObjectMapper();
        String message = "";
        try {
            message = mapper.writerFor(new TypeReference<List<String>>() {
            }).writeValueAsString(resourcesIds);
        } catch (JsonProcessingException e) {
            log.error(e);
        }
        sendMessage(this.resourceExchangeName, this.resourceRemovedRoutingKey, message,
                resourcesIds.getClass().getCanonicalName());
    }

    /**
     * Triggers sending message containing Smart Space accordingly to Operation Type.
     *
     * @param ssp
     * @param operationType
     */
    public void sendSspOperationMessage(SmartSpace ssp, RegistryOperationType operationType) {
        try {
            ObjectMapper mapper = new ObjectMapper();
            String message = mapper.writeValueAsString(ssp);

            switch (operationType) {
                case CREATION:
                    sendMessage(this.sspExchangeName, this.sspCreatedRoutingKey, message,
                            ssp.getClass().getCanonicalName());
                    log.info("- ssp created message sent");
                    break;
                case MODIFICATION:
                    sendMessage(this.sspExchangeName, this.sspModifiedRoutingKey, message,
                            ssp.getClass().getCanonicalName());
                    log.info("- ssp modified message sent");
                    break;
                case REMOVAL:
                    sendMessage(this.sspExchangeName, this.sspRemovedRoutingKey, message,
                            ssp.getClass().getCanonicalName());
                    log.info("- ssp removed message sent");
                    break;
            }

        } catch (JsonProcessingException e) {
            log.error(ERROR_OCCURRED_WHEN_PARSING_OBJECT_TO_JSON + ssp, e);
        }
    }

    /**
     * Triggers method for contact with Semantic Manager to verify RDF Resources (with RDF description type)
     * and translate to JSON Core Resources.
     *
     * @param rpcConsumer          rabbit consumer that received the request
     * @param rpcProperties        properties of request message received
     * @param rpcEnvelope          envelope of request message received
     * @param message              body of message in form of a JSON String with a CoreResourceRegistryRequest
     * @param platformId           id of a platform corresponding to request
     * @param operationType        type of request - creation or modification
     * @param authorizationManager - authorization manager bean
     * @param policiesMap          - map with security policies
     */
    public void sendResourceRdfValidationRpcMessage(DefaultConsumer rpcConsumer,
                                                    AMQP.BasicProperties rpcProperties,
                                                    Envelope rpcEnvelope,
                                                    String message,
                                                    String platformId,
                                                    RegistryOperationType operationType,
                                                    AuthorizationManager authorizationManager,
                                                    Map<String, IAccessPolicySpecifier> policiesMap) {
        sendResourceOperationRpcMessageToSemanticManager(rpcConsumer, rpcProperties, rpcEnvelope,
                this.rdfResourceValidationRequestedRoutingKey,
                RDF,
                operationType,
                message,
                platformId,
                authorizationManager,
                policiesMap,
                null); //// TODO: 15.01.2018
        log.info("- rdf resource to validation message sent");
    }

    /**
     * Triggers method for contact with Semantic Manager to translate JSON Resources (BASIC description type) to RDFs.
     *
     * @param rpcConsumer          rabbit consumer that received the request
     * @param rpcProperties        properties of request message received
     * @param rpcEnvelope          envelope of request message received
     * @param message              body of message in form of a JSON String with a CoreResourceRegistryRequest
     * @param platformId           id of a platform corresponding to request
     * @param operationType        type of request - creation or modification
     * @param authorizationManager - authorization manager bean
     * @param policiesMap          - map with security policies
     * @param requestBody          body from received request in form of a JSON String with a Map of a String and Resource
     */
    public void sendResourceJsonTranslationRpcMessage(DefaultConsumer rpcConsumer,
                                                      AMQP.BasicProperties rpcProperties,
                                                      Envelope rpcEnvelope,
                                                      String message,
                                                      String platformId,
                                                      RegistryOperationType operationType,
                                                      AuthorizationManager authorizationManager,
                                                      Map<String, IAccessPolicySpecifier> policiesMap,
                                                      String requestBody) {
        sendResourceOperationRpcMessageToSemanticManager(rpcConsumer, rpcProperties, rpcEnvelope,
                this.jsonResourceTranslationRequestedRoutingKey,
                BASIC,
                operationType,
                message,
                platformId,
                authorizationManager,
                policiesMap,
                requestBody);
    }

    /**
     * Triggers method for contact with Semantic Manager to translate JSON Resources (BASIC description type) to RDFs.
     *
     * @param rpcConsumer         rabbit consumer that received the request
     * @param rpcProperties       properties of request message received
     * @param rpcEnvelope         envelope of request message received
     * @param message             body of message in form of a JSON String with a CoreResourceRegistryRequest
     * @param sDevId              id of a platform corresponding to request
     * @param operationType       type of request - creation or modification
     * @param policiesMap         - map with security policies
     * @param requestResourcesMap body from received request - Map of a String and Resource
     */
    public void sendSspResourceJsonTranslationRpcMessage(DefaultConsumer rpcConsumer,
                                                         AMQP.BasicProperties rpcProperties,
                                                         Envelope rpcEnvelope,
                                                         String message,
                                                         String sDevId,
                                                         RegistryOperationType operationType,
                                                         Map<String, IAccessPolicySpecifier> policiesMap,
                                                         Map<String, Resource> requestResourcesMap) {

        sendSspResourceOperationRpcMessageToSemanticManager(rpcConsumer, rpcProperties, rpcEnvelope,
                this.jsonResourceTranslationRequestedRoutingKey,
                operationType,
                message,
                sDevId,
                authorizationManager,
                policiesMap,
                requestResourcesMap);
    }

    /**
     * Sends reply message with given body to rabbit queue, for specified RPC sender.
     *
     * @param consumer
     * @param properties
     * @param envelope
     * @param response
     * @throws IOException
     */
    public void sendRPCReplyMessage(DefaultConsumer consumer, AMQP.BasicProperties properties, Envelope envelope,
                                    String response) throws IOException {
        if (properties.getReplyTo() != null || properties.getCorrelationId() != null) {
            AMQP.BasicProperties replyProps = new AMQP.BasicProperties
                    .Builder()
                    .correlationId(properties.getCorrelationId())
                    .build();

            consumer.getChannel().basicPublish("", properties.getReplyTo(), replyProps, response.getBytes());
            log.info("- RPC reply Message sent back!");
        } else {
            log.error("Received RPC message without ReplyTo or CorrelationId props.");
        }
        consumer.getChannel().basicAck(envelope.getDeliveryTag(), false);
    }

    /**
     * Publishes message on chosen routing key and creates a consumer waiting for responses.
     *
     * @param rpcConsumer          rabbit consumer that received the request
     * @param rpcProperties        properties of request message received
     * @param rpcEnvelope          envelope of request message received
     * @param routingKey           routing key that is supposed to be used to publish the message on
     * @param descriptionType      BASIC (json) or RDF descrption type of content
     * @param message              request in form of a JSON String (a CoreResourceRegistryRequest)
     * @param platformId           id of a platform corresponding to request
     * @param operationType        type of request - creation or modification
     * @param authorizationManager - authorization manager bean
     * @param policiesMap          - map with security policies
     * @param requestBody          body from received request in form of a JSON String with a Map of a String and Resource
     */
    private void sendResourceOperationRpcMessageToSemanticManager(DefaultConsumer rpcConsumer, AMQP.BasicProperties rpcProperties, Envelope rpcEnvelope,
                                                                  String routingKey,
                                                                  DescriptionType descriptionType, RegistryOperationType operationType,
                                                                  String message, String platformId, AuthorizationManager authorizationManager,
                                                                  Map<String, IAccessPolicySpecifier> policiesMap,
                                                                  String requestBody) {
        try {
            String replyQueueName = rpcChannel.queueDeclare().getQueue();

            String correlationId = UUID.randomUUID().toString();
            AMQP.BasicProperties props = new AMQP.BasicProperties()
                    .builder()
                    .correlationId(correlationId)
                    .replyTo(replyQueueName)
                    .build();

            ResourceValidationResponseConsumer responseConsumer =
                    new ResourceValidationResponseConsumer(rpcConsumer, rpcProperties, rpcEnvelope,
                            rpcChannel, repositoryManager, this, platformId, operationType, descriptionType,
                            authorizationManager, policiesMap, requestBody);

            rpcChannel.basicConsume(replyQueueName, true, responseConsumer);

            log.info("Sending RPC message to Semantic Manager... \nMessage params:\nExchange name: "
                    + this.resourceExchangeName + "\nRouting key: " + routingKey + "\nProps: " + props);

            rpcChannel.basicPublish(this.resourceExchangeName, routingKey, true, props, message.getBytes());

        } catch (IOException e) {
            log.error("Unable to send message. Params: \n RPC consumer: " + rpcConsumer +
                    "\nRpc props: " + rpcProperties +
                    "\nrpc envelope: " + rpcEnvelope +
                    "\nrouting key: " + routingKey +
                    "\nmessage: " + message +
                    "\nplatform id: " + platformId +
                    this.resourceExchangeName + "  -  " + routingKey +
                    "\nerror message: " + e.getMessage() +
                    "\nerror cause:" + e.getCause());
        }
    }

    /**
     * Publishes message on chosen routing key and creates a consumer waiting for responses.
     *
     * @param rpcConsumer          rabbit consumer that received the request
     * @param rpcProperties        properties of request message received
     * @param rpcEnvelope          envelope of request message received
     * @param routingKey           routing key that is supposed to be used to publish the message on
     * @param message              request in form of a JSON String (a CoreResourceRegistryRequest)
     * @param sdevId               id of a platform corresponding to request
     * @param operationType        type of request - creation or modification
     * @param authorizationManager - authorization manager bean
     * @param policiesMap          - map with security policies
     * @param requestResourcesMap  body from received request in form of a JSON String with a Map of a String and Resource
     */

    private void sendSspResourceOperationRpcMessageToSemanticManager(DefaultConsumer rpcConsumer, AMQP.BasicProperties rpcProperties, Envelope rpcEnvelope,
                                                                     String routingKey, RegistryOperationType operationType,
                                                                     String message, String sdevId, AuthorizationManager authorizationManager,
                                                                     Map<String, IAccessPolicySpecifier> policiesMap,
                                                                     Map<String, Resource> requestResourcesMap) {
        try {
            String replyQueueName = rpcChannel.queueDeclare().getQueue();

            String correlationId = UUID.randomUUID().toString();
            AMQP.BasicProperties props = new AMQP.BasicProperties()
                    .builder()
                    .correlationId(correlationId)
                    .replyTo(replyQueueName)
                    .build();

            SspResourceTranslationResponseConsumer responseConsumer = new SspResourceTranslationResponseConsumer(
                    rpcConsumer, rpcProperties, rpcEnvelope,
                    rpcChannel, repositoryManager, this, sdevId, operationType,
                    authorizationManager, policiesMap, requestResourcesMap
            );

            rpcChannel.basicConsume(replyQueueName, true, responseConsumer);

            log.info("Sending RPC message to Semantic Manager... \nMessage params:\nExchange name: "
                    + this.resourceExchangeName + "\nRouting key: " + routingKey + "\nProps: " + props);

            rpcChannel.basicPublish(this.resourceExchangeName, routingKey, true, props, message.getBytes());

        } catch (IOException e) {
            log.error("Unable to send message. Params: \n RPC consumer: " + rpcConsumer +
                    "\nRpc props: " + rpcProperties +
                    "\nrpc envelope: " + rpcEnvelope +
                    "\nrouting key: " + routingKey +
                    "\nmessage: " + message +
                    "\nsdev id: " + sdevId +
                    this.resourceExchangeName + "  -  " + routingKey +
                    "\nerror message: " + e.getMessage() +
                    "\nerror cause:" + e.getCause());
        }
    }

    /**
     * Method publishes a custom RPC Message through RabbitMQ using given parameters.
     *
     * @param exchangeName
     * @param routingKey
     * @param message
     * @param responseConsumer
     */
    public void sendCustomRpcMessage(String exchangeName, String routingKey,
                                     String message, Consumer responseConsumer) {
        try {
            String replyQueueName = "Queue" + Math.random();
            rpcChannel.queueDeclare(replyQueueName, true, false, true, null);

            String correlationId = UUID.randomUUID().toString();
            AMQP.BasicProperties props = new AMQP.BasicProperties()
                    .builder()
                    .correlationId(correlationId)
                    .replyTo(replyQueueName)
                    .build();

            rpcChannel.basicConsume(replyQueueName, true, responseConsumer);

            rpcChannel.basicPublish(exchangeName, routingKey, true, props, message.getBytes());

            log.info("Sending Custom RPC Message... \nMessage params:\nExchange name: "
                    + exchangeName + "\nRouting key: " + routingKey + "\nProps: " + props);
        } catch (IOException e) {
            log.error(e);
        }

    }

    public void closeConsumer(DefaultConsumer consumerToClose, Channel channel) throws IOException {
        channel.basicCancel(consumerToClose.getConsumerTag());
    }

    /**
     * Method publishes given message to the given exchange and routing key.
     * Props are set for correct message handle on the receiver side.
     *
     * @param exchange   name of the proper Rabbit exchange, adequate to topic of the communication
     * @param routingKey name of the proper Rabbit routing key, adequate to topic of the communication
     * @param message    message content in JSON String format
     * @param classType  message content in JSON String format
     */
    private void sendMessage(String exchange, String routingKey, String message, String classType) {
        try {
            Map<String, Object> headers = new HashMap<>();
            headers.put("__TypeId__", classType);
            headers.put("__ContentTypeId__", Object.class.getCanonicalName());
            AMQP.BasicProperties props = new AMQP.BasicProperties()
                    .builder()
                    .contentType("application/json")
                    .headers(headers)
                    .build();

            log.info("Sending message...");
            channel.basicPublish(exchange, routingKey, props, message.getBytes());
        } catch (IOException e) {
            log.error(e);
        } finally {
//            closeChannel(channel);
        }
    }

    public void sendCustomMessage(String exchange, String routingKey, String message, String classType) {
        sendMessage(exchange, routingKey, message, classType);
    }

    /**
     * Closes given channel if it exists and is open.
     *
     * @param channel rabbit channel to close
     */
    private void closeChannel(Channel channel) {
        try {
            if (channel != null && channel.isOpen())
                channel.close();
        } catch (IOException | TimeoutException e) {
            log.error(e);
        }
    }

    public void sendInformationModelValidationRpcMessage(DefaultConsumer rpcConsumer,
                                                         AMQP.BasicProperties rpcProperties, Envelope rpcEnvelope,
                                                         String message, RegistryOperationType operationType) {
        try {
            String replyQueueName = rpcChannel.queueDeclare().getQueue();

            String correlationId = UUID.randomUUID().toString();
            AMQP.BasicProperties props = new AMQP.BasicProperties()
                    .builder()
                    .correlationId(correlationId)
                    .replyTo(replyQueueName)
                    .build();

            InformationModelValidationResponseConsumer responseConsumer =
                    new InformationModelValidationResponseConsumer(rpcConsumer, rpcProperties, rpcEnvelope,
                            rpcChannel, repositoryManager, this, operationType);

            rpcChannel.basicConsume(replyQueueName, true, responseConsumer);

            log.info("Sending RPC message to Semantic Manager... \nMessage params:" +
                    "\nExchange name: " + informationModelExchangeName
                    + "\nRouting key: " + rdfInformationModelValidationRequestedRoutingKey
                    + "\nProps: " + props);

            rpcChannel.basicPublish(
                    this.informationModelExchangeName,
                    this.rdfInformationModelValidationRequestedRoutingKey,
                    true, props, message.getBytes());

        } catch (IOException e) {
            log.error(e);
        }
    }

    /**
     * Triggers sending message containing Federation accordingly to Operation Type.
     *
     * @param federation
     * @param operationType
     */
    public void sendFederationOperationMessage(Federation federation, RegistryOperationType operationType) {
        try {
            ObjectMapper mapper = new ObjectMapper();
            String message = mapper.writeValueAsString(federation);

            switch (operationType) {
                case CREATION:
                    sendMessage(this.federationExchangeName, this.federationCreatedRoutingKey, message,
                            federation.getClass().getCanonicalName());
                    log.info("- federation created message sent");
                    break;
                case MODIFICATION:
                    sendMessage(this.federationExchangeName, this.federationModifiedRoutingKey, message,
                            federation.getClass().getCanonicalName());
                    log.info("- federation modified message sent");
                    break;
                case REMOVAL:
                    sendMessage(this.federationExchangeName, this.federationRemovedRoutingKey, message,
                            federation.getClass().getCanonicalName());
                    log.info("- federation removed message sent");
                    break;
            }
        } catch (JsonProcessingException e) {
            log.error(ERROR_OCCURRED_WHEN_PARSING_OBJECT_TO_JSON + federation, e);
        }
    }
    /**
     * Triggers sending message containing Sdev accordingly to Operation Type.
     *
     * @param sDev
     * @param operationType
     */
    public void sendSdevOperationMessage(SspRegInfo sDev, RegistryOperationType operationType) {
        try {
            ObjectMapper mapper = new ObjectMapper();
            String message = mapper.writeValueAsString(sDev);

            switch (operationType) {
                case CREATION:
                    sendMessage(this.sspExchangeName, this.sspSdevCreatedRoutingKey, message,
                            sDev.getClass().getCanonicalName());
                    log.info("- sDev created message sent");
                    break;
                case MODIFICATION:
                    sendMessage(this.sspExchangeName, this.sspSdevModifiedRoutingKey, message,
                            sDev.getClass().getCanonicalName());
                    log.info("- sDev modified message sent");
                    break;
                case REMOVAL:
                    sendMessage(this.sspExchangeName, this.sspSdevRemovedRoutingKey, message,
                            sDev.getClass().getCanonicalName());
                    log.info("- sDev removed message sent");
                    break;
            }
        } catch (JsonProcessingException e) {
            log.error(ERROR_OCCURRED_WHEN_PARSING_OBJECT_TO_JSON + sDev, e);
        }
    }

    /**
     * Method used to send message via RPC (Remote Procedure Call) pattern.
     * Before sending a message, a temporary response queue is declared and its name is passed along with the message.
     * When a consumer handles the message, it returns the result via the response queue.
     * Since this is a synchronous pattern, it uses timeout of 20 seconds. If the response doesn't come in that time, the method returns with null result.
     *
     * @param exchangeName name of the exchange to send message to
     * @param routingKey   routing key to send message to
     * @param message      message to be sent
     * @return response from the consumer or null if timeout occurs
     */
    public String sendRpcMessageAndConsumeResponse(String exchangeName, String routingKey, String message) {

        Channel channel = null;
        try {
            channel = this.connection.createChannel();
        } catch (Exception e) {
            log.error(e);
        }

        QueueingConsumer consumer = new QueueingConsumer(channel);

        try {
            log.debug("Sending RPC message...");

            String replyQueueName = channel.queueDeclare().getQueue();

            String correlationId = UUID.randomUUID().toString();
            AMQP.BasicProperties props = new AMQP.BasicProperties()
                    .builder()
                    .correlationId(correlationId)
                    .replyTo(replyQueueName)
                    .contentType("application/json")
                    .build();

            channel.basicConsume(replyQueueName, true, consumer);

            String responseMsg = null;

            channel.basicPublish(exchangeName, routingKey, props, message.getBytes());
            while (true) {
                QueueingConsumer.Delivery delivery = consumer.nextDelivery(20000);
                if (delivery == null) {
                    log.info("Timeout in response retrieval");
                    return null;
                }

                if (delivery.getProperties().getCorrelationId().equals(correlationId)) {
                    log.debug("Got reply with correlationId: " + correlationId);
                    responseMsg = new String(delivery.getBody());
                    log.debug("reply content: " + responseMsg);
                    break;
                } else {
                    log.debug("Got answer with wrong correlationId... should be " + correlationId + " but got " + delivery.getProperties().getCorrelationId());
                }
            }
            log.debug("Finished rpc loop");

            return responseMsg;
        } catch (IOException | InterruptedException e) {
            log.error("Error while sending RPC Message via RabbitMQ", e);
        } finally {
            try {
                channel.basicCancel(consumer.getConsumerTag());
            } catch (Exception e) {
                log.error(e.getMessage(), e);
            }
        }
        return null;
    }
}