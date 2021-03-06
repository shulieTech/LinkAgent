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
import java.util.HashMap;
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
                                    /*   MatchConfig matching = matching(name, matchConfig);*/
                                    MatchConfig matching = match(name, matchConfig);
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
     * ??????????????????????????? rpc ??????
     * ?????? ??????+????????? ???????????????
     *
     * @param className  ??????
     * @param methodName ?????????
     */
    public static MatchConfig shallWePassRpc(String className, String methodName) {
        if (!PradarSwitcher.whiteListSwitchOn()) {
            return MatchConfig.success(new WhiteListStrategy());
        }
        if (StringUtils.isBlank(methodName)) {
            return copyMatchConfig(rpcMatchResult.getUnchecked(className));
        }
        return copyMatchConfig(rpcMatchResult.getUnchecked(className + '#' + methodName));
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
                    // ????????????????????? # ??????????????????????????????
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
     * ??????????????????????????????????????????
     */
    public static MatchConfig shallWePassHttpString(String url) {
        try {
            if (!PradarSwitcher.whiteListSwitchOn()) {
                return MatchConfig.success(new WhiteListStrategy());
            }
            return copyMatchConfig(httpMatchResult.get(url));
        } catch (ExecutionException e) {
            LOGGER.warn("WhiteListError: shallWePassHttpString cache exception!", e);
            return failure();
        }
    }

    private static MatchConfig copyMatchConfig(MatchConfig matchConfig) {
        return new MatchConfig(matchConfig);
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
        //?????????????????????????????????????????????
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
                //???????????????????????????uri????????????????????????
            }

            if (StringUtils.isBlank(url) || "/".equals(url)) {
                /**
                 * ?????? url ???????????????/?????????????????????????????? url ????????????
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
     * ????????????????????????
     * <p>
     * ????????????????????????????????????????????????(*)?????????(?)????????????Simulator????????????????????????????????????????????????
     * ?????????java.lang.String????????????"*String"?????????
     * </p>
     * <ul>
     * <li>(null) matching (null) == false</li>
     * <li>    ANY matching ("*") == true</li>
     * </ul>
     *
     * @param string      ???????????????
     * @param matchConfig ?????????????????????
     * @return true:?????????????????????????????????;false:????????????????????????????????????
     */


    private static Matcher matcher = new HttpMatcher();

    public static MatchConfig match(final String string, final MatchConfig matchConfig) {
        return matcher.match(string, matchConfig);
    }

    public static MatchConfig matching(final String string, final MatchConfig matchConfig) {
        String wildcard = matchConfig.getUrl();
        if ("*".equals(matchConfig.getUrl())) {
            return matchConfig;
        }
        if (matchConfig.getUrl() == null || string == null) {
            return failure();
        }
        /**
         * ?????????????????????????????????
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
