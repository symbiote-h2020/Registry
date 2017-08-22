package eu.h2020.symbiote.model;

import eu.h2020.symbiote.core.internal.StatusResponse;
import eu.h2020.symbiote.core.model.Federation;

import java.util.List;

/**
 * Created by mateuszl on 22.08.2017.
 */
@Deprecated //todo move to SYM LIBS and remove Deprecation
public class FederationListResponse extends StatusResponse<List<Federation>> {

    public FederationListResponse() {
    }

    public FederationListResponse(int status, String message, List<Federation> body) {
        super(status, message, body);
    }

    public List<Federation> getFederations() {
        return super.getBody();
    }

    public void setFederations(List<Federation> federations) {
        super.setBody(federations);
    }
}
