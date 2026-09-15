#!/bin/bash
cd "$(dirname "$0")"
./runtime/bin/java -Xms64m -Xmx256m -Dio.netty.maxDirectMemory=0 -jar urban-spork-client-gui.jar
