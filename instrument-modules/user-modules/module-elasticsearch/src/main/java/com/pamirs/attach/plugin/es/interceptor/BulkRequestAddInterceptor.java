package com.pamirs.attach.plugin.es.interceptor;

import com.pamirs.attach.plugin.es.common.RequestIndexRename;
import com.pamirs.attach.plugin.es.common.RequestIndexRenameProvider;
import com.pamirs.attach.plugin.es.destroy.ElasticSearchDestroy;
import com.pamirs.pradar.MiddlewareType;
import com.pamirs.pradar.Pradar;
import com.pamirs.pradar.exception.PressureMeasureError;
import com.pamirs.pradar.interceptor.TraceInterceptorAdaptor;
import com.pamirs.pradar.pressurement.ClusterTestUtils;
import com.shulie.instrument.simulator.api.annotation.Destroyable;
import com.shulie.instrument.simulator.api.listener.ext.Advice;

/**
 * @author jiangjibo
 * @date 2022/3/8 5:35 下午
 * @description:
 */
@Destroyable(ElasticSearchDestroy.class)
public class BulkRequestAddInterceptor extends TraceInterceptorAdaptor {

    @Override
    public void beforeFirst(Advice advice) {
        ClusterTestUtils.validateClusterTest();
        if (!Pradar.isClusterTest()) {
            return;
        }
        Object[] args = advice.getParameterArray();

        RequestIndexRename requestIndexRename = RequestIndexRenameProvider.get(args[0]);
        if (requestIndexRename == null) {
            throw new PressureMeasureError("elasticsearch " + args[0].getClass().getName() + " is not supported!");
        }

        if (requestIndexRename.supportedDirectReindex(args[0])) {
            requestIndexRename.reindex(args[0]);
        } else {
            Object index = requestIndexRename.indirectIndex(args[0]);
            advice.changeParameter(0, index);
        }
    }

    @Override
    public String getPluginName() {
        return "es";
    }

    @Override
    public int getPluginType() {
        return MiddlewareType.TYPE_SEARCH;
    }
}
