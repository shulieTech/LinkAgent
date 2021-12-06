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

/**
 * @author angju
 * @date 2021/11/24 15:04
 */
public class HeartCommandConstants {
    /**
     * 在线升级的指令，只有这个指令由当前agent操作
     */
    public static final long onlineUpgradeCommandId = 110000;

    /**
     * 一批指令中包涵这个和升级的指令，需要优先处理这个指令，忽略升级指令
     */
    public static final long checkStorageCommandId = 100100;

    /**
     * 启动命令，有该指令时直接只执行这个指令
     */
    public static final long startCommandId = 200000;

    public static final long getSimulatorStatusCommandId = -10001;


    public static final String PATH_TYPE_KEY = "pathType";

    /**
     * -1表示使用本地包，无需下载远程包
     */
    public static final int PATH_TYPE_LOCAL_VALUE = -1;

    /**
     * 升级批次号key
     */
    public static final String UPGRADE_BATCH_KEY = "upgradeBatch";


    public static final String UN_INIT_UPGRADE_BATCH = "-1";

    public static final String MODULE_ID_KEY = "moduleId";

    public static final String MODULE_ID_VALUE_PRADAR_REGISTER = "pradar-register";

//    public static final String MODULE_ID_VALUE_PRADAR_CONFIG_FETCHER = "pradar-config-fetcher";
    public static final String MODULE_ID_VALUE_PRADAR_CONFIG_FETCHER = "command-execute-module";


    public static final String MODULE_METHOD_KEY = "moduleMethod";

    public static final String MODULE_METHOD_VALUE_PRADAR_REGISTER = "getSimulatorStatus";

    public static final String MODULE_METHOD_VALUE_PRADAR_CONFIG_FETCHER_DO_COMAAND = "doCommand";


    public static final String MODULE_METHOD_VALUE_PRADAR_CONFIG_FETCHER_GET_COMMAND_RESULT = "getCommandResult";



    public static final String MODULE_EXECUTE_COMMAND_TASK_SYNC_KEY = "sync";


    public static final String REQUEST_COMMAND_TASK_RESULT_KEY = "requestCommandTaskResult";

    public static final String COMMAND_ID_KEY = "commandId";

    public static final String TASK_ID_KEY = "taskId";


    public static final String ACCESSKEYID_KEY = "accessKeyId";
    public static final String ACCESSKEYSECRET_KEY = "accessKeySecret";
    public static final String ENDPOINT_KEY = "endpoint";
    public static final String BUCKETNAME_KEY = "bucketName";
    public static final String SALT_KEY = "salt";

    public static final String BASEPATH_KEY = "basePath";
    public static final String FTPHOST_KEY = "ftpHost";
    public static final String FTPPORT_KEY = "ftpPort";
    public static final String PASSWD_KEY = "passwd";
    public static final String USERNAME_KEY = "username";

}
