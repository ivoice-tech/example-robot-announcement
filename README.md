# Overview

Example implementation of [Ivoice robot-sip-server](https://github.com/ivoice-tech/robot-sip-server) with media.

Robot accepting incoming call and saying phrase.

How to use:

1. Run tech.ivoice.sip.Robot from IDE
2. Call from softphone on "robot@127.0.0.1:5081" - you'll hear audio notification in softphone.

# Implementation

This project contains 2 modules:

1. Robot call agent.

Implemented on [Ivoice robot-sip-server](https://github.com/ivoice-tech/robot-sip-server).

Call agent is term from MGCP protocol - call control "intelligence", it commands media gateway (mediaserver).

Robot functions:

- accepts SIP call
- commands mediaserver to play notification

2. Mediaserver

Low-level mediaserver on Restcomm mediaserver components:

- waits for Robot command with audio URL
- plays audio in RTP channel

# References

- [MGCP wikipedia](https://en.wikipedia.org/wiki/Media_Gateway_Control_Protocol)
- [Restcomm media-core Spinoco fork (active)](https://github.com/Spinoco/mediaserver)
- [media-core examples](https://github.com/achernetsov/media-core-examples)
