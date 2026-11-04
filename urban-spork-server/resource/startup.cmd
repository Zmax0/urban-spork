@echo off
chcp 65001
set server="urban-spork-server"
java ^
--enable-native-access=com.urbanspork.jni ^
--enable-native-access=io.netty.common ^
--add-opens java.base/jdk.internal.misc=io.netty.common ^
--add-opens java.base/java.nio=io.netty.common ^
-Dio.netty.eventLoopThreads=8 ^
-Xms32m -Xmx192m -jar %server%.jar