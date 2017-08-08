package eu.h2020.symbiote.model;

import eu.h2020.symbiote.core.model.resources.Resource;
import org.apache.commons.lang.builder.ReflectionToStringBuilder;

/**
 * Class used as a response to RPC call requesting resource actions
 *
 * Created by mateuszl
 */
public class ResourcePersistenceResult extends AbstractRegistryPersistenceResult<Resource> {

    public ResourcePersistenceResult() {
    }

    public ResourcePersistenceResult(int status, String message, Resource persistenceObject) {
        super(status, message, persistenceObject);
    }

    public void setResource(Resource resource){
        super.setPersistenceObject(resource);
    }

    public Resource getResource(){
        return super.getPersistenceObject();
    }

    @Override
    public String toString() {
        return ReflectionToStringBuilder.toString(this);
    }
}
