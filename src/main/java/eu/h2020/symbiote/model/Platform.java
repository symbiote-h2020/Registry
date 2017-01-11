package eu.h2020.symbiote.model;

import com.google.gson.Gson;
import com.google.gson.JsonSyntaxException;
import org.apache.commons.lang.builder.EqualsBuilder;
import org.apache.commons.lang.builder.HashCodeBuilder;
import org.springframework.data.annotation.Id;

import java.net.URL;

/**
 * Created by mateuszl on 09.01.2017.
 */
public class Platform {

    @Id
    private String id;
    private String owner;
    private String name;
    private String type;
    private String description;
    private URL resourceAccessProxyUrl;

    public Platform() {

    }

    public Platform(String owner, String name, String type, String description, URL resourceAccessProxyUrl) {
        this.owner = owner;
        this.name = name;
        this.type = type;
        this.description = description;
        this.resourceAccessProxyUrl = resourceAccessProxyUrl;
    }

    public Platform(String id, String owner, String name, String type, String description, URL resourceAccessProxyUrl) {
        this.id = id;
        this.owner = owner;
        this.name = name;
        this.type = type;
        this.description = description;
        this.resourceAccessProxyUrl = resourceAccessProxyUrl;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getOwner() {
        return owner;
    }

    public void setOwner(String owner) {
        this.owner = owner;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public URL getResourceAccessProxyUrl() {
        return resourceAccessProxyUrl;
    }

    public void setResourceAccessProxyUrl(URL resourceAccessProxyUrl) {
        this.resourceAccessProxyUrl = resourceAccessProxyUrl;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }


    @Override
    public int hashCode() {
        return HashCodeBuilder.reflectionHashCode(this);
    }

    @Override
    public boolean equals(Object obj) {
        return EqualsBuilder.reflectionEquals(this, obj);
    }

}
