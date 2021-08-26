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
package com.shulie.instrument.simulator.agent.api;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

/**
 * @author jirenhe | jirenhe@shulie.io
 * @since 2021/06/01 11:41 上午
 */
public class AgentFileResolver {

    private final static String SIMULATOR_HOME = "simulator";

    private final static Map<String, String> NEED_EXIST = new HashMap<String, String>();

    static {
        NEED_EXIST.put(joinFileSeparator(SIMULATOR_HOME, "bootstrap", "instrument-simulator-messager.jar"),
            "instrument-simulator-messager.jar 缺失!");
        NEED_EXIST.put(joinFileSeparator(SIMULATOR_HOME, "bootstrap", "simulator-bootstrap-api-1.0.0.jar"),
            "simulator-bootstrap-api-1.0.0.jar 缺失!");
        NEED_EXIST.put(joinFileSeparator(SIMULATOR_HOME, "bootstrap", "simulator-internal-bootstrap-api-1.0.0.jar"),
            "simulator-internal-bootstrap-api-1.0.0.jar 缺失!");
        NEED_EXIST.put(joinFileSeparator(SIMULATOR_HOME, "instrument-simulator-agent.jar"),
            "instrument-simulator-agent.jar 缺失!");
        NEED_EXIST.put(joinFileSeparator(SIMULATOR_HOME, "config", "simulator.properties"),
            "配置文件 simulator.properties 缺失!");
        NEED_EXIST.put(joinFileSeparator(SIMULATOR_HOME, "config", "version"),
            "缺失版本文件!");
        NEED_EXIST.put(joinFileSeparator(SIMULATOR_HOME, "lib", "instrument-simulator-core.jar"),
            "instrument-simulator-core.jar 缺失!");
        NEED_EXIST.put(joinFileSeparator(SIMULATOR_HOME, "provider", "instrument-simulator-management-provider.jar"),
            "instrument-simulator-management-provider.jar 缺失!");
    }

    private AgentFileResolver() {}

    public static String getSimulatorVersion(File dir) throws IOException {
        String path = dir.getAbsolutePath();
        String versionPath = joinFileSeparator(path, SIMULATOR_HOME, "config", "version");
        File file = new File(versionPath);
        if (!file.exists()) {
            throw new FileNotFoundException(String.format("版本文件：%s 不存在", versionPath));
        }
        final BufferedReader reader = new BufferedReader(new FileReader(file));
        try {
            return reader.readLine();
        } finally {
            reader.close();
        }
    }

    public static List<String> check(File dir) {
        String path = dir.getAbsolutePath();
        List<String> result = new ArrayList<String>();
        for (Entry<String, String> entry : NEED_EXIST.entrySet()) {
            File file = new File(joinFileSeparator(path, entry.getKey()));
            if (!file.exists()) {
                result.add(entry.getValue());
            }
        }
        return result;
    }

    private static String joinFileSeparator(String... paths) {
        StringBuilder builder = new StringBuilder();
        boolean first = true;
        for (String s : paths) {
            if (!first) {
                builder.append(File.separator);
            }
            builder.append(s);
            first = false;
        }
        return builder.toString();
    }

    public static void main(String[] args) throws IOException {
        File dir = new File("/Users/jirenhe/IdeaProjects/shulie/flpt/simulator-agent/target/simulator-agent/agent");
        List<String> strings = AgentFileResolver.check(dir);
        String version = AgentFileResolver.getSimulatorVersion(dir);
        for (String s : strings) {
            System.out.println(s);
        }
        System.out.println(version);
    }
}
