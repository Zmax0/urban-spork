@echo off
chcp 65001
set server="urban-spork-server"
java ^
--add-opens java.base/jdk.internal.misc=ALL-UNNAMED ^
--add-opens java.base/java.nio=ALL-UNNAMED ^
--add-reads io.netty.handler=org.bouncycastle.lts.prov ^
--add-reads io.netty.handler=ALL-UNNAMED ^
-Dio.netty.tryReflectionSetAccessible=true ^
-Xms64m -Xmx256m -jar %server%.jar