#!/bin/bash

HOME=$(dirname $(pwd))

cd ${HOME}/instrument-simulator/bin/
sh simulator-packages.sh

isUnNeed=${un_need_modules}
if [ "${isUnNeed}" != "1" ];then
	cd ${HOME}/instrument-modules/bin/
	sh packages.sh
fi


cd ${HOME}/simulator-agent/bin/
sh agent-packages.sh
