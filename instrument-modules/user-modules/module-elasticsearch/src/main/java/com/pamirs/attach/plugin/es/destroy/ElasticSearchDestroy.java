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
package com.pamirs.attach.plugin.es.destroy;

import com.pamirs.attach.plugin.es.common.ElasticSearchParser;
import com.pamirs.attach.plugin.es.common.RequestIndexRenameProvider;
import com.pamirs.attach.plugin.es.shadowserver.ShadowEsClientHolder;
import com.pamirs.attach.plugin.es.shadowserver.rest.RestClientDefinitionStrategy;
import com.pamirs.attach.plugin.es.shadowserver.transport.TransportClientDefinitionStrategy;
import com.shulie.instrument.simulator.api.listener.Destroyed;

/**
 * @author xiaobin.zfb|xiaobin@shulie.io
 * @since 2021/6/16 2:11 下午
 */
public class ElasticSearchDestroy implements Destroyed {
    @Override
    public void destroy() {
        ElasticSearchParser.release();
        RequestIndexRenameProvider.release();
        ShadowEsClientHolder.release();
        RestClientDefinitionStrategy.release();
        TransportClientDefinitionStrategy.release();
    }
}
