package eu.h2020.symbiote.model;

import eu.h2020.symbiote.model.cim.Resource;

/**
 * Smart Space Resource Object, contains of a Resource Object and its Sdev ID.
 */
public class CoreSspResource {

    private String SdevId;
    private Resource resource;

    public CoreSspResource() {
    }

    public CoreSspResource(String sdevId, Resource resource) {
        SdevId = sdevId;
        this.resource = resource;
    }

    public String getSdevId() {
        return SdevId;
    }

    public void setSdevId(String sdevId) {
        SdevId = sdevId;
    }

    public Resource getResource() {
        return resource;
    }

    public void setResource(Resource resource) {
        this.resource = resource;
    }
}
