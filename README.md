# urban-spork

[![codecov](https://codecov.io/gh/Zmax0/urban-spork/branch/master/graph/badge.svg?token=6QAZQ05HZV)](https://codecov.io/gh/Zmax0/urban-spork)

A sock5 proxy

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
      }
    }
  ]
}
```

> `protocol`: "shadowsocks" | "vmess" | "trojan"

> `cipher`: see *Ciphers*

> `transport`: see *Transport*

> `packetEncoding`: "None" | "Packet"

> `user`: (OPTIONAL for shadowsocks) support multiple users with [*Shadowsocks 2022 Extensible Identity Headers*](https://github.com/Shadowsocks-NET/shadowsocks-specs/blob/main/2022-2-shadowsocks-2022-extensible-identity-headers.md)

> `sslSetting`: (REQUIRED for trojan) SSL specific configurations

>> `certificateFile`: certificate file

>> `keyFile`: private key file for encryption

>> `keyPassword`: password of the private key file

>> `serverName`: the Server Name Indication field in the SSL handshake. If left blank, it will be set to `server.host`

>> `verifyHostname`: whether to verify SSL hostname, default is `true`

## Features

### Transport

|     | Shadowsocks | VMess | Trojan |
|:----|:-----------:|:-----:|:------:|
| TCP |      ✔      |   ✔   |   ✔    |
| UDP |      ✔      |   ✔   |   ✔    |

### Ciphers

|                         | Shadowsocks |  VMess  |
|:------------------------|:-----------:|:-------:|
| aes-128-gcm             |   `C` `S`   | `C` `S` |
| aes-256-gcm             |   `C` `S`   |         |
| chacha20-poly1305       |   `C` `S`   | `C` `S` |
| 2022-blake3-aes-128-gcm |   `C` `S`   |         |
| 2022-blake3-aes-256-gcm |   `C` `S`   |         |

`C` for client `S` for server

## Build

    mvn clean package

### Require

Java 21+

### Build Output

server

    urban-spork-server/target/urban-spork-server.zip

client

    urban-spork-client-gui/target/urban-spork-client-gui.zip
