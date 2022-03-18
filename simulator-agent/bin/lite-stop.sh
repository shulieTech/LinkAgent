#!/bin/bash

LITEPID=`ps -ef | grep "simulator-launcher-lite.jar" | grep -v grep | awk '{print $2}'`
if [ ! "$LITEPID" ];
then
        echo "pid not found"
else
        kill -9 $LITEPID
        echo "success"
fi