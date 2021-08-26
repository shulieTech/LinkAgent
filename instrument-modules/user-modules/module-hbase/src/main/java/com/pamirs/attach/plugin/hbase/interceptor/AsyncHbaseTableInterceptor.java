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
package com.pamirs.attach.plugin.hbase.interceptor;

import com.pamirs.attach.plugin.hbase.destroy.HbaseDestroyed;
import com.pamirs.pradar.Pradar;
import com.pamirs.pradar.interceptor.ParametersWrapperInterceptorAdaptor;
import com.pamirs.pradar.pressurement.ClusterTestUtils;
import com.shulie.instrument.simulator.api.annotation.Destroyable;
import com.shulie.instrument.simulator.api.listener.ext.Advice;
import org.hbase.async.HBaseRpc;
import org.hbase.async.Scanner;


/**
 * @Auther: vernon
 * @Date: 2020/7/26 10:16
 * @Description:
 */
@Destroyable(HbaseDestroyed.class)
public class AsyncHbaseTableInterceptor extends ParametersWrapperInterceptorAdaptor {

    @Override
    public Object[] getParameter0(Advice advice) throws Throwable {
        ClusterTestUtils.validateClusterTest();
        if (!Pradar.isClusterTest()) {
            return advice.getParameterArray();
        }
        if (advice.getTarget() instanceof HBaseRpc) {

            if (advice.getParameterArray()[0] instanceof byte[]) {

                byte[] bytes = (byte[]) advice.getParameterArray()[0];
                if ((bytes.length == 1 && bytes[0] == 0)) {
                    return advice.getParameterArray();
                }
                String table = new String(bytes);
                if (table.startsWith("hbase")) {
                    return advice.getParameterArray();
                }
                if (!table.startsWith(Pradar.CLUSTER_TEST_PREFIX)) {
                    table = Pradar.CLUSTER_TEST_PREFIX + table;
                    advice.getParameterArray()[0] = table.getBytes();
                }
                return advice.getParameterArray();
            }
        } else if (advice.getTarget() instanceof Scanner) {
            byte[] bytes = (byte[]) advice.getParameterArray()[1];
            if (bytes.length == 1 && bytes[0] == 0) {
                return advice.getParameterArray();
            }
            String table = new String(bytes);
            if (table.startsWith("hbase")) {
                return advice.getParameterArray();
            }
            if (!table.startsWith(Pradar.CLUSTER_TEST_PREFIX)) {
                table = Pradar.CLUSTER_TEST_PREFIX + table;
                advice.getParameterArray()[1] = table.getBytes();
            }

            return advice.getParameterArray();

        }

        return advice.getParameterArray();
    }
}
