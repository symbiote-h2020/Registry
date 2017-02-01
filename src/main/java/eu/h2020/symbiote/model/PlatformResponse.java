package eu.h2020.symbiote.model;

/**
 * Class used as a response to RPC call requesting platform actions
 */
public class PlatformResponse {
    private int status;
    private Platform platform;

    public PlatformResponse() {
    }

    public PlatformResponse(int status, Platform platform) {
        this.status = status;
        this.platform = platform;
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
