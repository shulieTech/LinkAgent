package com.pamirs.attach.plugin.rabbitmq.interceptor;

import com.pamirs.attach.plugin.rabbitmq.RabbitmqConstants;
import com.pamirs.attach.plugin.rabbitmq.destroy.RabbitmqDestroy;
import com.pamirs.pradar.Pradar;
import com.pamirs.pradar.interceptor.TraceInterceptorAdaptor;
import com.pamirs.pradar.pressurement.ClusterTestUtils;
import com.shulie.instrument.simulator.api.annotation.Destroyable;
import com.shulie.instrument.simulator.api.listener.ext.Advice;
import org.apache.commons.lang.StringUtils;

/**
 * @author jiangjibo
 * @date 2021/10/14 11:19 上午
 * @description: TODO
 */
@Destroyable(RabbitmqDestroy.class)
public class ChannelNQueueDeclareInterceptor extends TraceInterceptorAdaptor {

    @Override
    public void beforeFirst(Advice advice) throws Exception {
        ClusterTestUtils.validateClusterTest();
        if (!Pradar.isClusterTest()) {
            return;
        }
        Object[] args = advice.getParameterArray();
        String queue = (String) args[0];
        if (StringUtils.isNotBlank(queue) && !Pradar.isClusterTestPrefix(queue)) {
            args[0] = Pradar.addClusterTestPrefix(queue);
        }
    }

    @Override
    public String getPluginName() {
        return RabbitmqConstants.PLUGIN_NAME;
    }

    @Override
    public int getPluginType() {
        return RabbitmqConstants.PLUGIN_TYPE;
    }

}
