package org.mobicents.media.server.bootstrap.decoder;

import com.google.common.io.BaseEncoding;
import org.mobicents.media.server.component.Dsp;
import org.mobicents.media.server.component.DspFactoryImpl;
import org.mobicents.media.server.impl.resource.audio.RecorderFileSink;
import org.mobicents.media.server.spi.format.Format;
import org.mobicents.media.server.spi.format.FormatFactory;
import org.mobicents.media.server.spi.memory.Frame;
import org.mobicents.media.server.spi.memory.Memory;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * Decodes RTP Dump.
 *
 *
 */
public class DecodeRTPDump {

    private static final int FORMAT = 3;

    // ulaw and linear fromats
    static Format ulaw = FormatFactory.createAudioFormat("pcmu", 8000, 8, 1);
    static Format linear = FormatFactory.createAudioFormat("linear", 8000, 16, 1);
    static Format opus = FormatFactory.createAudioFormat("opus", 48000, 8, 2);

    /**
     * Expects 1 parameter - file to decode (jbr) and decodes it to same filename with .wav extension
     */
    public static void main(String[] args) throws Throwable {

        if (args.length != 1) {
            System.err.println("Invalid parameters. Expecting name of the file to decode");
            System.exit(1);
        }

        Path decode = Paths.get(args[0]);
        Path wavFile = decode.getParent().resolve(decode.getFileName() + ".wav");

        // remove tmp file
        Path tmpWav = wavFile.getParent().resolve(wavFile.getFileName() + "~");
        if (Files.exists(tmpWav)) Files.delete(tmpWav);

        // use standard recorder sink
        RecorderFileSink sink = new RecorderFileSink(wavFile, false);

        // decoder to decode rtp samples written in hex
        BaseEncoding decoder = BaseEncoding.base16().lowerCase();

        // configure dsp factory
        DspFactoryImpl dspFactory = new DspFactoryImpl();
        dspFactory.addCodec("org.mobicents.media.server.impl.dsp.audio.g711.alaw.Encoder");
        dspFactory.addCodec("org.mobicents.media.server.impl.dsp.audio.g711.alaw.Decoder");
        dspFactory.addCodec("org.mobicents.media.server.impl.dsp.audio.g711.ulaw.Encoder");
        dspFactory.addCodec("org.mobicents.media.server.impl.dsp.audio.g711.ulaw.Decoder");
        dspFactory.addCodec("org.restcomm.media.codec.opus.Encoder");
        dspFactory.addCodec("org.restcomm.media.codec.opus.Decoder");

        Dsp dsp = dspFactory.newProcessor();

        Files.lines(decode)
        .skip(2)         // remove 2 header lines
        .forEach( line -> {

            String[] data = line.split(";");
            String sample = data[data.length -1];

            // this contains rtp paylod
            byte[] rtpPayload = decoder.decode(sample.trim());
            Frame rtpFrame = Memory.allocate(rtpPayload.length);
            rtpFrame.setLength(rtpPayload.length);
            System.arraycopy(rtpPayload, 0, rtpFrame.getData(), 0, rtpPayload.length);

            // decode to PCM
            Frame pcmFrame;
            if (data[FORMAT].equals("111")) {
                pcmFrame = dsp.process(rtpFrame, opus, linear);
            } else {
                pcmFrame = dsp.process(rtpFrame, ulaw, linear);
            }

            try {
                sink.write(ByteBuffer.wrap(pcmFrame.getData()));
            } catch (IOException e) {
                e.printStackTrace();
            }

        });

        // close file and write header
        sink.commit();

    }

}
