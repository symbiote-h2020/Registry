package eu.h2020.symbiote.model;

/**
 * Created by mateuszl on 17.01.2017.
 */
public class ResourceCreationResponse {
    private int status;
    private Resource resource;

    public ResourceCreationResponse(int status, Resource resource) {
        this.status = status;
        this.resource = resource;
    }

    public ResourceCreationResponse() {
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
