# urban-spork

[![codecov](https://codecov.io/gh/Zmax0/urban-spork/branch/master/graph/badge.svg?token=6QAZQ05HZV)](https://codecov.io/gh/Zmax0/urban-spork)

A sock5 proxy

**WARNING**: Be aware of the risk when using this software because neither **Detection Prevention** nor **Replay Protection** is implemented currently

## Quick start

put *config.json* file into the unpacked folder before running

```json5
{
  "servers": [
    {
      "cipher": "{cipher}",
      "password": "{password}",
      "port": "{port}",
      "protocol": "{protocol}",
      "packetEncoding": "{packetEncoding}",
      "networks": [
        "{networks}"
      ]
    }
  ]
}
```

> `protocol`: "shadowsocks" | "vmess"

> `cipher`: see *Ciphers*

> `networks`: see *Transmission*

> `packetEncoding`: "None" | "Packet"

## Features

### Transmission

|     | Shadowsocks | VMess |
|:----|:-----------:|:-----:|
| TCP |      ✔     |   ✔   |
| UDP |      ✔     |   ✔   |

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

Java 17+

### Build Output

server

    urban-spork-server/target/urban-spork-server.zip

client

    urban-spork-client-gui/target/urban-spork-client-gui.zip
