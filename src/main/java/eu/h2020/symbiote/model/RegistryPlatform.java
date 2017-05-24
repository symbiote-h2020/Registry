package eu.h2020.symbiote.model;

import eu.h2020.symbiote.core.model.InterworkingService;
import org.apache.commons.lang.builder.EqualsBuilder;
import org.apache.commons.lang.builder.HashCodeBuilder;
import org.apache.commons.lang.builder.ReflectionToStringBuilder;
import org.springframework.data.annotation.Id;

import java.util.ArrayList;
import java.util.List;

/**
 * Registry Platform object
 * <p>
 * Created by mateuszl
 */
public class RegistryPlatform {
    @Id
    private String id;
    private List<String> labels;
    private List<String> comments;
    private String body;
    private String rdfFormat;
    private List<InterworkingService> interworkingServices;

    public RegistryPlatform() {
        // Empty constructor
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
    public List<String> getLabels() {
        if (this.labels == null) this.labels = new ArrayList<>();
        return labels;
    }

    /**
     * @param labels
     */
    public void setLabels(List<String> labels) {
        this.labels = labels;
    }

    /**
     * @return
     */
    public List<String> getComments() {
        if (this.comments == null) {
            this.comments = new ArrayList<>();
        }
        return comments;
    }

    /**
     * @param comments
     */
    public void setComments(List<String> comments) {
        this.comments = comments;
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
    public String getRdfFormat() {
        return rdfFormat;
    }

    /**
     * @param rdfFormat
     */
    public void setRdfFormat(String rdfFormat) {
        this.rdfFormat = rdfFormat;
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
    public String toString() {
        /*
        StringBuilder sb = new StringBuilder();
        sb.append("Platform with id: " + this.getId() + ", body: " + body + ", rdfFormat: " + rdfFormat + ", labels: [");
        this.getLabels().forEach(s->sb.append(s + ", "));
        sb.append("], comments: [");
        this.getComments().forEach(s->sb.append(s + ", "));
        sb.append("], interworkingServices: [");
        this.getInterworkingServices().forEach(s->sb.append(s +", "));
        sb.append("].");
        return  sb.toString();
        */
        return ReflectionToStringBuilder.toString(this);
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
