package com.pamirs.attach.plugin.es.common.impl;

import java.util.Collections;
import java.util.List;

public class MainRequestIndexRename extends AbstractReadRequestIndexRename{
    @Override
    public String getName() {
        return "ping";
    }

    @Override
    public List<String> reindex0(Object target) {
        return Collections.emptyList();
    }

    @Override
    public List<String> getIndex0(Object target) {
        return Collections.emptyList();
    }
}
