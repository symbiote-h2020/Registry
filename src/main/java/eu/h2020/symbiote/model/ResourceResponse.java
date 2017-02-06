package eu.h2020.symbiote.model;

/**
 * Class used as a response to RPC call requesting resource actions
 */
public class ResourceResponse {
    private int status;
    private Resource resource;

    public ResourceResponse(int status, Resource resource) {
        this.status = status;
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
}
