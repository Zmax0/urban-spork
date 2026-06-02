#!/bin/bash
server="urban-spork-client-gui"
"$JAVA_HOME"/bin/java --module-path lib --add-modules javafx.controls -Xms64m -Xmx256m -Dio.netty.maxDirectMemory=0 -jar ${server}.jar