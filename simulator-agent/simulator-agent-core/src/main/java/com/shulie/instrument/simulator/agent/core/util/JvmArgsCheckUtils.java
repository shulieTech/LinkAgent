/**
 * Copyright 2021 Shulie Technology, Co.Ltd
 * Email: shulie@shulie.io
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * <p>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.shulie.instrument.simulator.agent.core.util;

import com.shulie.instrument.simulator.agent.core.gson.SimulatorGsonFactory;
import com.shulie.instrument.simulator.agent.spi.config.AgentConfig;

import java.io.File;
import java.util.*;

/**
 * @author angju
 * @date 2021/8/18 13:55
 */
public class JvmArgsCheckUtils {

    /**
     * 获取toolsJarPath路径
     *
     * @param jarPaths
     * @return
     */
    private static String scanByJarPaths(Set<String> jarPaths) {
        for (String jarPath : jarPaths) {
            if (jarPath != null && jarPath.contains("tools.jar")) {
                return jarPath;
            }

        }
        return "";
    }

    private static boolean checkJvmArgsStatus = true;

    public static boolean getCheckJvmArgsStatus() {
        return checkJvmArgsStatus;
    }

    /**
     * 启动jvm参数校验
     * 校验规则
     * 1、transmittable-thread-local-2.10.2.jar参数是否放在所有agent参数前校验（tro端校验）
     * 2、JDK7及以下参数是否配置-XX:PermSize=256M -XX:MaxPermSize=512M校验，参数值大小暂不校验（tro端校验）
     * 3、JDK8及以上参数是否配置-XX:MetaspaceSize=256M -XX:MaxMetaspaceSize=256M参数，并且如果使用了UseParallelGC垃圾收集器，校验是否配置了参数-XX
     * :SurvivorRatio=8
     * -XX:-UseAdaptiveSizePolicy（tro端校验）
     * 4、JDK9除了JDK8的参数校验，还要校验是否配置了--add-exports java.base/jdk.internal.module=ALL-UNNAMED（tro端校验）
     * 5、skywalking兼容性配置校验，启动参数包含了skywalking启动参数，需要校验是否配置-Dskywalking.agent.is_cache_enhanced_class=true
     * -Dskywalking.agent.class_cache_mode=MEMORY（tro端校验）
     * 6、启动参数appName校验，如果未配置appName，则将default作为应用名称（tro端校验）
     * 7、-Xbootclasspath/a:$JAVA_HOME/lib/tools.jar参数校验，包是否存在（tro端校验）
     * 8、探针日志目录存在以及权限校验（agent端校验
     *
     * @return
     */
    public static Map<String, Object> checkJvmArgs(String jdkVersion, String inputArgs, AgentConfig agentConfig) {
        List<String> jvmArgsList = SimulatorGsonFactory.getGson().fromJson(inputArgs, List.class);
        //数据解析准备
        int transmittableIndex = -1;
        int simulatorLauncherInstrumentIndex = -1;
        int minJavaagentIndex = -2;
        boolean skyWalkingExists = false;
        boolean skyWalkingCompatibleArgsExists = false;//skywalking兼容参数
        String permSizeValue = null;
        String maxMetaspaceSize = null;
        boolean jdk9ExportArgsExists = false;
        boolean useUseParallelGC = false;
        boolean useSurvivorRatio = false;
        boolean useAdaptiveSizePolicy = false;
        boolean delayAgent = false;
        String toolsJarPath = null;
        List<Integer> agentIndex = new ArrayList<>();
        for (int i = 0; i < jvmArgsList.size(); i++) {
            String arg = jvmArgsList.get(i);
            if (arg.contains("transmittable-thread-local")) {
                transmittableIndex = i;
                agentIndex.add(i);
            } else if (arg.contains("-javaagent")) {
                minJavaagentIndex = i;
                if (arg.contains("simulator-launcher-instrument.jar")) {
                    simulatorLauncherInstrumentIndex = i;
                }
                if (arg.contains("skywalking")) {
                    skyWalkingExists = true;
                }
                agentIndex.add(i);
            } else if (arg.contains("PermSize")) {
                permSizeValue = arg;
            } else if (arg.contains("MaxMetaspaceSize")) {
                maxMetaspaceSize = arg;
            } else if (arg.contains("jdk.internal.module=ALL-UNNAMED")) {
                jdk9ExportArgsExists = true;
            } else if (arg.contains("UseParallelGC")) {
                useUseParallelGC = true;
            } else if (arg.contains("SurvivorRatio")) {
                useSurvivorRatio = true;
            } else if (arg.contains("UseAdaptiveSizePolicy")) {
                useAdaptiveSizePolicy = true;
            } else if (arg.contains("tools.jar")) {
                toolsJarPath = arg;
            } else if (arg.contains("skywalking.agent.is_cache_enhanced_class")
                    || arg.contains("skywalking.agent.class_cache_mode")) {
                skyWalkingCompatibleArgsExists = true;
            } else if (arg.contains("simulator.delay")) {
                String delay = arg.substring(arg.indexOf("=") + 1);
                delayAgent = !"0".endsWith(delay.trim());
            }
        }
        Map<String, String> result = new HashMap<String, String>();
        //1、校验     * 1、transmittable-thread-local-2.10.2.jar参数是否放在所有agent参数前校验
        boolean transmittableResult = transmittableCheck(result, transmittableIndex, delayAgent, simulatorLauncherInstrumentIndex, new ArrayList<>(agentIndex));
        //2     * 2、JDK7及以下参数是否配置-XX:PermSize=256M -XX:MaxPermSize=512M校验，参数值大小暂不校验
        boolean permSizeValueCheckResult = permSizeValueCheck(result, jdkVersion, permSizeValue);
        //3       * 3、JDK8及以上参数是否配置-XX:MetaspaceSize=256M -XX:MaxMetaspaceSize=256M参数，
        // 并且如果使用了UseParallelGC垃圾收集器，校验是否配置了参数-XX:SurvivorRatio=8 -XX:-UseAdaptiveSizePolicy
        boolean metaspaceSizeValueCheckResult = metaspaceSizeValueCheck(result, jdkVersion, maxMetaspaceSize,
                useUseParallelGC, useSurvivorRatio, useAdaptiveSizePolicy);
        //4      * 4、JDK9除了JDK8的参数校验，还要校验是否配置了--add-exports java.base/jdk.internal.module=ALL-UNNAMED（tro端校验）
        boolean jdk9ExportArgsExistsCheckResult = jdk9ExportArgsExistsCheck(result, jdkVersion, jdk9ExportArgsExists);
        //5      * 5、skywalking兼容性配置校验，启动参数包含了skywalking启动参数，
        // 需要校验是否配置-Dskywalking.agent.is_cache_enhanced_class=true
        // -Dskywalking.agent.class_cache_mode=MEMORY（tro端校验）
        boolean skyWalkingCheckResult = skyWalkingCheck(result, skyWalkingExists, skyWalkingCompatibleArgsExists);
        //6     * 7、-Xbootclasspath/a:$JAVA_HOME/lib/tools.jar参数校验，包是否存在（tro端校验）
//        boolean checkToolsJarPathResult = checkToolsJarPath(result, toolsJarPath);
        //7.-javaagent:/Users/angju/Downloads/deploy-agent/simulator-agent/simulator-launcher-instrument.jar 需要放在最后
        boolean checkSimulatorLauncherInstrumentResult = delayAgent ? true : checkSimulatorLauncherInstrument(result, simulatorLauncherInstrumentIndex, minJavaagentIndex);

        Map<String, Object> r = new HashMap<String, Object>(2, 1);
        if (skipJvmArgsCheck(agentConfig) || (transmittableResult && permSizeValueCheckResult && metaspaceSizeValueCheckResult
                && jdk9ExportArgsExistsCheckResult && skyWalkingCheckResult
                && checkSimulatorLauncherInstrumentResult)) {
            checkJvmArgsStatus = true;
            r.put("status", "true");
        } else {
            checkJvmArgsStatus = false;
            r.put("status", "false");
            r.put("detail", result);
        }
        return r;
    }

    private static boolean skipJvmArgsCheck(AgentConfig agentConfig) {
        return agentConfig.getBooleanProperty("simulator.agent.skip.jvmArgsCheck", false);
    }

    private static boolean checkSimulatorLauncherInstrument(Map<String, String> result,
                                                            int simulatorLauncherInstrumentIndex, int minJavaagentIndex) {
        if (simulatorLauncherInstrumentIndex != minJavaagentIndex) {
            result.put(JvmArgsConstants.simulatorLauncherInstrumentNotLastCode,
                    JvmArgsConstants.simulatorLauncherInstrumentNotLastErrorMsg_1);
            return false;
        }
        return true;
    }

    private static boolean checkToolsJarPath(Map<String, String> result, String jvmToolsJarPath) {
        String toolsJarPath = scanByJarPaths(
                new HashSet<String>(Arrays.asList(System.getProperty("java.class.path").split(File.pathSeparator))));
        if (jvmToolsJarPath == null) {
            result.put(JvmArgsConstants.checkToolsJarPathCode, JvmArgsConstants.checkToolsJarPathErrorMsg_1);
            return false;
        }
        if (toolsJarPath == null) {
            result.put(JvmArgsConstants.checkToolsJarPathCode, JvmArgsConstants.checkToolsJarPathErrorMsg_2);
            return false;
        }
        return true;
    }

    private static boolean skyWalkingCheck(Map<String, String> result, boolean skyWalkingExists,
                                           boolean skyWalkingCompatibleArgsExists) {
        if (skyWalkingExists && !skyWalkingCompatibleArgsExists) {
            result.put(JvmArgsConstants.skyWalkingCheckCode, JvmArgsConstants.skyWalkingCheckErrorMsg_1);
            return false;
        }
        return true;
    }

    private static boolean jdk9ExportArgsExistsCheck(Map<String, String> result, String jdkVersion,
                                                     boolean jdk9ExportArgsExists) {
        if (jdkVersion == null) {
            result.put(JvmArgsConstants.jdkVersionNullCode, JvmArgsConstants.jdkVersionNullErrorMsg_1);
            return false;
        }
        if (!jdkVersion.startsWith("1.4") &&
                !jdkVersion.startsWith("1.5") &&
                !jdkVersion.startsWith("1.6") &&
                !jdkVersion.startsWith("1.7") &&
                !jdkVersion.startsWith("1.8")) {//jdk9及以上
            if (!jdk9ExportArgsExists) {
                result.put(JvmArgsConstants.jdk9ExportArgsExistsCheckCode,
                        JvmArgsConstants.jdk9ExportArgsExistsCheckErrorMsg_1);
                return false;
            }

        }
        return true;
    }

    private static boolean metaspaceSizeValueCheck(Map<String, String> result, String jdkVersion, String maxMetaspaceSize,
                                                   boolean useUseParallelGC, boolean useSurvivorRatio, boolean useAdaptiveSizePolicy) {
        if (jdkVersion == null) {
            result.put(JvmArgsConstants.jdkVersionNullCode, JvmArgsConstants.jdkVersionNullErrorMsg_1);
            return false;
        }
        if (!jdkVersion.startsWith("1.4") &&
                !jdkVersion.startsWith("1.5") &&
                !jdkVersion.startsWith("1.6") &&
                !jdkVersion.startsWith("1.7")) {//jdk8及以上

            if (maxMetaspaceSize != null) {
                maxMetaspaceSize = maxMetaspaceSize.toUpperCase();
                if (maxMetaspaceSize.endsWith("M")) {
                    int size = Integer.parseInt(maxMetaspaceSize.substring(maxMetaspaceSize.lastIndexOf("=") + 1, maxMetaspaceSize.length() - 1));
                    if (size < 100) {
                        result.put(JvmArgsConstants.metaspaceSizeValueCheckCode,
                                JvmArgsConstants.metaspaceSizeValueCheckErrorMsg_1);
                        return false;
                    }
                }
            }
            if (useUseParallelGC) {
                if (!useSurvivorRatio || !useAdaptiveSizePolicy) {
                    result.put(JvmArgsConstants.metaspaceSizeValueCheckCode,
                            JvmArgsConstants.metaspaceSizeValueCheckErrorMsg_2);
                    return false;
                }
            }
        }
        return true;
    }

    private static boolean permSizeValueCheck(Map<String, String> result, String jdkVersion, String permSizeValue) {
        if (jdkVersion == null) {
            result.put(JvmArgsConstants.jdkVersionNullCode, JvmArgsConstants.jdkVersionNullErrorMsg_1);
            return false;
        }
        if (jdkVersion.startsWith("1.4") ||
                jdkVersion.startsWith("1.5") ||
                jdkVersion.startsWith("1.6") ||
                jdkVersion.startsWith("1.7")) {
            if (permSizeValue == null) {
                result.put(JvmArgsConstants.permSizeValueCheckCode, JvmArgsConstants.permSizeValueCheckErrorMsg_1);
                return false;
            }
        }
        return true;
    }

    private static boolean transmittableCheck(Map<String, String> result, int transmittableIndex, boolean delayAgent, Integer simulatorLauncherInstrumentIndex, List<Integer> agentIndex) {
        if (transmittableIndex == -1) {
            result.put(JvmArgsConstants.transmittableCheckCode, JvmArgsConstants.transmittableCheckErrorMsg_1);
            return false;
        }
        // -Dsimulator.delay=0, 当有延迟时探针参数顺序可以随意
        if (delayAgent) {
            agentIndex.remove(simulatorLauncherInstrumentIndex);
        }
        if (transmittableIndex > agentIndex.get(0)) {
            result.put(JvmArgsConstants.transmittableCheckCode, JvmArgsConstants.transmittableCheckErrorMsg_2);
            return false;
        }
        return true;
    }
}
