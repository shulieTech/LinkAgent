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

package com.shulie.instrument.simulator.agent.core.util;

import com.shulie.instrument.simulator.agent.core.CoreLauncher;

import java.io.File;

/**
 * @author angju
 * @date 2021/12/2 20:50
 */
public class UpgradeFileUtils {
    public static String getUpgradeFileTempSaveDir(){
        return new File(CoreLauncher.class.getProtectionDomain().getCodeSource().getLocation().getFile())
                .getParent();
    }

    public static String getUpgradeFileTempFileName(String upgradeBatch){
        return "simulator_" + upgradeBatch + ".zip";
    }

    public static void clearOldUpgradeFileTempFile(String upgradeBatch){
        String saveFileName = getUpgradeFileTempFileName(upgradeBatch);
        String saveTempDir = getUpgradeFileTempSaveDir();
        //存的包已经存在先删除
        File tempSimulatorFile = new File(saveTempDir + File.separator + saveFileName);
        if (tempSimulatorFile.exists()){
            tempSimulatorFile.delete();
        }
        tempSimulatorFile = null;
        //oss下到一半的包 simulator_111111111.zip.tmp
        File ossTempSimulatorFile = new File(saveTempDir + File.separator + saveFileName + ".tmp");
        if (ossTempSimulatorFile.exists()){
            ossTempSimulatorFile.delete();
        }
        ossTempSimulatorFile = null;
    }

    public static void checkLocal(){
        String saveTempDir = getUpgradeFileTempSaveDir();
        //判断agent目录是否存在，存在则做移动agent_upgradeBatch_时间戳
        String agentBasePath = saveTempDir.replace("core", "");
        File file = new File(agentBasePath + "agent/simulator");
        File localFile = new File(agentBasePath + "agent/simulator_-1");
        if (file.exists()){
            if (localFile.exists()){
                deleteFolder(file);
                localFile.renameTo(file);
            }
        } else if (localFile.exists()){
            localFile.renameTo(file);
        } else {
            throw new RuntimeException("本地simulator包不存在，无法启动探针!");
        }
    }

    public static void unzipUpgradeFile(String upgradeBatch){
        String saveFileName = getUpgradeFileTempFileName(upgradeBatch);
        String saveTempDir = getUpgradeFileTempSaveDir();
        //判断agent目录是否存在，存在则做移动agent_upgradeBatch_时间戳
        String agentBasePath = saveTempDir.replace("core", "");
        File file = new File(agentBasePath + "agent/simulator");
        if (file.exists()){
            //保留最开始的本地版本，回滚可用
            File localSimulatorFile = new File(file.getAbsolutePath() + "_-1" );
            if (!localSimulatorFile.exists()){
                file.renameTo(localSimulatorFile);
            } else {
                file.delete();
            }
        }
        File agentDir = new File(agentBasePath + "agent");
        if (!agentDir.exists()){
            agentDir.mkdir();
        }
        ZipUtils.unZip(saveTempDir + File.separator + saveFileName,
                agentDir.getAbsolutePath() + "/simulator");
    }

    /**
     * 删除文件夹
     * @param folder
     * @throws Exception
     */
    private static void deleteFolder(File folder) {
        if (!folder.exists()) {
            return;
        }
        File[] files = folder.listFiles();
        if (files != null) {
            for (File file : files) {
                if (file.isDirectory()) {
                    //递归直到目录下没有文件
                    deleteFolder(file);
                } else {
                    //删除
                    file.delete();
                }
            }
        }
        //删除
        folder.delete();

    }

}
