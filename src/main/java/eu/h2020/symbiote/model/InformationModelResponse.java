package eu.h2020.symbiote.model;

import org.apache.commons.lang.builder.ReflectionToStringBuilder;

/**
 * Created by mateuszl on 16.03.2017.
 */
public class InformationModelResponse {
    private int status;
    private String message;
    private InformationModel informationModel;

    /**
     * @return
     */
    public int getStatus() {
        return status;
    }

    /**
     * @param status
     */
    public void setStatus(int status) {
        this.status = status;
    }

    /**
     * @return
     */
    public String getMessage() {
        return message;
    }

    /**
     * @param message
     */
    public void setMessage(String message) {
        this.message = message;
    }

    /**
     * @return
     */
    public InformationModel getInformationModel() {
        return informationModel;
    }

    /**
     * @param informationModel
     */
    public void setInformationModel(InformationModel informationModel) {
        this.informationModel = informationModel;
    }

    @Override
    public String toString() {
        return ReflectionToStringBuilder.toString(this);
    }
}
