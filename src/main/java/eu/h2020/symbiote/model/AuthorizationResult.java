package eu.h2020.symbiote.model;

/**
 * Created by mateuszl on 12.06.2017.
 */
public class AuthorizationResult {

    private String message;
    private boolean validated;

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public boolean isValidated() {
        return validated;
    }

    public void setValidated(boolean validated) {
        this.validated = validated;
    }

    public AuthorizationResult(String message, boolean validated) {
        this.message = message;
        this.validated = validated;
    }
}
