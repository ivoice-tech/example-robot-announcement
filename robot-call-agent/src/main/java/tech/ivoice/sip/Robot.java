package tech.ivoice.sip;

import gov.nist.javax.sip.message.SIPRequest;
import gov.nist.javax.sip.message.SIPResponse;
import io.smallrye.mutiny.Uni;
import io.vertx.core.DeploymentOptions;
import io.vertx.core.json.Json;
import io.vertx.mutiny.core.Vertx;
import io.vertx.mutiny.core.eventbus.Message;
import tech.ivoice.media.Mediaserver;
import tech.ivoice.media.cmd.PlayAudio;
import tech.ivoice.sip.vertx.AbstractSipUserAgent;
import tech.ivoice.sip.vertx.SipVerticleConfig;

import java.io.UnsupportedEncodingException;
import java.time.Instant;

import static java.lang.String.format;

/**
 * Listens for incoming call on: robot@127.0.0.1:5081
 */
public class Robot extends AbstractSipUserAgent<Void> {

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
            PlayAudio playAudio = new PlayAudio(invite.getMessageContent(),
                    "http://audios.ivoice.online/tests/goodbye.wav");
            Uni<Message<String>> request = vertx.eventBus()
                    .request(Mediaserver.CREATE_MEDIA_SESSION_CMD_ADDRESS, Json.encode(playAudio));
            request.subscribe()
                    .with(reply -> {
                        PlayAudio.Result result = Json.decodeValue(reply.body(),
                                PlayAudio.Result.class);
                        if (!result.isSuccess()) {
                            log.error(result.getError());
                            throw new IllegalStateException(result.getError());
                        }
                        log.info(result);
                        String sdp = createSdp(config.getHost(), result.getRtpPort());
                        SIPResponse ok = createOk(callId, sdp);
                        sendResponse(ok);

                    });
        } catch (UnsupportedEncodingException e) {
            throw new IllegalArgumentException(e);
        }
    }

    private static String createSdp(String host, int rtpPort) {
        // origin https://datatracker.ietf.org/doc/html/rfc4566#section-5.2
        String username = "robot";
        long sessId = Instant.now().getEpochSecond();
        long sessVer = Instant.now().getEpochSecond();
        String netType = "IN";
        String addrType = "IP4";
        String origin = format("o=%s %s %s %s %s %s", username, sessId, sessVer, netType, addrType, host);

        // connection data https://datatracker.ietf.org/doc/html/rfc4566#section-5.7
        String connectionData = format("c=%s %s %s", netType, addrType, host);

        // https://datatracker.ietf.org/doc/html/rfc4566#section-5.14  m=<media> <port> <proto> <fmt>
        int pcmaFmtId = 8;
        String pcmaRtpmap = format("a=rtpmap:%d PCMA/8000", pcmaFmtId);
        String mediaDescriptions = format("m=audio %s RTP/AVP %d", rtpPort, pcmaFmtId);

        return String.join(
                "\r\n",
                "v=0",
                origin,
                "s=-",
                connectionData,
                "t=0 0",
                mediaDescriptions,
                pcmaRtpmap,
                "a=ptime:20"
        );
    }

    @Override
    protected void onBye(SIPRequest bye) {
        SIPResponse ok = createOk(bye.getCallId().getCallId());
        sendResponse(ok);
    }
}
