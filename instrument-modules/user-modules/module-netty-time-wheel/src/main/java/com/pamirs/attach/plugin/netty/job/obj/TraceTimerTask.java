package com.pamirs.attach.plugin.netty.job.obj;

import java.util.Map;

import com.pamirs.attach.plugin.netty.NettyTimeWheelConstants;
import com.pamirs.pradar.Pradar;
import com.pamirs.pradar.PradarService;
import io.netty.util.Timeout;
import io.netty.util.TimerTask;

/**
 * @author angju
 * @date 2021/6/11 15:39
 */
public class TraceTimerTask implements TimerTask {

    /**
     * 代理业务对象
     */
    private TimerTask busTimerTask;
    private Map<String, String> rpcContext;

    @Override
    public void run(Timeout timeout) throws Exception {
        String invokeId = rpcContext.get(PradarService.PRADAR_INVOKE_ID_KEY);
        Pradar.startTrace(rpcContext.get(PradarService.PRADAR_TRACE_ID_KEY), invokeId + ".1",
            busTimerTask.getClass().getName(), NettyTimeWheelConstants.PLUGIN_NAME, NettyTimeWheelConstants.PLUGIN_NAME);
        Pradar.setClusterTest("1".equals(rpcContext.get(PradarService.PRADAR_CLUSTER_TEST_KEY)));
        try {
            busTimerTask.run(timeout);
        } finally {
            Pradar.endTrace("200", NettyTimeWheelConstants.PLUGIN_TYPE);//类型待确认
            Pradar.setClusterTest(false);
            rpcContext = null;
        }
    }

    public TraceTimerTask(TimerTask BUSTimerTask, Map<String, String> rpcContext) {
        this.busTimerTask = BUSTimerTask;
        this.rpcContext = rpcContext;
    }

    public void setBusTimerTask(TimerTask busTimerTask) {
        this.busTimerTask = busTimerTask;
    }

    public void setRpcContext(Map<String, String> rpcContext) {
        this.rpcContext = rpcContext;
    }
}

