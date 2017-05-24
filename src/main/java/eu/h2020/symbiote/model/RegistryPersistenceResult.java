package eu.h2020.symbiote.model;

import eu.h2020.symbiote.core.model.internal.CoreResource;
import org.apache.commons.lang.builder.ReflectionToStringBuilder;

/**
 * Class used as a response to RPC call requesting resource actions
 *
 * Created by mateuszl
 */
public class RegistryPersistenceResult {
    private int status;
    private String message;
    private CoreResource resource;

    public RegistryPersistenceResult(int status, String message, CoreResource resource) {
        this.status = status;
        this.message = message;
        this.resource = resource;
    }

    public RegistryPersistenceResult() {
        // Empty constructor
    }

    /**
     * @return
     */
    public int getStatus() {
        return status;
    }

    /**
     * @param status
     */
    public void setStatus(int status) {
        this.status = status;
    }

    /**
     * @return
     */
    public CoreResource getResource() {
        return resource;
    }

    /**
     * @param resource
     */
    public void setResource(CoreResource resource) {
        this.resource = resource;
    }

    /**
     * @return
     */
    public String getMessage() {
        return message;
    }

    /**
     * @param message
     */
    public void setMessage(String message) {
        this.message = message;
    }

    @Override
    public String toString() {
        return ReflectionToStringBuilder.toString(this);
    }

}
