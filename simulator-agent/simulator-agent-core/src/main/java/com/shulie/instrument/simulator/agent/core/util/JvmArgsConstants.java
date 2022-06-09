/**
 * Copyright 2021 Shulie Technology, Co.Ltd
 * Email: shulie@shulie.io
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.shulie.instrument.simulator.agent.core.util;

/**
 * @author angju
 * @date 2021/8/18 13:51
 */
public class JvmArgsConstants {
    public static final String transmittableCheckCode = "TransmittableCheck";
    public static final String transmittableCheckErrorMsg_1 = "transmittable-thread-local-2.10.2.jar 启动参数未添加，请检查启动参数";
    public static final String transmittableCheckErrorMsg_2 = "transmittable-thread-local-2.10.2.jar 启动参数未在所有javaagent参数前，请检查启动参数";


    public static final String permSizeValueCheckCode = "PermSizeValueCheck";
    public static final String permSizeValueCheckErrorMsg_1 = "jdk7以及以下，未配置-XX:PermSize=256M -XX:MaxPermSize=512M，请检查启动参数";

    public static final String metaspaceSizeValueCheckCode = "MetaspaceSizeValueCheck";
    public static final String metaspaceSizeValueCheckErrorMsg_1 = "jdk8及以上，-XX:MaxMetaspaceSize 不能小于100M，请检查启动参数";
    public static final String metaspaceSizeValueCheckErrorMsg_2 = "jdk8及以上，配置了-XX:UseParallelGC，未配置-XX:SurvivorRatio=8 -XX:-UseAdaptiveSizePolicy，请检查启动参数";



    public static final String jdk9ExportArgsExistsCheckCode = "Jdk9ExportArgsExistsCheck";
    public static final String jdk9ExportArgsExistsCheckErrorMsg_1 = "jdk9及以上未配置--add-exports java.base/jdk.internal.module=ALL-UNNAMED，请检查启动参数";



    public static final String skyWalkingCheckCode = "SkyWalkingCheck";
    public static final String skyWalkingCheckErrorMsg_1 = "应用有skyWalking使用，未配置skyWalking兼容参数，并确保skyWalking版本至少为8.1.0";

    public static final String checkToolsJarPathCode = "CheckToolsJarPath";
    public static final String checkToolsJarPathErrorMsg_1 = "未配置-Xbootclasspath/a:$JAVA_HOME/lib/tools.jar参数，请检查启动参数";
    public static final String checkToolsJarPathErrorMsg_2 = "应用服务器未检查到$JAVA_HOME/lib/tools.jar的包信息，请检查tools.jar是否存在";

    public static final String jdkVersionNullCode = "JdkVersionNull";
    public static final String jdkVersionNullErrorMsg_1 = "无法获取jdk版本，jdk版本相关数据无法验证";


    public static final String simulatorLauncherInstrumentNotLastCode = "simulatorLauncherInstrumentNotLastCode";
    public static final String simulatorLauncherInstrumentNotLastErrorMsg_1 = "simulator-launcher-instrument.jar 需要放在所有javaagent的参数最后";


    public static final String simulatorLogPathCode = "simulatorLogPathCode";
    public static final String simulatorLogPathCodeErrorMsg_1 = "日志目录不存在，%s";
    public static final String simulatorLogPathCodeErrorMsg_2 = "日志目录无读权限，%s";
    public static final String simulatorLogPathCodeErrorMsg_3 = "日志目录无写权限，%s";
    public static final String simulatorLogPathCodeErrorMsg_4 = "日志目录未配置，%s";

}
