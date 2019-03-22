# urban-spork
Shadowsocks proxy build by java

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
