# urban-spork

A sock5 proxy

## Require

Java 17+

## Quick start

put *config.json* file into the unpacked folder before running

```json5
{
  "servers": [
    {
      "cipher": "{cipher}",
      "password": "{password}",
      "port": "{port}",
      "protocol": "{protocol}"
    }
  ]
}
```

## Features

### Transmission

|     | Shadowsocks | VMess |
|:----|:-----------:|:-----:|
| TCP |      ✔      |   ✔   |
| UDP |      ✔      |       |

### Cipher

|                   | Shadowsocks | VMess |
|:------------------|:-----------:|:-----:|
| aes-128-gcm       |   `C` `S`   |  `C`  |
| aes-256-gcm       |   `C` `S`   |       |
| chacha20-poly1305 |   `C` `S`   |  `C`  |

`C` for client `S` for server

## Build

    mvn clean package

### Build Output

server

    urban-spork-server/target/urban-spork-server.zip

client

    urban-spork-client-gui/target/urban-spork-client-gui.zip
