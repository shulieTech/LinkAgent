package com.pamirs.attach.plugin.shadow.preparation.command;

import com.pamirs.attach.plugin.shadow.preparation.es.EsConfigEntity;

import java.util.List;

public class EsConfigPushCommand {

    private List<EsConfigEntity> data;

    public List<EsConfigEntity> getData() {
        return data;
    }

    public void setData(List<EsConfigEntity> data) {
        this.data = data;
    }
}
