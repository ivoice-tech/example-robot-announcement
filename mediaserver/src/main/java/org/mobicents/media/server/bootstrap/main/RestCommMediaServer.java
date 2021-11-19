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

package org.mobicents.media.server.bootstrap.main;

import com.google.inject.Inject;
import org.apache.logging.log4j.Logger;
import org.mobicents.media.server.io.network.UdpManager;
import org.mobicents.media.server.scheduler.CancelableTask;
import org.mobicents.media.server.scheduler.EventQueueType;
import org.mobicents.media.server.scheduler.PriorityQueueScheduler;
import org.mobicents.media.server.scheduler.Scheduler;
import org.mobicents.media.server.spi.ControlProtocol;
import org.mobicents.media.server.spi.MediaServer;
import org.mobicents.media.server.spi.ServerManager;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * @author Henrique Rosa (henrique.rosa@telestax.com)
 *
 */
public class RestCommMediaServer implements MediaServer {

    private static final Logger log = org.apache.logging.log4j.LogManager.getLogger(RestCommMediaServer.class);

    // Media Server State
    private boolean started;
    private Map<ControlProtocol, ServerManager> controllers;

    // Core Components
    private final PriorityQueueScheduler mediaScheduler;
    private final Scheduler taskScheduler;
    private final UdpManager udpManager;

    // Heart beat
    private final HeartBeat heartbeat;
    private final int heartbeatTime;
    private volatile long ttl;
    private AtomicBoolean active = new AtomicBoolean(false);

    @Inject
    public RestCommMediaServer(PriorityQueueScheduler mediaScheduler, Scheduler taskScheduler, UdpManager udpManager,
            ServerManager controller) {
        // Core Components
        this.mediaScheduler = mediaScheduler;
        this.taskScheduler = taskScheduler;
        this.udpManager = udpManager;

        // Media Server State
        this.started = false;
        this.controllers = new HashMap<>(2);
        addManager(controller);

        // Heartbeat
        this.heartbeat = new HeartBeat();
        this.heartbeatTime = 1;
    }

    @Override
    public void addManager(ServerManager manager) {
        if (manager != null) {
            ControlProtocol protocol = manager.getControlProtocol();
            if (this.controllers.containsKey(protocol)) {
                throw new IllegalArgumentException(protocol + " controller is already registered");
            } else {
                this.controllers.put(protocol, manager);
            }
        }
    }

    @Override
    public void removeManager(ServerManager manager) {
        if (manager != null) {
            this.controllers.remove(manager.getControlProtocol());
        }
    }

    @Override
    public void start() throws IllegalStateException {
        if (this.started) {
            throw new IllegalStateException("Media Server already started.");
        }

        this.started = true;
        this.heartbeat.restart();
        this.mediaScheduler.start();
        this.taskScheduler.start();
        this.udpManager.start();
        for (ServerManager controller : this.controllers.values()) {
            controller.activate();
        }

        if (log.isInfoEnabled()) {
            log.info("Media Server started");
        }
    }

    @Override
    public void stop() throws IllegalStateException {
        if (!this.started) {
            throw new IllegalStateException("Media Server already stopped.");
        }

        this.started = false;
        this.udpManager.stop();
        this.taskScheduler.stop();
        this.mediaScheduler.stop();
        this.active.set(false);
        for (ServerManager controller : this.controllers.values()) {
            controller.deactivate();
        }
        if (log.isInfoEnabled()) {
            log.info("Media Server stopped");
        }
    }
    
    @Override
    public boolean isRunning() {
        return this.started;
    }

    private final class HeartBeat extends CancelableTask {

        public HeartBeat() {
            super(active);
        }

        @Override
        public EventQueueType getQueueType() {
            return EventQueueType.HEARTBEAT;
        }

        public void restart() {
            ttl = heartbeatTime * 600;
            active.set(true);
            mediaScheduler.submitHeartbeat(this);
        }

        @Override
        public long perform() {
            ttl--;
            if (ttl == 0) {
                log.info("Global hearbeat is still alive");
                restart();
            } else {
                mediaScheduler.submitHeartbeat(this);
            }
            return 0;
        }
    }

}
