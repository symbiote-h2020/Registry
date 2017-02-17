package eu.h2020.symbiote.model;

import org.springframework.data.annotation.Id;

import java.util.List;

/**
 * Registry Resource object
 *
 * Created by mateuszl
 */
public class Resource {
    @Id
    private String id;
    private String name;
    private String owner;
    private String description;
    private List<String> observedProperties;
    private String resourceURL;
    private Location location;
    private String featureOfInterest = null;
    private String platformId;

    public Resource() {
    }

    /**
     * @return
     */
    public String getId() {
        return id;
    }

    /**
     * @param id
     */
    public void setId(String id) {
        this.id = id;
    }

    /**
     * @return
     */
    public String getName() {
        return name;
    }

    /**
     * @param name
     */
    public void setName(String name) {
        this.name = name;
    }

    /**
     * @return
     */
    public String getOwner() {
        return owner;
    }

    /**
     * @param owner
     */
    public void setOwner(String owner) {
        this.owner = owner;
    }

    /**
     * @return
     */
    public String getDescription() {
        return description;
    }

    /**
     * @param description
     */
    public void setDescription(String description) {
        this.description = description;
    }

    /**
     * @return
     */
    public List<String> getObservedProperties() {
        return observedProperties;
    }

    /**
     * @param observedProperties
     */
    public void setObservedProperties(List<String> observedProperties) {
        this.observedProperties = observedProperties;
    }

    /**
     * @return
     */
    public String getResourceURL() {
        return resourceURL;
    }

    /**
     * @param resourceURL
     */
    public void setResourceURL(String resourceURL) {
        this.resourceURL = resourceURL;
    }

    /**
     * @return
     */
    public Location getLocation() {
        return location;
    }

    /**
     * @param location
     */
    public void setLocation(Location location) {
        this.location = location;
    }

    /**
     * @return
     */
    public String getFeatureOfInterest() {
        return featureOfInterest;
    }

    /**
     * @param featureOfInterest
     */
    public void setFeatureOfInterest(String featureOfInterest) {
        this.featureOfInterest = featureOfInterest;
    }

    /**
     * @return
     */
    public String getPlatformId() {
        return platformId;
    }

    /**
     * @param platformId
     */
    public void setPlatformId(String platformId) {
        this.platformId = platformId;
    }
}
