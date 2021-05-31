# Agent Packaging

## Environment Dependency

### 1.1 jdk8

Configure environment variables JAVA_8_HOME.
Example (Mac):
```
JAVA_8_HOME=/Library/Java/JavaVirtualMachines/jdk1.8.0_181.jdk/Contents/Home
CLASSPATH=.:$JAVA_8_HOME/lib/dt.jar:$JAVA_8_HOME/lib/tools.jar
PATH=$JAVA_8_HOME/bin:$PATH:
export JAVA_8_HOME
```

### 1.2 jdk9

Configure environment variables JAVA_9_HOME.
Example (Mac):
```aidl
JAVA_9_HOME=/Library/Java/JavaVirtualMachines/jdk-9.0.4.jdk/Contents/Home
CLASSPATH=.:$JAVA_9_HOME/lib/dt.jar:$JAVA_9_HOME/lib/tools.jar
PATH=$JAVA_9_HOME/bin:$PATH:
export JAVA_9_HOME
```

JAVA_HOME must be configured. If you have JDK 1.6 then configure it to JDK 1.6， if not, you can use JDK8 instead.

## Packaging

1. Enter project `simulator-agent` directory `bin`，Excute script`agent-packages.sh`
2. Enter project  `simulator-agent`directory`bin`，Excute script`agent-packages.sh`
3. Enter project  `instrument-simulator`directory`bin`，Excute script`simulator-packages.sh`
4. Enter project  `instrument-modules`directory`bin`，Excute script`packages.sh`
5. Copy project `instrument-modules`directory`target` 's `modules` and `bootstrap` to project `instrument-simulator`directory`target` 's `simulator`
6. Copy project `instrument-modules`directory`target` 's `biz-classloader-jars/` to project `instrument-simulator`directory`target`'s`simulator`'s`biz-classloader-jars`
7. Copy project `instrument-modules`directory`target` 's `bootstrap/` to project`instrument-simulator`directory`target`'s`simulator`directory`bootstrap`
8. Copy project `instrument-simulator`directory`target` 's `simulator` to project `simulator-agent` 's `target/simulator-agent/agent`

> If copy target directory doesn't exist, create a new one.



