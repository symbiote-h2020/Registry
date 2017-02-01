package eu.h2020.symbiote.model;

/**
 * Created by mateuszl on 17.01.2017.
 */
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

    public int getStatus() {
        return status;
    }

    public void setStatus(int status) {
        this.status = status;
    }

    public Resource getResource() {
        return resource;
    }

    public void setResource(Resource resource) {
        this.resource = resource;
    }
}
