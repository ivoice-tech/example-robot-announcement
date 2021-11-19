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
        
package org.mobicents.media.server.bootstrap.ioc.provider;

import com.google.inject.Inject;
import com.google.inject.Provider;
import org.mobicents.media.core.configuration.MediaServerConfiguration;
import org.mobicents.media.server.io.network.UdpManager;
import org.mobicents.media.server.scheduler.Scheduler;

/**
 * @author Henrique Rosa (henrique.rosa@telestax.com)
 *
 */
public class UdpManagerProvider implements Provider<UdpManager> {

    private final Scheduler scheduler;
    private final MediaServerConfiguration config;
    
    @Inject
    public UdpManagerProvider(MediaServerConfiguration config, Scheduler scheduler) {
        this.scheduler = scheduler;
        this.config = config;
    }
    
    @Override
    public UdpManager get() {
        UdpManager udpManager = new UdpManager(scheduler);
        udpManager.setBindAddress(config.getNetworkConfiguration().getBindAddress());
        udpManager.setLocalBindAddress(config.getControllerConfiguration().getAddress());
        udpManager.setExternalAddress(config.getNetworkConfiguration().getExternalAddress());
        udpManager.setWebRTCAddress(config.getNetworkConfiguration().getWebRTCAddress());
        udpManager.setLocalNetwork(config.getNetworkConfiguration().getNetwork());
        udpManager.setLocalSubnet(config.getNetworkConfiguration().getSubnet());
        udpManager.setUseSbc(config.getNetworkConfiguration().isSbc());
        udpManager.setRtpTimeout(config.getMediaConfiguration().getTimeout());
        udpManager.setLowestPort(config.getMediaConfiguration().getLowPort());
        udpManager.setHighestPort(config.getMediaConfiguration().getHighPort());
        return udpManager;
    }

}
