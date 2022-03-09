JAVAHOME=$JAVA_HOME
toolsPath=$JAVA_HOME/lib/tools.jar

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

checkToolsResult=1
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

checkJavaHome
checkTools
echo $checkToolsResult
while [ 1 == $checkToolsResult ]; do
    read -p "请手动指定tools.jar路径: " toolsPath
    checkTools
done

echo "环境检查成功，启动探针...，启动日志请查看'../logs/lite.log'"

nohup java -Xbootclasspath/a:$toolsPath -jar ../simulator-launcher-lite.jar >../logs/lite.log 2>&1 &