package org.mobicents.media.server.bootstrap.ioc.provider;

import com.google.inject.Inject;
import com.google.inject.Provider;
import com.google.inject.TypeLiteral;
import org.mobicents.media.core.configuration.MediaServerConfiguration;
import org.mobicents.media.server.impl.resource.asr.ASRFactory;
import org.mobicents.media.server.impl.resource.asr.ASRImpl;
import org.mobicents.media.server.scheduler.PriorityQueueScheduler;
import org.mobicents.media.server.spi.pooling.PooledObjectFactory;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.atomic.AtomicInteger;

public class ASRFactoryProvider implements Provider<ASRFactory> {

    private final MediaServerConfiguration config;
    private final PriorityQueueScheduler mediaScheduler;
    private final ExecutorService googleRunner;
    private final AtomicInteger index = new AtomicInteger(0);

    @Inject
    public ASRFactoryProvider(MediaServerConfiguration config, PriorityQueueScheduler mediaScheduler) {
        this.config = config;
        this.mediaScheduler = mediaScheduler;

        // this is now to run unbounded threads. We using this in ASR, so this shall never outgrow rapidly.
        this.googleRunner = Executors.newCachedThreadPool(new ThreadFactory() {
            @Override
            public Thread newThread(Runnable r) {
                Thread t =  new Thread(r, "rms-google-gcp-" + index.incrementAndGet());
                t.setDaemon(true);
                return t;
            }
        });
    }

    @Override
    public ASRFactory get() {
        return new ASRFactory(mediaScheduler, googleRunner);
    }


    public static final class AsrFactoryType extends TypeLiteral<PooledObjectFactory<ASRImpl>> {

        public static final AsrFactoryType INSTANCE = new AsrFactoryType();

        private AsrFactoryType() { super(); }
    }
}
