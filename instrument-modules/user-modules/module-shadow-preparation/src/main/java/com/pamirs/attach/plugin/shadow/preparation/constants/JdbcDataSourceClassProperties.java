package com.pamirs.attach.plugin.shadow.preparation.constants;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

public enum JdbcDataSourceClassProperties {

    DRUID("com.alibaba.druid.pool.DruidDataSource", new String[]{"driverClass", "jdbcUrl", "username", "password"}),

    DBCP("org.apache.commons.dbcp.BasicDataSource", new String[]{"driverClassName", "url", "username", "password"}),

    DBCP2("org.apache.commons.dbcp2.BasicDataSource", new String[]{"driverClassName", "url", "userName", "password"}),

    HIKARICP("com.zaxxer.hikari.HikariDataSource", new String[]{"driverClassName", "jdbcUrl", "username", "password"});

    private String className;
    private String[] fields;

    private static final Map<String, JdbcDataSourceClassProperties> values = new HashMap<>();

    static {
        for (JdbcDataSourceClassProperties properties : JdbcDataSourceClassProperties.values()) {
            values.put(properties.className, properties);
        }
    }

    JdbcDataSourceClassProperties(String className, String[] fields) {
        this.className = className;
        this.fields = fields;
    }

    public String getClassName() {
        return className;
    }

    public static JdbcDataSourceClassProperties getEnumByClassName(String className) {
        return values.get(className);
    }

    public String getDriverClassProperty() {
        return fields[0];
    }

    public String getJdbcUrlProperty() {
        return fields[1];
    }

    public String getUsernameProperty() {
        return fields[2];
    }

    public static Collection<JdbcDataSourceClassProperties> getValues() {
        return values.values();
    }


}
