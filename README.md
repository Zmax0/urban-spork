# urban-spork

A sock5 proxy

[![Codacy Badge](https://api.codacy.com/project/badge/Grade/3286f43f4c854b4da8c1058637343273)](https://www.codacy.com/manual/Zmax0/urban-spork?utm_source=github.com&amp;utm_medium=referral&amp;utm_content=Zmax0/urban-spork&amp;utm_campaign=Badge_Grade)

## Require

Java 17+

## Quick start

put *config.json* file into the unpacked folder before running

```json5
{
  "servers": [
    {
      "cipher": "{cipher}", // see Features
      "password": "{password}",
      "port": "{port}",
      "protocol": "{protocol}"
    }
  ]
}
```

## Features

| cipher            | Shadowsocks | VMess |
|:------------------|:-----------:|:-----:|
| aes-128-gcm       |   `C` `S`   |  `C`  |
| aes-192-gcm       |   `C` `S`   |       |
| aes-256-gcm       |   `C` `S`   |       |
| chacha20-poly1305 |   `C` `S`   |  `C`  |

`C` for client `S` for server

## Build

    mvn clean install