/*
 * Copyright 2021 Shulie Technology, Co.Ltd
 * Email: shulie@shulie.io
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.pamirs.attach.plugin.httpclient.utils;

import org.apache.http.HttpEntity;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.ResponseHandler;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.util.EntityUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;

/**
 * @Description
 * @Author ocean_wll
 * @Date 2022/7/25 11:14 上午
 */
public class ResponseHandlerUtil {

    private static final Logger logger = LoggerFactory.getLogger(ResponseHandlerUtil.class);

    /**
     * 处理response对象
     *
     * @param responseHandler ResponseHandler
     * @param response        response
     * @return 处理后的值
     */
    public static Object handleResponse(ResponseHandler responseHandler, CloseableHttpResponse response) throws IOException {
        Object result = null;
        try {
            result = responseHandler.handleResponse(response);
            final HttpEntity entity = response.getEntity();
            EntityUtils.consume(entity);
            return result;
        } catch (final ClientProtocolException t) {
            // Try to salvage the underlying connection in case of a protocol exception
            final HttpEntity entity = response.getEntity();
            try {
                EntityUtils.consume(entity);
            } catch (final Exception t2) {
                // Log this exception. The original exception is more
                // important and will be thrown to the caller.
                logger.warn("Error consuming content after an exception.", t2);
            }
        } finally {
            response.close();
        }
        return result;
    }
}
