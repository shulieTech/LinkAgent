package com.pamirs.attach.plugin.lettuce.interceptor;

import com.pamirs.attach.plugin.lettuce.LettuceConstants;
import com.pamirs.attach.plugin.lettuce.destroy.LettuceDestroy;
import com.pamirs.pradar.InvokeContext;
import com.pamirs.pradar.Pradar;
import com.pamirs.pradar.ResultCode;
import com.pamirs.pradar.interceptor.AroundInterceptor;
import com.shulie.instrument.simulator.api.annotation.Destroyable;
import com.shulie.instrument.simulator.api.listener.ext.Advice;
import com.shulie.instrument.simulator.api.resource.DynamicFieldManager;
import io.lettuce.core.output.CommandOutput;
import io.lettuce.core.protocol.CommandWrapper;
import io.lettuce.core.protocol.RedisCommand;

import javax.annotation.Resource;
import java.util.Map;

/**
 * @author angju
 * @date 2022/10/19 17:13
 */
@Destroyable(LettuceDestroy.class)
public class RedisCommandResultInterceptor extends AroundInterceptor {


    @Resource
    protected DynamicFieldManager manager;

    @Override
    public void doBefore(Advice advice) {
        Object redisCommand = advice.getTarget();
        while (redisCommand instanceof CommandWrapper) {
            redisCommand = ((CommandWrapper<?, ?, ?>)redisCommand).getDelegate();
        }
        CommandOutput<?, ?, ?> output = ((RedisCommand<?, ?, ?>) redisCommand).getOutput();
        Object o = output == null ? null : output.get();


        if (!manager.hasDynamicField(redisCommand, LettuceConstants.INVOKE_CONTENT)) {
            return;
        }
        Map<String, String> context = manager.getDynamicField(redisCommand, LettuceConstants.INVOKE_CONTENT);



        if (context != null) {
            try {
                Pradar.setInvokeContext(context);
                InvokeContext invokeContext = Pradar.getInvokeContext();
                invokeContext.setResponse(o);
                String method = advice.getBehaviorName();
                if ("complete".equals(method)) {
                    Pradar.endClientInvoke(ResultCode.INVOKE_RESULT_SUCCESS, Pradar.LOG_TYPE_INVOKE_CLIENT);
                } else if ("completeExceptionally".equals(method)) {
                    invokeContext.setResponse(advice.getParameterArray());
                    Pradar.endClientInvoke(ResultCode.INVOKE_RESULT_FAILED, Pradar.LOG_TYPE_INVOKE_CLIENT);
                } else {
                    //cancel
                    Pradar.endClientInvoke(ResultCode.INVOKE_RESULT_SUCCESS, Pradar.LOG_TYPE_INVOKE_CLIENT);
                }
            } finally {
                //记得清理
                manager.removeField(redisCommand, LettuceConstants.INVOKE_CONTENT);
            }
        }
    }



    @Override
    public void doAfter(Advice advice) throws Throwable {
        if (!Pradar.hasInvokeContext()) {
            return;
        }
        Pradar.popInvokeContext();
    }

    @Override
    public void doException(Advice advice) throws Throwable {
        Object command = advice.getTarget();
        while (command instanceof CommandWrapper) {
            command = ((CommandWrapper<?, ?, ?>)command).getDelegate();
        }
        if(!(command instanceof RedisCommand)){
            return;
        }
        if (manager.hasDynamicField(command, LettuceConstants.INVOKE_CONTENT)) {
            manager.removeField(command, LettuceConstants.INVOKE_CONTENT);
        }
        if (!Pradar.hasInvokeContext()) {
            return;
        }
        Pradar.popInvokeContext();
    }
}
