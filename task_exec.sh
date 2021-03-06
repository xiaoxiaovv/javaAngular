#!/bin/sh
. /etc/profile
. ~/.bash_profile

task_home=/usr/local/bjjtask
log_path=/var/logs/bjjtask/

for i in $task_home/lib/*.jar;
    do CLASSPATH="$CLASSPATH":$i;
done

export CLASSPATH=$task_home/mediabroken-1.0.jar:$task_home/:$CLASSPATH

pid=`ps -ef | grep $1 | grep java | head -1 | awk '{print  $2}'`
if [ -z "$pid" ]  
then  
    java -Xms512m -Xmx512m -XX:MaxPermSize=64m -XX:PermSize=64m -XX:MaxNewSize=128m -XX:NewSize=128m -classpath $CLASSPATH com.istar.mediabroken.task.TaskManager $1 > $log_path/$1.log 2>&1 &
else
    echo "$1 is running"
fi