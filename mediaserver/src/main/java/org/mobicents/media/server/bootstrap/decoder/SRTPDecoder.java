package org.mobicents.media.server.bootstrap.decoder;

import com.google.common.io.BaseEncoding;
import org.bouncycastle.pqc.math.linearalgebra.ByteUtils;
import org.mobicents.media.server.impl.rtp.RtpPacket;
import org.mobicents.media.server.impl.rtp.crypto.PacketTransformer;
import org.mobicents.media.server.impl.rtp.crypto.SRTPPolicy;
import org.mobicents.media.server.impl.rtp.crypto.SRTPTransformEngine;

import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;


public class SRTPDecoder {

    static class DecoderConfig {
        byte[] masterClientKey;
        byte[] masterServerKey;
        byte[] masterClientSalt;
        byte[] masterServerSalt;

        SRTPPolicy srtpPolicy;
        SRTPPolicy srtcpPolicy;

        public DecoderConfig(
                byte[] masterClientKey
                , byte[] masterServerKey
                , byte[] masterClientSalt
                , byte[] masterServerSalt
                , SRTPPolicy srtpPolicy
                , SRTPPolicy srtcpPolicy
        ) {
            this.masterClientKey = masterClientKey;
            this.masterServerKey = masterServerKey;
            this.masterClientSalt = masterClientSalt;
            this.masterServerSalt = masterServerSalt;
            this.srtpPolicy = srtpPolicy;
            this.srtcpPolicy = srtcpPolicy;
        }
    }

    public static void printHelp() {
        System.out.println("usage: SRTPDecoder masterClientKey masterServerKey masterClientSalt masterServerSalt srtpPolicy srtcpPolicy file");
    }


    public static void main(String[] args) throws Throwable {

        if (args[0].equalsIgnoreCase("-h") || args[0].equalsIgnoreCase("--help")) {
            printHelp();
            System.exit(0);
        } else if (args.length < 7) {
            System.err.println("Not enough arguments was specified");
            printHelp();
            System.exit(1);
        }

        DecoderConfig config = new DecoderConfig(
            ByteUtils.fromHexString(args[0])
            , ByteUtils.fromHexString(args[1])
            , ByteUtils.fromHexString(args[2])
            , ByteUtils.fromHexString(args[3])
            , SRTPPolicy.fromString(args[4])
            , SRTPPolicy.fromString(args[5])
        );

        Path decode = Paths.get(args[6]);
        Path jbrFile = decode.getParent().resolve(decode.getFileName() + ".jbr");

        BufferedWriter jbrWriter = Files.newBufferedWriter(jbrFile);
        BaseEncoding encoder = BaseEncoding.base16().lowerCase();

        PacketTransformer transformer = new SRTPTransformEngine(config.masterClientKey, config.masterClientSalt, config.srtpPolicy, config.srtpPolicy).getRTPTransformer();

        jbrWriter.write("RECEIVED RTP DUMP FROM decoded jbrs: " + args[6]);
        jbrWriter.newLine();
        jbrWriter.write("timestamp ; sequence ; timestamp_rtp ; format ; jbrSize; sample length; sample hex");
        jbrWriter.newLine();

        Files.lines(decode).forEach( line -> {
            String[] data = line.split(";");
            String encryptedPacket = data[data.length -1];
            byte[] packetData = ByteUtils.fromHexString(encryptedPacket);
            byte[] decrypted = transformer.reverseTransform(packetData, null, null);
            RtpPacket packet = RtpPacket.fromRaw(null, null, decrypted, 0, decrypted.length);
            byte[] dataRaw = new byte[packet.getPayloadLength()];
            packet.getPayload(dataRaw, 0);

            try {
                jbrWriter.write("0;");
                jbrWriter.write(packet.getSeqNumber() + ";");
                jbrWriter.write(packet.getTimestamp() + ";");
                jbrWriter.write(packet.getPayloadType() + ";");
                jbrWriter.write( "0;"); //compensate for no jitter buffer -> no packet here
                jbrWriter.write(encoder.encode(dataRaw));
                jbrWriter.newLine();
            } catch (IOException e) {
                System.err.println("Could not write a packet to the file");
            }
        });

        jbrWriter.flush();
        jbrWriter.close();

    }

}
