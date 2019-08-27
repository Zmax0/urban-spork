#!/bin/bash
server="urban-spork-server"
eval pid=$(pgrep -f ${server})
if [ ${pid} ]
then
    echo "kill ${server} pid "${pid}
    kill -9 ${pid}
    sleep 1
fi
nohup $JAVA_HOME/bin/java -jar -Dio.netty.noPreferDirect=true -Dio.netty.allocator.type=unpooled -Dio.netty.maxDirectMemory=0 -Dio.netty.leakDetectionLevel=advanced ${server}.jar > log 2>&1 &
eval pid=$(pgrep -f ${server})
echo "start ${server} pid "${pid}
tail -1000f log