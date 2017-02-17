package eu.h2020.symbiote.model;

/**
 * Class used as a response to RPC call requesting platform actions
 *
 * Created by mateuszl
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
    public Platform getPlatform() {
        return platform;
    }

    /**
     * @param platform
     */
    public void setPlatform(Platform platform) {
        this.platform = platform;
    }
}
