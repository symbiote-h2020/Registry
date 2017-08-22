package eu.h2020.symbiote.model;

import eu.h2020.symbiote.core.model.Federation;

/**
 * Created by mateuszl on 22.08.2017.
 */
@Deprecated //todo move to SYM LIBS and remove Deprecation
public class FederationRegistryRequest {

    private String token;
    private Federation federation;

    public FederationRegistryRequest() {
    }

    public String getToken() {
        return token;
    }

    public void setToken(String token) {
        this.token = token;
    }

    public Federation getFederation() {
        return federation;
    }

    public void setFederation(Federation federation) {
        this.federation = federation;
    }
}