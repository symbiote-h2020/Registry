package eu.h2020.symbiote.model;

import eu.h2020.symbiote.core.model.Federation;

/**
 * Created by mateuszl on 22.08.2017.
 */
@Deprecated //todo move to SYM LIBS and remove Deprecation
public class FederationRegistryResponse {

    private int status;
    private String message;
    private Federation federation;

    public FederationRegistryResponse() {
    }

    public int getStatus() {
        return status;
    }

    public void setStatus(int status) {
        this.status = status;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public Federation getFederation() {
        return federation;
    }

    public void setFederation(Federation federation) {
        this.federation = federation;
    }
}
