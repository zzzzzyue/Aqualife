package aqua.common.msgtypes;

import java.io.Serializable;

public class NameResolutionRequest implements Serializable {
    private final String tandID;
    private final String requestID;

    public NameResolutionRequest(String tandID, String requestID) {
        this.tandID = tandID;
        this.requestID = requestID;
    }

    public String getTandID() {
        return tandID;
    }

    public String getRequestID() {
        return requestID;
    }
}
