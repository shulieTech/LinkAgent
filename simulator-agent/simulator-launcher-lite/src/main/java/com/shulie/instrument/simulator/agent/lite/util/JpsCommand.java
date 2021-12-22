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
    private final static String JPS_COMMAND = "jps";

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
                        JpsResult jpsResult = new JpsResult(pidItem[0], pidItem[1]);
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
     * jps对象
     */
    public static class JpsResult {
        /**
         * pid
         */
        private String pid;

        /**
         * jar包名
         */
        private String appName;

        public JpsResult() {
        }

        public JpsResult(String pid, String appName) {
            this.pid = pid;
            this.appName = appName;
        }

        public String getPid() {
            return pid;
        }

        public void setPid(String pid) {
            this.pid = pid;
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
            return pid != null && !"".equals(pid.trim()) && appName != null && !"".equals(appName.trim());
        }
    }
}
