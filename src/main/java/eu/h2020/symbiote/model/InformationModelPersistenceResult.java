package eu.h2020.symbiote.model;

import eu.h2020.symbiote.model.mim.InformationModel;

/**
 * Created by mateuszl on 11.08.2017.
 */
public class InformationModelPersistenceResult extends AbstractPersistenceResult<InformationModel> {
    public InformationModelPersistenceResult() {
    }

    public InformationModelPersistenceResult(int status, String message, InformationModel persistenceObject) {
        super(status, message, persistenceObject);
    }

    public InformationModel getInformationModel() {
        return super.getPersistenceObject();
    }

    public void setInformationModel(InformationModel informationModel) {
        super.setPersistenceObject(informationModel);
    }
}
