package eu.h2020.symbiote.model;

/**
 * Created by mateuszl on 20.01.2017.
 */
public class PlatformRemovalResponse {
    private int status;
    private Platform platform;

    public PlatformRemovalResponse() {
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
