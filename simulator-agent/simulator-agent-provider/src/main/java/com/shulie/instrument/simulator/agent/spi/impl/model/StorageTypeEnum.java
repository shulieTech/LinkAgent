package com.shulie.instrument.simulator.agent.spi.impl.model;

/**
 * @author angju
 * @date 2021/11/17 17:40
 */
public enum StorageTypeEnum {

    ONS("ons"),FTP("ftp");

    private String name;
    private StorageTypeEnum(String name){
        this.name = name;
    }
}
