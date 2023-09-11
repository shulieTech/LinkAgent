/*
 * *
 *  * Copyright 2021 Shulie Technology, Co.Ltd
 *  * Email: shulie@shulie.io
 *  * Licensed under the Apache License, Version 2.0 (the "License");
 *  * you may not use this file except in compliance with the License.
 *  * You may obtain a copy of the License at
 *  *
 *  * http://www.apache.org/licenses/LICENSE-2.0
 *  *
 *  * Unless required by applicable law or agreed to in writing, software
 *  * distributed under the License is distributed on an "AS IS" BASIS,
 *  * See the License for the specific language governing permissions and
 *  * limitations under the License.
 *
 */

package com.pamirs.attach.plugin.okhttp.utils;

import com.pamirs.pradar.Pradar;
import com.pamirs.pradar.exception.PressureMeasureError;
import com.pamirs.pradar.internal.adapter.ExecutionStrategy;
import com.pamirs.pradar.internal.config.MatchConfig;
import com.pamirs.pradar.pressurement.mock.JsonMockStrategy;
import com.shulie.instrument.simulator.api.ProcessControlException;
import com.shulie.instrument.simulator.api.ProcessController;
import okhttp3.Call;
import okhttp3.Protocol;
import okhttp3.Response;

/**
 * @author angju
 * @date 2022/2/22 15:22
 */
public class MockReturnUtils {

    public static ExecutionStrategy fixJsonStrategy =
            new JsonMockStrategy() {
                @Override
                public Object processBlock(Class returnType, ClassLoader classLoader, Object params) throws ProcessControlException {
                    if (params instanceof MatchConfig) {
                        try {
                            MatchConfig config = (MatchConfig) params;
                            String scriptContent = config.getScriptContent().trim();
                            final Call call = (Call) config.getArgs().get("call");
                            Pradar.mockResponse(scriptContent);

                            Response response = new Response.Builder().code(200)
                                    .body(RealResponseBodyUtil.buildResponseBody(config.getScriptContent()))
                                    .request(call.request())
                                    .protocol(Protocol.HTTP_1_0)
                                    .message("OK")
                                    .build();
                            ProcessController.returnImmediately(returnType, response);
                        } catch (ProcessControlException pe) {
                            throw pe;
                        } catch (Throwable t) {
                            throw new PressureMeasureError(t);
                        }
                    }
                    return null;
                }
            };
}
