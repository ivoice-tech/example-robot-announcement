# Overview

Robot accepting incoming call and saying phrase.

This project contains 2 modules:

1. Robot call agent.

Implemented on [Ivoice robot-sip-server](https://github.com/ivoice-tech/robot-sip-server).

Call agent is term from MGCP protocol - call control "intelligence", it commands media gateway (mediaserver).

- accept SIP call form human SIP UA (user agent): for example softphone
- establish media session with mediaserver via the MGCP protocol
- command mediaserver to play notification

2. Mediaserver (or media gateway in MGCP terms)

Provides announcement endpoint, see [spec >>](https://datatracker.ietf.org/doc/html/rfc3435#section-2.1.1.3)

Implementation copied from Spinoco mediaserver repository (bootstrap module).

- execute commands from robot call agent via the MGCP protocol
- plays audio notification

# How to run demo

1. Run mediaserver (TODO instructions)

# References

- [MGCP wikipedia](https://en.wikipedia.org/wiki/Media_Gateway_Control_Protocol)
- [MGCP specification - RFC 3435](https://datatracker.ietf.org/doc/html/rfc3435)
- [JSR 309](https://jcp.org/en/jsr/detail?id=309)
- [Restcomm media-core Spinoco fork (active)](https://github.com/Spinoco/mediaserver)
- [media-core examples](https://github.com/achernetsov/media-core-examples)
