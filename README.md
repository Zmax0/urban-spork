# urban-spork
Shadowsocks proxy build by java

## Build

    mvn clean install
      
### Server config
put *config.json* file into the unpacked folder
  
    {
      "socks5.server.password":"{password}",
      "socks5.server.port":{port}
    }
