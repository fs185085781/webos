#!/bin/bash
kill -9 `cat pidfile.txt`
ps -ef | grep webos.jar | grep -v grep | awk '{print $2}' | xargs -I {} kill -9 {}
sleep 3
cp=$(pwd)
javaPath="$cp/../jre/bin/java"
lc="java"
if [ -f "$javaPath" ]; then
    lc="$javaPath"
fi
nohup $lc -Dfile.encoding=UTF-8 -jar webos.jar > webos.log &
echo 'Starting...'
sleep 5
cat webos.log