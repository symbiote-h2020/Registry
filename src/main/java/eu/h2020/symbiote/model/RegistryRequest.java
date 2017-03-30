package eu.h2020.symbiote.model;

import org.apache.commons.lang.builder.ReflectionToStringBuilder;

/**
 * Created by mateuszl on 20.03.2017.
 */

public class RegistryRequest {
    private RequestType type;
    private String body;
    private String token;

    public RequestType getType() {
        return type;
    }

    public void setType(RequestType type) {
        this.type = type;
    }

    public String getBody() {
        return body;
    }

    public void setBody(String body) {
        this.body = body;
    }

    public String getToken() {
        return token;
    }

    public void setToken(String token) {
        this.token = token;
    }

    @Override
    public String toString() {
        return ReflectionToStringBuilder.toString(this);
    }
}
