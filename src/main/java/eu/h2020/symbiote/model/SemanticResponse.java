package eu.h2020.symbiote.model;

import org.apache.commons.lang.builder.ReflectionToStringBuilder;

/**
 * Created by mateuszl on 20.03.2017.
 */
public class SemanticResponse {

    private int status;
    private String message;
    private String body; //todo List<CoreResources>

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

    @Override
    public String toString() {
        return ReflectionToStringBuilder.toString(this);
    }
}
