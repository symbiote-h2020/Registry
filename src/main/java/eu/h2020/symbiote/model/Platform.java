package eu.h2020.symbiote.model;

import org.apache.commons.lang.builder.EqualsBuilder;
import org.apache.commons.lang.builder.HashCodeBuilder;
import org.springframework.data.annotation.Id;

/**
 * Registry Platform object
 *
 * Created by mateuszl
 */
public class Platform {
    @Id
    private String platformId;
    private String name;
    private String description;
    private String url;
    private String informationModelId;

    public Platform() {

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
    public String getUrl() {
        return url;
    }

    /**
     * @param url
     */
    public void setUrl(String url) {
        this.url = url;
    }

    /**
     * @return
     */
    public String getInformationModelId() {
        return informationModelId;
    }

    /**
     * @param informationModelId
     */
    public void setInformationModelId(String informationModelId) {
        this.informationModelId = informationModelId;
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
