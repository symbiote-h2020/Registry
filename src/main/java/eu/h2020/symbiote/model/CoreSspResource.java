package eu.h2020.symbiote.model;

import eu.h2020.symbiote.core.internal.CoreResource;

/**
 * Smart Space Resource Object, contains of an unique ID, a Resource Object and Sdev ID.
 */
public class CoreSspResource {

    private String id;
    private String SdevId;
    private CoreResource resource;

    public CoreSspResource() {
    }

    public CoreSspResource(String sdevId, CoreResource resource) {
        SdevId = sdevId;
        this.resource = resource;
    }

    public CoreSspResource(String id, String sdevId, CoreResource resource) {
        this.id = id;
        SdevId = sdevId;
        this.resource = resource;
    }

    public String getSdevId() {
        return SdevId;
    }

    public void setSdevId(String sdevId) {
        SdevId = sdevId;
    }

    public CoreResource getResource() {
        return resource;
    }

    public void setResource(CoreResource resource) {
        this.resource = resource;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }
}
