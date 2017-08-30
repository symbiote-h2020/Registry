package eu.h2020.symbiote.managers;

import eu.h2020.symbiote.core.model.Federation;
import eu.h2020.symbiote.core.model.InformationModel;
import eu.h2020.symbiote.core.model.InterworkingService;
import eu.h2020.symbiote.core.model.Platform;
import eu.h2020.symbiote.core.model.internal.CoreResource;
import eu.h2020.symbiote.core.model.resources.Resource;
import eu.h2020.symbiote.model.FederationPersistenceResult;
import eu.h2020.symbiote.model.InformationModelPersistenceResult;
import eu.h2020.symbiote.model.PlatformPersistenceResult;
import eu.h2020.symbiote.model.ResourcePersistenceResult;
import eu.h2020.symbiote.repository.FederationRepository;
import eu.h2020.symbiote.repository.InformationModelRepository;
import eu.h2020.symbiote.repository.PlatformRepository;
import eu.h2020.symbiote.repository.ResourceRepository;
import eu.h2020.symbiote.utils.RegistryUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.http.HttpStatus;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

/**
 * Class managing persistence actions for Platforms, Resources and Locations using MongoDB repositories.
 * <p>
 * Created by mateuszl
 */
@Component
public class RepositoryManager {

    private static final String RESOURCE_HAS_NULL_OR_EMPTY_ID = "Resource has null or empty ID!";
    private static final String IM_HAS_NULL_OR_EMPTY_ID = "Information Model has null or empty ID!";
    private static final String FEDERATION_HAS_NULL_OR_EMPTY_ID = "Federation has null or empty ID!";
    private static final String GIVEN_PLATFORM_DOES_NOT_EXIST_IN_DATABASE = "Given platform does not exist in database!";
    private static Log log = LogFactory.getLog(RepositoryManager.class);
    private PlatformRepository platformRepository;
    private ResourceRepository resourceRepository;
    private InformationModelRepository informationModelRepository;
    private FederationRepository federationRepository;

    @Autowired
    public RepositoryManager(PlatformRepository platformRepository,
                             ResourceRepository resourceRepository,
                             InformationModelRepository informationModelRepository,
                             FederationRepository federationRepository) {
        this.platformRepository = platformRepository;
        this.resourceRepository = resourceRepository;
        this.informationModelRepository = informationModelRepository;
        this.federationRepository = federationRepository;
    }

    /**
     * Saves given Platform in MongoDB. It triggers save action in Platform Repository and if it ends successfully
     * it returns http status '200' and Platform object with generated ID field.
     * If given platform is null or it already has an id the method will return 'bad request' status.
     * If saving in DB goes wrong it returns 'internal server error' status.
     * Url of given platform is appended with "/" if it does not end with it.
     *
     * @param platformToSave Platform to save - in JSON format
     * @return PlatformRegistryResponse with Http status code and Platform object with unique "id" (generated in MongoDB)
     */
    public PlatformPersistenceResult savePlatform(Platform platformToSave) {
        PlatformPersistenceResult platformSavingResult = new PlatformPersistenceResult();
        Platform savedPlatform;
        platformSavingResult.setPlatform(platformToSave);

        log.info("Received platform to save: " + platformToSave);

        if (platformToSave.getId() == null || platformToSave.getId().isEmpty()) {
            log.error("Given platform has null or empty id!");
            platformSavingResult.setMessage("Given platform has null or empty id!");
            platformSavingResult.setStatus(HttpStatus.SC_BAD_REQUEST);
        } else {
            if (platformSavingResult.getStatus() != HttpStatus.SC_BAD_REQUEST) {
                try {
                    log.info("Saving platform: " + platformToSave.getId());

                    savedPlatform = platformRepository.save(platformToSave);
                    log.info("Platform \"" + savedPlatform + "\" saved !");
                    platformSavingResult.setStatus(HttpStatus.SC_OK);
                    platformSavingResult.setMessage("OK");
                    platformSavingResult.setPlatform(savedPlatform);
                } catch (Exception e) {
                    log.error("Error occurred during Platform saving to db", e);
                    platformSavingResult.setMessage("Error occurred during Platform saving to db");
                    platformSavingResult.setStatus(HttpStatus.SC_INTERNAL_SERVER_ERROR);
                }
            }
        }
        return platformSavingResult;
    }

    /**
     * Modifies (existing in mongodb) Platform accordingly to fields in given Platform.
     * It triggers delete and save actions in Platform Repository and if it ends successfully,
     * it returns http status '200' and new modified Platform object.
     * Url of given platform is appended with "/" if it does not end with it.
     * If given platform has any null field, it is retrieved from DB and fulfilled.
     * If given platform has no ID or has an empty 'id' field the method will return 'bad request' status.
     * If there is no Platform in database with ID same as given one, it returns 'bad request' status.
     * If saving in DB goes wrong it returns 'internal server error' status.
     *
     * @param platformToModify Platform to remove - in JSON format
     * @return PlatformRegistryResponse with Http status code and modified Platform object - in JSON format
     */
    public PlatformPersistenceResult modifyPlatform(Platform platformToModify) {
        PlatformPersistenceResult platformModifyingResult = new PlatformPersistenceResult();
        platformModifyingResult.setPlatform(platformToModify);

        normalizePlatformsInterworkingServicesUrls(platformToModify);

        Platform foundPlatform = null;
        if (platformToModify.getId() == null || platformToModify.getId().isEmpty()) {
            log.error("Given platform has empty PlatformId!");
            platformModifyingResult.setMessage("Given platform has empty PlatformId!");
            platformModifyingResult.setStatus(HttpStatus.SC_BAD_REQUEST);
        } else {
            foundPlatform = platformRepository.findOne(platformToModify.getId());
        }

        if (foundPlatform == null) {
            log.error(GIVEN_PLATFORM_DOES_NOT_EXIST_IN_DATABASE);
            platformModifyingResult.setMessage(GIVEN_PLATFORM_DOES_NOT_EXIST_IN_DATABASE);
            platformModifyingResult.setStatus(HttpStatus.SC_BAD_REQUEST);
        } else {
            try {
                //fulfilment of empty Platform fields before saving
                Platform modifiedPlatform = copyExistingPlatformData(platformToModify, foundPlatform);

                platformRepository.save(modifiedPlatform);
                log.info("Platform with id: " + modifiedPlatform.getId() + " modified !");

                platformModifyingResult.setStatus(HttpStatus.SC_OK);
                platformModifyingResult.setMessage("OK");
                platformModifyingResult.setPlatform(modifiedPlatform);
            } catch (Exception e) {
                log.error("Error occurred during Platform modifying in db", e);
                platformModifyingResult.setMessage("Error occurred during Platform modifying in db");
                platformModifyingResult.setStatus(HttpStatus.SC_INTERNAL_SERVER_ERROR);
            }
        }
        return platformModifyingResult;
    }

    /**
     * Removes given Platform from MongoDB. It triggers delete action in Platform Repository and if it ends successfully
     * it returns http status '200' and removed Platform object.
     * If given platform is null or it has no id or has an empty 'id' field the method will return 'bad request' status.
     * If saving in DB goes wrong it returns 'internal server error' status.
     *
     * @param platformToRemove Platform to remove - in JSON format
     * @return PlatformRegistryResponse with Http status code and removed Platform object - in JSON format
     */
    public PlatformPersistenceResult removePlatform(Platform platformToRemove) {
        PlatformPersistenceResult platformRemovingResult = new PlatformPersistenceResult();
        platformRemovingResult.setPlatform(platformToRemove);

        if (platformToRemove == null || platformToRemove.getId() == null || platformToRemove.getId().isEmpty()) {
            log.error("Given platform is null or has empty PlatformId!");
            platformRemovingResult.setMessage("Given platform is null or has empty PlatformId!");
            platformRemovingResult.setStatus(HttpStatus.SC_BAD_REQUEST);
        } else if (resourceRepository.findByInterworkingServiceURL(platformToRemove.getId()) != null
                && !resourceRepository.findByInterworkingServiceURL(platformToRemove.getId()).isEmpty()) {
            log.error("Given Platform has registered resources. Take care of resources first.");
            platformRemovingResult.setMessage("Given Platform has registered resources. Take care of resources first.");
            platformRemovingResult.setStatus(HttpStatus.SC_CONFLICT);
        } else {
            try {
                platformRepository.delete(platformToRemove.getId());
                log.info("Platform with id: " + platformToRemove.getId() + " removed !");

                platformRemovingResult.setStatus(HttpStatus.SC_OK);
                platformRemovingResult.setMessage("OK");
            } catch (Exception e) {
                log.error("Error occurred during Platform removing from db", e);
                platformRemovingResult.setMessage("Error occurred during Platform removing from db");
                platformRemovingResult.setStatus(HttpStatus.SC_INTERNAL_SERVER_ERROR);
            }
        }
        return platformRemovingResult;
    }

    //// TODO: 25.07.2017 test method!
    private Platform copyExistingPlatformData(Platform requestedPlatform, Platform foundPlatform) {
        if ((requestedPlatform.getComments() == null || requestedPlatform.getComments().isEmpty() || requestedPlatform.getComments().get(0) == null)
                && foundPlatform.getComments() != null)
            requestedPlatform.setComments(foundPlatform.getComments());
        if (requestedPlatform.getRdfFormat() == null && foundPlatform.getRdfFormat() != null)
            requestedPlatform.setRdfFormat(foundPlatform.getRdfFormat());
        if ((requestedPlatform.getLabels() == null || requestedPlatform.getLabels().isEmpty() || requestedPlatform.getLabels().get(0) == null)
                && foundPlatform.getLabels() != null)
            requestedPlatform.setLabels(foundPlatform.getLabels());
        if (requestedPlatform.getRdf() == null && foundPlatform.getRdf() != null)
            requestedPlatform.setRdf(foundPlatform.getRdf());
        if ((requestedPlatform.getInterworkingServices() == null || requestedPlatform.getInterworkingServices().isEmpty() ||
                requestedPlatform.getInterworkingServices().get(0).getUrl() == null) && foundPlatform.getInterworkingServices() != null)
            requestedPlatform.setInterworkingServices(foundPlatform.getInterworkingServices());

        return requestedPlatform;
    }

    /**
     * Saves resource in MongoDB. Checks if URL in given resource ends with "/" and if not, appends it.
     * If in database there is no Platform with given PlatformId field, the method will return 'bad request' status.
     * If given resource has not null Id field, the method will return 'bad request' status.
     * If in database there is no Platform with given URL same as in given Resource, the method will return 'bad request'.
     * If saving in DB goes wrong it returns 'internal server error' status.
     *
     * @param resource Resource with given properties in JSON format
     * @return ResourceSavingResult containing Http status code and Resource with added 'Id' (generated in MongoDB),
     * in JSON format
     */
    public ResourcePersistenceResult saveResource(CoreResource resource) {
        ResourcePersistenceResult resourceSavingResult = new ResourcePersistenceResult();
        resourceSavingResult.setResource(resource);

        if (resource.getId() == null || resource.getId().isEmpty()) {
            log.error(RESOURCE_HAS_NULL_OR_EMPTY_ID);
            resourceSavingResult.setMessage(RESOURCE_HAS_NULL_OR_EMPTY_ID);
            resourceSavingResult.setStatus(HttpStatus.SC_BAD_REQUEST);
        } else {
            try {
                log.info("Saving Resource: " + resource.toString());

                CoreResource savedResource = resourceRepository.save(resource);
                log.info("Resource with id: " + savedResource.getId() + " saved !");

                resourceSavingResult.setStatus(HttpStatus.SC_OK);
                resourceSavingResult.setMessage("OK");
                resourceSavingResult.setResource(savedResource);
            } catch (Exception e) {
                log.error("Error occurred during Resource saving in db", e);
                resourceSavingResult.setMessage("Error occurred during Resource saving in db");
                resourceSavingResult.setStatus(HttpStatus.SC_INTERNAL_SERVER_ERROR);
            }
        }
        return resourceSavingResult;
    }

    /**
     * Modifies given resource in MongoDB. If given resource does not consist some of the fields, empty ones are
     * fulfilled with data from the database.
     *
     * @param resource Resource with given properties in JSON format
     * @return ResourceSavingResult containing Http status code and Modified Resource, in JSON format
     */
    public ResourcePersistenceResult modifyResource(CoreResource resource) {
        ResourcePersistenceResult resourceSavingResult = new ResourcePersistenceResult();
        CoreResource foundResource;
        resourceSavingResult.setResource(resource);

        if (resource.getId() == null || resource.getId().isEmpty()) {
            log.error(RESOURCE_HAS_NULL_OR_EMPTY_ID);
            resourceSavingResult.setMessage(RESOURCE_HAS_NULL_OR_EMPTY_ID);
            resourceSavingResult.setStatus(HttpStatus.SC_BAD_REQUEST);
            return resourceSavingResult;
        }

        if (resource.getInterworkingServiceURL() == null || resource.getInterworkingServiceURL().isEmpty()) {
            log.error("Given resource has null or empty Interworking service URL!");
            resourceSavingResult.setMessage("Given resource has null or empty Interworking service URL!");
            resourceSavingResult.setStatus(HttpStatus.SC_BAD_REQUEST);
            return resourceSavingResult;
        }

        normalizeResourceInterworkingServiceUrl(resource);

        foundResource = resourceRepository.findOne(resource.getId());

        if (foundResource == null) {
            log.error("Given resource does not exist in database!");
            resourceSavingResult.setMessage("Given resource does not exist in database!");
            resourceSavingResult.setStatus(HttpStatus.SC_BAD_REQUEST);
            return resourceSavingResult;
        }

        try {
            CoreResource savedResource = resourceRepository.save(resource);
            log.info("Resource with id: " + resource.getId() + " modified !");

            resourceSavingResult.setStatus(HttpStatus.SC_OK);
            resourceSavingResult.setMessage("OK");
            resourceSavingResult.setResource(savedResource);
        } catch (Exception e) {
            log.error("Error occurred during Resource modifying in db", e);
            resourceSavingResult.setMessage("Error occurred during Resource modifying in db");
            resourceSavingResult.setStatus(HttpStatus.SC_INTERNAL_SERVER_ERROR);
        }
        return resourceSavingResult;
    }

    /**
     * Deletes resource from MongoDB
     *
     * @param resource Resource with given properties in JSON format
     * @return ResourceSavingResult containing Http status code and Deleted Resource, in JSON format
     */
    public ResourcePersistenceResult removeResource(Resource resource) {
        ResourcePersistenceResult resourceRemovalResult = new ResourcePersistenceResult();

        if (resource == null || resource.getId() == null || resource.getId().isEmpty()) {
            log.error("Given resource is null or it has null or empty ID!");
            resourceRemovalResult.setMessage("Given resource is null or it has null or empty ID!");
            resourceRemovalResult.setStatus(HttpStatus.SC_BAD_REQUEST);
        } else {
            resourceRemovalResult.setResource(RegistryUtils.convertResourceToCoreResource(resource));

            CoreResource foundResource = resourceRepository.findOne(resource.getId());
            if (foundResource != null) {
                try {
                    resourceRepository.delete(resource.getId());
                    resourceRemovalResult.setStatus(HttpStatus.SC_OK);
                    resourceRemovalResult.setMessage("OK");
                    log.info("Resource with id: " + resource.getId() + " removed !");
                } catch (Exception e) {
                    log.error("Error occurred during Resource deleting from db", e);
                    resourceRemovalResult.setMessage("Error occurred during Resource deleting from db");
                    resourceRemovalResult.setStatus(HttpStatus.SC_INTERNAL_SERVER_ERROR);
                }
            } else {
                log.error("Given resource does not exist in database");
                resourceRemovalResult.setMessage("Given resource does not exist in database");
                resourceRemovalResult.setStatus(HttpStatus.SC_BAD_REQUEST);
            }
        }
        return resourceRemovalResult;
    }

    public InformationModelPersistenceResult saveInformationModel(InformationModel informationModel) {

        InformationModelPersistenceResult informationModelPersistenceResult = new InformationModelPersistenceResult();
        informationModelPersistenceResult.setInformationModel(informationModel);

        if (informationModel.getId() == null || informationModel.getId().isEmpty()) {
            log.error(IM_HAS_NULL_OR_EMPTY_ID);
            informationModelPersistenceResult.setMessage(IM_HAS_NULL_OR_EMPTY_ID);
            informationModelPersistenceResult.setStatus(HttpStatus.SC_BAD_REQUEST);
        } else {
            try {
                log.info("Saving Information Model: " + informationModel.toString());

                InformationModel savedIM = informationModelRepository.save(informationModel);
                log.info("Information Model with id: " + savedIM.getId() + " saved !");

                informationModelPersistenceResult.setStatus(HttpStatus.SC_OK);
                informationModelPersistenceResult.setMessage("OK");
                informationModelPersistenceResult.setInformationModel(savedIM);
            } catch (Exception e) {
                log.error("Error occurred during Information Model saving in db", e);
                informationModelPersistenceResult.setMessage("Error occurred during Information Model saving in db");
                informationModelPersistenceResult.setStatus(HttpStatus.SC_INTERNAL_SERVER_ERROR);
            }
        }
        return informationModelPersistenceResult;
    }

    public InformationModelPersistenceResult modifyInformationModel(InformationModel informationModel) {
        InformationModelPersistenceResult informationModelPersistenceResult = new InformationModelPersistenceResult();
        informationModelPersistenceResult.setInformationModel(informationModel);

        if (informationModel.getId() == null || informationModel.getId().isEmpty()) {
            log.error(IM_HAS_NULL_OR_EMPTY_ID);
            informationModelPersistenceResult.setMessage(IM_HAS_NULL_OR_EMPTY_ID);
            informationModelPersistenceResult.setStatus(HttpStatus.SC_BAD_REQUEST);
            return informationModelPersistenceResult;
        }

        //todo?? normalizeResourceInterworkingServiceUrl(informationModel);

        InformationModel foundInformationModel = informationModelRepository.findOne(informationModel.getId());

        if (foundInformationModel == null) {
            log.error("Given informationModel does not exist in database!");
            informationModelPersistenceResult.setMessage("Given informationModel does not exist in database!");
            informationModelPersistenceResult.setStatus(HttpStatus.SC_BAD_REQUEST);
            return informationModelPersistenceResult;
        }

        try {
            InformationModel savedInformationModel = informationModelRepository.save(informationModel);
            log.info("informationModel with id: " + informationModel.getId() + " modified !");

            informationModelPersistenceResult.setStatus(HttpStatus.SC_OK);
            informationModelPersistenceResult.setMessage("OK");
            informationModelPersistenceResult.setInformationModel(savedInformationModel);
        } catch (Exception e) {
            log.error("Error occurred during informationModel modifying in db", e);
            informationModelPersistenceResult.setMessage("Error occurred during informationModel modifying in db");
            informationModelPersistenceResult.setStatus(HttpStatus.SC_INTERNAL_SERVER_ERROR);
        }

        //todo ?? rollback ??
        return informationModelPersistenceResult;
    }

    public InformationModelPersistenceResult removeInformationModel(InformationModel informationModelReceived) {
        InformationModelPersistenceResult informationModelPersistenceResult = new InformationModelPersistenceResult();
        informationModelPersistenceResult.setInformationModel(informationModelReceived);

        InformationModel foundInformationModel = informationModelRepository.findOne(informationModelReceived.getId());
        if (foundInformationModel != null) {
            try {
                informationModelRepository.delete(informationModelReceived);
                informationModelPersistenceResult.setStatus(200);
                informationModelPersistenceResult.setMessage("ok");
            } catch (Exception e) {
                log.error(e);
                informationModelPersistenceResult.setStatus(HttpStatus.SC_INTERNAL_SERVER_ERROR);
                informationModelPersistenceResult.setMessage(e.toString());
            }
        } else {
            log.info("Given IM does not exist in database!");
            informationModelPersistenceResult.setStatus(HttpStatus.SC_BAD_REQUEST);
            informationModelPersistenceResult.setMessage("Given IM does not exist in database!");
        }
        return informationModelPersistenceResult;
    }

    public FederationPersistenceResult saveFederation(Federation federation) {
        FederationPersistenceResult federationPersistenceResult = new FederationPersistenceResult();
        federationPersistenceResult.setFederation(federation);

        if (federation.getId() == null || federation.getId().isEmpty()) {
            log.error(FEDERATION_HAS_NULL_OR_EMPTY_ID);
            federationPersistenceResult.setMessage(FEDERATION_HAS_NULL_OR_EMPTY_ID);
            federationPersistenceResult.setStatus(HttpStatus.SC_BAD_REQUEST);
        } else {
            try {
                log.info("Saving Federation: " + federation.toString());

                Federation savedFederation = federationRepository.save(federation);
                log.info("Federation with id: " + savedFederation.getId() + " saved !");

                federationPersistenceResult.setStatus(HttpStatus.SC_OK);
                federationPersistenceResult.setMessage("OK");
                federationPersistenceResult.setFederation(savedFederation);
            } catch (Exception e) {
                log.error("Error occurred during Federation saving in db", e);
                federationPersistenceResult.setMessage("Error occurred during Federation saving in db");
                federationPersistenceResult.setStatus(HttpStatus.SC_INTERNAL_SERVER_ERROR);
            }
        }
        return federationPersistenceResult;
    }

    public FederationPersistenceResult modifyFederation(Federation federation) {
        FederationPersistenceResult federationPersistenceResult = new FederationPersistenceResult();
        Federation federationFound;
        federationPersistenceResult.setFederation(federation);

        if (federation.getId() == null || federation.getId().isEmpty()) {
            log.error(FEDERATION_HAS_NULL_OR_EMPTY_ID);
            federationPersistenceResult.setMessage(FEDERATION_HAS_NULL_OR_EMPTY_ID);
            federationPersistenceResult.setStatus(HttpStatus.SC_BAD_REQUEST);
        }

        federationFound = federationRepository.findOne(federation.getId());

        if (federationFound == null) {
            log.error("Given federation does not exist in database!");
            federationPersistenceResult.setMessage("Given federation does not exist in database!");
            federationPersistenceResult.setStatus(HttpStatus.SC_BAD_REQUEST);
            return federationPersistenceResult;
        }

        try {
            Federation savedFederation = federationRepository.save(federation);
            log.info("federation with id: " + federation.getId() + " modified !");
            federationPersistenceResult.setStatus(HttpStatus.SC_OK);
            federationPersistenceResult.setMessage("OK");
            federationPersistenceResult.setFederation(savedFederation);
        } catch (Exception e) {
            log.error("Error occurred during federation modifying in db", e);
            federationPersistenceResult.setMessage("Error occurred during federation modifying in db");
            federationPersistenceResult.setStatus(HttpStatus.SC_INTERNAL_SERVER_ERROR);
        }

        //todo ?? rollback ??
        return federationPersistenceResult;
    }

    public FederationPersistenceResult removeFederation(Federation federation) {
        FederationPersistenceResult federationPersistenceResult = new FederationPersistenceResult();
        federationPersistenceResult.setFederation(federation);
        if (federation == null || federation.getId() == null || federation.getId().isEmpty()) {
            log.error("Given resource is null or it has null or empty ID!");
            federationPersistenceResult.setMessage("Given resource is null or it has null or empty ID!");
            federationPersistenceResult.setStatus(HttpStatus.SC_BAD_REQUEST);
        } else {
            Federation foundFederation = federationRepository.findOne(federation.getId());
            if (foundFederation != null) {
                try {
                    federationRepository.delete(federation);
                    federationPersistenceResult.setStatus(200);
                    federationPersistenceResult.setMessage("ok");
                } catch (Exception e) {
                    log.error(e);
                    federationPersistenceResult.setStatus(HttpStatus.SC_INTERNAL_SERVER_ERROR);
                    federationPersistenceResult.setMessage(e.toString());
                }
            } else {
                log.info("Given Federation does not exist in database!");
                federationPersistenceResult.setStatus(HttpStatus.SC_BAD_REQUEST);
                federationPersistenceResult.setMessage("Given Federation does not exist in database!");
            }
        }
        return federationPersistenceResult;
    }

    public List<CoreResource> getResourcesForPlatform(String platformId) {
        Platform platform = platformRepository.findOne(platformId);
        List<CoreResource> coreResources = new ArrayList<>();
        for (InterworkingService interworkingService : platform.getInterworkingServices()) {
            coreResources.addAll(resourceRepository.findByInterworkingServiceURL(interworkingService.getUrl()));
        }
        return coreResources;
    }

    private void normalizeResourceInterworkingServiceUrl(CoreResource resource) {
        if (resource.getInterworkingServiceURL().trim().charAt(resource.getInterworkingServiceURL().length() - 1)
                != "/".charAt(0)) {
            resource.setInterworkingServiceURL(resource.getInterworkingServiceURL().trim() + "/");
        }
    }

    private void normalizePlatformsInterworkingServicesUrls(Platform platform) {
        if (platform.getInterworkingServices() != null && !platform.getInterworkingServices().isEmpty()) {
            for (InterworkingService service : platform.getInterworkingServices()) {
                if (service.getUrl().trim().charAt(service.getUrl().length() - 1) != "/".charAt(0)) {
                    service.setUrl(service.getUrl().trim() + "/");
                }
            }
        }
    }

    public List<InformationModel> getAllInformationModels() {
        return informationModelRepository.findAll();
    }

    public String getInformationModelIdByInterworkingServiceUrl(String platformId, String requestedInterworkingServiceUrl) {
        String id = null;
        Platform platform = platformRepository.findOne(platformId);

        for (InterworkingService interworkingService : platform.getInterworkingServices()) {
            if (interworkingService.getUrl().equals(requestedInterworkingServiceUrl)) {
                id = interworkingService.getInformationModelId();
            }
        }

        return id;
    }

    public List<Federation> getFederationsForPlatform(Platform platform) {
        List<Federation> federations = new ArrayList<>();
        federations = federationRepository.findByMembersPlatformId(platform.getId()); //// TODO: 28.08.2017 check!
        return federations;
    }

    public List<Federation> getAllFederations() {
        return federationRepository.findAll();
    }
}
