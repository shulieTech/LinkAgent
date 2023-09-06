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
package com.shulie.instrument.simulator.agent.instrument;

import java.io.*;
import java.lang.instrument.Instrumentation;
import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.jar.JarFile;
import java.util.zip.ZipEntry;

/**
 * @author xiaobin.zfb|xiaobin@shulie.io         入口类
 * @since 2020/12/7 3:56 下午
 */
public class InstrumentLauncher {
    private static String DEFAULT_AGENT_HOME;

    private final static String SIMULATOR_KEY_DELAY = "simulator.delay";
    private final static String SIMULATOR_KEY_UNIT = "simulator.unit";

    static {
        try {
            DEFAULT_AGENT_HOME = new File(InstrumentLauncher.class.getProtectionDomain().getCodeSource().getLocation().getFile()).getParent();
        } catch (Exception e) {

        }
    }

    /**
     * 其他探针里启动takin, 方便传入探针目录
     *
     * @param agentHome
     * @param agentArgs
     * @param instrumentation
     */
    public static void premain(final String agentHome, final String agentArgs, final Instrumentation instrumentation) {
        DEFAULT_AGENT_HOME = agentHome;
        start(agentArgs, instrumentation);
    }

    public static void premain(final String agentArgs, final Instrumentation instrumentation) {
        start(agentArgs, instrumentation);
    }

    public static void premain(String agentArgs) {
        start(agentArgs, null);
    }

    public static void agentmain(String agentArgs, Instrumentation inst) {
        start(agentArgs, inst);
    }

    public static void agentmain(String agentArgs) {
        start(agentArgs, null);
    }

    /**
     * 是否不是空字符串
     *
     * @param string 字符串
     * @return true|false
     */
    private static boolean isNotBlank(final String string) {
        return null != string && string.length() > 0 && !string.matches("^\\s*$");
    }

    /**
     * 是否是空字符串
     *
     * @param string 字符串
     * @return true|false
     */
    private static boolean isBlank(final String string) {
        return !isNotBlank(string);
    }

    /**
     * 将字符串转换成键值对
     *
     * @param featureString 字符串
     * @return 键值对
     */
    private static Map<String, String> toFeatureMap(final String featureString) {
        final Map<String, String> featureMap = new LinkedHashMap<String, String>();

        // 不对空字符串进行解析
        if (isBlank(featureString)) {
            return featureMap;
        }

        // KV对片段数组
        final String[] kvPairSegmentArray = featureString.split(";");
        if (kvPairSegmentArray.length <= 0) {
            return featureMap;
        }

        for (String kvPairSegmentString : kvPairSegmentArray) {
            if (isBlank(kvPairSegmentString)) {
                continue;
            }
            final String[] kvSegmentArray = kvPairSegmentString.split("=");
            if (kvSegmentArray.length != 2 || isBlank(kvSegmentArray[0]) || isBlank(kvSegmentArray[1])) {
                continue;
            }
            featureMap.put(kvSegmentArray[0], kvSegmentArray[1]);
        }

        return featureMap;
    }

    /**
     * trim 字符串
     *
     * @param str 字符串
     * @return 返回 trim后的字符串
     */
    public static String trim(String str) {
        return str == null ? null : str.trim();
    }

    /**
     * 是否是数字
     *
     * @param str 字符串
     * @return 返回 true|false
     */
    public static boolean isNumeric(final String str) {
        if (isBlank(str)) {
            return false;
        }
        final int sz = str.length();
        for (int i = 0; i < sz; i++) {
            if (!Character.isDigit(str.charAt(i))) {
                return false;
            }
        }
        return true;
    }

    /**
     * 获取延时加载的时间间隔
     *
     * @param args         参数
     * @param defaultValue 默认值
     * @return
     */
    private static Integer getDelay(Map<String, String> args, Integer defaultValue) {
        String property = System.getProperty(SIMULATOR_KEY_DELAY);
        if (isNumeric(property)) {
            return Integer.valueOf(property);
        }
        return defaultValue;
    }

    /**
     * 获取延时加载的时间单位
     *
     * @param args         参数
     * @param defaultValue 默认值
     * @return
     */
    private static TimeUnit getTimeUnit(Map<String, String> args, TimeUnit defaultValue) {
        String property = System.getProperty(SIMULATOR_KEY_UNIT);
        property = trim(property);
        for (TimeUnit tUnit : TimeUnit.values()) {
            if (tUnit.name().equalsIgnoreCase(property)) {
                return tUnit;
            }
        }
        return defaultValue;
    }

    /**
     * 启动启动器
     *
     * @param featureString 字符串
     * @param inst          Instrumentation
     */
    public static void start(String featureString, Instrumentation inst) {
        // 先尝试启动ttl探针
        TtlAgentBootstrapper.tryToBootstrapTtlAgent(featureString, inst, InstrumentLauncher.class.getClassLoader());

        final Map<String, String> args = toFeatureMap(featureString);
        final Integer delay = getDelay(args, null);
        final TimeUnit timeUnit = getTimeUnit(args, null);

        /**
         * 延迟加载，因为使用 instrument 方式增强，不使用独立进程方式，所以
         * 需要防止此 jar 包中可能存在一些与应用相冲突的依赖如 zk 等，需要等待这些资源
         * 在应用加载后再加载，防止与应用之前发生冲突。这种情况一般见于如 weblogic、jboss、
         * websphere 等 web 容器中
         */
        final long pid = RuntimeMXBeanUtils.getPid();
        final String processName = RuntimeMXBeanUtils.getName();
        try {
            /**
             * 如果是 jdk9及以上则采用外置进程方式attach 进程
             * 如果是 jdk9以下则使用内部方式attach 进程
             */
            startInternal(pid, processName, delay, timeUnit, inst);
        } catch (Throwable e) {
            System.err.println("SIMULATOR: start Agent failed. \n" + getStackTraceAsString(e));
        }
    }

    private static String getStackTraceAsString(Throwable throwable) {
        StringWriter stringWriter = new StringWriter();
        throwable.printStackTrace(new PrintWriter(stringWriter));
        return stringWriter.toString();
    }

    /**
     * 获取加载的 tag 文件名称
     *
     * @return
     */
    private static String getTagFileName() {
        File file = new File(DEFAULT_AGENT_HOME);
        File[] files = file.listFiles(new FileFilter() {
            @Override
            public boolean accept(File pathname) {
                return pathname.isFile() && pathname.getName().endsWith(".tag");
            }
        });
        if (files == null || files.length == 0) {
            return null;
        }
        return files[0].getName();
    }

    /**
     * 内部启动 agent
     *
     * @param pid
     * @param processName
     * @throws MalformedURLException
     * @throws ClassNotFoundException
     * @throws NoSuchMethodException
     * @throws InstantiationException
     * @throws IllegalAccessException
     * @throws java.lang.reflect.InvocationTargetException
     */
    private static void startInternal(final long pid, final String processName, Integer delay, TimeUnit unit, Instrumentation inst) throws IOException, ClassNotFoundException, NoSuchMethodException, InstantiationException, IllegalAccessException, java.lang.reflect.InvocationTargetException {
        File file = new File(DEFAULT_AGENT_HOME + File.separator + "core", "simulator-agent-core.jar");
        AgentClassLoader agentClassLoader = new AgentClassLoader(extractSimulatorAgentCoreImportResources(file));
        Class coreLauncherOfClass = agentClassLoader.loadClass("com.shulie.instrument.simulator.agent.core.CoreLauncher");
        Constructor constructor = coreLauncherOfClass.getConstructor(String.class, long.class, String.class, String.class, Instrumentation.class, ClassLoader.class);
        Object coreLauncherOfInstance = constructor.newInstance(DEFAULT_AGENT_HOME, pid, processName, getTagFileName(), inst, InstrumentLauncher.class.getClassLoader());

        if (delay != null) {
            Method setDelayMethod = coreLauncherOfClass.getDeclaredMethod("setDelay", int.class);
            setDelayMethod.invoke(coreLauncherOfInstance, delay.intValue());
        }

        if (unit != null) {
            Method setUnitMethod = coreLauncherOfClass.getDeclaredMethod("setUnit", TimeUnit.class);
            setUnitMethod.invoke(coreLauncherOfInstance, unit);
        }

        Method startMethod = coreLauncherOfClass.getDeclaredMethod("start");
        startMethod.invoke(coreLauncherOfInstance);
    }

    private static URL[] extractSimulatorAgentCoreImportResources(File jar) throws IOException {
        URL[] noImportUrls = new URL[]{jar.toURI().toURL()};
        JarFile jarFile;
        jarFile = new JarFile(jar);
        ZipEntry jarEntry = jarFile.getEntry("import-dependencies.config");
        if (jarEntry == null) {
            return noImportUrls;
        }

        String[] baseDirs;
        String[] importArtifacts;
        InputStream in = null;
        try {
            in = jarFile.getInputStream(jarEntry);
            Properties properties = new Properties();
            properties.load(in);
            String baseDir = properties.getProperty("import-dependency-dir");
            if (baseDir == null || baseDir.length() == 0) {
                return noImportUrls;
            }
            String lib = properties.getProperty("import-artifacts");
            if (lib == null || lib.length() == 0) {
                return noImportUrls;
            }
            importArtifacts = lib.split(",");
            baseDirs = baseDir.split(",");
        } finally {
            if (in != null) {
                in.close();
            }
        }

        if (importArtifacts == null || baseDirs == null) {
            return noImportUrls;
        }

        File simulatorAgent = jar;
        while (true) {
            simulatorAgent = simulatorAgent.getParentFile();
            if (simulatorAgent.getName().equals("simulator-agent")) {
                break;
            }
        }

        Map<String, String> importJars = new HashMap<String, String>();

        for (String s : importArtifacts) {
            for (String baseDir : baseDirs) {
                File importJar = extractArtifacts(simulatorAgent, baseDir.trim(), s.trim());
                if (importJar == null) {
                    continue;
                }
                // 排除重复的jar
                String artifactId = importJar.getName().substring(0, importJar.getName().lastIndexOf("-"));
                if (!importJars.containsKey(artifactId)) {
                    importJars.put(artifactId, importJar.getAbsolutePath());
                }
            }
        }

        List<URL> urls = new ArrayList<URL>();
        urls.add(jar.toURI().toURL());
        for (String jarPath : importJars.values()) {
            urls.add(new File(jarPath).toURI().toURL());
        }

        return urls.toArray(new URL[0]);
    }

    private static File extractArtifacts(File agentPath, String baseDir, final String artifactId) {
        if (artifactId.length() == 0) {
            return null;
        }

        if (baseDir.startsWith("..")) {
            while (baseDir.startsWith("..")) {
                agentPath = agentPath.getParentFile();
                baseDir = baseDir.substring(3);
            }
        }

        File dir = new File(agentPath, baseDir);
        if (!dir.exists()) {
            return null;
        }

        File[] files = dir.listFiles(new FileFilter() {
            @Override
            public boolean accept(File file) {
                String name = file.getName();
                return name.contains("-") && extractArtifactId(file).equals(artifactId);
            }
        });
        if (files.length == 0) {
            return null;
        }
        return new File(agentPath, baseDir + File.separator + files[0].getName());
    }

    private static String extractArtifactId(File jarFile) {
        String name = jarFile.getName();
        while (name.contains(".") && name.contains("-")) {
            name = name.substring(0, name.lastIndexOf("-"));
        }
        return name;
    }
}
