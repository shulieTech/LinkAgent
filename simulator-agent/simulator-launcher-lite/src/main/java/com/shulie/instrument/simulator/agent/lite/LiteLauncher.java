/*
 * Copyright 2021 Shulie Technology, Co.Ltd
 * Email: shulie@shulie.io
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.shulie.instrument.simulator.agent.lite;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import com.shulie.instrument.simulator.agent.lite.util.JpsCommand;
import com.shulie.instrument.simulator.agent.lite.util.JpsCommand.JpsResult;
import com.shulie.instrument.simulator.agent.lite.util.RuntimeMXBeanUtils;
import com.sun.tools.attach.VirtualMachine;

/**
 * @Description agent attach入口
 * @Author ocean_wll
 * @Date 2021/12/16 2:09 下午
 */
public class LiteLauncher {

    /**
     * 对象锁
     */
    private static final Object LOCK = new Object();

    /**
     * simulator 延迟加载时间
     */
    private static final Integer SIMULATOR_DELAY = 5 * 60;

    /**
     * agentHome地址
     */
    private static final String DEFAULT_AGENT_HOME
        = new File(LiteLauncher.class.getProtectionDomain().getCodeSource().getLocation().getFile())
        .getParent();

    /**
     * PID文件目录
     */
    private static final String PIDS_DIRECTORY_PATH = DEFAULT_AGENT_HOME + File.separator + "pids";

    /**
     * simulator-launcher-instrument.jar 包地址
     */
    private static final String SIMULATOR_BEGIN_JAR_PATH = DEFAULT_AGENT_HOME + File.separator
        + "simulator-launcher-instrument.jar";

    /**
     * ignore.config 文件地址
     */
    private static final String IGNORE_CONFIG_PATH = DEFAULT_AGENT_HOME + File.separator + "config" + File.separator
        + "ignore.config";

    /**
     * 定时任务线程池
     */
    private static final ScheduledThreadPoolExecutor poolExecutor = new ScheduledThreadPoolExecutor(1,
        runnable -> new Thread(runnable, "Takin-Lite-Scan-Server"));

    /**
     * agent启动参数配置
     */
    private static final String AGENT_START_PARAM = ";simulator.delay=%s";

    ///**
    // * 启动agent
    // *
    // * @param args 参数
    // */
    //public static void main(String[] args) throws IOException {
    //    String agentLauncherPath
    //        = "/Users/ocean_wll/IdeaProjects/LinkAgent/deploy/simulator-agent/simulator-launcher-instrument.jar";
    //    System.out.println(new File(agentLauncherPath).exists());
    //    VirtualMachine vm = null;
    //    try {
    //        vm = VirtualMachine.attach("2679");
    //        vm.loadAgent(agentLauncherPath, ";simulator.delay=10");
    //    } catch (Exception e) {
    //        System.out.println(e.getLocalizedMessage());
    //    } finally {
    //        if (vm != null) {
    //            vm.detach();
    //        }
    //    }
    //}

    public static void main(String[] args) throws IOException {
        System.out.println("启动 simulator-launcher-lite");
        //首次执行延迟1分钟，之后1分钟执行一次
        poolExecutor.scheduleAtFixedRate(() -> {
            try {
                // 获取attach的Pid列表
                List<JpsResult> systemProcessList = JpsCommand.getSystemProcessList();
                if (systemProcessList.size() > 0) {
                    List<String> pidList = getAttachPidList(systemProcessList);
                    System.out.println("attach pid list: " + pidList);
                    //生产需要attach的PID列表和生产对应的PID文件
                    attachAgent(pidList, SIMULATOR_BEGIN_JAR_PATH);
                    deletePidFiles(pidList);
                }
            } catch (Throwable t) {
                t.printStackTrace(System.err);
            }

        }, 60, 1, TimeUnit.MINUTES);
        System.out.println("simulator-launcher-lite 启动成功");
    }

    /**
     * 加载agent
     *
     * @param targetJvmPid 目标进程PID
     * @param agentJarPath agent路径
     * @throws Exception
     */
    private static void attachAgent(List<String> targetJvmPid,
        final String agentJarPath) throws Exception {
        VirtualMachine vm = null;
        for (String str : targetJvmPid) {
            try {
                vm = VirtualMachine.attach(str);
                if (vm != null) {
                    vm.loadAgent(agentJarPath, String.format(AGENT_START_PARAM, SIMULATOR_DELAY));
                }
                System.out.println(str + "进程 attach成功");
            } catch (Throwable e) {
                deletePidFiles(Collections.singletonList(str));
                System.out.println(str + "进程 attach失败");
            } finally {
                if (null != vm) {
                    vm.detach();
                }
            }
        }
    }

    /**
     * 获得需要attach的进程列表,并生成对应的PID文件
     *
     * @param systemProcessList 进程列表
     */
    private static List<String> getAttachPidList(List<JpsResult> systemProcessList) {
        // 读取文件获取需要跳过的应用名
        List<String> ignoreApps = ignoreAppList();
        File directory = new File(PIDS_DIRECTORY_PATH);
        if (!directory.exists()) {
            //如果目录不存在则进行创建
            directory.mkdirs();
        }
        List<String> attachPidList = new ArrayList<>();
        for (JpsResult jpsResult : systemProcessList) {
            // 过滤数据
            if (!jpsResult.isLegal()
                || ignoreApps.contains(jpsResult.getAppName().trim())
                || jpsResult.getPid().equals(String.valueOf(RuntimeMXBeanUtils.getPid()))) {
                continue;
            }
            System.out.println("need attach pid: " + jpsResult.getPid() + " ,appName: " + jpsResult.getAppName());
            //创建文件
            String fileName = PIDS_DIRECTORY_PATH + File.separator + jpsResult.getPid();
            File file = new File(fileName);
            try {
                if (!file.exists()) {
                    synchronized (LOCK) {
                        if (!file.exists()) {
                            file.createNewFile();
                            //如果创建了则代表需要attach
                            attachPidList.add(jpsResult.getPid());
                        }
                    }
                }
            } catch (IOException e) {
                System.out.println(Arrays.toString(e.getStackTrace()));
            }
        }
        return attachPidList;
    }

    /**
     * 读取 ignore.config 文件获取需要忽略的应用名
     *
     * @return 需要忽略的应用名
     */
    private static List<String> ignoreAppList() {
        List<String> ignoreAppList = new ArrayList<>();
        File file = new File(IGNORE_CONFIG_PATH);
        if (!file.exists()) {
            return ignoreAppList;
        }
        try (FileInputStream fileInputStream = new FileInputStream(file);
             BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(fileInputStream));) {
            String line;
            while ((line = bufferedReader.readLine()) != null) {
                ignoreAppList.add(line.trim());
            }
        } catch (Exception e) {
            System.out.println("read " + IGNORE_CONFIG_PATH + " error。" + Arrays.toString(e.getStackTrace()));
        }
        return ignoreAppList;
    }

    /**
     * 删除PID文件
     *
     * @param pidList
     */
    private static void deletePidFiles(List<String> pidList) {
        File[] pidFileList = new File(PIDS_DIRECTORY_PATH).listFiles();
        if (pidFileList == null || pidFileList.length == 0) {
            return;
        }
        List<String> pidNameList = new ArrayList<>();
        Arrays.stream(pidFileList).forEach(pidFile -> pidNameList.add(pidFile.getName()));
        //pid文件根据pid列表移除，剩下的就是不需要的，size=0说明没有重启过，或者刚好一致，
        // 如果>0就说明剩下的文件没有进程与之匹配需要删除
        pidNameList.removeAll(pidList);
        if (pidNameList.size() > 0) {
            pidNameList.forEach(item -> new File(PIDS_DIRECTORY_PATH + File.separator + item).delete());
        }
    }
}