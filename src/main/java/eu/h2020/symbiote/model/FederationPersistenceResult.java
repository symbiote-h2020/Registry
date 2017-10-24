package eu.h2020.symbiote.model;

import eu.h2020.symbiote.model.mim.Federation;

/**
 * Created by mateuszl on 22.08.2017.
 */
public class FederationPersistenceResult extends AbstractPersistenceResult<Federation> {

    public FederationPersistenceResult() {
    }

    public FederationPersistenceResult(int status, String message, Federation persistenceObject) {
        super(status, message, persistenceObject);
    }

    public Federation getFederation() {
        return super.getPersistenceObject();
    }

    public void setFederation(Federation federation) {
        super.setPersistenceObject(federation);
    }
}