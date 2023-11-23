package com.pamirs.attach.plugin.lettuce.utils;

/**
 * @author jirenhe | jirenhe@shulie.io
 * @since 2022/08/30 11:45 AM
 */
public class LettuceMetaData {

    private String host;

    private Integer port;

    private int db;

    private static final LettuceMetaData DEFAULT = new LettuceMetaData("unknown", 6379, 0);

    public LettuceMetaData(String host, Integer port, int db) {
        this.host = host;
        this.port = port;
        this.db = db;
    }

    public LettuceMetaData() {
    }

    public static LettuceMetaData getDefault() {
        return DEFAULT;
    }

    public String getHost() {
        return host;
    }

    public void setHost(String host) {
        this.host = host;
    }

    public Integer getPort() {
        return port;
    }

    public void setPort(Integer port) {
        this.port = port;
    }

    public int getDb() {
        return db;
    }

    public void setDb(int db) {
        this.db = db;
    }
}
