package com.pamirs.attach.plugin.lettuce.interceptor;

import com.pamirs.attach.plugin.dynamic.reflect.ReflectionUtils;
import com.pamirs.attach.plugin.lettuce.LettuceConstants;
import com.pamirs.attach.plugin.lettuce.destroy.LettuceDestroy;
import com.pamirs.attach.plugin.lettuce.utils.ParameterUtils;
import com.pamirs.pradar.Pradar;
import com.pamirs.pradar.ResultCode;
import com.pamirs.pradar.interceptor.SpanRecord;
import com.shulie.instrument.simulator.api.annotation.Destroyable;
import com.shulie.instrument.simulator.api.listener.ext.Advice;
import io.lettuce.core.protocol.CommandArgs;
import io.lettuce.core.protocol.ProtocolKeyword;

import java.lang.reflect.Field;
import java.nio.charset.CharacterCodingException;
import java.nio.charset.Charset;
import java.util.List;

/**
 * @author jiangjibo
 * @date 2022/3/18 4:38 下午
 * @description:
 */
@Destroyable(LettuceDestroy.class)
public class LettuceCommandDispatchTraceInterceptor extends LettuceMethodInterceptor {

    @Override
    public SpanRecord beforeTrace(Advice advice) {
        if (LettuceMethodInterceptor.interceptorApplied.get()) {
            return null;
        }
        Object[] args = advice.getParameterArray();
        Object target = advice.getTarget();
        SpanRecord spanRecord = new SpanRecord();
        appendEndPoint(target, spanRecord);
        spanRecord.setService(((ProtocolKeyword)args[0]).name());
        spanRecord.setMethod(getMethodNameExt(args));
        spanRecord.setRequest(toArgs(args));
        spanRecord.setMiddlewareName(LettuceConstants.MIDDLEWARE_NAME);
        return spanRecord;
    }

    @Override
    public SpanRecord afterTrace(Advice advice) {
        if (LettuceMethodInterceptor.interceptorApplied.get()) {
            return null;
        }
        SpanRecord spanRecord = new SpanRecord();
        spanRecord.setMiddlewareName(LettuceConstants.MIDDLEWARE_NAME);
        spanRecord.setCallbackMsg(LettuceConstants.PLUGIN_NAME);
        /**
         * 附加属性
         */
        ext();
        return spanRecord;
    }

    @Override
    public SpanRecord exceptionTrace(Advice advice) {
        if (LettuceMethodInterceptor.interceptorApplied.get()) {
            return null;
        }
        SpanRecord spanRecord = new SpanRecord();
        spanRecord.setResponse(advice.getThrowable());
        spanRecord.setResultCode(ResultCode.INVOKE_RESULT_FAILED);
        spanRecord.setMiddlewareName(LettuceConstants.MIDDLEWARE_NAME);
        spanRecord.setCallbackMsg(LettuceConstants.PLUGIN_NAME);
        /**
         * 附加属性
         */
        ext();
        return spanRecord;
    }

    private static Field keyField;

    private Object[] toArgs(Object[] args) {
        List singularArguments = ReflectionUtils.get(args[2], "singularArguments");
        Object[] ret = new Object[singularArguments.size()];
        try {
            if (keyField == null) {
                keyField = singularArguments.get(0).getClass().getDeclaredField("key");
                if (keyField == null) {
                    return new Object[]{((CommandArgs<byte[], byte[]>) args[2]).toCommandString()};
                }
                keyField.setAccessible(true);
            }
            for (int i = 0; i < singularArguments.size(); i++) {
                String v = new String((byte[]) keyField.get(singularArguments.get(i)));
                // 第一个参数是key
                if(i == 0){
                    if(Pradar.isClusterTest() && !Pradar.isClusterTestPrefix(v)){
                        v = Pradar.addClusterTestPrefix(v);
                    }
                }
                ret[i] = v;
            }
            return ret;
        } catch (NoSuchFieldException e) {
            logger.error("redis_lettuce_002: CommandArgs.KeyArgument not exists field :key", e);
        } catch (IllegalAccessException e) {

        }
        return new Object[]{((CommandArgs<byte[], byte[]>) args[2]).toCommandString()};
    }

    public static String getMethodNameExt(Object... args) {
        if (args == null || args.length == 0) {
            return "";
        }
        try {
            String key = ParameterUtils.toString(200, Charset.forName("UTF-8").newDecoder().decode(((CommandArgs<byte[], byte[]>) args[2]).getFirstEncodedKey()));
            if(Pradar.isClusterTest() && !Pradar.isClusterTestPrefix(key)){
                key = Pradar.addClusterTestPrefix(key);
            }
            return key;
        } catch (CharacterCodingException e) {
            logger.error("redis_lettuce_003: decode args falied", e);
            return ParameterUtils.toString(200, args[2]);
        }
    }
}
