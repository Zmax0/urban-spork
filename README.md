# urban-spork

Shadowsocks proxy

[![Codacy Badge](https://api.codacy.com/project/badge/Grade/3286f43f4c854b4da8c1058637343273)](https://www.codacy.com/manual/Zmax0/urban-spork?utm_source=github.com&amp;utm_medium=referral&amp;utm_content=Zmax0/urban-spork&amp;utm_campaign=Badge_Grade)

## Require

Java 17

## Build

    mvn clean install

### Server config
put *config.json* file into the unpacked folder
  
    {
        "servers":[
            {
                "cipher":"{cipher}",
                "password":"{password}",
                "port":"{port}"
            }
        ]
    }

### Supported Ciphers

-  [x] aes-128-gcm
-  [x] aes-192-gcm
-  [x] aes-256-gcm
-  [x] chacha20-ietf-poly1305
-  [ ] xchacha20-ietf-poly1305
