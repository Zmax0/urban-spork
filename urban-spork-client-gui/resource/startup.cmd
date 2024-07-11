@echo off
set server="urban-spork-client-gui"
start javaw --add-opens java.base/java.lang.reflect=com.jfoenix --add-opens javafx.controls/com.sun.javafx.scene.control.behavior=com.jfoenix --add-opens javafx.controls/com.sun.javafx.scene.control=com.jfoenix -jar %server%.jar
exit