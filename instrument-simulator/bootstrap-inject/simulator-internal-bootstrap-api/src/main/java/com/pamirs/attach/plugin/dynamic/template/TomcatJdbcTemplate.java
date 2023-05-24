package com.pamirs.attach.plugin.dynamic.template;

public class TomcatJdbcTemplate extends ConnectionPoolTemplate{

    @Info(describe = "地址", modifier = Info.ModifierType.UNMODIFIABLE)
    private String url;
    @Info(describe = "用户名", modifier = Info.ModifierType.UNMODIFIABLE)
    private String username;
    @Info(describe = "密码", modifier = Info.ModifierType.UNMODIFIABLE)
    private String password;
    @Info(describe = "驱动")
    private String driverClassName;

    private int initialSize;
    private int maxActive;
    private int maxIdle;
    private int minIdle;



    public String getUrl() {
        return url;
    }

    public TomcatJdbcTemplate setUrl(String url) {
        this.url = url;
        return this;
    }

    public String getUsername() {
        return username;
    }

    public TomcatJdbcTemplate setUsername(String username) {
        this.username = username;
        return this;
    }

    public String getPassword() {
        return password;
    }

    public TomcatJdbcTemplate setPassword(String password) {
        this.password = password;
        return this;
    }

    public int getInitialSize() {
        return initialSize;
    }

    public TomcatJdbcTemplate setInitialSize(int initialSize) {
        this.initialSize = initialSize;
        return this;
    }

    public int getMaxActive() {
        return maxActive;
    }

    public TomcatJdbcTemplate setMaxActive(int maxActive) {
        this.maxActive = maxActive;
        return this;
    }

    public int getMaxIdle() {
        return maxIdle;
    }

    public TomcatJdbcTemplate setMaxIdle(int maxIdle) {
        this.maxIdle = maxIdle;
        return this;
    }

    public int getMinIdle() {
        return minIdle;
    }

    public TomcatJdbcTemplate setMinIdle(int minIdle) {
        this.minIdle = minIdle;
        return this;
    }

    public String getDriverClassName() {
        return driverClassName;
    }

    public TomcatJdbcTemplate setDriverClassName(String driverClassName) {
        this.driverClassName = driverClassName;
        return this;
    }

    @Override
    public String getName() {
        return "apache-tomcat-jdbc";
    }


}
