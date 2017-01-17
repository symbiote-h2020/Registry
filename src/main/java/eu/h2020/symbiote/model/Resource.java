package eu.h2020.symbiote.model;

import java.util.List;

/**
 * Created by jawora on 21.12.16.
 */
public class Resource {
    private String id;
    private String name;
    private String owner;
    private String description;
    private List<String> observedProperties;
    private String resourceURL;
    private Location location;

    public Resource() {
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getOwner() {
        return owner;
    }

    public void setOwner(String owner) {
        this.owner = owner;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public List<String> getObservedProperties() {
        return observedProperties;
    }

    public void setObservedProperties(List<String> observedProperties) {
        this.observedProperties = observedProperties;
    }

    public String getResourceURL() {
        return resourceURL;
    }

    public void setResourceURL(String resourceURL) {
        this.resourceURL = resourceURL;
    }

    public Location getLocation() {
        return location;
    }

    public void setLocation(Location location) {
        this.location = location;
    }
}
