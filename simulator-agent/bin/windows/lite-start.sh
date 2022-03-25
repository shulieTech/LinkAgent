#!/bin/bash

sh lite-stop.sh

current_path=$(dirname $(pwd))
JAVAHOME=$JAVA_HOME
toolsPath=$JAVA_HOME\\lib\\tools.jar
checkToolsResult=1

function checkJavaHome(){
	echo "检查JAVA_HOME！"
  if [ ! $JAVAHOME ]; then
    echo "启动失败，未配置JAVA_HOME，请配置后重启！"
    exit 1
  else
    echo "JAVA_HOME检查成功！"
  fi
  return 0;
}

function checkTools(){
	echo "检查tools.jar!"
  if [ ! -f "${toolsPath}" ]; then
    echo "$toolsPath 不存在，请检查后再重启！"
    checkToolsResult=1
  else
    echo "tools.jar检查成功！"
    checkToolsResult=0
  fi
}

sed -i 's/simulator.log.path=~/simulator.log.path=/g' ..\\config\\agent.properties

line=$(sed -n '/simulator.log.path=/p' ..\\config\\agent.properties)

sed -i 's~'$line'~simulator.log.path='$current_path'\\logs~g' ..\\config\\agent.properties

checkJavaHome
checkTools
while [ 1 == $checkToolsResult ]; do
    read -p "请手动指定tools.jar路径: " toolsPath
    checkTools
done

echo "环境检查成功，启动探针...,启动日志请查看..\logs\lite.log"


nohup java -Xbootclasspath/a:"$toolsPath" -jar ..\\simulator-launcher-lite.jar >..\\logs\\lite.log 2>&1 &