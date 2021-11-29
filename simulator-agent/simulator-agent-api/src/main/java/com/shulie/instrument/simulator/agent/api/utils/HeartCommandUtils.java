/*
 * *
 *  * Copyright 2021 Shulie Technology, Co.Ltd
 *  * Email: shulie@shulie.io
 *  * Licensed under the Apache License, Version 2.0 (the "License");
 *  * you may not use this file except in compliance with the License.
 *  * You may obtain a copy of the License at
 *  *
 *  * http://www.apache.org/licenses/LICENSE-2.0
 *  *
 *  * Unless required by applicable law or agreed to in writing, software
 *  * distributed under the License is distributed on an "AS IS" BASIS,
 *  * See the License for the specific language governing permissions and
 *  * limitations under the License.
 *
 */
package com.shulie.instrument.simulator.agent.api.utils;

import com.shulie.instrument.simulator.agent.api.model.CommandExecuteKey;
import com.shulie.instrument.simulator.agent.spi.model.CommandExecuteResponse;

import java.io.*;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.*;

/**
 * @author angju
 * @date 2021/11/18 15:27
 */
public class HeartCommandUtils {


    /**
     * 是否已经执行成功startCommandId名
     */
    public static boolean startCommandResult = false;

    public static String allModuleVersionDetail = null;






    private static final Map<CommandExecuteKey, CommandExecuteResponse> futureMap = new ConcurrentHashMap<CommandExecuteKey, CommandExecuteResponse>(16, 1);


    private static final Map<CommandExecuteKey, CommandExecuteResponse> completedMap = new ConcurrentHashMap<CommandExecuteKey, CommandExecuteResponse>(16, 1);

    static {
        //.../.../.../simulator-agent/
        String defaultAgentHome
                = new File(HeartCommandUtils.class.getProtectionDomain().getCodeSource().getLocation().getFile())
                .getParent().replace("core", "");
        String agentModuleProperties = defaultAgentHome + "module.properties";
        String simulatorModuleProperties = defaultAgentHome + "agent/simulator/module.properties";
        String userModuleProperties = defaultAgentHome + "agent/simulator/modules/module.properties";

        File agentModulePropertiesFile = new File(agentModuleProperties);
        File simulatorModulePropertiesFile = new File(simulatorModuleProperties);
        File userModulePropertiesFile = new File(userModuleProperties);
        if (!agentModulePropertiesFile.exists() || !simulatorModulePropertiesFile.exists()
            || !userModulePropertiesFile.exists()){
            //TODO
        } else {
            List<String> agentModulePropertiesLines = readFile(agentModulePropertiesFile);
            List<String> simulatorModulePropertiesLines = readFile(simulatorModulePropertiesFile);
            List<String> userModulePropertiesLines = readFile(userModulePropertiesFile);
            agentModulePropertiesLines.addAll(simulatorModulePropertiesLines);
            agentModulePropertiesLines.addAll(userModulePropertiesLines);
            Collections.sort(agentModulePropertiesLines);
            StringBuilder stringBuilder = new StringBuilder();
            for (String s : agentModulePropertiesLines){
                stringBuilder.append(s).append(";");
            }
            allModuleVersionDetail = stringBuilder.toString();
        }
    }

    private static List<String> readFile(File file){
        FileInputStream inputStream = null;
        List<String> result = new ArrayList<String>();
        try {
            inputStream = new FileInputStream(file);
            BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(inputStream));

            String str;
            while((str = bufferedReader.readLine()) != null)
            {
                //只取模块名称和版本
                String[] temp = str.split(";");
                result.add(temp[0] + "," + temp[1]);
            }
            //close
            inputStream.close();
            bufferedReader.close();
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            return result;
        }
    }



    public static final Map<CommandExecuteKey, CommandExecuteResponse> getCompletedMap(){
        return completedMap;
    }

    public static final Map<CommandExecuteKey, CommandExecuteResponse> getFutureMap(){
        return futureMap;
    }

    public static int futureMapSize(){
        return futureMap.size();
    }

    public static int completedMapSize(){
        return completedMap.size();
    }

    public static boolean futureMapEmpty(){
        return futureMap.isEmpty();
    }

    public static boolean completedMapEmpty(){
        return completedMap.isEmpty();
    }

    public static CommandExecuteResponse getCommandExecuteResponseFuture(long commandId, String taskId){
        CommandExecuteKey commandExecuteKey = new CommandExecuteKey(commandId, taskId);
        return futureMap.get(commandExecuteKey);
    }

    public static CommandExecuteResponse getCommandExecuteResponse(long commandId, String taskId){
        CommandExecuteKey commandExecuteKey = new CommandExecuteKey(commandId, taskId);
        return completedMap.get(commandExecuteKey);
    }


    public static CommandExecuteResponse putCommandExecuteResponseFuture(long commandId, String taskId, CommandExecuteResponse commandExecuteResponse){
        return futureMap.put(new CommandExecuteKey(commandId, taskId), commandExecuteResponse);
    }


    public static CommandExecuteResponse putCommandExecuteResponse(long commandId, String taskId, CommandExecuteResponse commandExecuteResponse){
        return completedMap.put(new CommandExecuteKey(commandId, taskId), commandExecuteResponse);
    }


    public static CommandExecuteResponse removeCommandExecuteResponseFuture(long commandId, String taskId){
        return futureMap.remove(new CommandExecuteKey(commandId, taskId));
    }


    public static CommandExecuteResponse removeCommandExecuteResponse(long commandId, String taskId){
        return completedMap.remove(new CommandExecuteKey(commandId, taskId));
    }
}
