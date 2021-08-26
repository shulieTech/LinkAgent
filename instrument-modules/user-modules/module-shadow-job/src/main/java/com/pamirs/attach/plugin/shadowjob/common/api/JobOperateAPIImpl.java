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

import java.util.Collection;

/**
 * 操作作业的实现类.
 *
 * @author zhangliang
 */
public final class JobOperateAPIImpl implements JobOperateAPI {

    private final CoordinatorRegistryCenter registryCenter;

    private final JobOperateTemplate jobOperatorTemplate;

    public JobOperateAPIImpl(final CoordinatorRegistryCenter registryCenter) {
        this.registryCenter = registryCenter;
        jobOperatorTemplate = new JobOperateTemplate(registryCenter);
    }

    @Override
    public void trigger(final String jobName, final String serverIp) {
        jobOperatorTemplate.operate(jobName, serverIp, new JobOperateCallback() {

            @Override
            public boolean doOperate(final String jobName, final String serverIp) {
                registryCenter.persist(new JobNodePath(jobName).getServerNodePath(serverIp, JobNodePath.TRIGGER_NODE), "");
                return true;
            }
        });
    }

    @Override
    public void pause(final String jobName, final String serverIp) {
        jobOperatorTemplate.operate(jobName, serverIp, new JobOperateCallback() {

            @Override
            public boolean doOperate(final String jobName, final String serverIp) {
                registryCenter.persist(new JobNodePath(jobName).getServerNodePath(serverIp, JobNodePath.PAUSED_NODE), "");
                return true;
            }
        });
    }

    @Override
    public void resume(final String jobName, final String serverIp) {
        jobOperatorTemplate.operate(jobName, serverIp, new JobOperateCallback() {

            @Override
            public boolean doOperate(final String jobName, final String serverIp) {
                registryCenter.remove(new JobNodePath(jobName).getServerNodePath(serverIp, JobNodePath.PAUSED_NODE));
                return true;
            }
        });
    }

    @Override
    public void disable(final String jobName, final String serverIp) {
        jobOperatorTemplate.operate(jobName, serverIp, new JobOperateCallback() {

            @Override
            public boolean doOperate(final String jobName, final String serverIp) {
                registryCenter.persist(new JobNodePath(jobName).getServerNodePath(serverIp, JobNodePath.DISABLED_NODE), "");
                return true;
            }
        });
    }

    @Override
    public void enable(final String jobName, final String serverIp) {
        jobOperatorTemplate.operate(jobName, serverIp, new JobOperateCallback() {

            @Override
            public boolean doOperate(final String jobName, final String serverIp) {
                registryCenter.remove(new JobNodePath(jobName).getServerNodePath(serverIp, JobNodePath.DISABLED_NODE));
                return true;
            }
        });
    }

    @Override
    public void shutdown(final String jobName, final String serverIp) {
        jobOperatorTemplate.operate(jobName, serverIp, new JobOperateCallback() {

            @Override
            public boolean doOperate(final String jobName, final String serverIp) {
                registryCenter.persist(new JobNodePath(jobName).getServerNodePath(serverIp, JobNodePath.SHUTDOWN_NODE), "");
                return true;
            }
        });
    }

    @Override
    public Collection<String> remove(final String jobName, final String serverIp) {
        return jobOperatorTemplate.operate(jobName, serverIp, new JobOperateCallback() {

            @Override
            public boolean doOperate(final String jobName, final String serverIp) {
                JobNodePath jobNodePath = new JobNodePath(jobName);
                if (registryCenter.isExisted(jobNodePath.getServerNodePath(serverIp, JobNodePath.STATUS_NODE))) {
                    return false;
                }
                registryCenter.remove(jobNodePath.getServerNodePath(serverIp));
                if (registryCenter.getChildrenKeys(jobNodePath.getServerNodePath()).isEmpty()) {
                    registryCenter.remove("/" + jobName);
                }
                return true;
            }
        });
    }
}
