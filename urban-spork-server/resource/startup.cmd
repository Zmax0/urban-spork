@echo off
set server="urban-spork-server"
java --add-opens java.base/jdk.internal.misc=ALL-UNNAMED --illegal-access=warn -Dio.netty.tryReflectionSetAccessible=true -Dio.netty.leakDetectionLevel=SIMPLE -Dio.netty.leakDetection.targetRecords=1 -XX:MaxDirectMemorySize=16m -Xms64m -Xmx256m -jar %server%.jar