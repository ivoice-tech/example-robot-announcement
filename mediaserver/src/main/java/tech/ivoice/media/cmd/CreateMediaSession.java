package tech.ivoice.media.cmd;

import io.vertx.codegen.annotations.DataObject;

@DataObject
public class CreateMediaSession {
    private String sdp;

    // for jackson
    private CreateMediaSession() {
    }

    public CreateMediaSession(String sdp) {
        this.sdp = sdp;
    }

    public String getSdp() {
        return sdp;
    }

    @Override
    public String toString() {
        return "CreateMediaSession{" +
                "sdp='" + sdp + '\'' +
                '}';
    }

    @DataObject
    public static class Result {
        private boolean success;
        private int rtpPort;
        private String error;

        // for jackson
        private Result() {
        }

        public static Result success(int rtpPort) {
            return new Result(true, rtpPort, null);
        }

        public static Result failure(String error) {
            return new Result(false, -1, error);
        }

        private Result(boolean success, int rtpPort, String error) {
            this.success = success;
            this.rtpPort = rtpPort;
            this.error = error;
        }

        public boolean isSuccess() {
            return success;
        }

        public int getRtpPort() {
            return rtpPort;
        }

        public String getError() {
            return error;
        }

        @Override
        public String toString() {
            return "Result{" +
                    "success=" + success +
                    ", rtpPort=" + rtpPort +
                    ", error='" + error + '\'' +
                    '}';
        }
    }
}
