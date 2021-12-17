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
     * 获取pid集合
     *
     * @throws IOException io异常
     */
    public static List<String> getPidList() throws IOException {
        List<String> pidList = new ArrayList<>();
        Process process = Runtime.getRuntime().exec(JPS_COMMAND);

        try (InputStream inputStream = process.getInputStream();
             InputStreamReader inputStreamReader = new InputStreamReader(inputStream);
             BufferedReader reader = new BufferedReader(inputStreamReader)) {
            process.waitFor(3, TimeUnit.SECONDS);
            String line;
            while ((line = reader.readLine()) != null) {
                System.out.println(line);
                if (!line.trim().matches("^[0-9]*$")) {
                    String[] pidItem = line.split(" ");
                    if (!needFilterPid(pidItem[0], pidItem[1])) {
                        pidList.add(pidItem[0]);
                    }
                }
            }
            return pidList;
        } catch (Exception e) {
            e.printStackTrace();
            return pidList;
        }
    }

    /**
     * 判断是否需要过滤此pid
     *
     * @param pid     pid
     * @param appName 应用名
     * @return true:需要过滤，false:不需要过滤
     */
    private static Boolean needFilterPid(String pid, String appName) {
        if (pid == null || pid.trim().length() == 0 || appName == null || appName.trim().length() == 0) {
            return true;
        }
        return "Jps".equals(appName) || pid.equals(String.valueOf(RuntimeMXBeanUtils.getPid()));
    }
}
