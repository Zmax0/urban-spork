@echo off
set server="urban-spork-server"
java --add-opens java.base/jdk.internal.misc=ALL-UNNAMED --add-opens java.base/java.nio=ALL-UNNAMED -Dio.netty.tryReflectionSetAccessible=true -Dio.netty.leakDetection.level=SIMPLE -Dio.netty.leakDetection.targetRecords=1 -Xms64m -Xmx256m -jar %server%.jar