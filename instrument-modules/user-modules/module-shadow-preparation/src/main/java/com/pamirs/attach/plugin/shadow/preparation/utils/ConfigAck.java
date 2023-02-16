package com.pamirs.attach.plugin.shadow.preparation.utils;

public class ConfigAck {

    private static final java.lang.String SUCC_DESC = "SUCC";
    private java.lang.String type;
    private java.lang.String version;
    private java.lang.Integer resultCode;
    private java.lang.String resultDesc;

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getVersion() {
        return version;
    }

    public void setVersion(String version) {
        this.version = version;
    }

    public Integer getResultCode() {
        return resultCode;
    }

    public void setResultCode(Integer resultCode) {
        this.resultCode = resultCode;
    }

    public String getResultDesc() {
        return resultDesc;
    }

    public void setResultDesc(String resultDesc) {
        this.resultDesc = resultDesc;
    }
}
