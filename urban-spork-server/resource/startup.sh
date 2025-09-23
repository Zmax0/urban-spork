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
--enable-native-access=io.netty.common \
--add-opens java.base/jdk.internal.misc=io.netty.common \
--add-opens java.base/java.nio=io.netty.common \
--add-reads io.netty.handler=org.bouncycastle.lts.prov \
-Dio.netty.eventLoopThreads=8 \
-Xms32m -Xmx192m -jar "${server}.jar" >/dev/null 2>&1 &
eval pid="$(pgrep -f ${server})"
echo "start ${server} pid ${pid}"
tail -100f logs/server.log