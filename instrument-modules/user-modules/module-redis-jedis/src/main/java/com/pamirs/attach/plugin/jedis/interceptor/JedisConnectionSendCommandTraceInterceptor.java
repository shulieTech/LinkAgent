package com.pamirs.attach.plugin.jedis.interceptor;

import com.pamirs.attach.plugin.dynamic.Attachment;
import com.pamirs.attach.plugin.dynamic.reflect.ReflectionUtils;
import com.pamirs.attach.plugin.dynamic.template.RedisTemplate;
import com.pamirs.attach.plugin.jedis.RedisConstants;
import com.pamirs.attach.plugin.jedis.destroy.JedisDestroyed;
import com.pamirs.attach.plugin.jedis.util.RedisUtils;
import com.pamirs.pradar.Pradar;
import com.pamirs.pradar.ResultCode;
import com.pamirs.pradar.interceptor.SpanRecord;
import com.pamirs.pradar.interceptor.TraceInterceptorAdaptor;
import com.shulie.instrument.simulator.api.annotation.Destroyable;
import com.shulie.instrument.simulator.api.annotation.ListenerBehavior;
import com.shulie.instrument.simulator.api.listener.ext.Advice;
import redis.clients.jedis.Client;
import redis.clients.jedis.Protocol;

import java.util.Set;

/**
 * @author jiangjibo
 * @date 2022/3/16 10:21 上午
 * @description:
 */
@Destroyable(JedisDestroyed.class)
@ListenerBehavior(isFilterBusinessData = true)
public class JedisConnectionSendCommandTraceInterceptor extends TraceInterceptorAdaptor {

    private static final Set<String> METHOD_KEYS = RedisUtils.get().keySet();

    @Override
    public String getPluginName() {
        return RedisConstants.PLUGIN_NAME;
    }

    @Override
    public int getPluginType() {
        return RedisConstants.PLUGIN_TYPE;
    }

    @Override
    public SpanRecord beforeTrace(Advice advice) {

        if(JedisInterceptor.interceptorApplied.get()){
            return null;
        }

        Object[] args = advice.getParameterArray();
        Client client = (Client) advice.getTarget();
        String method = ((Protocol.Command) advice.getParameterArray()[0]).name().toLowerCase();
        if (!METHOD_KEYS.contains(method)) {
            return null;
        }
        SpanRecord record = new SpanRecord();
        record.setService(method);
        record.setMethod(method);
        record.setRequestSize(0);
        record.setRemoteIp(client.getHost());
        record.setPort(client.getPort());
        record.setRequest(toArgs(args));
        record.setMiddlewareName(RedisConstants.MIDDLEWARE_NAME);
        return record;
    }

    @Override
    public SpanRecord afterTrace(Advice advice) {
        if(JedisInterceptor.interceptorApplied.get()){
            return null;
        }
        Protocol.Command command = (Protocol.Command) advice.getParameterArray()[0];
        String method = command.name().toLowerCase();
        if (!METHOD_KEYS.contains(method)) {
            return null;
        }
        SpanRecord record = new SpanRecord();
        Client client = (Client) advice.getTarget();
        // 写返回值需要拷贝流, 对性能有影响
//        record.setResponse(client.getOne());
        record.setResultCode(ResultCode.INVOKE_RESULT_SUCCESS);
        record.setMiddlewareName(RedisConstants.MIDDLEWARE_NAME);
        attachment(advice);
        return record;
    }

    @Override
    public SpanRecord exceptionTrace(Advice advice) {
        if(JedisInterceptor.interceptorApplied.get()){
            return null;
        }
        String method = ((Protocol.Command) advice.getParameterArray()[0]).name().toLowerCase();
        if (!METHOD_KEYS.contains(method)) {
            return null;
        }
        SpanRecord record = new SpanRecord();
        record.setResponse(advice.getThrowable());
        record.setResultCode(ResultCode.INVOKE_RESULT_FAILED);
        record.setMiddlewareName(RedisConstants.MIDDLEWARE_NAME);
        return record;
    }

    private Object[] toArgs(Object[] args) {
        if (args.length == 1) {
            return null;
        }
        Object[] ret = new Object[args.length - 1];
        for (int i = 0; i < args.length - 1; i++) {
            Object arg = args[i + 1];
            if (arg instanceof String) {
                ret[i] = arg;
            } else if (arg instanceof byte[]) {
                ret[i] = new String((byte[]) arg);
            } else if (arg instanceof char[]) {
                ret[i] = new String((char[]) arg);
            } else {
                ret[i] = arg;
            }
        }
        return ret;
    }

    void attachment(Advice advice) {
        if (Pradar.isClusterTest()) {
            return;
        }
        Client client = (Client) advice.getTarget();
        //单机模式
        String node = client.getHost().concat(":").concat(String.valueOf(client.getPort()));
        String password = ReflectionUtils.get(client,"password");
        int db = Integer.parseInt(String.valueOf(ReflectionUtils.get(client,"db")));
        Attachment ext = new Attachment(node, RedisConstants.PLUGIN_NAME,
                new String[]{RedisConstants.MIDDLEWARE_NAME}
                , new RedisTemplate.JedisSingleTemplate()
                .setNodes(node)
                .setPassword(password)
                .setDatabase(db)
        );
        Pradar.getInvokeContext().setExt(ext);
    }

}
