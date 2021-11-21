package tech.ivoice.media;

import io.smallrye.mutiny.Uni;
import io.smallrye.mutiny.vertx.core.AbstractVerticle;
import io.vertx.core.json.Json;
import io.vertx.mutiny.core.eventbus.MessageConsumer;
import org.mobicents.media.server.component.Dsp;
import org.mobicents.media.server.component.DspFactoryImpl;
import org.mobicents.media.server.component.audio.AudioComponent;
import org.mobicents.media.server.component.audio.AudioMixer;
import org.mobicents.media.server.impl.resource.mediaplayer.audio.AudioPlayerImpl;
import org.mobicents.media.server.impl.rtp.ChannelsManager;
import org.mobicents.media.server.impl.rtp.RtpChannel;
import org.mobicents.media.server.impl.rtp.RtpClock;
import org.mobicents.media.server.impl.rtp.statistics.RtpStatistics;
import org.mobicents.media.server.io.network.UdpManager;
import org.mobicents.media.server.io.sdp.format.AVProfile;
import org.mobicents.media.server.scheduler.*;
import org.mobicents.media.server.spi.ConnectionMode;
import org.mobicents.media.server.spi.format.AudioFormat;
import org.mobicents.media.server.spi.format.FormatFactory;
import org.mobicents.media.server.spi.format.Formats;
import tech.ivoice.media.cmd.CreateMediaSession;

import java.net.InetSocketAddress;
import java.util.regex.Pattern;

/**
 * Run as worker verticle.
 */
// TODO terminate mediasession
// TODO implement proper blocking code execution
public class Mediaserver extends AbstractVerticle {
    public static final String CREATE_MEDIA_SESSION_CMD_ADDRESS = "ms.create";
    private static final Pattern SDP_HOST_PATTERN = Pattern.compile("o=.*IP4 (.*)");
    private static final Pattern SDP_PORT_PATTERN = Pattern.compile("m=audio (\\d+)");

    private final Scheduler scheduler;
    private final DspFactoryImpl dspFactory = new DspFactoryImpl();

    public Mediaserver() {
        scheduler = new ServiceScheduler();
    }

    @Override
    public Uni<Void> asyncStart() {
        return Uni.createFrom().voidItem()
                .invoke(init -> {
                            MessageConsumer<String> consumer = vertx.eventBus()
                                    .consumer(CREATE_MEDIA_SESSION_CMD_ADDRESS);
                            consumer.handler(msg -> {
                                CreateMediaSession request = Json.decodeValue(msg.body(), CreateMediaSession.class);
                                String sdp = request.getSdp();
                                String host = SDP_HOST_PATTERN.matcher(sdp)
                                        .results().findFirst().orElseThrow().group(1);
                                int port = Integer.parseInt(SDP_PORT_PATTERN.matcher(sdp)
                                        .results().findFirst().orElseThrow().group(1));
                                int localRtpPort = createMediaSession(host, port);

                                CreateMediaSession.Result result = CreateMediaSession.Result.success(localRtpPort);
                                msg.reply(Json.encode(result));
                            });
                        }
                );
    }

    /**
     * @return local rtp port
     */
    public int createMediaSession(String remoteHost, int remoteRtpPort) {
        try {
            AudioFormat pcma = FormatFactory.createAudioFormat("pcma", 8000, 8, 1);
            Formats fmts = new Formats();
            fmts.add(pcma);

            Formats dstFormats = new Formats();
            dstFormats.add(FormatFactory.createAudioFormat("LINEAR", 8000, 16, 1));

            dspFactory.addCodec("org.mobicents.media.server.impl.dsp.audio.g711.alaw.Encoder");
            dspFactory.addCodec("org.mobicents.media.server.impl.dsp.audio.g711.alaw.Decoder");

            Dsp dsp11 = dspFactory.newProcessor();
            Dsp dsp12 = dspFactory.newProcessor();

            //use default clock
            Clock clock = new WallClock();
            RtpClock rtpClock = new RtpClock(clock);
            RtpClock oobClock = new RtpClock(clock);

            //create single thread scheduler
            PriorityQueueScheduler mediaScheduler = new PriorityQueueScheduler();
            mediaScheduler.setClock(clock);
            mediaScheduler.start();

            UdpManager udpManager = new UdpManager(scheduler);
            scheduler.start();
            udpManager.start();

            ChannelsManager channelsManager = new ChannelsManager(udpManager);
            channelsManager.setScheduler(mediaScheduler);

            // Can generate audio using org.mobicents.media.server.component.audio..Sine, instead of playing URL
//            Sine audioProducer = new Sine(mediaScheduler);
//            audioProducer.setFrequency(100);
            AudioPlayerImpl audioProducer = new AudioPlayerImpl("player", mediaScheduler);
            audioProducer.setURL("http://audios.ivoice.online/tests/goodbye.wav");

            RtpStatistics statistics = new RtpStatistics(rtpClock);

            RtpChannel remoteChannel = channelsManager.getRtpChannel(statistics, rtpClock, oobClock, null);
            remoteChannel.updateMode(ConnectionMode.SEND_RECV);
            remoteChannel.setOutputDsp(dsp11);
            remoteChannel.setOutputFormats(fmts);
            remoteChannel.setInputDsp(dsp12);

            remoteChannel.bind(false, false);
            remoteChannel.setRemotePeer(new InetSocketAddress(remoteHost, remoteRtpPort));
            remoteChannel.setFormatMap(AVProfile.audio);

            AudioMixer audioMixer = new AudioMixer(mediaScheduler);

            AudioComponent audioComponent = new AudioComponent(1);
            audioComponent.addInput(audioProducer.getAudioInput());
            audioComponent.updateMode(true, true);

            audioMixer.addComponent(audioComponent);
            audioMixer.addComponent(remoteChannel.getAudioComponent());

            audioProducer.activate();
            audioMixer.start();

            // TODO get proper rtp port for local media session
            return udpManager.getPortManager().current();
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }
}
