# Overview

Example implementation of [Ivoice robot-sip-server](https://github.com/ivoice-tech/robot-sip-server) with media.

Robot accepting incoming call and saying phrase.

## How to build

1. Build and install (mvn install) [Ivoice robot-sip-server](https://github.com/ivoice-tech/robot-sip-server)
2. Build and install 2 [media-core](https://github.com/Spinoco/mediaserver) modules:

- org.mobicents.media.io:rtp
- org.mobicents.media.resources:player

We'll write instructions or include libs for media-core in the nearest future :)

## How to use

1. Run tech.ivoice.sip.Robot from IDE
2. Call from softphone on "robot@127.0.0.1:5081" - you'll hear audio notification in softphone.

# Implementation

This project contains 2 modules:

1. Robot call agent.

Implemented on [Ivoice robot-sip-server](https://github.com/ivoice-tech/robot-sip-server).

Robot is call agent (term from [MGCP](https://en.wikipedia.org/wiki/Media_Gateway_Control_Protocol)), containing
intelligence of the system. Robot functions:

- accept SIP call
- command mediaserver to play notification

2. Mediaserver

Low-level mediaserver based on Restcomm mediaserver components:

- wait for Robot command with audio URL
- establish RTP connection with softphone
- transmit audio to RTP channel

# References

- [Ivoice robot-sip-server](https://github.com/ivoice-tech/robot-sip-server)
- [Restcomm media-core Spinoco fork](https://github.com/Spinoco/mediaserver)
- [media-core examples](https://github.com/achernetsov/media-core-examples)
