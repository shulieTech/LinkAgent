package com.pamirs.attach.plugin.shadow.preparation.command.processor;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.pamirs.pradar.ConfigNames;
import com.pamirs.pradar.PradarSwitcher;
import com.pamirs.pradar.internal.adapter.ExecutionStrategy;
import com.pamirs.pradar.internal.config.MatchConfig;
import com.pamirs.pradar.pressurement.agent.shared.exit.ArbiterHttpExit;
import com.pamirs.pradar.pressurement.agent.shared.service.GlobalConfig;
import com.pamirs.pradar.pressurement.mock.ForwardStrategy;
import com.pamirs.pradar.pressurement.mock.JsonMockStrategy;
import com.pamirs.pradar.pressurement.mock.MockStrategy;
import com.pamirs.pradar.pressurement.mock.WhiteListStrategy;
import com.pamirs.pradar.utils.MD5Util;
import io.shulie.agent.management.client.model.Config;
import io.shulie.agent.management.client.model.ConfigAck;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashSet;
import java.util.Set;
import java.util.function.Consumer;

public class WhiteListPushCommandProcessor {

    private final static Logger LOGGER = LoggerFactory.getLogger(WhiteListPushCommandProcessor.class.getName());

    private static final String DUBBO = "dubbo";
    private static final String FEIGN = "feign";

    private static final String RPC = "rpc";
    private static final String GRPC = "grpc";
    private static final String MQ = "mq";
    private static final String SEARCH = "search";
    private static final String HTTP = "http";
    private static final String TYPE2 = "TYPE";
    private static final String TYPE3 = "checkType";

    private static final String MOCK_CONTENT = "content";
    private static final String FORWARD_URL = "forwardUrl";
    private static final String INTERFACE_NAME = "INTERFACE_NAME";

    private static final ExecutionStrategy forwardStrategy = new ForwardStrategy();
    private static final ExecutionStrategy mockStrategy = new MockStrategy();
    private static final ExecutionStrategy whiteListStrategy = new WhiteListStrategy();
    private static final ExecutionStrategy jsonMockStrategy = new JsonMockStrategy() {
    };

    private static String previousCommandContentMD5;

    public static void handlerConfigPushCommand(final Config config, final Consumer<ConfigAck> callback) {
        String whiteLists = config.getParam();
        LOGGER.info("[shadow-preparation] accept whitelist command, content:{}", whiteLists);

        ConfigAck ack = new ConfigAck();
        ack.setType(config.getType());
        ack.setVersion(config.getVersion());
        ack.setResultCode(200);

        String md5 = MD5Util.MD5_32(whiteLists, "utf8");
        if (previousCommandContentMD5 == null || !previousCommandContentMD5.equals(md5)) {
            LOGGER.info("[shadow-preparation] whitelist config need to refresh!");
            activeWhiteListConfigs(whiteLists);
            ack.setResultDesc("白名单配置发生变更,主动生效");
        } else {
            ack.setResultDesc("白名单配置没有变动,已经生效");
        }
        previousCommandContentMD5 = md5;
        callback.accept(ack);
    }

    private static void activeWhiteListConfigs(String content) {
        final Set<String> mqList = new HashSet<String>();
        final Set<String> blockList = new HashSet<String>();
        final Set<String> searchWhiteList = new HashSet<String>();

        final Set<MatchConfig> urlWarList = new HashSet<MatchConfig>();
        final Set<MatchConfig> rpcClassMethodName = new HashSet<MatchConfig>();

        final JSONArray whitelist = JSON.parseArray(content);
        for (int i = 0; i < whitelist.size(); i++) {
            final JSONObject jsonObject1 = whitelist.getJSONObject(i);
            final String name = StringUtils.trim(jsonObject1.getString(INTERFACE_NAME));
            final String type = jsonObject1.getString(TYPE2);
            final String checkType = jsonObject1.getString(TYPE3);

            if (HTTP.equals(type)) {
                if (name.startsWith("mq:")) {
                    mqList.add(name.substring(3));
                } else if (name.startsWith("rabbitmq:")) {
                    mqList.add(name.substring(9));
                } else if (name.startsWith("search:")) {
                    searchWhiteList.add(name.substring(7));
                } else {
                    MatchConfig matchConfig = getMatchConfig(checkType, name, jsonObject1);
                    urlWarList.add(matchConfig);
                }
            } else if (DUBBO.equals(type) || FEIGN.equals(type)) {
                MatchConfig matchConfig = getMatchConfig(checkType, name, jsonObject1);
                rpcClassMethodName.add(matchConfig);
            } else if (RPC.equals(type) || GRPC.equals(type)) {
                MatchConfig matchConfig = getMatchConfig(checkType, name, jsonObject1);
                rpcClassMethodName.add(matchConfig);
            } else if (MQ.equals(type)) {
                mqList.add(name);
            } else if (SEARCH.equals(type)) {
                searchWhiteList.add(name);
            } else if ("block".equals(type)) {
                blockList.add(name);
            }
        }

        GlobalConfig.getInstance().setUrlWhiteList(urlWarList);
        ArbiterHttpExit.clearHttpMatch();
        PradarSwitcher.turnConfigSwitcherOn(ConfigNames.URL_WHITE_LIST);
    }

    public static MatchConfig getMatchConfig(String type, String value, JSONObject jsonObject) {
        MatchConfig config = new MatchConfig();
        if (OperateType.mock.name().equals(type)) {
            String content = jsonObject.getString(MOCK_CONTENT);
            config.setScriptContent(content);
            config.setStrategy(mockStrategy);
        } else if (OperateType.forward.name().equals(type)) {
            String url = jsonObject.getString(FORWARD_URL);
            config.setForwarding(url);
            config.setStrategy(forwardStrategy);
        } else if (OperateType.fix_mock.name().equals(type)) {
            String content = jsonObject.getString(MOCK_CONTENT);
            config.setScriptContent(content);
            config.setStrategy(jsonMockStrategy);
        } else {
            config.setStrategy(whiteListStrategy);
        }
        config.setUrl(value);
        return config;
    }

    enum OperateType {
        /**
         * guava动态 mock
         */
        mock,
        /**
         * 白名单
         */
        white,
        /**
         * 转发
         */
        forward,
        /**
         * 固定值mock
         */
        fix_mock

    }

}
