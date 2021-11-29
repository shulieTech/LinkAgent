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

package com.shulie.instrument.module.register.utils;

/**
 * @author angju
 * @date 2021/11/24 19:03
 */
public class SimulatorStatus {

    private static String INIT_STATUS = "UNINSTALL";

    private static String INSTALL_FAILED_STATUS = "INSTALL_FAILED";

    private static String INSTALLED_STATUS = "INSTALLED";

    private static String current_status = INIT_STATUS;

    private static String errorMsg = null;

    public static void installFailed(String msg){
        current_status = INSTALL_FAILED_STATUS;
        errorMsg = msg;
    }

    public static void installed(){
        current_status = "INSTALLED";
    }

    public static String getStatus() {
        return current_status;
    }


    public static boolean statusCalculated(){
        return current_status != INIT_STATUS;
    }

    public static String getErrorMsg() {
        return errorMsg;
    }
}
