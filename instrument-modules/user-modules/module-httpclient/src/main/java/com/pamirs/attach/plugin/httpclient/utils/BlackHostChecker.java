package com.pamirs.attach.plugin.httpclient.utils;

import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * @author jirenhe | jirenhe@shulie.io
 * @since 2022/05/16 2:59 PM
 */
public class BlackHostChecker {

    public final static String BLACK_HOST_MARK = "-1";

    private final static Set<String> BLACK_HOST = new HashSet<String>();

    private final static Map<String, Boolean> URL_CHECK_MAP = new HashMap<String, Boolean>();

    static {
        String blackHostConfig = System.getProperty("pradar.black.host");
        if (blackHostConfig != null) {
            BLACK_HOST.addAll(Arrays.asList(blackHostConfig.split(",")));
        }
    }

    public static boolean isBlackHost(String url) {
        if (url == null || "".equals(url)) {
            return false;
        }
        final int indexOfQuestion = url.indexOf('?');
        if (indexOfQuestion != -1) {
            url = url.substring(0, indexOfQuestion);
        }
        Boolean result = URL_CHECK_MAP.get(url);
        if (result == null) {
            synchronized (BlackHostChecker.class) {
                result = URL_CHECK_MAP.get(url);
                if (result == null) {
                    result = check(url);
                    URL_CHECK_MAP.put(url, result);
                }
            }
        }
        if (URL_CHECK_MAP.size() > 500) {
            synchronized (BlackHostChecker.class) {
                URL_CHECK_MAP.clear();
            }
        }
        return result;
    }

    private static Boolean check(String url) {
        if (url.startsWith("http://")) {
            url = url.substring(7);
        } else if (url.startsWith("https://")) {
            url = url.substring(8);
        }
        int index = url.indexOf("/");
        if (index != -1) {
            url = url.substring(0, index);
        }
        return BLACK_HOST.contains(url);
    }
}
