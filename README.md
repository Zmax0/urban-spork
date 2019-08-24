# urban-spork
Shadowsocks proxy

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
- [x] aes-256-cfb
- [x] aes-256-ctr
- [x] aes-256-gcm
- [x] chacha20-ietf
- [x] camellia-256-cfb
