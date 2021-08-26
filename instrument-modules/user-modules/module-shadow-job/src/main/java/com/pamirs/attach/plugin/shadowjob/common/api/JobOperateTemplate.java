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
package com.pamirs.attach.plugin.shadowjob.common.api;

import com.dangdang.ddframe.job.reg.base.CoordinatorRegistryCenter;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

/**
 * 作业操作的模板.
 *
 * @author zhangliang
 */
public final class JobOperateTemplate {

    private final CoordinatorRegistryCenter registryCenter;

    public JobOperateTemplate(CoordinatorRegistryCenter registryCenter) {
        this.registryCenter = registryCenter;
    }

    /**
     * 作业操作.
     *
     * @param jobName  作业名称
     * @param serverIp 作业服务器IP地址
     * @return 操作失败的作业服务器IP地址列表(作业维度操作)或作业名称列表(IP维度操作)
     */
    public Collection<String> operate(final String jobName, final String serverIp, final JobOperateCallback callback) {
        Collection<String> result;
        if (jobName != null && serverIp != null) {
            boolean isSuccess = callback.doOperate(jobName, serverIp);
            if (!isSuccess) {
                result = new ArrayList<String>(1);
                result.add(serverIp);
            } else {
                result = Collections.emptyList();
            }
        } else if (jobName != null) {
            JobNodePath jobNodePath = new JobNodePath(jobName);
            List<String> ipList = registryCenter.getChildrenKeys(jobNodePath.getServerNodePath());
            result = new ArrayList<String>(ipList.size());
            for (String each : ipList) {
                boolean isSuccess = callback.doOperate(jobName, each);
                if (!isSuccess) {
                    result.add(each);
                }
            }
        } else {
            List<String> jobNames = registryCenter.getChildrenKeys("/");
            result = new ArrayList<String>(jobNames.size());
            for (String each : jobNames) {
                boolean isSuccess = callback.doOperate(each, serverIp);
                if (!isSuccess) {
                    result.add(each);
                }
            }
        }
        return result;
    }
}
