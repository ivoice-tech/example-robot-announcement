package tech.ivoice.sip;

import tech.ivoice.sip.vertx.AbstractSipUserAgent;
import tech.ivoice.sip.vertx.SipVerticleConfig;

public class Robot extends AbstractSipUserAgent<Void> {
    private final String user = "Robot";

    public Robot(SipVerticleConfig config) {
        super(config);
    }

    @Override
    protected void onServerStartedListening() {
        log.info("Started");
    }
}
