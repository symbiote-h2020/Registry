package eu.h2020.symbiote.model;

/**
 * Created by mateuszl on 29.03.2017.
 */
public class RegistryResponse {

    private int status;
    private String message;
    private String body;

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

    public String getBody() {
        return body;
    }

    public void setBody(String body) {
        this.body = body;
    }
}
