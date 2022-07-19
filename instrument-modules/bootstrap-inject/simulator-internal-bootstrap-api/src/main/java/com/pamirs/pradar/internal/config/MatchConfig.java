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
package com.pamirs.pradar.internal.config;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

import com.pamirs.pradar.internal.adapter.ExecutionStrategy;

/**
 * @Author <a href="tangyuhan@shulie.io">yuhan.tang</a>
 * @package: com.pamirs.pradar.internal.config
 * @Date 2021/6/7 6:56 下午
 */
public class MatchConfig implements Serializable {

    private String url;
    private ExecutionStrategy strategy;

    /**
     * 执行脚本
     */
    private String scriptContent;

    private Map<String, Object> args = new HashMap<String, Object>();

    /**
     * 转发地址
     */
    private String forwarding;

    private boolean success = true;

    public MatchConfig(MatchConfig matchConfig) {
        this.url = matchConfig.url;
        this.strategy = matchConfig.strategy;
        this.scriptContent = matchConfig.scriptContent;
        this.forwarding = matchConfig.forwarding;
        this.success = matchConfig.success;
        this.args = new HashMap<String, Object>(matchConfig.args);
    }

    public MatchConfig() {

    }

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public ExecutionStrategy getStrategy() {
        return strategy;
    }

    public void setStrategy(ExecutionStrategy strategy) {
        this.strategy = strategy;
    }

    public String getScriptContent() {
        return scriptContent;
    }

    public void setScriptContent(String scriptContent) {
        this.scriptContent = scriptContent;
    }

    public String getForwarding() {
        return forwarding;
    }

    public void setForwarding(String forwarding) {
        this.forwarding = forwarding;
    }

    public boolean isSuccess() {
        return success;
    }

    public MatchConfig setSuccess(boolean success) {
        this.success = success;
        return this;
    }

    public static MatchConfig success(ExecutionStrategy strategy) {
        final MatchConfig matchConfig = new MatchConfig();
        matchConfig.setStrategy(strategy);
        return matchConfig.setSuccess(true);
    }

    public static MatchConfig failure(ExecutionStrategy strategy) {
        MatchConfig config = new MatchConfig().setSuccess(false);
        config.setStrategy(strategy);
        return config;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        MatchConfig config = (MatchConfig) o;
        return url.equals(config.getUrl());
    }

    public Map<String, Object> getArgs() {
        return args;
    }

    public void setArgs(Map<String, Object> args) {
        this.args = args;
    }

    public void addArgs(String key, Object v) {
        args.put(key, v);
    }

    @Override
    public int hashCode() {
        return url.hashCode();
    }

    @Override
    public String toString() {
        return "{" +
                "url='" + url + "',\n" +
                "script='" + scriptContent + "',\n" +
                "forwarding='" + forwarding + "',\n" +
                "strategy='" + strategy + "'\n" +
                '}';
    }
}
