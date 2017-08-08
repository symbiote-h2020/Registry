package eu.h2020.symbiote.model;

import eu.h2020.symbiote.core.model.internal.CoreResource;
import org.apache.commons.lang.builder.ReflectionToStringBuilder;

/**
 * Class used as a response to RPC call requesting resource actions
 *
 * Created by mateuszl
 */
public class ResourcePersistenceResult extends AbstractPersistenceResult<CoreResource> {

    public ResourcePersistenceResult() {
    }

    public ResourcePersistenceResult(int status, String message, CoreResource persistenceObject) {
        super(status, message, persistenceObject);
    }

    public void setResource(CoreResource resource){
        super.setPersistenceObject(resource);
    }

    public CoreResource getResource(){
        return super.getPersistenceObject();
    }

    @Override
    public String toString() {
        return ReflectionToStringBuilder.toString(this);
    }
}
