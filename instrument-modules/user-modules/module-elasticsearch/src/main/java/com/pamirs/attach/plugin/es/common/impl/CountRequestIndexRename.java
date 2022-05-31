package com.pamirs.attach.plugin.es.common.impl;

import com.pamirs.pradar.Pradar;
import com.pamirs.pradar.pressurement.agent.shared.service.GlobalConfig;
import com.shulie.instrument.simulator.api.reflect.Reflect;
import org.apache.commons.lang.ArrayUtils;
import org.elasticsearch.client.core.CountRequest;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public class CountRequestIndexRename extends AbstractReadRequestIndexRename{
    @Override
    public String getName() {
        return "count";
    }

    @Override
    public List<String> reindex0(Object target) {
        CountRequest request = (CountRequest) target;
        String[] indices = request.indices();
        if (ArrayUtils.isEmpty(indices)) {
            return Collections.EMPTY_LIST;
        }
        for (int i = 0, len = indices.length; i < len; i++) {
            /**
             * 如果索引在白名单中，则不需要走
             */
            if (GlobalConfig.getInstance().getSearchWhiteList().contains(indices[i])) {
                continue;
            }
            if (!Pradar.isClusterTestPrefix(indices[i])) {
                indices[i] = Pradar.addClusterTestPrefixLower(indices[i]);
            }
        }

        Reflect.on(request).set("indices", indices);
        return Arrays.asList(indices);
    }

    @Override
    public List<String> getIndex0(Object target) {
        CountRequest req = (CountRequest) target;
        return (req.indices() == null || req.indices().length == 0) ? Collections.EMPTY_LIST : Arrays.asList(req.indices());
    }
}
