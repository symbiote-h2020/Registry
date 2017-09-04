package eu.h2020.symbiote.managers;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.rabbitmq.client.*;
import eu.h2020.symbiote.core.internal.CoreResourceRegisteredOrModifiedEventPayload;
import eu.h2020.symbiote.core.internal.DescriptionType;
import eu.h2020.symbiote.core.model.Federation;
import eu.h2020.symbiote.core.model.InformationModel;
import eu.h2020.symbiote.core.model.Platform;
import eu.h2020.symbiote.messaging.consumers.informationModel.*;
import eu.h2020.symbiote.messaging.consumers.platform.PlatformCreationRequestConsumer;
import eu.h2020.symbiote.messaging.consumers.platform.PlatformModificationRequestConsumer;
import eu.h2020.symbiote.messaging.consumers.platform.PlatformRemovalRequestConsumer;
import eu.h2020.symbiote.messaging.consumers.platform.PlatformResourcesRequestConsumer;
import eu.h2020.symbiote.messaging.consumers.resource.ResourceCreationRequestConsumer;
import eu.h2020.symbiote.messaging.consumers.resource.ResourceModificationRequestConsumer;
import eu.h2020.symbiote.messaging.consumers.resource.ResourceRemovalRequestConsumer;
import eu.h2020.symbiote.messaging.consumers.resource.ResourceValidationResponseConsumer;
import eu.h2020.symbiote.model.RegistryOperationType;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.annotation.PreDestroy;
import java.io.IOException;
import java.util.*;
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

    private static final String RDF_RESOURCE_VALIDATION_REQUESTED_QUEUE = "rdfResourceValidationRequestedQueue";
    private static final String JSON_RESOURCE_TRANSLATION_REQUESTED_QUEUE = "jsonResourceTranslationRequestedQueue";
    private static final String RESOURCE_CREATION_REQUESTED_QUEUE = "symbIoTe-Registry-resourceCreationRequestedQueue";
    private static final String RESOURCE_MODIFICATION_REQUESTED_QUEUE = "symbIoTe-Registry-resourceModificationRequestedQueue";
    private static final String RESOURCE_REMOVAL_REQUESTED_QUEUE = "symbIoTe-Registry-resourceRemovalRequestedQueue";
    private static final String PLATFORM_CREATION_REQUESTED_QUEUE = "symbIoTe-Registry-platformCreationRequestedQueue";
    private static final String PLATFORM_MODIFICATION_REQUESTED_QUEUE = "symbIoTe-Registry-platformModificationRequestedQueue";
    private static final String PLATFORM_REMOVAL_REQUESTED_QUEUE = "symbIoTe-Registry-platformRemovalRequestedQueue";
    private static final String INFORMATION_MODEL_CREATION_REQUESTED_QUEUE = "symbIoTe-Registry-informationModelCreationRequestedQueue";
    private static final String INFORMATION_MODEL_MODIFICATION_REQUESTED_QUEUE = "symbIoTe-Registry-informationModelModificationRequestedQueue";
    private static final String INFORMATION_MODEL_REMOVAL_REQUESTED_QUEUE = "symbIoTe-Registry-informationModelRemovalRequestedQueue";
    private static final String PLATFORM_RESOURCES_REQUESTED_QUEUE = "symbIoTe-Registry-platformResourcesRequestedQueue";
    private static final String INFORMATION_MODELS_REQUESTED_QUEUE = "symbIoTe-Registry-informationModelsRequestedQueue";
    private static final String ERROR_OCCURRED_WHEN_PARSING_OBJECT_TO_JSON = "Error occurred when parsing Resource object JSON: ";

    private static Log log = LogFactory.getLog(RabbitManager.class);
    private AuthorizationManager authorizationManager;
    private RepositoryManager repositoryManager;
    private Connection connection;
    private Channel rpcChannel;

    @Value("${rabbit.host}")
    private String rabbitHost;
    @Value("${rabbit.username}")
    private String rabbitUsername;
    @Value("${rabbit.password}")
    private String rabbitPassword;
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
    @Value("${rabbit.routingKey.resource.removed}")
    private String resourceRemovedRoutingKey;
    @Value("${rabbit.routingKey.resource.modificationRequested}")
    private String resourceModificationRequestedRoutingKey;
    @Value("${rabbit.routingKey.resource.modified}")
    private String resourceModifiedRoutingKey;

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

    @Value("${rabbit.routingKey.resource.instance.translationRequested}")
    private String jsonResourceTranslationRequestedRoutingKey; //dla JSONów
    @Value("${rabbit.routingKey.resource.instance.validationRequested}")
    private String rdfResourceValidationRequestedRoutingKey; //dla RDFów
    @Value("${rabbit.routingKey.platform.model.validationRequested}")
    private String rdfInformationModelValidationRequestedRoutingKey;

    @Value("${rabbit.exchange.aam.name}")
    private String aamExchangeName;
    @Value("${rabbit.routingKey.get.platform.owners.names}")
    private String aamGetPlatformOwners;

    @Autowired
    public RabbitManager(RepositoryManager repositoryManager, AuthorizationManager authorizationManager) {
        this.repositoryManager = repositoryManager;
        this.authorizationManager = authorizationManager;
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
        Channel channel = null;

        try {
            getConnection();
        } catch (TimeoutException e) {
            log.error(e);
        }

        if (connection != null) {
            try {
                channel = this.connection.createChannel();

                this.rpcChannel = connection.createChannel();

                channel.exchangeDeclare(this.platformExchangeName,
                        this.platformExchangeType,
                        this.plaftormExchangeDurable,
                        this.platformExchangeAutodelete,
                        this.platformExchangeInternal,
                        null);

                channel.exchangeDeclare(this.resourceExchangeName,
                        this.resourceExchangeType,
                        this.resourceExchangeDurable,
                        this.resourceExchangeAutodelete,
                        this.resourceExchangeInternal,
                        null);
            } catch (IOException e) {
                log.error(e);
            } finally {
                closeChannel(channel);
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
        try {
            startConsumerOfResourceCreationMessages();
            startConsumerOfResourceModificationMessages();
            startConsumerOfResourceRemovalMessages();

            startConsumerOfPlatformCreationMessages();
            startConsumerOfPlatformModificationMessages();
            startConsumerOfPlatformRemovalMessages();

            startConsumerOfInformationModelCreationMessages();
            startConsumerOfInformationModelModificationMessages();
            startConsumerOfInformationModelRemovalMessages();

            //// TODO: 22.08.2017 Start Federation Consumers!

            startConsumerOfPlatformResourcesRequestsMessages();
            startConsumerOfListAllInformationModelsRequestsMessages();
        } catch (InterruptedException e) {
            log.error(e);
        }
    }

    /**
     * Method creates queue and binds it globally available exchange and adequate Routing Key.
     * It also creates a consumer for messages incoming to this queue, regarding to Resource creation requests.
     *
     * @throws InterruptedException
     * @throws IOException
     */
    public void startConsumerOfResourceCreationMessages() throws InterruptedException {
        Channel channel;
        try {
            channel = this.connection.createChannel();
            channel.queueDeclare(RESOURCE_CREATION_REQUESTED_QUEUE, true, false, false, null);
            channel.queueBind(RESOURCE_CREATION_REQUESTED_QUEUE, this.resourceExchangeName, this.resourceCreationRequestedRoutingKey);
//            channel.basicQos(1); // to spread the load over multiple servers we set the prefetchCount setting

            log.info("Receiver waiting for Resource Creation messages....");

            Consumer consumer = new ResourceCreationRequestConsumer(channel, this, authorizationManager, repositoryManager);
            channel.basicConsume(RESOURCE_CREATION_REQUESTED_QUEUE, false, consumer);
        } catch (IOException e) {
            log.error(e);
        }
    }

    /**
     * Method creates queue and binds it globally available exchange and adequate Routing Key.
     * It also creates a consumer for messages incoming to this queue, regarding to Resource modification requests.
     *
     * @throws InterruptedException
     * @throws IOException
     */
    public void startConsumerOfResourceModificationMessages() throws InterruptedException {
        Channel channel;
        try {
            channel = this.connection.createChannel();
            channel.queueDeclare(RESOURCE_MODIFICATION_REQUESTED_QUEUE, true, false, false, null);
            channel.queueBind(RESOURCE_MODIFICATION_REQUESTED_QUEUE, this.resourceExchangeName,
                    this.resourceModificationRequestedRoutingKey);
//            channel.basicQos(1); // to spread the load over multiple servers we set the prefetchCount setting

            log.info("Receiver waiting for Resource Modification messages....");

            Consumer consumer = new ResourceModificationRequestConsumer(channel, this, authorizationManager, repositoryManager);
            channel.basicConsume(RESOURCE_MODIFICATION_REQUESTED_QUEUE, false, consumer);
        } catch (IOException e) {
            log.error(e);
        }
    }

    /**
     * Method creates queue and binds it globally available exchange and adequate Routing Key.
     * It also creates a consumer for messages incoming to this queue, regarding to Resource removal requests.
     *
     * @throws InterruptedException
     * @throws IOException
     */
    public void startConsumerOfResourceRemovalMessages() throws InterruptedException {
        Channel channel;
        try {
            channel = this.connection.createChannel();
            channel.queueDeclare(RESOURCE_REMOVAL_REQUESTED_QUEUE, true, false, false, null);
            channel.queueBind(RESOURCE_REMOVAL_REQUESTED_QUEUE, this.resourceExchangeName, this.resourceRemovalRequestedRoutingKey);
//            channel.basicQos(1); // to spread the load over multiple servers we set the prefetchCount setting

            log.info("Receiver waiting for Resource Removal messages....");

            Consumer consumer = new ResourceRemovalRequestConsumer(channel, repositoryManager, this, authorizationManager);
            channel.basicConsume(RESOURCE_REMOVAL_REQUESTED_QUEUE, false, consumer);
        } catch (IOException e) {
            log.error(e);
        }
    }

    /**
     * Method creates queue and binds it globally available exchange and adequate Routing Key.
     * It also creates a consumer for messages incoming to this queue, regarding to Platform creation requests.
     *
     * @throws InterruptedException
     * @throws IOException
     */
    public void startConsumerOfPlatformResourcesRequestsMessages() throws InterruptedException {
        Channel channel;
        try {
            channel = this.connection.createChannel();
            channel.queueDeclare(PLATFORM_RESOURCES_REQUESTED_QUEUE, true, false, false, null);
            channel.queueBind(PLATFORM_RESOURCES_REQUESTED_QUEUE, this.platformExchangeName, this.platformResourcesRequestedRoutingKey);
//            channel.basicQos(1); // to spread the load over multiple servers we set the prefetchCount setting

            log.info("Receiver waiting for Platform Resources Requests messages....");

            Consumer consumer = new PlatformResourcesRequestConsumer(channel, repositoryManager, this, authorizationManager);
            channel.basicConsume(PLATFORM_RESOURCES_REQUESTED_QUEUE, false, consumer);
        } catch (IOException e) {
            log.error(e);
        }
    }

    /**
     * Method creates queue and binds it globally available exchange and adequate Routing Key.
     * It also creates a consumer for messages incoming to this queue, regarding to Platform creation requests.
     *
     * @throws InterruptedException
     * @throws IOException
     */
    public void startConsumerOfPlatformCreationMessages() throws InterruptedException {
        Channel channel;
        try {
            channel = this.connection.createChannel();
            channel.queueDeclare(PLATFORM_CREATION_REQUESTED_QUEUE, true, false, false, null);
            channel.queueBind(PLATFORM_CREATION_REQUESTED_QUEUE, this.platformExchangeName, this.platformCreationRequestedRoutingKey);
//            channel.basicQos(1); // to spread the load over multiple servers we set the prefetchCount setting

            log.info("Receiver waiting for Platform Creation messages....");

            Consumer consumer = new PlatformCreationRequestConsumer(channel, repositoryManager, this);
            channel.basicConsume(PLATFORM_CREATION_REQUESTED_QUEUE, false, consumer);
        } catch (IOException e) {
            log.error(e);
        }
    }

    /**
     * Method creates queue and binds it globally available exchange and adequate Routing Key.
     * It also creates a consumer for messages incoming to this queue, regarding to Platform modification requests.
     *
     * @throws InterruptedException
     * @throws IOException
     */
    public void startConsumerOfPlatformModificationMessages() throws InterruptedException {
        Channel channel;
        try {
            channel = this.connection.createChannel();
            channel.queueDeclare(PLATFORM_MODIFICATION_REQUESTED_QUEUE, true, false, false, null);
            channel.queueBind(PLATFORM_MODIFICATION_REQUESTED_QUEUE, this.platformExchangeName, this.platformModificationRequestedRoutingKey);
//            channel.basicQos(1); // to spread the load over multiple servers we set the prefetchCount setting

            log.info("Receiver waiting for Platform Modification messages....");

            Consumer consumer = new PlatformModificationRequestConsumer(channel, repositoryManager, this);
            channel.basicConsume(PLATFORM_MODIFICATION_REQUESTED_QUEUE, false, consumer);
        } catch (IOException e) {
            log.error(e);
        }
    }

    /**
     * Method creates queue and binds it globally available exchange and adequate Routing Key.
     * It also creates a consumer for messages incoming to this queue, regarding to Platform removal requests.
     *
     * @throws InterruptedException
     * @throws IOException
     */
    public void startConsumerOfPlatformRemovalMessages() throws InterruptedException {
        Channel channel;
        try {
            channel = this.connection.createChannel();
            channel.queueDeclare(PLATFORM_REMOVAL_REQUESTED_QUEUE, true, false, false, null);
            channel.queueBind(PLATFORM_REMOVAL_REQUESTED_QUEUE, this.platformExchangeName, this.platformRemovalRequestedRoutingKey);
//            channel.basicQos(1); // to spread the load over multiple servers we set the prefetchCount setting

            log.info("Receiver waiting for Platform Removal messages....");

            Consumer consumer = new PlatformRemovalRequestConsumer(channel, repositoryManager, this);
            channel.basicConsume(PLATFORM_REMOVAL_REQUESTED_QUEUE, false, consumer);
        } catch (IOException e) {
            log.error(e);
        }
    }

    /**
     * Method creates queue and binds it globally available exchange and adequate Routing Key.
     * It also creates a consumer for messages incoming to this queue, regarding to Platform creation requests.
     *
     * @throws InterruptedException
     * @throws IOException
     */
    public void startConsumerOfInformationModelCreationMessages() throws InterruptedException {
        Channel channel;
        try {
            channel = this.connection.createChannel();
            channel.queueDeclare(INFORMATION_MODEL_CREATION_REQUESTED_QUEUE, true, false, false, null);
            channel.queueBind(INFORMATION_MODEL_CREATION_REQUESTED_QUEUE, this.informationModelExchangeName, this.informationModelCreationRequestedRoutingKey);
//            channel.basicQos(1); // to spread the load over multiple servers we set the prefetchCount setting

            log.info("Receiver waiting for Information Model Creation messages....");

            Consumer consumer = new InformationModelCreationRequestConsumer(channel, this, authorizationManager);
            channel.basicConsume(INFORMATION_MODEL_CREATION_REQUESTED_QUEUE, false, consumer);
        } catch (IOException e) {
            log.error(e);
        }
    }

    /**
     * Method creates queue and binds it globally available exchange and adequate Routing Key.
     * It also creates a consumer for messages incoming to this queue, regarding to Platform modification requests.
     *
     * @throws InterruptedException
     * @throws IOException
     */
    public void startConsumerOfInformationModelModificationMessages() throws InterruptedException {
        Channel channel;
        try {
            channel = this.connection.createChannel();
            channel.queueDeclare(INFORMATION_MODEL_MODIFICATION_REQUESTED_QUEUE, true, false, false, null);
            channel.queueBind(INFORMATION_MODEL_MODIFICATION_REQUESTED_QUEUE, this.informationModelExchangeName, this.informationModelModificationRequestedRoutingKey);
//            channel.basicQos(1); // to spread the load over multiple servers we set the prefetchCount setting

            log.info("Receiver waiting for Information Model Modification messages....");

            Consumer consumer = new InformationModelModificationRequestConsumer(channel, this, authorizationManager);
            channel.basicConsume(INFORMATION_MODEL_MODIFICATION_REQUESTED_QUEUE, false, consumer);
        } catch (IOException e) {
            log.error(e);
        }
    }

    /**
     * Method creates queue and binds it globally available exchange and adequate Routing Key.
     * It also creates a consumer for messages incoming to this queue, regarding to Platform removal requests.
     *
     * @throws InterruptedException
     * @throws IOException
     */
    public void startConsumerOfInformationModelRemovalMessages() throws InterruptedException {
        Channel channel;
        try {
            channel = this.connection.createChannel();
            channel.queueDeclare(INFORMATION_MODEL_REMOVAL_REQUESTED_QUEUE, true, false, false, null);
            channel.queueBind(INFORMATION_MODEL_REMOVAL_REQUESTED_QUEUE, this.informationModelExchangeName, this.informationModelRemovalRequestedRoutingKey);
//            channel.basicQos(1); // to spread the load over multiple servers we set the prefetchCount setting

            log.info("Receiver waiting for Information Model Removal messages....");

            Consumer consumer = new InformationModelRemovalRequestConsumer(channel, this, authorizationManager, repositoryManager);
            channel.basicConsume(INFORMATION_MODEL_REMOVAL_REQUESTED_QUEUE, false, consumer);
        } catch (IOException e) {
            log.error(e);
        }
    }

    /**
     * Method creates queue and binds it globally available exchange and adequate Routing Key.
     * It also creates a consumer for messages incoming to this queue, regarding to Platform creation requests.
     *
     * @throws InterruptedException
     * @throws IOException
     */
    public void startConsumerOfListAllInformationModelsRequestsMessages() throws InterruptedException {
        Channel channel;
        try {
            channel = this.connection.createChannel();
            channel.queueDeclare(INFORMATION_MODELS_REQUESTED_QUEUE, true, false, false, null);
            channel.queueBind(INFORMATION_MODELS_REQUESTED_QUEUE, this.informationModelExchangeName, this.informationModelsRequestedRoutingKey);
//            channel.basicQos(1); // to spread the load over multiple servers we set the prefetchCount setting

            log.info("Receiver waiting for List All Information Models Requests messages....");

            Consumer consumer = new ListInformationModelsRequestConsumer(channel, repositoryManager, this, authorizationManager);
            channel.basicConsume(INFORMATION_MODELS_REQUESTED_QUEUE, false, consumer);
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
            log.info("- resources operation (" + operationType + ") message sent (fanout). Contents:\n" + message);
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
            log.info("- information model operation (" + operationType + ") message sent (fanout). Contents:\n" + message);
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

    public void sendResourceRdfValidationRpcMessage(DefaultConsumer rpcConsumer,
                                                    AMQP.BasicProperties rpcProperties,
                                                    Envelope rpcEnvelope,
                                                    String message,
                                                    String platformId,
                                                    RegistryOperationType operationType,
                                                    AuthorizationManager authorizationManager) {
        sendResourceValidationRpcMessageToSemanticManager(rpcConsumer, rpcProperties, rpcEnvelope,
                this.resourceExchangeName,
                this.rdfResourceValidationRequestedRoutingKey,
                RDF,
                operationType,
                message,
                platformId,
                authorizationManager);
        log.info("- rdf resource to validation message sent");
    }

    public void sendResourceJsonTranslationRpcMessage(DefaultConsumer rpcConsumer,
                                                      AMQP.BasicProperties rpcProperties,
                                                      Envelope rpcEnvelope,
                                                      String message,
                                                      String platformId,
                                                      RegistryOperationType operationType,
                                                      AuthorizationManager authorizationManager) {
        sendResourceValidationRpcMessageToSemanticManager(rpcConsumer, rpcProperties, rpcEnvelope,
                this.resourceExchangeName,
                this.jsonResourceTranslationRequestedRoutingKey,
                BASIC,
                operationType,
                message,
                platformId,
                authorizationManager);
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
            log.info("- RPC reply Message sent back! Content: " + response);
        } else {
            log.error("Received RPC message without ReplyTo or CorrelationId props.");
        }
        consumer.getChannel().basicAck(envelope.getDeliveryTag(), false);
    }

    private void sendResourceValidationRpcMessageToSemanticManager(DefaultConsumer rpcConsumer, AMQP.BasicProperties rpcProperties,
                                                                   Envelope rpcEnvelope, String exchangeName, String routingKey,
                                                                   DescriptionType descriptionType, RegistryOperationType operationType,
                                                                   String message, String platformId, AuthorizationManager authorizationManager) {
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
                            authorizationManager);

            rpcChannel.basicConsume(replyQueueName, true, responseConsumer);

            log.info("Sending RPC message to Semantic Manager... \nMessage params:\nExchange name: "
                    + exchangeName + "\nRouting key: " + routingKey + "\nProps: " + props + "\nMessage: "
                    + message);

            rpcChannel.basicPublish(exchangeName, routingKey, true, props, message.getBytes());

        } catch (IOException e) {
            log.error(e);
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
                    + exchangeName + "\nRouting key: " + routingKey + "\nProps: " + props + "\nMessage: "
                    + message);
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
        Channel channel = null;
        try {
            channel = this.connection.createChannel();
            Map<String, Object> headers = new HashMap<>();
            headers.put("__TypeId__", classType);
            headers.put("__ContentTypeId__", Object.class.getCanonicalName());
            AMQP.BasicProperties props = new AMQP.BasicProperties()
                    .builder()
                    .contentType("application/json")
                    .headers(headers)
                    .build();

            log.info("Sending message. Content: " + message);
            channel.basicPublish(exchange, routingKey, props, message.getBytes());
        } catch (IOException e) {
            log.error(e);
        } finally {
            closeChannel(channel);
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
                            rpcChannel, repositoryManager, this, operationType, authorizationManager);

            rpcChannel.basicConsume(replyQueueName, true, responseConsumer);

            log.info("Sending RPC message to Semantic Manager... \nMessage params:" +
                    "\nExchange name: " + informationModelExchangeName
                    + "\nRouting key: " + rdfInformationModelValidationRequestedRoutingKey
                    + "\nProps: " + props
                    + "\nMessage: " + message);

            rpcChannel.basicPublish(
                    this.informationModelExchangeName,
                    this.rdfInformationModelValidationRequestedRoutingKey,
                    true, props, message.getBytes());

        } catch (IOException e) {
            log.error(e);
        }
    }

    public void sendFederationOperationMessage(Federation federation, RegistryOperationType operationType) {
        //// TODO: 22.08.2017 IMPLEMENT!
    }

    public String getOwnersOfPlatformsFromAAM(String platformIds) {
        return sendRpcMessageAndConsumeResponse(aamExchangeName, aamGetPlatformOwners, platformIds);
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