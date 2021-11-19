/*
 * TeleStax, Open Source Cloud Communications
 * Copyright 2011-2016, Telestax Inc and individual contributors
 * by the @authors tag. 
 *
 * This is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 2.1 of
 * the License, or (at your option) any later version.
 *
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this software; if not, write to the Free
 * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 02110-1301 USA, or see the FSF site: http://www.fsf.org.
 */

package org.mobicents.media.server.bootstrap.ioc;

import com.google.inject.AbstractModule;
import com.google.inject.Singleton;
import org.mobicents.media.core.configuration.MediaServerConfiguration;
import org.mobicents.media.server.bootstrap.ioc.provider.*;
import org.mobicents.media.server.bootstrap.ioc.provider.ASRFactoryProvider.AsrFactoryType;
import org.mobicents.media.server.bootstrap.ioc.provider.ASRPoolProvider.ASRPoolType;
import org.mobicents.media.server.bootstrap.ioc.provider.AudioPlayerFactoryProvider.AudioPlayerFactoryType;
import org.mobicents.media.server.bootstrap.ioc.provider.AudioPlayerPoolProvider.AudioPlayerPoolType;
import org.mobicents.media.server.bootstrap.ioc.provider.AudioRecorderFactoryProvider.AudioRecorderFactoryType;
import org.mobicents.media.server.bootstrap.ioc.provider.AudioRecorderPoolProvider.AudioRecorderPoolType;
import org.mobicents.media.server.bootstrap.ioc.provider.DtmfDetectorFactoryProvider.DtmfDetectorFactoryType;
import org.mobicents.media.server.bootstrap.ioc.provider.DtmfDetectorPoolProvider.DtmfDetectorPoolType;
import org.mobicents.media.server.bootstrap.ioc.provider.DtmfGeneratorFactoryProvider.DtmfGeneratorFactoryType;
import org.mobicents.media.server.bootstrap.ioc.provider.DtmfGeneratorPoolProvider.DtmfGeneratorPoolType;
import org.mobicents.media.server.bootstrap.ioc.provider.EndpointInstallerListProvider.EndpointInstallerListType;
import org.mobicents.media.server.bootstrap.ioc.provider.LocalConnectionFactoryProvider.LocalConnectionFactoryType;
import org.mobicents.media.server.bootstrap.ioc.provider.LocalConnectionPoolProvider.LocalConnectionPoolType;
import org.mobicents.media.server.bootstrap.ioc.provider.PhoneSignalDetectorFactoryProvider.PhoneSignalDetectorFactoryType;
import org.mobicents.media.server.bootstrap.ioc.provider.PhoneSignalDetectorPoolProvider.PhoneSignalDetectorPoolType;
import org.mobicents.media.server.bootstrap.ioc.provider.PhoneSignalGeneratorFactoryProvider.PhoneSignalGeneratorFactoryType;
import org.mobicents.media.server.bootstrap.ioc.provider.PhoneSignalGeneratorPoolProvider.PhoneSignalGeneratorPoolType;
import org.mobicents.media.server.bootstrap.ioc.provider.RtpConnectionFactoryProvider.RtpConnectionFactoryType;
import org.mobicents.media.server.bootstrap.ioc.provider.RtpConnectionPoolProvider.RtpConnectionPoolType;
import org.mobicents.media.server.impl.rtp.ChannelsManager;
import org.mobicents.media.server.io.network.UdpManager;
import org.mobicents.media.server.mgcp.resources.ResourcesPool;
import org.mobicents.media.server.scheduler.Clock;
import org.mobicents.media.server.scheduler.PriorityQueueScheduler;
import org.mobicents.media.server.scheduler.Scheduler;
import org.mobicents.media.server.spi.ServerManager;
import org.mobicents.media.server.spi.dsp.DspFactory;

/**
 * @author Henrique Rosa (henrique.rosa@telestax.com)
 *
 */
public class BootstrapModule extends AbstractModule {

    private final MediaServerConfiguration config;

    public BootstrapModule(MediaServerConfiguration config) {
        this.config = config;
    }

    @Override
    protected void configure() {
        bind(MediaServerConfiguration.class).toInstance(this.config);
        bind(Clock.class).toProvider(WallClockProvider.class).in(Singleton.class);
        bind(PriorityQueueScheduler.class).toProvider(MediaSchedulerProvider.class).in(Singleton.class);
        bind(Scheduler.class).toProvider(TaskSchedulerProvider.class).in(Singleton.class);
        bind(UdpManager.class).toProvider(UdpManagerProvider.class).in(Singleton.class);
        bind(ChannelsManager.class).toProvider(ChannelsManagerProvider.class).in(Singleton.class);
        bind(DspFactory.class).toProvider(DspProvider.class).in(Singleton.class);
        bind(RtpConnectionFactoryType.INSTANCE).toProvider(RtpConnectionFactoryProvider.class).in(Singleton.class);
        bind(RtpConnectionPoolType.INSTANCE).toProvider(RtpConnectionPoolProvider.class).in(Singleton.class);
        bind(LocalConnectionFactoryType.INSTANCE).toProvider(LocalConnectionFactoryProvider.class).in(Singleton.class);
        bind(LocalConnectionPoolType.INSTANCE).toProvider(LocalConnectionPoolProvider.class).in(Singleton.class);
        bind(AudioPlayerFactoryType.INSTANCE).toProvider(AudioPlayerFactoryProvider.class).in(Singleton.class);
        bind(AudioPlayerPoolType.INSTANCE).toProvider(AudioPlayerPoolProvider.class).in(Singleton.class);
        bind(AudioRecorderFactoryType.INSTANCE).toProvider(AudioRecorderFactoryProvider.class).in(Singleton.class);
        bind(AudioRecorderPoolType.INSTANCE).toProvider(AudioRecorderPoolProvider.class).in(Singleton.class);
        bind(DtmfDetectorFactoryType.INSTANCE).toProvider(DtmfDetectorFactoryProvider.class).in(Singleton.class);
        bind(DtmfDetectorPoolType.INSTANCE).toProvider(DtmfDetectorPoolProvider.class).in(Singleton.class);
        bind(DtmfGeneratorFactoryType.INSTANCE).toProvider(DtmfGeneratorFactoryProvider.class).in(Singleton.class);
        bind(AsrFactoryType.INSTANCE).toProvider(ASRFactoryProvider.class).in(Singleton.class);
        bind(ASRPoolType.INSTANCE).toProvider(ASRPoolProvider.class).in(Singleton.class);
        bind(DtmfGeneratorPoolType.INSTANCE).toProvider(DtmfGeneratorPoolProvider.class).in(Singleton.class);
        bind(PhoneSignalDetectorFactoryType.INSTANCE).toProvider(PhoneSignalDetectorFactoryProvider.class).in(Singleton.class);
        bind(PhoneSignalDetectorPoolType.INSTANCE).toProvider(PhoneSignalDetectorPoolProvider.class).in(Singleton.class);
        bind(PhoneSignalGeneratorFactoryType.INSTANCE).toProvider(PhoneSignalGeneratorFactoryProvider.class).in(Singleton.class);
        bind(PhoneSignalGeneratorPoolType.INSTANCE).toProvider(PhoneSignalGeneratorPoolProvider.class).in(Singleton.class);
        bind(ResourcesPool.class).toProvider(ResourcesPoolProvider.class).in(Singleton.class);
        bind(EndpointInstallerListType.INSTANCE).toProvider(EndpointInstallerListProvider.class).in(Singleton.class);
        bind(ServerManager.class).toProvider(MgcpControllerProvider.class).in(Singleton.class);
    }

}
