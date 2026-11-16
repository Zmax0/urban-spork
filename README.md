# urban-spork

[![codecov](https://codecov.io/gh/Zmax0/urban-spork/branch/master/graph/badge.svg?token=6QAZQ05HZV)](https://codecov.io/gh/Zmax0/urban-spork)

A network tool for improved privacy and security

## Features

### Local

- http (s)
- socks5

### Transport

| Local-Peer | Client-Server | Peer DoH | Shadowsocks | VMess | Trojan |
|:----------:|:-------------:|:--------:|:-----------:|:-----:|:------:|
|   `tcp`    |     `tcp`     |    ✔    |     ✔      |  ✔   |        |
|   `tcp`    |     `tls`     |    ✔    |     ✔      |  ✔   |   ✔   |
|   `tcp`    |     `ws`      |    ✔    |     ✔      |  ✔   |        |
|   `tcp`    |     `wss`     |    ✔    |     ✔      |  ✔   |   ✔   |
|   `tcp`    |    `quic`     |    ✔    |    ✔(*)    |  ✔   |   ✔   |
|   `udp`    |     `udp`     |    ✔    |     ✔      |       |        |
|   `udp`    |     `tcp`     |    ✔    |             |  ✔   |        |
|   `udp`    |     `tls`     |    ✔    |             |  ✔   |   ✔   |
|   `udp`    |     `ws`      |    ✔    |             |  ✔   |        |
|   `udp`    |     `wss`     |    ✔    |             |  ✔   |   ✔   |
|   `udp`    |    `quic`     |    ✔    |             |  ✔   |   ✔   |

`*` starting the shadowsocks quic server will bind udp socket but process tcp payload

#### Priority

client

- `quic` > `tcp` | `tls` | `ws` | `wss`

server

- `shadowsocks`: `udp` > `quic`

### Ciphers(*)

|                                   | Shadowsocks |  VMess  |
|:----------------------------------|:-----------:|:-------:|
| aes-128-gcm                       |   `C` `S`   | `C` `S` |
| aes-256-gcm                       |   `C` `S`   |         |
| chacha20-poly1305                 |   `C` `S`   | `C` `S` |
| 2022-blake3-aes-128-gcm           |   `C` `S`   |         |
| 2022-blake3-aes-256-gcm           |   `C` `S`   |         |
| 2022-blake3-chacha8-poly1305      |   `C` `S`   |         |
| 2022-blake3-chacha20-poly1305     |   `C` `S`   |         |

`C` for client `S` for server

**NOTE**: all ciphers implemented via JNI, has only been tested on the amd64 arch


## Config

put *config.json* file into the unpacked folder before running a server

```json5
{
  "servers": [
    {
      "cipher": "aes-128-gcm",
      "password": "foobar",
      "host": "example.com",
      "port": "443",
      "protocol": "shadowsocks",
      "packetEncoding": "None",
      "transport": [
        "tcp",
        "udp"
      ],
      "user": [
        {
          "name": "John Doe",
          "password": "foobar"
        }
      ],
      "ssl": {
        "certificateFile": "/path/to/certificate.crt",
        "keyFile": "/path/to/private.key",
        "keyPassword": "",
        "serverName": ""
      },
      "ws": {
        "header": {
          "Host": "example.com"
        },
        "path": "/ws"
      }
    }
  ]
}
```

> `protocol`: "shadowsocks" | "vmess" | "trojan"

> `cipher`: see *Ciphers*

> `transport`: "udp" | "tcp" | "quic"

> `packetEncoding`: "None" | "Packet"

> `user`: (OPTIONAL for shadowsocks) support multiple users with [*Shadowsocks 2022 Extensible Identity
Headers*](https://github.com/Shadowsocks-NET/shadowsocks-specs/blob/main/2022-2-shadowsocks-2022-extensible-identity-headers.md)

> `dns`: (OPTIONAL) DNS specific configurations

> > `nameServer`: The DoH (DNS over HTTPS - [RFC 8484](https://datatracker.ietf.org/doc/html/rfc8484)) resolver endpoint
> > URL

> `ssl`: (OPTIONAL) SSL specific configurations

> > `certificateFile`: certificate file

> > `keyFile`: private key file for encryption

> > `keyPassword`: password of the private key file

> > `serverName`: the Server Name Indication field in the SSL handshake. If left blank, it will be set to `server.host`

> > `verifyHostname`: whether to verify SSL hostname, default is `true`

> `ws`: (OPTIONAL) WebSocket specific configurations

> > `header`: the header to be sent in HTTP request, should be key-value pairs in clear-text string format

> > `path`: the HTTP path for the websocket request

> `quic`: (OPTIONAL) QUIC specific configurations

> > `certificateFile`: certificate file

> > `keyFile`: private key file for encryption

> > `keyPassword`: password of the private key file

## Build

    mvn clean package

### Require

JDK 25+

Rust 1.85+

### Build Output

server

    urban-spork-server/target/urban-spork-server-<version>.zip

client(with a bundled java runtime, no java required to run)

    urban-spork-client-gui/target/urban-spork-client-gui-<platform>-<version>.zip

## Interoperability

[![Total interoperability](https://raw.githubusercontent.com/Zmax0/urban-spork/badges/interop/dev/protocol-interop-ci/badge.svg)][interop-results]

| Implementation | Configurations | Result |
|:---------------|:---------------|:-------|
| shadowsocks-rust | AES-128-GCM / TCP, UDP | [![Shadowsocks AES-128-GCM / TCP, UDP](https://raw.githubusercontent.com/Zmax0/urban-spork/badges/interop/dev/protocol-interop-ci/configurations/shadowsocks-aes-128-gcm.svg)][interop-results] |
| shadowsocks-rust | AES-256-GCM / TCP, UDP | [![Shadowsocks AES-256-GCM / TCP, UDP](https://raw.githubusercontent.com/Zmax0/urban-spork/badges/interop/dev/protocol-interop-ci/configurations/shadowsocks-aes-256-gcm.svg)][interop-results] |
| shadowsocks-rust | CHACHA20-POLY1305 / TCP, UDP | [![Shadowsocks CHACHA20-POLY1305 / TCP, UDP](https://raw.githubusercontent.com/Zmax0/urban-spork/badges/interop/dev/protocol-interop-ci/configurations/shadowsocks-chacha20-poly1305.svg)][interop-results] |
| shadowsocks-rust | 2022 AES-128-GCM / TCP, UDP | [![Shadowsocks 2022 AES-128-GCM / TCP, UDP](https://raw.githubusercontent.com/Zmax0/urban-spork/badges/interop/dev/protocol-interop-ci/configurations/shadowsocks-2022-blake3-aes-128-gcm.svg)][interop-results] |
| shadowsocks-rust | 2022 AES-256-GCM / TCP, UDP | [![Shadowsocks 2022 AES-256-GCM / TCP, UDP](https://raw.githubusercontent.com/Zmax0/urban-spork/badges/interop/dev/protocol-interop-ci/configurations/shadowsocks-2022-blake3-aes-256-gcm.svg)][interop-results] |
| shadowsocks-rust | 2022 CHACHA20-POLY1305 / TCP, UDP | [![Shadowsocks 2022 CHACHA20-POLY1305 / TCP, UDP](https://raw.githubusercontent.com/Zmax0/urban-spork/badges/interop/dev/protocol-interop-ci/configurations/shadowsocks-2022-blake3-chacha20-poly1305.svg)][interop-results] |
| V2Ray (V2Fly) | AES-128-GCM / TCP | [![VMess AES-128-GCM / TCP](https://raw.githubusercontent.com/Zmax0/urban-spork/badges/interop/dev/protocol-interop-ci/configurations/vmess-aes-128-gcm-tcp.svg)][interop-results] |
| V2Ray (V2Fly) | AES-128-GCM / TLS | [![VMess AES-128-GCM / TLS](https://raw.githubusercontent.com/Zmax0/urban-spork/badges/interop/dev/protocol-interop-ci/configurations/vmess-aes-128-gcm-tls.svg)][interop-results] |
| V2Ray (V2Fly) | AES-128-GCM / WS | [![VMess AES-128-GCM / WS](https://raw.githubusercontent.com/Zmax0/urban-spork/badges/interop/dev/protocol-interop-ci/configurations/vmess-aes-128-gcm-ws.svg)][interop-results] |
| V2Ray (V2Fly) | AES-128-GCM / WSS | [![VMess AES-128-GCM / WSS](https://raw.githubusercontent.com/Zmax0/urban-spork/badges/interop/dev/protocol-interop-ci/configurations/vmess-aes-128-gcm-wss.svg)][interop-results] |
| V2Ray (V2Fly) | CHACHA20-POLY1305 / TCP | [![VMess CHACHA20-POLY1305 / TCP](https://raw.githubusercontent.com/Zmax0/urban-spork/badges/interop/dev/protocol-interop-ci/configurations/vmess-chacha20-poly1305-tcp.svg)][interop-results] |
| V2Ray (V2Fly) | CHACHA20-POLY1305 / TLS | [![VMess CHACHA20-POLY1305 / TLS](https://raw.githubusercontent.com/Zmax0/urban-spork/badges/interop/dev/protocol-interop-ci/configurations/vmess-chacha20-poly1305-tls.svg)][interop-results] |
| V2Ray (V2Fly) | CHACHA20-POLY1305 / WS | [![VMess CHACHA20-POLY1305 / WS](https://raw.githubusercontent.com/Zmax0/urban-spork/badges/interop/dev/protocol-interop-ci/configurations/vmess-chacha20-poly1305-ws.svg)][interop-results] |
| V2Ray (V2Fly) | CHACHA20-POLY1305 / WSS | [![VMess CHACHA20-POLY1305 / WSS](https://raw.githubusercontent.com/Zmax0/urban-spork/badges/interop/dev/protocol-interop-ci/configurations/vmess-chacha20-poly1305-wss.svg)][interop-results] |
| Trojan (trojan-gfw) | TLS | [![Trojan TLS](https://raw.githubusercontent.com/Zmax0/urban-spork/badges/interop/dev/protocol-interop-ci/configurations/trojan-tls.svg)][interop-results] |

[interop-results]: https://github.com/Zmax0/urban-spork/blob/badges/interop/dev/protocol-interop-ci/summary.md
