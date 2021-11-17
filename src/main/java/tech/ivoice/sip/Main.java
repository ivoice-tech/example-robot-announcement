package tech.ivoice.sip;

import io.vertx.mutiny.core.Vertx;
import tech.ivoice.sip.vertx.SipVerticleConfig;

public class Main {
    public static void main(String[] args) {
        var vertx = Vertx.vertx();

        SipVerticleConfig config = new SipVerticleConfig("127.0.0.1", 5081, "udp");
        vertx.deployVerticleAndForget(new Robot(config));
    }
}
