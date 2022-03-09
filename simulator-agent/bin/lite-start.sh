JAVAHOME=$JAVA_HOME
if [ ! $JAVAHOME ]; then
  echo "启动失败，未配置JAVA_HOME，请配置后重试！"
  exit 1
else
  echo "环境检查成功，日志请查看../logs/lite.log文件。"
fi

nohup java -Xbootclasspath/a:$JAVA_HOME/lib/tools.jar  -jar ../simulator-launcher-lite.jar >../logs/lite.log 2>&1 &