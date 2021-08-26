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
package com.pamirs.pradar.pressurement.agent.shared.exit;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.Iterator;
import java.util.Set;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;

import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import com.pamirs.pradar.PradarSwitcher;
import com.pamirs.pradar.internal.config.MatchConfig;
import com.pamirs.pradar.pressurement.agent.shared.custominterfacebase.Exit;
import com.pamirs.pradar.pressurement.agent.shared.service.GlobalConfig;
import com.pamirs.pradar.pressurement.mock.WhiteListStrategy;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * ArbiterHttpExit
 *
 * @author 311183
 */
public class ArbiterHttpExit implements Exit {

    private static final Logger LOGGER = LoggerFactory.getLogger(ArbiterHttpExit.class);

    private static LoadingCache<String, MatchConfig> patternCache = CacheBuilder.newBuilder()
            .maximumSize(300).expireAfterAccess(5 * 60, TimeUnit.SECONDS).build(
                    new CacheLoader<String, MatchConfig>() {
                        @Override
                        public MatchConfig load(String name) throws Exception {
                            Set<MatchConfig> urlWhiteList = GlobalConfig.getInstance().getUrlWhiteList();
                            if (urlWhiteList != null) {
                                for (MatchConfig matchConfig : urlWhiteList) {
                                    MatchConfig matching = matching(name, matchConfig);
                                    if (null != matching && matching.isSuccess()) {
                                        return matching;
                                    }
                                }
                            }
                            return failure();
                        }
                    }
            );

    private static LoadingCache<String, MatchConfig> httpMatchResult = CacheBuilder.newBuilder()
            .maximumSize(300).expireAfterAccess(5 * 60, TimeUnit.SECONDS).build(
                    new CacheLoader<String, MatchConfig>() {
                        @Override
                        public MatchConfig load(String url) throws Exception {
                            return shallWePassHttpStringCache(url);
                        }
                    }
            );

    private static LoadingCache<String, MatchConfig> rpcMatchResult = CacheBuilder.newBuilder()
            .maximumSize(100).expireAfterAccess(5 * 60, TimeUnit.SECONDS).build(
                    new CacheLoader<String, MatchConfig>() {
                        @Override
                        public MatchConfig load(String name) throws Exception {
                            return getRpcPassedCache(name);
                        }
                    }
            );

    public static void release() {
        patternCache.invalidateAll();
        httpMatchResult.invalidateAll();
        rpcMatchResult.invalidateAll();
    }

    public static void clearRpcMatch() {
        rpcMatchResult.invalidateAll();
    }

    public static void clearHttpMatch() {
        httpMatchResult.invalidateAll();
        patternCache.invalidateAll();
    }

    public static MatchConfig failure() {
        return MatchConfig.failure(new WhiteListStrategy());
    }

    /**
     * 判断是否可以通过此 rpc 调用
     * 只支持单独类名
     *
     * @param className 类名
     */
    public static MatchConfig shallWePassRpc(String className) {
        if (!PradarSwitcher.whiteListSwitchOn()) {
            return MatchConfig.success(new WhiteListStrategy());
        }
        return rpcMatchResult.getUnchecked(className);
    }

    /**
     * 判断是否可以通过此 rpc 调用
     * 支持 类名+方法名 的组合方式
     *
     * @param className  类名
     * @param methodName 方法名
     */
    public static MatchConfig shallWePassRpc(String className, String methodName) {
        if (!PradarSwitcher.whiteListSwitchOn()) {
            return MatchConfig.success(new WhiteListStrategy());
        }
        if (StringUtils.isBlank(methodName)) {
            return rpcMatchResult.getUnchecked(className);
        }
        return rpcMatchResult.getUnchecked(className + '#' + methodName);
    }

    private static MatchConfig getRpcPassedCache(String name) {
        if (!PradarSwitcher.whiteListSwitchOn()) {
            return MatchConfig.success(new WhiteListStrategy());
        }
        if (StringUtils.isBlank(name)) {
            return failure();
        }

        MatchConfig config = new MatchConfig();
        config.setUrl(name);

        Set<MatchConfig> rpcNameList = GlobalConfig.getInstance().getRpcNameWhiteList();
        if (rpcNameList == null) {
            return failure();
        }
        Iterator<MatchConfig> iterator = rpcNameList.iterator();
        while (iterator.hasNext()) {
            MatchConfig next = iterator.next();
            if (next.equals(config)) {
                return next;
            }
        }

        if (StringUtils.indexOf(name, '#') != -1) {
            return failure();
        } else {
            for (MatchConfig matchConfig : rpcNameList) {
                String value = matchConfig.getUrl();
                final int index = StringUtils.indexOf(value, '#');
                if (index == -1) {
                    if (StringUtils.equals(value, name)) {
                        return matchConfig;
                    }
                } else {
                    // 如果白名单包含 # ，则取出类名进行比较
                    String className = StringUtils.substring(value, index);
                    if (StringUtils.equals(value, className)) {
                        return matchConfig;
                    }
                }
            }
        }
        return failure();
    }

    /**
     * 判断压测数据是否能通过这道门
     */
    public static MatchConfig shallWePassHttpString(String url) {
        try {
            if (!PradarSwitcher.whiteListSwitchOn()) {
                return MatchConfig.success(new WhiteListStrategy());
            }
            return httpMatchResult.get(url);
        } catch (ExecutionException e) {
            LOGGER.warn("WhiteListError: shallWePassHttpString cache exception!", e);
            return failure();
        }
    }

    public static void main(String[] args) {
        MatchConfig matchConfig = shallWePassHttpStringCache("http://pt-agent-test-oss3-1627285144888.oss-cn-hangzhou.aliyuncs.com/");
    }

    private static MatchConfig shallWePassHttpStringCache(String url) {
        if (url != null) {
            final int index = url.indexOf("&#47;");
            if (index != -1) {
                url = url.replace("&#47;", "/");
            }
        }
        MatchConfig config = null;
        String orgUrl = url;
        //如果列表为空，同样是无法调用的
        if (url != null) {
            try {
                URI uri = URI.create(url);
                if (uri.getHost().startsWith("pt")) {
                    try {
                        String substring = uri.getHost().substring(2);
                        if (substring.startsWith("-")) {
                            substring = substring.substring(1);
                        }
                        url = new URI(uri.getScheme(),
                                uri.getUserInfo(),
                                substring,
                                uri.getPort(),
                                uri.getPath(),
                                uri.getQuery(),
                                uri.getFragment())
                                .getPath();
                    } catch (URISyntaxException e) {
                    }
                } else {
                    url = uri.getPath();
                }
            } catch (Throwable e) {
                if (url.startsWith("http://")) {
                    url = url.substring(7);
                    int index = url.indexOf("/");
                    if (index != -1) {
                        url = url.substring(index + 1);
                    } else {
                        url = "/";
                    }
                } else if (url.startsWith("https://")) {
                    url = url.substring(8);
                    int index = url.indexOf("/");
                    if (index != -1) {
                        url = url.substring(index + 1);
                    } else {
                        url = "/";
                    }
                }
                final int indexOfQuestion = url.indexOf('?');
                if (indexOfQuestion != -1) {
                    url = url.substring(0, indexOfQuestion);
                }
                final int indexOfx = url.indexOf('#');
                if (indexOfx != -1) {
                    url = url.substring(0, indexOfx);
                }
                //如果不是一个正常的uri则直接忽略这一步
            }

            if (StringUtils.isBlank(url) || "/".equals(url)) {
                /**
                 * 如果 url 为空或者是/没有其他值，则使用原 url 匹配一次
                 */
                config = patternCache.getUnchecked(orgUrl);
                if (null != config) {
                    return config;
                }
            }
            config = patternCache.getUnchecked(url);
            if (null != config) {
                return config;
            }
            if (null != PradarSwitcher.httpPassPrefix.get()
                    && url.startsWith(PradarSwitcher.httpPassPrefix.get())) {
                return config;
            }
            LOGGER.warn("WhiteListError: url is not allowed:" + url);
        }
        return failure();
    }

    /**
     * 通配符表达式匹配
     * <p>
     * 通配符是一种特殊语法，主要有星号(*)和问号(?)组成，在Simulator中主要用来模糊匹配类名和方法名。
     * 比如：java.lang.String，可以被"*String"所匹配
     * </p>
     * <ul>
     * <li>(null) matching (null) == false</li>
     * <li>    ANY matching ("*") == true</li>
     * </ul>
     *
     * @param string      目标字符串
     * @param matchConfig 通配符匹配模版
     * @return true:目标字符串符合匹配模版;false:目标字符串不符合匹配模版
     */
    public static MatchConfig matching(final String string, final MatchConfig matchConfig) {
        String wildcard = matchConfig.getUrl();
        if ("*".equals(matchConfig.getUrl())) {
            return matchConfig;
        }
        if (matchConfig.getUrl() == null || string == null) {
            return failure();
        }
        /**
         * 如果没有通配符则全匹配
         */
        if (!wildcard.contains("*") && wildcard.equals(string)) {
            return matchConfig;
        }

        return matching(string, matchConfig, 0, 0);

    }

    /**
     * Internal matching recursive function.
     */
    private static MatchConfig matching(String string, MatchConfig matchConfig, int stringStartNdx, int patternStartNdx) {
        String wildcard = matchConfig.getUrl();
        int pNdx = patternStartNdx;
        int sNdx = stringStartNdx;
        int pLen = wildcard.length();
        if (pLen == 1) {
            if (wildcard.charAt(0) == '*') {     // speed-up
                return matchConfig;
            }
        }
        int sLen = string.length();
        boolean nextIsNotWildcard = false;
        MatchConfig config = failure();
        while (true) {

            // check if end of string and/or pattern occurred
            if ((sNdx >= sLen)) {   // end of string still may have pending '*' callback pattern
                while ((pNdx < pLen) && (wildcard.charAt(pNdx) == '*')) {
                    pNdx++;
                }
                return pNdx >= pLen ? matchConfig : null;
            }
            if (pNdx >= pLen) {         // end of pattern, but not end of the string
                return config;
            }
            char p = wildcard.charAt(pNdx);    // pattern char

            // perform logic
            if (!nextIsNotWildcard) {

                if (p == '\\') {
                    pNdx++;
                    nextIsNotWildcard = true;
                    continue;
                }
                if (p == '?') {
                    sNdx++;
                    pNdx++;
                    continue;
                }
                if (p == '*') {
                    char pnext = 0;           // next pattern char
                    if (pNdx + 1 < pLen) {
                        pnext = wildcard.charAt(pNdx + 1);
                    }
                    if (pnext == '*') {         // double '*' have the same effect as one '*'
                        pNdx++;
                        continue;
                    }
                    int i;
                    pNdx++;

                    // find recursively if there is any substring from the end of the
                    // line that matches the rest of the pattern !!!
                    for (i = string.length(); i >= sNdx; i--) {
                        if (null != matching(string, matchConfig, i, pNdx)) {
                            return matchConfig;
                        }
                    }
                    return config;
                }
            } else {
                nextIsNotWildcard = false;
            }

            // check if pattern char and string char are equals
            if (p != string.charAt(sNdx)) {
                return config;
            }

            // everything matches for now, continue
            sNdx++;
            pNdx++;
        }
    }

}
