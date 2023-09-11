package com.shulie.instrument.simulator.agent.instrument;

import java.io.File;
import java.lang.instrument.Instrumentation;
import java.lang.management.ManagementFactory;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.jar.JarFile;

public class TtlAgentBootstrapper {

    public static void tryToBootstrapTtlAgent(String featureString, Instrumentation instrumentation, ClassLoader parentClassLoader) {
        List<String> agentArgs = new ArrayList<String>();
        List<String> arguments = ManagementFactory.getRuntimeMXBean().getInputArguments();
        boolean hasTtlArgs = false;
        String agentPath = null;

        for (String argument : arguments) {
            if (argument.startsWith("-javaagent:")) {
                agentArgs.add(argument);
                if (argument.contains("transmittable-thread-local")) {
                    hasTtlArgs = true;
                }
                if (argument.endsWith("simulator-launcher-instrument.jar")) {
                    agentPath = argument;
                }
            }
        }
        // 超过1个agent就不启动ttl，因为顺序不能保证 || 如果参数指定了ttl, 也不启动
        if (agentArgs.size() > 1 || hasTtlArgs) {
            return;
        }
        if (agentPath == null) {
            System.err.println("[Instrument-Simulator]: can`t find simulator-launcher-instrument.jar from jvm arguments!");
            return;
        }
        try {
            String agentHome = agentPath.substring("-javaagent:".length(), agentPath.length() - "simulator-launcher-instrument.jar".length() - 1);
            String ttlAgentPath = agentHome + File.separator + "bootstrap" + File.separator + "transmittable-thread-local-2.12.1.jar";
            JarFile ttlJar = new JarFile((new File(ttlAgentPath)));
            instrumentation.appendToBootstrapClassLoaderSearch(ttlJar);
            instrumentation.appendToSystemClassLoaderSearch(ttlJar);

            Class<?> ttlLauncherClass = Class.forName("com.alibaba.ttl.threadpool.agent.TtlAgent", false, parentClassLoader);
            Method ttlPremain = ttlLauncherClass.getDeclaredMethod("premain", String.class, Instrumentation.class);
            ttlPremain.setAccessible(true);
            ttlPremain.invoke(null, featureString, instrumentation);
            System.setProperty("TtlAgent_internal_bootstrapped", "true");
        } catch (Throwable t) {
            System.err.println("[Simulator-Agent]: bootstrap ttl agent internal failed");
            t.printStackTrace();
            System.exit(1);
        }
    }


}
