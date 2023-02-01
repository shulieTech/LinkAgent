/**
 * Copyright 2021 Shulie Technology, Co.Ltd
 * Email: shulie@shulie.io
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * <p>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.pamirs.pradar.pressurement.agent.shared.service;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import com.pamirs.pradar.Pradar;
import com.pamirs.pradar.internal.adapter.JobAdapter;
import com.pamirs.pradar.internal.config.MatchConfig;
import com.pamirs.pradar.internal.config.MockConfig;
import com.pamirs.pradar.internal.config.ShadowDatabaseConfig;
import com.pamirs.pradar.internal.config.ShadowEsServerConfig;
import com.pamirs.pradar.internal.config.ShadowHbaseConfig;
import com.pamirs.pradar.internal.config.ShadowJob;
import com.pamirs.pradar.internal.config.ShadowRedisConfig;
import com.pamirs.pradar.spi.ShadowDataSourceSPIManager;
import org.apache.commons.lang.StringUtils;

/**
 * 所有的全局共享配置
 */
public final class GlobalConfig {

    private static GlobalConfig INSTANCE;

    /**
     * debug detail info
     */
    private Set<String> wrongSqlDetail = new HashSet<String>();

    /**
     * war name list
     */
    private Set<MatchConfig> urlWhiteList = new HashSet<MatchConfig>();

    /**
     * rpc name list，包含 dubbo、grpc 等
     */
    private Set<MatchConfig> rpcNameWhiteList = new HashSet<MatchConfig>();

    /**
     * context path block list
     */
    private Set<String> contextPathBlockList = new HashSet<String>();

    /**
     * 搜索白名单，在搜索白名单中则可以走业务索引，但是只有只读操作，写操作不允许
     */
    private Set<String> searchWhiteList = new HashSet<String>();

    /**
     * redis key list
     */
    private Set<String> cacheKeyWhiteList = new HashSet<String>();

    /**
     * mq 白名单
     */
    private Set<String> mqWhiteList = new HashSet<String>();

    /**
     * 业务topic >> 自定义影子topic 的映射
     */
    private Map<String, String> shadowTopicMappings = new HashMap<>();

    /**
     * 业务group >> 自定义影子group 的映射
     */
    private Map<String, String> shadowGroupMappings = new HashMap<>();

    /**
     * 所有的入口规则
     */
    private Set<String> traceRules = new HashSet<String>();

    /**
     * 探针动态参数
     */
    private SimulatorDynamicConfig simulatorDynamicConfig = new SimulatorDynamicConfig(new HashMap<String, String>(1, 1));
    /**
     * 影子库表的配置
     * key: 不带参数的url#username
     */
    private Map<String, ShadowDatabaseConfig> shadowDatabaseConfigs = new ConcurrentHashMap<String, ShadowDatabaseConfig>();
    private Map<String, ShadowRedisConfig> shadowRedisConfigs = new ConcurrentHashMap<String, ShadowRedisConfig>();
    private Map<String, ShadowEsServerConfig> shadowEsServerConfigs = new ConcurrentHashMap<String, ShadowEsServerConfig>();
    public static Map<String, ShadowHbaseConfig> shadowHbaseServerConfigs
            = new ConcurrentHashMap<String, ShadowHbaseConfig>();

    //应用启动特定埋点状态信息，如数据库启动对应影子库数据源加入应用是否正常
    private Map<String, String> applicationAccessStatus = new ConcurrentHashMap<String, String>();
    private Map<String, JobAdapter> jobAdapterMap = new HashMap<String, JobAdapter>(5);
    private Map<String, ShadowJob> registerdJobs = new ConcurrentHashMap<String, ShadowJob>();
    private Map<String, ShadowJob> needRegisterJobs = new ConcurrentHashMap<String, ShadowJob>(8, 1);
    private Map<String, ShadowJob> needStopJobs = new ConcurrentHashMap<String, ShadowJob>(8, 1);
    private Set<ShadowJob> errorRegister = new HashSet<ShadowJob>();
    private Set<MockConfig> mockConfigs = new HashSet<MockConfig>();

    /**
     * redis影子库表 默认为影子表
     */
    private volatile boolean isShadowDbRedisServer = false;

    /**
     * es默认影子表
     */
    private volatile boolean isShadowEsServer = Boolean.FALSE;

    private volatile boolean isShadowHbaseServer = Boolean.FALSE;

    private Map<String, Set<String>> shadowTable = new HashMap<String, Set<String>>();

    private volatile Set<String> apis = new HashSet<String>();

    /**
     * redis压测数据最大过期时间
     */
    private static Float maxRedisExpireTime = -1f;

    private GlobalConfig() {
    }

    public static GlobalConfig getInstance() {
        if (INSTANCE == null) {
            synchronized (GlobalConfig.class) {
                if (INSTANCE == null) {
                    INSTANCE = new GlobalConfig();
                }
            }
        }
        return INSTANCE;
    }

    public void release() {
        wrongSqlDetail.clear();
        urlWhiteList.clear();
        rpcNameWhiteList.clear();
        contextPathBlockList.clear();
        searchWhiteList.clear();
        cacheKeyWhiteList.clear();
        mqWhiteList.clear();
        traceRules.clear();
        shadowDatabaseConfigs.clear();
        shadowRedisConfigs.clear();
        shadowEsServerConfigs.clear();
        shadowHbaseServerConfigs.clear();
        applicationAccessStatus.clear();
        jobAdapterMap.clear();
        registerdJobs.clear();
        needRegisterJobs.clear();
        needStopJobs.clear();
        errorRegister.clear();
        mockConfigs.clear();
        shadowTable.clear();
        apis.clear();
    }

    public Set<String> getApis() {
        return apis;
    }

    public void setApis(Set<String> apis) {
        this.apis = apis;
    }

    public boolean isShadowDbRedisServer() {
        return isShadowDbRedisServer;
    }

    public void setShadowDbRedisServer(boolean isShadowDbRedisServer) {
        this.isShadowDbRedisServer = isShadowDbRedisServer;
    }

    public Set<MockConfig> getMockConfigs() {
        return mockConfigs;
    }

    public void setMockConfigs(Set<MockConfig> mockConfigs) {
        this.mockConfigs = mockConfigs;
    }

    public Set<String> getWrongSqlDetail() {
        return wrongSqlDetail;
    }

    public void addWrongSqlDetail(String sql) {
        wrongSqlDetail.add(sql);
    }

    public void setWrongSqlDetail(Set<String> wrongSqlDetail) {
        this.wrongSqlDetail = wrongSqlDetail;
    }

    public Map<String, Set<String>> getShadowTables() {
        return shadowTable;
    }

    public void addShadowTable(String url, Set<String> tables) {
        shadowTable.put(url, tables);
    }

    public Set<ShadowJob> getErrorRegisterJobs() {
        return errorRegister;
    }

    public void addErrorRegisteredJob(ShadowJob shadowJob) {
        errorRegister.add(shadowJob);
    }

    public void clearErrorRegisteredJobs() {
        errorRegister.clear();
    }

    public Map<String, ShadowJob> getRegisteredJobs() {
        return registerdJobs;
    }

    public void addRegisteredJob(ShadowJob shadowJob) {
        registerdJobs.put(shadowJob.getClassName(), shadowJob);
    }

    public Map<String, ShadowJob> getNeedRegisterJobs() {
        return needRegisterJobs;
    }

    public void addNeedRegisterJobs(ShadowJob shadowJob) {
        if (needRegisterJobs.size() > 32) {
            needRegisterJobs.clear();
        }
        needRegisterJobs.put(shadowJob.getClassName(), shadowJob);
    }

    public Map<String, ShadowJob> getNeedStopJobs() {
        return needStopJobs;
    }

    public void addNeedStopJobs(ShadowJob shadowJob) {
        if (needStopJobs.size() > 32) {
            needStopJobs.clear();
        }
        needStopJobs.put(shadowJob.getClassName(), shadowJob);
    }

    public void removeRegisteredJob(ShadowJob shadowJob) {
        registerdJobs.remove(shadowJob.getClassName());
    }

    public Map<String, String> getApplicationAccessStatus() {
        return applicationAccessStatus;
    }

    public void addApplicationAccessStatus(String key, String value) {
        applicationAccessStatus.put(key, value);
    }

    public Map<String, JobAdapter> getJobAdaptors() {
        return jobAdapterMap;
    }

    public void addJobAdaptor(String name, JobAdapter jobAdapter) {
        jobAdapterMap.put(name, jobAdapter);
    }

    public JobAdapter getJobAdaptor(String name) {
        return jobAdapterMap.get(name);
    }

    public Map<String, ShadowDatabaseConfig> getShadowDatasourceConfigs() {
        return shadowDatabaseConfigs;
    }

    public boolean containsShadowDatabaseConfig(String key) {
        return shadowDatabaseConfigs.containsKey(key);
    }

    public ShadowDatabaseConfig getShadowDatabaseConfig(String key) {
        return shadowDatabaseConfigs.get(key);
    }

    public boolean isEmptyShadowDatabaseConfigs() {
        return shadowDatabaseConfigs == null || shadowDatabaseConfigs.isEmpty();
    }

    public void setShadowDatabaseConfigs(Map<String, ShadowDatabaseConfig> map, boolean refresh) {
        clearShadowDatasourceConfigs();
        if (refresh) {
            this.shadowDatabaseConfigs.putAll(ShadowDataSourceSPIManager.refreshAllShadowDatabaseConfigs(map));
        } else {
            this.shadowDatabaseConfigs.putAll(map);
        }
    }

    public void clearShadowDatasourceConfigs() {
        shadowDatabaseConfigs.clear();
    }

    public Map<String, ShadowRedisConfig> getShadowRedisConfigs() {
        return shadowRedisConfigs;
    }

    public void setShadowRedisConfigs(Map<String, ShadowRedisConfig> map) {
        shadowRedisConfigs.clear();
        shadowRedisConfigs.putAll(map);
    }

    public ShadowRedisConfig getShadowRedisConfig(String key) {
        return shadowRedisConfigs.get(key);
    }

    public Set<String> getTraceRules() {
        return traceRules;
    }

    public void setTraceRules(Set<String> traceRules) {
        this.traceRules = traceRules;
    }

    /**
     * 返回搜索白名单，在搜索白名单中则可以走业务索引，但是只有只读操作，写操作不允许
     *
     * @return
     */
    public Set<String> getSearchWhiteList() {
        return searchWhiteList;
    }

    public void setSearchWhiteList(Set<String> searchWhiteList) {
        this.searchWhiteList = searchWhiteList;
    }

    public Set<String> getCacheKeyWhiteList() {
        return cacheKeyWhiteList;
    }

    public void setCacheKeyWhiteList(Set<String> cacheKeyWhiteList) {
        this.cacheKeyWhiteList = cacheKeyWhiteList;
    }

    public Set<String> getMqWhiteList() {
        return mqWhiteList;
    }

    public void setMqWhiteList(Set<String> mqWhiteList) {
        this.mqWhiteList = mqWhiteList;
    }


    public void setShadowTopicMappings(Map<String, String> shadowTopicMappings) {
        this.shadowTopicMappings = shadowTopicMappings;
    }

    public Map<String, String> getShadowTopicMappings() {
        return shadowTopicMappings;
    }

    public void setShadowGroupMappings(Map<String, String> shadowGroupMappings) {
        this.shadowGroupMappings = shadowGroupMappings;
    }

    public Map<String, String> getShadowGroupMappings() {
        return shadowGroupMappings;
    }

    public Set<String> getContextPathBlockList() {
        return contextPathBlockList;
    }

    public void setContextPathBlockList(Set<String> contextPathBlockList) {
        this.contextPathBlockList = contextPathBlockList;
    }

    public Set<MatchConfig> getUrlWhiteList() {
        return urlWhiteList;
    }

    public void setUrlWhiteList(Set<MatchConfig> urlWhiteList) {
        this.urlWhiteList = urlWhiteList;
    }

    public Set<MatchConfig> getRpcNameWhiteList() {
        return rpcNameWhiteList;
    }

    public void setRpcNameWhiteList(Set<MatchConfig> rpcNameWhiteList) {
        this.rpcNameWhiteList = rpcNameWhiteList;
    }

    public Map<String, ShadowEsServerConfig> getShadowEsServerConfigs() {
        return shadowEsServerConfigs;
    }

    public Map<String, ShadowHbaseConfig> getShadowHbaseServerConfigs() {
        return shadowHbaseServerConfigs;
    }

    public void setShadowHbaseServerConfigs(Map<String, ShadowHbaseConfig> shadowHbaseServerConfigs) {
        GlobalConfig.shadowHbaseServerConfigs = shadowHbaseServerConfigs;
    }

    public void setShadowEsServerConfigs(
            Map<String, ShadowEsServerConfig> shadowEsServerConfigs) {
        this.shadowEsServerConfigs = shadowEsServerConfigs;
    }

    public boolean isShadowEsServer() {
        return isShadowEsServer;
    }

    public boolean isShadowHbaseServer() {
        return isShadowHbaseServer;
    }

    /**
     * hbase是否使用影子表替换
     * 影子库里的影子表模式 或者影子表模式
     *
     * @returnx
     */
    public boolean isShadowTableReplace() {
        if (isShadowHbaseServer) {
            //影子库影子表
            return isShadowTableInShadowDbHbaseServerMode();
        }
        //影子表
        return true;
    }

    /**
     * 是否为影子库下的影子表模式
     */
    static public boolean isShadowTableInShadowDbHbaseServerMode() {
        String s = System.getenv("shadowtable_in_shadowdbhbaseserver_mode");
        if (StringUtils.isNotBlank(s)) {
            return Boolean.parseBoolean(s);
        }
        s = getSystemProperty("shadowtable_in_shadowdbhbaseserver_mode", "false");
        return Boolean.parseBoolean(s);
    }

    static private String getSystemProperty(String key, String defau) {
        try {
            return System.getProperty(key, defau);
        } catch (Exception e) {
            return null;
        }
    }

    public void setShadowHbaseServer(boolean shadowHbaseServer) {
        isShadowHbaseServer = shadowHbaseServer;
    }

    public void setShadowEsServer(boolean shadowEsServer) {
        isShadowEsServer = shadowEsServer;
    }

    public Float getMaxRedisExpireTime() {
        return maxRedisExpireTime;
    }

    public void setMaxRedisExpireTime(Float maxRedisExpireTime) {
        GlobalConfig.maxRedisExpireTime = maxRedisExpireTime;
    }

    public SimulatorDynamicConfig getSimulatorDynamicConfig() {
        return simulatorDynamicConfig;
    }

    public void setSimulatorDynamicConfig(SimulatorDynamicConfig simulatorDynamicConfig) {
        this.simulatorDynamicConfig = simulatorDynamicConfig;
    }

    public boolean allowTraceRequestResponse() {
        return (Pradar.isClusterTest() &&
                GlobalConfig.getInstance().getSimulatorDynamicConfig().isShadowRequestResponseDataAllowTrace())
                || (!Pradar.isClusterTest() && GlobalConfig.getInstance().getSimulatorDynamicConfig()
                .isBusRequestResponseDataAllowTrace());
    }
}
