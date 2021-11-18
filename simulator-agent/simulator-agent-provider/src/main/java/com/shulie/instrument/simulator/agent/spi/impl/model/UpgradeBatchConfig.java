package com.shulie.instrument.simulator.agent.spi.impl.model;

import java.util.HashMap;
import java.util.Map;

/**
 * @author angju
 * @date 2021/11/17 17:39
 * 当前升级批次号
 */
public class UpgradeBatchConfig {

    private static final String OSS_ENDPOINT_KEY = "endpoint";
    private static final String OSS_ACCESS_KEY_ID_KEY = "accessKeyId";
    private static final String OSS_ACCESS_KEY_SECRET_KEY = "accessKeySecret";
    private static final String OSS_BUCKET_NAME_KEY = "bucketName";

    private static final String FTP_IP_KEY = "ip";
    private static final String FTP_PORT_KEY = "port";
    private static final String FTP_USERNAME_KEY = "username";
    private static final String FTP_PASSWORD_KEY = "password";


    private StorageTypeEnum storageTypeEnum;


    /**
     * 当前在用的版本批次号
     * 默认为-1，初始值需要通过心跳接口获取后设置
     */
    private String curUpgradeBatch = "-1";

    private Map<String, String> params = new HashMap<String, String>(4, 1);


    public String getEndPoint(){
        return params.get(OSS_ENDPOINT_KEY);
    }


    public String getAccessKeyId(){
        return params.get(OSS_ACCESS_KEY_ID_KEY);
    }

    public String getAccessKeySecret(){
        return params.get(OSS_ACCESS_KEY_SECRET_KEY);
    }

    public String getBucketName(){
        return params.get(OSS_BUCKET_NAME_KEY);
    }


    public String getIp(){
        return params.get(FTP_IP_KEY);
    }

    public String getPort(){
        return params.get(FTP_PORT_KEY);
    }

    public String getUsername(){
        return params.get(FTP_USERNAME_KEY);
    }


    public String getPassword(){
        return params.get(FTP_PASSWORD_KEY);
    }

    public StorageTypeEnum getStorageTypeEnum() {
        return storageTypeEnum;
    }

    public void setStorageTypeEnum(StorageTypeEnum storageTypeEnum) {
        this.storageTypeEnum = storageTypeEnum;
    }

    public String getCurUpgradeBatch() {
        return curUpgradeBatch;
    }

    public void setCurUpgradeBatch(String curUpgradeBatch) {
        this.curUpgradeBatch = curUpgradeBatch;
    }

    public Map<String, String> getParams() {
        return params;
    }

    public void setParams(Map<String, String> params) {
        this.params = params;
    }
}
