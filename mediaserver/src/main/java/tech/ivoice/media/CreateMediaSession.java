package tech.ivoice.media;

import io.vertx.codegen.annotations.DataObject;

@DataObject
public class CreateMediaSession {
    private final String sdp;

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
        private final boolean success;
        private final String error;

        public static Result success() {
            return new Result(true, null);
        }

        public static Result failure(String error) {
            return new Result(false, error);
        }

        private Result(boolean success, String error) {
            this.success = success;
            this.error = error;
        }

        public boolean isSuccess() {
            return success;
        }

        public String getError() {
            return error;
        }

        @Override
        public String toString() {
            return "Result{" +
                    "success=" + success +
                    ", error='" + error + '\'' +
                    '}';
        }
    }
}
