#!/bin/bash
server="urban-spork-server"
eval pid="$(pgrep -f ${server})"
if [ "${pid}" ]; then
  echo "kill ${server} pid ${pid}"
  kill -15 "${pid}"
  sleep 1
fi
mkdir -p logs
touch logs/server.log
nohup "$JAVA_HOME"/bin/java \
--add-opens java.base/jdk.internal.misc=ALL-UNNAMED \
--add-opens java.base/java.nio=ALL-UNNAMED \
--add-reads io.netty.handler=org.bouncycastle.lts.prov \
--add-reads io.netty.handler=ALL-UNNAMED \
-Dio.netty.tryReflectionSetAccessible=true \
-Xms64m -Xmx256m -jar "${server}.jar" >/dev/null 2>&1 &
eval pid="$(pgrep -f ${server})"
echo "start ${server} pid ${pid}"
tail -100f logs/server.log