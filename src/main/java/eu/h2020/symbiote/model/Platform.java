package eu.h2020.symbiote.model;

import org.apache.commons.lang.builder.EqualsBuilder;
import org.apache.commons.lang.builder.HashCodeBuilder;
import org.springframework.data.annotation.Id;

import java.util.List;

/**
 * Registry Platform object
 *
 * Created by mateuszl
 */
public class Platform {
    @Id
    private String id;
    private String label; //todo array of labels
    private String comment; //todo array of comments
    private String body;
    private String format;
    private List<InterworkingService> interworkingServices;

    public Platform() {
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
    public String getLabel() {
        return label;
    }

    /**
     * @param label
     */
    public void setLabel(String label) {
        this.label = label;
    }

    /**
     * @return
     */
    public String getComment() {
        return comment;
    }

    /**
     * @param comment
     */
    public void setComment(String comment) {
        this.comment = comment;
    }

    /**
     * @return
     */
    public String getBody() {
        return body;
    }

    /**
     * @param body
     */
    public void setBody(String body) {
        this.body = body;
    }

    /**
     * @return
     */
    public String getFormat() {
        return format;
    }

    /**
     * @param format
     */
    public void setFormat(String format) {
        this.format = format;
    }

    /**
     * @return
     */
    public List<InterworkingService> getInterworkingServices() {
        return interworkingServices;
    }

    /**
     * @param interworkingServices
     */
    public void setInterworkingServices(List<InterworkingService> interworkingServices) {
        this.interworkingServices = interworkingServices;
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
