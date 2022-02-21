package com.pamirs.attach.plugin.rabbitmq.common;

import java.util.Set;
import java.util.concurrent.atomic.AtomicReference;

import com.pamirs.pradar.pressurement.agent.shared.service.GlobalConfig;

public class LastMqWhiteListHolder {
    public static final AtomicReference<Set<String>> LAST_MQ_WHITELIST = new AtomicReference<Set<String>>(
        GlobalConfig.getInstance().getMqWhiteList());
}
