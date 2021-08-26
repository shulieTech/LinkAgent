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
package com.pamirs.attach.plugin.shadowjob.interceptor;

import java.lang.reflect.Field;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import com.pamirs.attach.plugin.shadowjob.obj.quartz.PtQuartzJobBean;
import com.pamirs.pradar.interceptor.ParametersWrapperInterceptorAdaptor;
import com.shulie.instrument.simulator.api.listener.ext.Advice;
import org.quartz.Job;
import org.quartz.spi.TriggerFiredBundle;

/**
 * @author angju
 * @date 2021/3/27 23:13
 */
public class JobExecutionContextInterceptor extends ParametersWrapperInterceptorAdaptor {

    private Field field;

    private Map<String, Job> stringJobConcurrentHashMap = new ConcurrentHashMap<String, Job>(16, 1);

    @Override
    public Object[] getParameter0(Advice advice) throws Throwable {
        Job job = (Job)advice.getParameterArray()[2];
        TriggerFiredBundle firedBundle = (TriggerFiredBundle)advice.getParameterArray()[1];
        if (job.getClass().getName().equals("com.pamirs.attach.plugin.shadowjob.obj.quartz.PtQuartzJobBean")) {
            Job busJob = stringJobConcurrentHashMap.get(firedBundle.getJobDetail().getDescription());
            PtQuartzJobBean ptQuartzJobBean = (PtQuartzJobBean)advice.getParameterArray()[2];
            if (field == null) {
                field = PtQuartzJobBean.class.getDeclaredField("busJob");
            }
            field.setAccessible(true);
            field.set(ptQuartzJobBean, busJob);
            return advice.getParameterArray();
        }

        if (stringJobConcurrentHashMap.containsKey(job.getClass().getName())) {
            stringJobConcurrentHashMap.put(job.getClass().getName(), job);
        }

        return advice.getParameterArray();
    }

    @Override
    protected void clean() {
        if (stringJobConcurrentHashMap != null) {
            stringJobConcurrentHashMap.clear();
            stringJobConcurrentHashMap = null;
        }
    }
}
