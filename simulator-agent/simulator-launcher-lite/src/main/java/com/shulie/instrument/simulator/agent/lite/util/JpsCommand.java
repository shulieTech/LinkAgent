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

package com.shulie.instrument.simulator.agent.lite.util;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * @Description jps命令处理器
 * @Author ocean_wll
 * @Date 2021/12/16 3:13 下午
 */
public class JpsCommand {

    /**
     * JPS命令
     */
    private final static String JPS_COMMAND = "jps -l";

    /**
     * 获取系统进程集合
     *
     * @throws IOException io异常
     */
    public static List<JpsResult> getSystemProcessList() throws IOException {
        List<JpsResult> systemProcessList = new ArrayList<>();
        Process process = Runtime.getRuntime().exec(JPS_COMMAND);
        try (InputStream inputStream = process.getInputStream();
             InputStreamReader inputStreamReader = new InputStreamReader(inputStream);
             BufferedReader reader = new BufferedReader(inputStreamReader)) {
            process.waitFor(3, TimeUnit.SECONDS);
            String line;
            while ((line = reader.readLine()) != null) {
                if (!line.trim().matches("^[0-9]*$")) {
                    String[] pidItem = line.split(" ");
                    if (pidItem.length == 2) {
                        JpsResult jpsResult = new JpsResult(pidItem[0], pidItem[1], buildAppName(pidItem[1]));
                        systemProcessList.add(jpsResult);
                    }
                }
            }
            return systemProcessList;
        } catch (Exception e) {
            LogUtil.error("获取系统进程集合异常, " + Arrays.toString(e.getStackTrace()));
            return systemProcessList;
        }
    }

    /**
     * 处理应用名
     *
     * app.main/cc.cc1234.PrettyZooApplication
     * io.shulie.takin.agent.server.TakinAgentServerApplication
     * sun.tools.jps.Jps
     * ../HelloWord.jar
     *
     * 处理步骤：
     * 1、有文件分隔符的取最后一个分隔符后的字符串
     * 2、以.jar结尾的取.jar前的字符串
     * 3、如果字符串中还包含.则取最后一个.后的字符串
     *
     * @param originalName 原始的进程名
     * @return 处理后的应用名
     */
    private static String buildAppName(String originalName) {
        if (originalName == null) {
            return "";
        }
        if (originalName.contains(File.separator)) {
            originalName = originalName.substring(originalName.lastIndexOf(File.separator) + 1);
        }
        if (originalName.endsWith(".jar")) {
            originalName = originalName.substring(0, originalName.length() - 4);
        }
        if (originalName.contains(".")) {
            originalName = originalName.substring(originalName.lastIndexOf(".") + 1);
        }
        return originalName;
    }

    /**
     * jps对象
     */
    public static class JpsResult {
        /**
         * pid
         */
        private String pid;

        /**
         * 原始名称
         */
        private String originalName;

        /**
         * jar包名
         */
        private String appName;

        public JpsResult() {
        }

        public JpsResult(String pid, String originalName, String appName) {
            this.pid = pid;
            this.originalName = originalName;
            this.appName = appName;
        }

        public String getPid() {
            return pid;
        }

        public void setPid(String pid) {
            this.pid = pid;
        }

        public String getOriginalName() {
            return originalName;
        }

        public void setOriginalName(String originalName) {
            this.originalName = originalName;
        }

        public String getAppName() {
            return appName;
        }

        public void setAppName(String appName) {
            this.appName = appName;
        }

        @Override
        public String toString() {
            return pid + " " + appName;
        }

        public boolean isLegal() {
            return pid != null && !"".equals(pid.trim()) && appName != null && !"".equals(appName.trim())
                && originalName != null && !"".equals(originalName.trim());
        }
    }
}
