package tech.ivoice.media.cmd;

import io.vertx.codegen.annotations.DataObject;

@DataObject
public class PlayAudio {
    private String sdp;
    private String audioUrl;

    // for jackson
    private PlayAudio() {
    }

    public PlayAudio(String sdp, String audioUrl) {
        this.sdp = sdp;
        this.audioUrl = audioUrl;
    }

    public String getSdp() {
        return sdp;
    }

    public String getAudioUrl() {
        return audioUrl;
    }

    @Override
    public String toString() {
        return "PlayAudio{" +
                "sdp='" + sdp + '\'' +
                ", audioUrl='" + audioUrl + '\'' +
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
