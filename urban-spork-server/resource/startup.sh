#!/bin/bash
server="urban-spork-server"
eval pid="$(pgrep -f ${server})"
if [ "${pid}" ]; then
  echo "kill ${server} pid ${pid}"
  kill -9 "${pid}"
  sleep 1
fi
nohup $JAVA_HOME/bin/java --add-opens java.base/jdk.internal.misc=ALL-UNNAMED --illegal-access=warn -Dio.netty.tryReflectionSetAccessible=true -Dio.netty.leakDetectionLevel=SIMPLE -Dio.netty.leakDetection.targetRecords=1 -XX:MaxDirectMemorySize=16m -Xms64m -Xmx256m -jar "${server}.jar" >log 2>&1 &
eval pid="$(pgrep -f ${server})"
echo "start ${server} pid ${pid}"
tail -1000f log