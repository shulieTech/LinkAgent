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

package com.pamirs.attach.plugin.cluster.test.check.interceptor;

import com.pamirs.attach.plugin.cluster.test.check.exception.ClusterTestNotReadyException;
import com.pamirs.pradar.PradarService;
import com.shulie.instrument.simulator.api.annotation.ListenerBehavior;
import com.shulie.instrument.simulator.api.listener.ext.Advice;
import com.shulie.instrument.simulator.api.listener.ext.AdviceListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Arrays;

/**
 * @Description
 * @Author ocean_wll
 * @Date 2022/8/22 15:06
 */
@ListenerBehavior(isNoSilence = true, isExecuteWithClusterTestDisable = true)
public abstract class AbstractCheckInterceptor extends AdviceListener {

    private static final Logger logger = LoggerFactory.getLogger(AbstractCheckInterceptor.class);

    @Override
    public void before(Advice advice) throws Throwable {
        Object uaValue;
        Object clusterTestValue;
        try {
            uaValue = getParam(advice, PradarService.PRADAR_HTTP_CLUSTER_TEST_KEY);
            clusterTestValue = getParam(advice, PradarService.PRADAR_CLUSTER_TEST_KEY);
        } catch (Throwable t) {
            logger.error("【LinkAgent】get request header error, className:{}, method:{}, requestType:{}",
                    advice.getTargetClass().getName(),
                    advice.getBehaviorName(),
                    Arrays.toString(advice.getBehavior().getParameterTypes()),
                    t);
            throw new ClusterTestNotReadyException("【LinkAgent】get request header error", t);
        }

        boolean isClusterTest = PradarService.PRADAR_CLUSTER_TEST_HTTP_USER_AGENT_SUFFIX.equals(uaValue)
                || "1".equals(clusterTestValue)
                || Boolean.TRUE.toString().equals(clusterTestValue);
        // 如果压测开关关闭，并且又是压测流量则直接抛异常
        if (!PradarService.isClusterTestEnabled() && isClusterTest) {
            throw new ClusterTestNotReadyException("【LinkAgent】The cluster test is not ready，and the cluster test request is not allowed to enter");
        }
    }

    /**
     * 根据key查询对应的value
     *
     * @param key    key
     * @param advice advice
     * @return value
     */
    public abstract Object getParam(Advice advice, String key);
}
