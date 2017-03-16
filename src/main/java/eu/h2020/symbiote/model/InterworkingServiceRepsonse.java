package eu.h2020.symbiote.model;

/**
 * Created by mateuszl on 16.03.2017.
 */
public class InterworkingServiceRepsonse {
    private int status;
    private String message;
    private InterworkingService interworkingService;

    public InterworkingServiceRepsonse(int status, String message, InterworkingService interworkingService) {
        this.status = status;
        this.message = message;
        this.interworkingService = interworkingService;
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
    public InterworkingService getInterworkingService() {
        return interworkingService;
    }

    /**
     * @param interworkingService
     */
    public void setInterworkingService(InterworkingService interworkingService) {
        this.interworkingService = interworkingService;
    }
}
