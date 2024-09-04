# urban-spork

[![codecov](https://codecov.io/gh/Zmax0/urban-spork/branch/master/graph/badge.svg?token=6QAZQ05HZV)](https://codecov.io/gh/Zmax0/urban-spork)

A network tool for improved privacy and security

## Features

### Local

- http
- socks5

### Transport

| Local-Peer | Client-Server | Shadowsocks | VMess | Trojan |
|:----------:|:-------------:|:-----------:|:-----:|:------:|
|   `tcp`    |     `tcp`     |      ✔      |   ✔   |        |
|   `tcp`    |     `tls`     |             |       |   ✔    |
|   `tcp`    |     `ws`      |      ✔      |   ✔   |        |
|   `tcp`    |     `wss`     |             |       |        |
|   `udp`    |     `udp`     |      ✔      |       |        |
|   `udp`    |     `tcp`     |             |   ✔   |        |
|   `udp`    |     `tls`     |             |       |   ✔    |
|   `udp`    |     `ws`      |             |   ✔   |        |
|   `udp`    |     `wss`     |             |       |        |

### Ciphers

|                         | Shadowsocks |  VMess  |
|:------------------------|:-----------:|:-------:|
| aes-128-gcm             |   `C` `S`   | `C` `S` |
| aes-256-gcm             |   `C` `S`   |         |
| chacha20-poly1305       |   `C` `S`   | `C` `S` |
| 2022-blake3-aes-128-gcm |   `C` `S`   |         |
| 2022-blake3-aes-256-gcm |   `C` `S`   |         |

`C` for client `S` for server

## Config

put *config.json* file into the unpacked folder before running server

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
        "TCP",
        "UDP"
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

> `transport`: see *Transport*

> `packetEncoding`: "None" | "Packet"

> `user`: (OPTIONAL for shadowsocks) support multiple users with [*Shadowsocks 2022 Extensible Identity
Headers*](https://github.com/Shadowsocks-NET/shadowsocks-specs/blob/main/2022-2-shadowsocks-2022-extensible-identity-headers.md)

> `ssl`: (OPTIONAL) SSL specific configurations

> > `certificateFile`: certificate file

> > `keyFile`: private key file for encryption

> > `keyPassword`: password of the private key file

> > `serverName`: the Server Name Indication field in the SSL handshake. If left blank, it will be set to `server.host`

> > `verifyHostname`: whether to verify SSL hostname, default is `true`

> `ws`: (OPTIONAL) WebSocket specific configurations

> > `header`: the header to be sent in HTTP request, should be key-value pairs in clear-text string format

> > `path`: the HTTP path for the websocket request

## Build

    mvn clean package

### Require

Java 21+

### Build Output

server

    urban-spork-server/target/urban-spork-server.zip

client

    urban-spork-client-gui/target/urban-spork-client-gui.zip
