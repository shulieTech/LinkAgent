package com.pamirs.attach.plugin.cus.trace.interceptor;

import com.pamirs.attach.plugin.cus.trace.module.CusTraceConfig;
import com.pamirs.attach.plugin.cus.trace.module.CusTraceConstans;
import com.pamirs.pradar.MiddlewareType;
import com.pamirs.pradar.Pradar;
import com.pamirs.pradar.gson.GsonFactory;
import com.pamirs.pradar.interceptor.SpanRecord;
import com.pamirs.pradar.interceptor.TraceInterceptorAdaptor;
import com.pamirs.pradar.utils.ObjectMatchers;
import com.shulie.instrument.simulator.api.listener.ext.Advice;
import com.shulie.instrument.simulator.api.util.StringUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author Licey
 * @date 2023/1/3
 */
public class CusTraceInterceptor extends TraceInterceptorAdaptor {
    private static final Logger logger = LoggerFactory.getLogger(CusTraceInterceptor.class);
    private ObjectMatchers.ObjectMatcher objectMatcher;

    private CusTraceConfig config = null;


    public CusTraceInterceptor(Object config) {
        if (config instanceof CusTraceConfig) {
            this.config = (CusTraceConfig) config;
        }
    }

    @Override
    protected boolean isClient(Advice advice) {
        return resetClient(advice);
    }

    @Override
    public String getPluginName() {
        return CusTraceConstans.MIDDLEWARE_NAME;
    }

    @Override
    public int getPluginType() {
        return MiddlewareType.TYPE_CUSTOMER;
    }

    @Override
    public SpanRecord beforeTrace(Advice advice) {
        if (config == null) {
            return null;
        }
        SpanRecord record = new SpanRecord();
        if (config.isRequestOn()) {
            record.setRequest(advice.getParameterArray());
        }
        record.setService(config.getClassName());
        record.setMethod(config.getMethodName());
        record.setMiddlewareName(CusTraceConstans.MIDDLEWARE_NAME);
        resetContext(advice, record);
        return record;
    }


    @Override
    public SpanRecord afterTrace(Advice advice) {
        if (config == null) {
            return null;
        }
        SpanRecord record = new SpanRecord();
        if (config.isResponseOn()) {
            record.setResponse(GsonFactory.getGson().toJson(advice.getReturnObj()));
        }
        return record;
    }

    @Override
    public SpanRecord exceptionTrace(Advice advice) {
        if (config == null) {
            return null;
        }
        SpanRecord record = new SpanRecord();
        record.setResultCode("-1");
        record.setResponse(advice.getThrowable().getMessage());
        return record;
    }

    /**
     * 重置上下文
     * 如果配置为重置上下文则将ThreadLocal清除，让框架重新生成
     * 主要解决部分参数解析导致中间件入口需要生成的情况
     *
     * @param advice
     * @return
     */
    private void resetContext(Advice advice, SpanRecord spanRecord) {
        if (isResetContext(advice)) {
            Pradar.clearInvokeContext();
//            spanRecord.setSpanKind(SpanKindEnum.SPAN_KIND_SERVER);
        }
    }

    /**
     * 判断是否需要重置上下文
     * 有两个条件，一个是配置为重置上下文并且表达式为空，另外一个条件是配置为重置上下文并且表达式执行检查成功
     *
     * @param advice
     * @return
     */
    private boolean isResetContext(Advice advice) {
        if (config == null) {
            return false;
        }

        if (config.isResetContext()) {

            if (advice.attachment() != null && advice.attachment() instanceof Boolean) {
                return advice.attachment();
            }

            if (StringUtil.isEmpty(config.getResetContextExpression())) {
                advice.attach(Boolean.TRUE);
                return true;
            }
            String expression = config.getResetContextExpression();
            if (null == objectMatcher) {
                objectMatcher = ObjectMatchers.getMatcher("reflect", "ok", expression);
            }
            String matchResult = objectMatcher.matcher(advice.getParameterArray(), "failed");
            if ("ok".equals(matchResult)) {
                advice.attach(Boolean.TRUE);
                return true;
            }
        }
        return false;
    }

    private boolean resetClient(Advice advice) {
        if (isResetContext(advice)) {
            return false;
        }
        return true;
    }
}
