package eu.h2020.symbiote.model;

/**
 * Created by mateuszl on 12.06.2017.
 */
public class AuthorizationResult {

    public AuthorizationResult(String message, boolean validated) {
        this.message = message;
        this.validated = validated;
    }

    private String message;
    private boolean validated;

    /**
     * @return
     */
    public String getMessage() {
        return message;
    }

    /**
     * @return
     */
    public boolean isValidated() {
        return validated;
    }
}
