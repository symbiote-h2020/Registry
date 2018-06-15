package eu.h2020.symbiote.model.persistenceResults;

import eu.h2020.symbiote.cloud.model.ssp.SspRegInfo;

public class SdevPersistenceResult extends AbstractPersistenceResult<SspRegInfo> {
    public SdevPersistenceResult() {
    }

    public SdevPersistenceResult(int status, String message){
        super(status, message, null);
    }

    public SdevPersistenceResult(int status, String message, SspRegInfo persistenceObject) {
        super(status, message, persistenceObject);
    }

    public SspRegInfo getSdev() {
        return super.getPersistenceObject();
    }

    public void setSdev(SspRegInfo persistenceObject) {
        super.setPersistenceObject(persistenceObject);
    }
}
