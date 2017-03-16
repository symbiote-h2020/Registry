package eu.h2020.symbiote.model;

/**
 * Created by mateuszl on 16.03.2017.
 */
public class InformationModelResponse {
    private int status;
    private String message;
    private InformationModel informationModel;

    public InformationModelResponse(int status, String message, InformationModel informationModel) {
        this.status = status;
        this.message = message;
        this.informationModel = informationModel;
    }

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
}
