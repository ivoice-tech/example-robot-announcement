package tech.ivoice.media;

import io.smallrye.mutiny.Uni;
import io.smallrye.mutiny.vertx.core.AbstractVerticle;
import io.vertx.core.json.Json;
import io.vertx.mutiny.core.eventbus.MessageConsumer;
import org.mobicents.media.server.component.Dsp;
import org.mobicents.media.server.component.DspFactoryImpl;
import org.mobicents.media.server.component.audio.AudioComponent;
import org.mobicents.media.server.component.audio.AudioMixer;
import org.mobicents.media.server.component.audio.Sine;
import org.mobicents.media.server.component.audio.SpectraAnalyzer;
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

import java.net.InetSocketAddress;

/**
 * Run as worker verticle.
 */
public class Mediaserver extends AbstractVerticle {
    public static final String CREATE_MEDIA_SESSION_EVENT_ADDRESS = "ms.create";

    private Clock clock;
    private PriorityQueueScheduler mediaScheduler;
    private Scheduler scheduler;

    private ChannelsManager channelsManager;
    private UdpManager udpManager;

    private SpectraAnalyzer analyzer1, analyzer2;
    private Sine source1, source2;

    private RtpChannel channel1, channel2;
    private RtpClock rtpClock1, rtpClock2;
    private RtpClock oobClock1, oobClock2;
    private RtpStatistics statistics1, statistics2;

    private int fcount;

    private DspFactoryImpl dspFactory = new DspFactoryImpl();

    private Dsp dsp11, dsp12;
    private Dsp dsp21, dsp22;

    private AudioMixer audioMixer1, audioMixer2;
    private AudioComponent component1, component2;

    public Mediaserver() {
        scheduler = new ServiceScheduler();
    }

    @Override
    public Uni<Void> asyncStart() {
        return Uni.createFrom().voidItem()
                .invoke(init -> {
                            MessageConsumer<CreateMediaSession> consumer = vertx.eventBus()
                                    .consumer(CREATE_MEDIA_SESSION_EVENT_ADDRESS);
                            consumer.handler(msg -> {
//                                System.out.println(msg.body());
                                setUp(8001, 8000);
//                                connect();

                                CreateMediaSession.Result result = CreateMediaSession.Result.success();
                                msg.reply(Json.encode(result));
                            });
                        }
                );
    }

    private void connect() {
        source1.activate();
        analyzer1.activate();
        audioMixer1.start();

        source2.start();
        analyzer2.activate();
        audioMixer2.start();
    }

    public void setUp(int localRtpPort, int remoteRtpPort) {
        try {
            AudioFormat pcma = FormatFactory.createAudioFormat("pcma", 8000, 8, 1);
            Formats fmts = new Formats();
            fmts.add(pcma);

            Formats dstFormats = new Formats();
            dstFormats.add(FormatFactory.createAudioFormat("LINEAR", 8000, 16, 1));

            dspFactory.addCodec("org.mobicents.media.server.impl.dsp.audio.g711.alaw.Encoder");
            dspFactory.addCodec("org.mobicents.media.server.impl.dsp.audio.g711.alaw.Decoder");

            dsp11 = dspFactory.newProcessor();
            dsp12 = dspFactory.newProcessor();

            dsp21 = dspFactory.newProcessor();
            dsp22 = dspFactory.newProcessor();

            //use default clock
            clock = new WallClock();
            rtpClock1 = new RtpClock(clock);
            rtpClock2 = new RtpClock(clock);
            oobClock1 = new RtpClock(clock);
            oobClock2 = new RtpClock(clock);

            //create single thread scheduler
            mediaScheduler = new PriorityQueueScheduler();
            mediaScheduler.setClock(clock);
            mediaScheduler.start();

            udpManager = new UdpManager(scheduler);
            scheduler.start();
            udpManager.start();

            channelsManager = new ChannelsManager(udpManager);
            channelsManager.setScheduler(mediaScheduler);

            source1 = new Sine(mediaScheduler);
            source1.setFrequency(200);

            source2 = new Sine(mediaScheduler);
            source2.setFrequency(100);

            analyzer1 = new SpectraAnalyzer("analyzer", mediaScheduler);
            analyzer2 = new SpectraAnalyzer("analyzer", mediaScheduler);

            this.statistics1 = new RtpStatistics(rtpClock1);
            this.statistics2 = new RtpStatistics(rtpClock2);

            channel1 = channelsManager.getRtpChannel(statistics1, rtpClock1, oobClock1, null);
            channel1.updateMode(ConnectionMode.SEND_RECV);
            channel1.setOutputDsp(dsp11);
            channel1.setOutputFormats(fmts);
            channel1.setInputDsp(dsp12);

            channel2 = channelsManager.getRtpChannel(statistics2, rtpClock2, oobClock2, null);
            channel2.updateMode(ConnectionMode.SEND_RECV);
            channel2.setOutputDsp(dsp21);
            channel2.setOutputFormats(fmts);
            channel2.setInputDsp(dsp22);

            channel1.bind(false, false);
            channel2.bind(false, false);

            // channel 1 is local
            channel1.setRemotePeer(new InetSocketAddress("127.0.0.1", remoteRtpPort));
            channel2.setRemotePeer(new InetSocketAddress("127.0.0.1", localRtpPort));

            channel1.setFormatMap(AVProfile.audio);
            channel2.setFormatMap(AVProfile.audio);

            audioMixer1 = new AudioMixer(mediaScheduler);
            audioMixer2 = new AudioMixer(mediaScheduler);

            component1 = new AudioComponent(1);
            component1.addInput(source1.getAudioInput());
            component1.addOutput(analyzer1.getAudioOutput());
            component1.updateMode(true, true);

            audioMixer1.addComponent(component1);
            audioMixer1.addComponent(channel1.getAudioComponent());

            component2 = new AudioComponent(2);
            component2.addInput(source2.getAudioInput());
            component2.addOutput(analyzer2.getAudioOutput());
            component2.updateMode(true, true);

            audioMixer2.addComponent(component2);
            audioMixer2.addComponent(channel2.getAudioComponent());

            source1.activate();
            analyzer1.activate();
            audioMixer1.start();

            source2.start();
            analyzer2.activate();
            audioMixer2.start();

        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }

    /**
     * see org.mobicents.media.server.mgcp.tx.cmd.CreateConnectionCmd
     */
    public void createConnection(String sdp) {

    }
}
