package eu.h2020.symbiote.model;

import org.apache.commons.lang.builder.EqualsBuilder;
import org.apache.commons.lang.builder.HashCodeBuilder;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.DBRef;

import java.util.List;

/**
 * Registry Resource object
 *
 * Created by mateuszl
 */
public class Resource {
    @Id
    private String id;
    private List<String> labels;
    private List<String> comments;
    private String body;
    private String format;
    @DBRef
    private InterworkingService interworkingService;

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
    public List<String> getLabels() {
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
    public InterworkingService getInterworkingService() {
        return interworkingService;
    }

    /**
     * @param interworkingService
     */
    public void setInterworkingService(InterworkingService interworkingService) {
        this.interworkingService = interworkingService;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("Resource with id: " + this.getId() + ", body: " + body + ", format: " + format + ", labels: [");
        this.getLabels().forEach(s->sb.append(s + ", "));
        sb.append("], comments: [");
        this.getComments().forEach(s->sb.append(s + ", "));
        sb.append("], interworkingService: " + this.getInterworkingService() +".");
        return  sb.toString();
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
