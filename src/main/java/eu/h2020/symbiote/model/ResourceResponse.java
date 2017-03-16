package eu.h2020.symbiote.model;

/**
 * Class used as a response to RPC call requesting resource actions
 *
 * Created by mateuszl
 */
public class ResourceResponse {
    private int status;
    private String message;
    private Resource resource;

    public ResourceResponse(int status, String message, Resource resource) {
        this.status = status;
        this.message = message;
        this.resource = resource;
    }

    public ResourceResponse() {
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
    public Resource getResource() {
        return resource;
    }

    /**
     * @param resource
     */
    public void setResource(Resource resource) {
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
}
