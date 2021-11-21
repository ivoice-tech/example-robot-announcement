package tech.ivoice.sip;

import gov.nist.javax.sip.message.SIPRequest;
import gov.nist.javax.sip.message.SIPResponse;
import io.smallrye.mutiny.Uni;
import io.vertx.core.DeploymentOptions;
import io.vertx.core.json.Json;
import io.vertx.mutiny.core.Vertx;
import io.vertx.mutiny.core.eventbus.Message;
import tech.ivoice.media.CreateMediaSession;
import tech.ivoice.media.Mediaserver;
import tech.ivoice.sip.vertx.AbstractSipUserAgent;
import tech.ivoice.sip.vertx.SipVerticleConfig;

import java.io.UnsupportedEncodingException;
import java.util.List;

/**
 * Listens for incoming call on: robot@127.0.0.1:5081
 */
public class Robot extends AbstractSipUserAgent<Void> {
    private static final int MS_RTP_PORT = 8001;

    public static void main(String[] args) {
        var vertx = Vertx.vertx();

        vertx.deployVerticleAndForget(new Mediaserver(), new DeploymentOptions().setWorker(true));

        SipVerticleConfig config = new SipVerticleConfig("127.0.0.1", 5081, "udp");
        vertx.deployVerticleAndForget(new Robot(config));
    }

    public Robot(SipVerticleConfig config) {
        super(config);
    }

    @Override
    protected void onServerStartedListening() {
        log.info("Robot listening on " + config.getHostPort());
    }

    @Override
    protected void onInvite(SIPRequest invite) {
        String callId = invite.getCallId().getCallId();

        SIPResponse trying = createTrying(callId);
        sendResponse(trying);

        try {
            CreateMediaSession createMediaSession = new CreateMediaSession(invite.getMessageContent());
            Uni<Message<CreateMediaSession.Result>> request = vertx.eventBus()
                    .request(Mediaserver.CREATE_MEDIA_SESSION_EVENT_ADDRESS, Json.encode(createMediaSession));
            request.subscribe()
                    .with(reply -> {
                        log.info(reply.body());
                    });
        } catch (UnsupportedEncodingException e) {
            throw new IllegalArgumentException(e);
        }

        List<String> sdpAttributes = List.of(
                "a=rtpmap:8 PCMA/8000",
                "a=ptime:20"
        );
        SIPResponse ok = createOkWithSdp(callId, MS_RTP_PORT, sdpAttributes);
        sendResponse(ok);
    }

    @Override
    protected void onBye(SIPRequest bye) {
        SIPResponse ok = createOk(bye.getCallId().getCallId());
        sendResponse(ok);
    }
}
