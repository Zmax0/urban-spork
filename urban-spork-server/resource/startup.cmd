@echo off
chcp 65001
set server="urban-spork-server"
java ^
--enable-native-access=io.netty.common ^
--add-opens java.base/jdk.internal.misc=io.netty.common ^
--add-opens java.base/java.nio=io.netty.common ^
--add-reads io.netty.handler=org.bouncycastle.lts.prov ^
-Dio.netty.eventLoopThreads=8 ^
-Xms64m -Xmx192m -jar %server%.jar