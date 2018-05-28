package eu.h2020.symbiote.model.persistenceResults;

import eu.h2020.symbiote.model.mim.SmartSpace;

/**
 * Class used for repository actions results for SmartSpace objects
 *
 * Created by mateuszl
 */
public class SspPersistenceResult extends AbstractPersistenceResult<SmartSpace> {
    public SspPersistenceResult(int status, String message, SmartSpace persistenceObject) {
        super(status, message, persistenceObject);
    }

    public SspPersistenceResult() {
        super();
    }

    public SmartSpace getSmartSpace() {
        return super.getPersistenceObject();
    }

    public void setSmartSpace(SmartSpace smartSpace) {
        super.setPersistenceObject(smartSpace);
    }
}
