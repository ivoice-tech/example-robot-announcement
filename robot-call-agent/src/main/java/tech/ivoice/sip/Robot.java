package tech.ivoice.sip;

import io.vertx.mutiny.core.Vertx;
import tech.ivoice.sip.vertx.AbstractSipUserAgent;
import tech.ivoice.sip.vertx.SipVerticleConfig;

public class Robot extends AbstractSipUserAgent<Void> {
    public static void main(String[] args) {
        var vertx = Vertx.vertx();

        SipVerticleConfig config = new SipVerticleConfig("127.0.0.1", 5081, "udp");
        vertx.deployVerticleAndForget(new Robot(config));
    }

    private final String user = "Robot";

    public Robot(SipVerticleConfig config) {
        super(config);
    }

    @Override
    protected void onServerStartedListening() {
        log.info("Started");
    }
}
