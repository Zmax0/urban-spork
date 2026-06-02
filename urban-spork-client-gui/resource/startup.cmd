@echo off
set server="urban-spork-client-gui"
start javaw --module-path lib --add-modules javafx.controls -Xms64m -Xmx256m -Dio.netty.maxDirectMemory=0 -jar %server%.jar
exit