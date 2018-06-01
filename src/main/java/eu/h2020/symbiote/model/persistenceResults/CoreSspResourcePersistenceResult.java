package eu.h2020.symbiote.model.persistenceResults;

import eu.h2020.symbiote.model.CoreSspResource;

/**
 * Created by mateuszl on 01.06.2018.
 */
public class CoreSspResourcePersistenceResult extends AbstractPersistenceResult<CoreSspResource> {
    public CoreSspResourcePersistenceResult() {
    }

    public CoreSspResourcePersistenceResult(int status, String message, CoreSspResource persistenceObject) {
        super(status, message, persistenceObject);
    }

    public CoreSspResource getCoreSspResource() {
        return super.getPersistenceObject();
    }

    public void setCoreSspResource(CoreSspResource persistenceObject) {
        super.setPersistenceObject(persistenceObject);
    }
}
