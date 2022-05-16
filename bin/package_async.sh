#!/bin/bash

HOME=$(dirname $(pwd))

t=$1

if [ "$t" == "1" ];then
cd ${HOME}/instrument-simulator/bin/
sh simulator-packages.sh
fi

if [ "$t" == "2" ];then
isUnNeed=${un_need_modules}
if [ "${isUnNeed}" != "1" ];then
	cd ${HOME}/instrument-modules/bin/
	sh packages.sh
fi
fi

if [ "$t" == "3" ];then
cd ${HOME}/simulator-agent/bin/
sh agent-packages.sh
fi
