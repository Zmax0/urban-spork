@echo off
set server="urban-spork-client-gui"
start javaw --module-path lib --add-modules javafx.controls --add-modules com.jfoenix --add-opens java.base/java.lang.reflect=com.jfoenix --add-opens javafx.controls/com.sun.javafx.scene.control.behavior=com.jfoenix --add-opens javafx.controls/com.sun.javafx.scene.control=com.jfoenix -Xms64m -Xmx256m -Dio.netty.maxDirectMemory=0 -jar %server%.jar
exit