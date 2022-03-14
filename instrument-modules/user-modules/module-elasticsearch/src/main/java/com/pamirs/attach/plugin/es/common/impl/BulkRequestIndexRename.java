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
package com.pamirs.attach.plugin.es.common.impl;

import com.pamirs.attach.plugin.es.common.RequestIndexRename;
import com.pamirs.attach.plugin.es.common.RequestIndexRenameProvider;
import org.elasticsearch.action.bulk.BulkRequest;

import java.util.ArrayList;
import java.util.List;

/**
 * @Description
 * @Author xiaobin.zfb
 * @mail xiaobin@shulie.io
 * @Date 2020/7/13 6:07 下午
 */
public class BulkRequestIndexRename extends AbstractReadRequestIndexRename {

    private static boolean isLowVersion;

    static {
        try {
            Class.forName("org.elasticsearch.action.ActionRequest");
            isLowVersion = true;
        } catch (ClassNotFoundException e) {
            isLowVersion = false;
        }
    }

    @Override
    public String getName() {
        return "bulk";
    }

    @Override
    public boolean supportedDirectReindex(Object target) {
        return false;
    }

    @Override
    public Object indirectIndex(Object target) {
        return target;
    }

    @Override
    public List<String> reindex0(Object target) {
        return null;
    }

    @Override
    public List<String> getIndex0(Object target) {
        BulkRequest req = (BulkRequest)target;
        List reqs = req.requests();
        List<String> list = new ArrayList<String>();
        for (Object r : reqs) {
            if (r == null) {
                continue;
            }
            RequestIndexRename requestIndexRename = RequestIndexRenameProvider.get(r);
            if (requestIndexRename != null) {
                list.addAll(requestIndexRename.getIndex(r));
            }
        }
        return list;
    }
}
