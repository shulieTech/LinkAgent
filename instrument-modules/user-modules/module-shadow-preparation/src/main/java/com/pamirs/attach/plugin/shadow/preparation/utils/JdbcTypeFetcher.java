package com.pamirs.attach.plugin.shadow.preparation.utils;

import com.pamirs.attach.plugin.shadow.preparation.constants.JdbcTypeEnum;

public abstract class JdbcTypeFetcher {

    public static JdbcTypeEnum fetchJdbcType(String driver) {
        if (driver.contains("mysql")) {
            return JdbcTypeEnum.MYSQL;
        }
        if (driver.contains("oracle")) {
            return JdbcTypeEnum.ORACLE;
        }
        if (driver.contains("sqlserver")) {
            return JdbcTypeEnum.SQLSERVER;
        }
        if (driver.contains("gbasedbt") || driver.contains("informix")) {
            return JdbcTypeEnum.GBASE;
        }
        return null;
    }

}
