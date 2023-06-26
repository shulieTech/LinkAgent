package com.pamirs.attach.plugin.okhttp.utils;

import com.pamirs.attach.plugin.dynamic.reflect.ReflectionUtils;
import okhttp3.Headers;
import okhttp3.internal.http.RealResponseBody;
import okio.Buffer;

import java.io.UnsupportedEncodingException;
import java.lang.reflect.Constructor;

public class RealResponseBodyUtil {

    private static Constructor realResponseBodyConstructor;

    public static RealResponseBody buildResponseBody(String content) throws UnsupportedEncodingException {
        if (realResponseBodyConstructor == null) {
            realResponseBodyConstructor = RealResponseBody.class.getDeclaredConstructors()[0];
        }

        Buffer buffer = new Buffer();
        buffer.write(content.getBytes("UTF-8"));

        Class firstType = realResponseBodyConstructor.getParameterTypes()[0];

        RealResponseBody responseBody;
        if (firstType == String.class) {
            responseBody = ReflectionUtils.newInstance(RealResponseBody.class, "UTF-8", (long)content.length(), buffer);
        } else {
            responseBody = ReflectionUtils.newInstance(RealResponseBody.class, new Headers.Builder().build(), buffer);
        }

        return responseBody;
    }
}
