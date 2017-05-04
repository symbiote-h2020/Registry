package eu.h2020.symbiote.messaging;

/**
 * For next release...
 * <p>
 * Created by mateuszl on 27.03.2017.
 */
public class InformationModelCreationRequestConsumer {
}

//public class InformationModelCreationRequestConsumer extends DefaultConsumer {
//
//    private static Log log = LogFactory.getLog(InformationModelCreationRequestConsumer.class);
//    private AuthorizationManager authorizationManager;
//    private InformationModelResponse informationModelResponse;
//    private ObjectMapper mapper;
//    private RepositoryManager repositoryManager;
//    private RabbitManager rabbitManager;
//
//    /**
//     * Constructs a new instance and records its association to the passed-in channel.
//     * Managers beans passed as parameters because of lack of possibility to inject it to consumer.
//     *
//     * @param channel           the channel to which this consumer is attached
//     * @param rabbitManager     rabbit manager bean passed for access to messages manager
//     * @param repositoryManager repository manager bean passed for persistence actions
//     */
//    public InformationModelCreationRequestConsumer(Channel channel,
//                                                   RepositoryManager repositoryManager,
//                                                   RabbitManager rabbitManager,
//                                                   AuthorizationManager authorizationManager) {
//        super(channel);
//        this.repositoryManager = repositoryManager;
//        this.rabbitManager = rabbitManager;
//        this.authorizationManager = authorizationManager;
//        this.informationModelResponse = new InformationModelResponse();
//        this.mapper = new ObjectMapper();
//    }
//
//
//    /**
//     * Called when a <code><b>basic.deliver</b></code> is received for this consumer.
//     *
//     * @param consumerTag the <i>consumer tag</i> associated with the consumer
//     * @param envelope    packaging data for the message
//     * @param properties  content header data for the message
//     * @param body        the message body (opaque, client-specific byte array)
//     * @throws IOException if the consumer encounters an I/O error while processing the message
//     * @see Envelope
//     */
//    @Override
//    public void handleDelivery(String consumerTag, Envelope envelope,
//                               AMQP.BasicProperties properties, byte[] body)
//            throws IOException {
//        CoreResourceRegistryRequest request = null;
//        CoreResourceRegistryResponse semanticResponse = new CoreResourceRegistryResponse();
//        semanticResponse.setStatus(HttpStatus.SC_BAD_REQUEST);
//        String response;
//        InformationModel informationModel = null;
//        String message = new String(body, "UTF-8");
//
//        log.info(" [x] Received information model to create: '" + message + "'");
//
//        try {
//            request = mapper.readValue(message, CoreResourceRegistryRequest.class);
//        } catch (JsonSyntaxException e) {
//            log.error("Error occured during getting Operation Request from Json", e);
//            informationModelResponse.setStatus(HttpStatus.SC_BAD_REQUEST);
//            informationModelResponse.setMessage("Error occured during getting Operation Request from Json");
//        }
//
//        if (request != null) {
//            informationModel = getInformationModel(request);
//        } else {
//            log.error("Request is null");
//            informationModelResponse.setStatus(HttpStatus.SC_BAD_REQUEST);
//            informationModelResponse.setMessage("Request is null");
//        }
//
//        if (informationModel != null) {
//            informationModelResponse.setInformationModel(informationModel);
//            if (RegistryUtils.validateFields(informationModel)) {
//                if (informationModel.getBody() == null)
//                    informationModel = RegistryUtils.getRdfBodyForObject(informationModel);
//                informationModelResponse = this.repositoryManager.saveInformationModel(informationModel);
//                if (informationModelResponse.getStatus() == 200) {
//                    rabbitManager.sendInformationModelCreatedMessage(informationModelResponse.getInformationModel());
//                }
//            } else {
//                log.error("Given Information Model has some fields null or empty");
//                informationModelResponse.setMessage("Given Information Model has some fields null or empty");
//                informationModelResponse.setStatus(HttpStatus.SC_BAD_REQUEST);
//            }
//        } else {
//            log.error("Information Model is null");
//            informationModelResponse.setStatus(HttpStatus.SC_BAD_REQUEST);
//            informationModelResponse.setMessage("Information Model is null");
//        }
//
//        response = mapper.writeValueAsString(informationModelResponse);
//        rabbitManager.sendRPCReplyMessage(this, properties, envelope, response);
//    }
//
//    private InformationModel getInformationModel(CoreResourceRegistryRequest request) throws IOException {
//        InformationModel informationModel = new InformationModel();
//
//        if (authorizationManager.checkAccess(request.getToken())) {
//            switch (request.getDescriptionType()) {
//                case RDF:
//                    informationModel = getInformationModelFromRdf(request);
//                    break;
//                case BASIC:
//                    informationModel = readInformationModelFromBasic(request.getBody());
//                    break;
//            }
//
//        } else {
//            log.error("Token invalid");
//            informationModelResponse.setStatus(HttpStatus.SC_UNAUTHORIZED);
//            informationModelResponse.setMessage("Token invalid");
//        }
//        return informationModel;
//    }
//
//    private InformationModel getInformationModelFromRdf(CoreResourceRegistryRequest request) throws IOException {
//        InformationModel informationModel1 = new InformationModel();
//        try {
//            CoreResourceRegistryResponse semanticResponse = RegistryUtils.getInformationModelFromRdf(request.getBody());
//            informationModel1 = readInformationModelFromBasic(semanticResponse.getBody());
//        } catch (JsonSyntaxException e) {
//            log.error("Error occured during getting model from Json received from Semantic Manager", e);
//            informationModelResponse.setStatus(HttpStatus.SC_BAD_REQUEST);
//            informationModelResponse.setMessage("Error occured during getting Platforms from Json");
//        }
//        return informationModel1;
//    }
//
//    private InformationModel readInformationModelFromBasic(String body) throws IOException {
//        InformationModel informationModel = new InformationModel();
//        try {
//            informationModel = mapper.readValue(body, InformationModel.class);
//        } catch (JsonSyntaxException e) {
//            log.error("Error occured during getting Information Model from Json", e);
//            informationModelResponse.setStatus(HttpStatus.SC_BAD_REQUEST);
//            informationModelResponse.setMessage("Error occured during getting Information Model from Json");
//        }
//        return informationModel;
//    }
//}