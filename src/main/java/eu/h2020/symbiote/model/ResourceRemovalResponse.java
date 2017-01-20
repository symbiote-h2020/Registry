package eu.h2020.symbiote.model;

/**
 * Created by mateuszl on 20.01.2017.
 */
public class ResourceRemovalResponse {
    private int status;
    private Platform platform;

    public ResourceRemovalResponse() {
    }

    public int getStatus() {
        return status;
    }

    public void setStatus(int status) {
        this.status = status;
    }

    public Platform getPlatform() {
        return platform;
    }

    public void setPlatform(Platform platform) {
        this.platform = platform;
    }
}
