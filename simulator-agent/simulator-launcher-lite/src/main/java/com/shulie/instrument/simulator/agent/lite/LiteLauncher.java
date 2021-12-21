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

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import com.shulie.instrument.simulator.agent.lite.util.JpsCommand;
import com.shulie.instrument.simulator.agent.lite.util.RuntimeMXBeanUtils;
import com.sun.tools.attach.VirtualMachine;

/**
 * @Description agent attach入口
 * @Author ocean_wll
 * @Date 2021/12/16 2:09 下午
 */
public class LiteLauncher {

    /**
     * agentHome地址
     */
    private static final String DEFAULT_AGENT_HOME
        = new File(LiteLauncher.class.getProtectionDomain().getCodeSource().getLocation().getFile())
        .getParent();

    /**
     * PID文件目录
     */
    private static final String DIRECTORY_PATH = DEFAULT_AGENT_HOME + File.separator + "pids";

    /**
     * 定时任务线程池
     */
    private static final ScheduledThreadPoolExecutor poolExecutor = new ScheduledThreadPoolExecutor(1);

    public static void main(String[] args) throws IOException {
        String agentLauncherPath
            = "/Users/ocean_wll/IdeaProjects/LinkAgent/deploy/simulator-agent/simulator-launcher-instrument.jar";
        System.out.println(new File(agentLauncherPath).exists());
        VirtualMachine vm = null;
        try {
            vm = VirtualMachine.attach("2678");
            vm.loadAgent(agentLauncherPath, ";simulator.delay=10");
        } catch (Exception e) {
            System.out.println(e.getLocalizedMessage());
        } finally {
            if (vm != null) {
                vm.detach();
            }
        }
    }

    public static void main11(String[] args) throws IOException {
        //首次执行延迟10S，之后1S执行一次
        poolExecutor.scheduleAtFixedRate(() -> {
            try {
                // 获取attach的Pid列表
                List<String> pidList = JpsCommand.getPidList();
                if (pidList.size() > 0) {
                    // TODO ocean_wll 这块需要修改一下，不应该引用 simulator-agent-core 包
                    String agentPath = DEFAULT_AGENT_HOME + File.separator + "core" + File.separator
                        + "simulator-agent-core.jar";

                    //生产需要attach的PID列表和生产对应的PID文件
                    attachAgent(getAttachPidList(pidList), agentPath);
                    deletePidFiles(pidList);
                }
            } catch (Throwable t) {
                t.printStackTrace(System.err);
            }

        }, 10, 1, TimeUnit.SECONDS);
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
                    vm.loadAgent(agentJarPath);
                }
                System.out.println(str + "进程 attach成功");
            } catch (Exception e) {
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
     * @param pidList 进程列表
     */
    private static List<String> getAttachPidList(List<String> pidList) {
        File directory = new File(DIRECTORY_PATH);
        if (!directory.exists()) {
            //如果目录不存在则进行创建
            directory.mkdirs();
        }
        List<String> attachPidList = new ArrayList<>();
        for (String str : pidList) {
            //创建文件
            String fileName = DEFAULT_AGENT_HOME + File.separator + str;
            File file = new File(fileName);
            try {
                if (!file.exists()) {
                    file.createNewFile();
                    //如果创建了则代表需要attach
                    attachPidList.add(str);
                }
            } catch (IOException e) {
                // TODO ocean_wll 后续研究下日志怎么记
                e.printStackTrace();
                // ignore
            }
        }
        return attachPidList;
    }

    /**
     * 删除PID文件
     *
     * @param pidList
     */
    private static void deletePidFiles(List<String> pidList) {
        File[] pidFileList = new File(DIRECTORY_PATH).listFiles();
        if (pidFileList == null || pidFileList.length == 0) {
            return;
        }
        List<String> pidNameList = new ArrayList<>();
        Arrays.stream(pidFileList).forEach(pidFile -> pidNameList.add(pidFile.getName()));
        //pid文件根据pid列表移除，剩下的就是不需要的，size=0说明没有重启过，或者刚好一致，
        // 如果>0就说明剩下的文件没有进程与之匹配需要删除
        pidNameList.removeAll(pidList);
        if (pidNameList.size() > 0) {
            pidNameList.forEach(item -> new File(DIRECTORY_PATH + File.separator + item).delete());
        }
    }
}