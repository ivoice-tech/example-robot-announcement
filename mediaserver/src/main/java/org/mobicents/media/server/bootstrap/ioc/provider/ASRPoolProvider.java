package org.mobicents.media.server.bootstrap.ioc.provider;

import com.google.inject.Inject;
import com.google.inject.Provider;
import com.google.inject.TypeLiteral;
import org.mobicents.media.core.configuration.MediaServerConfiguration;
import org.mobicents.media.server.impl.resource.asr.ASRImpl;
import org.mobicents.media.server.impl.resource.asr.ASRPool;
import org.mobicents.media.server.spi.pooling.PooledObjectFactory;
import org.mobicents.media.server.spi.pooling.ResourcePool;

public class ASRPoolProvider implements Provider<ASRPool> {

    private final MediaServerConfiguration config;
    private final PooledObjectFactory<ASRImpl> factory;

    @Inject
    public ASRPoolProvider(MediaServerConfiguration config, PooledObjectFactory<ASRImpl> factory) {
        this.config = config;
        this.factory = factory;
    }

    @Override
    public ASRPool get() {
        return new ASRPool(factory);
    }

    public static final class ASRPoolType extends TypeLiteral<ResourcePool<ASRImpl>> {

        public static final ASRPoolType INSTANCE = new ASRPoolType();

        private ASRPoolType() {
            super();
        }
    }
}
