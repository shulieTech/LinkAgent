/**
 * Copyright 2021 Shulie Technology, Co.Ltd
 * Email: shulie@shulie.io
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.pamirs.attach.plugin.dynamic.template;

/**
 * @Auther: vernon
 * @Date: 2021/8/19 12:56
 * @Description:
 */
public class HikariTemplate extends ConnectionPoolTemplate {
    @Override
    public String getName() {
        return "hikari";
    }

    public HikariTemplate setUrl(String url) {
        this.url = url;
        return this;
    }

    public HikariTemplate setUsername(String username) {
        this.username = username;
        return this;
    }

    public HikariTemplate setPassword(String password) {
        this.password = password;
        return this;
    }

    public HikariTemplate setDriverClassName(String driverClassName) {
        this.driverClassName = driverClassName;
        return this;
    }

    public HikariTemplate setConnectionTimeout(Long connectionTimeout) {
        this.connectionTimeout = connectionTimeout;
        return this;
    }

    public HikariTemplate setIdleTimeout(Long idleTimeout) {
        this.idleTimeout = idleTimeout;
        return this;
    }

    public HikariTemplate setMaxLifetime(Long maxLifetime) {
        this.maxLifetime = maxLifetime;
        return this;
    }

    public HikariTemplate setConnectionTestQuery(String connectionTestQuery) {
        this.connectionTestQuery = connectionTestQuery;
        return this;
    }

    public HikariTemplate setMinimumIdle(Long minimumIdle) {
        this.minimumIdle = minimumIdle;
        return this;
    }

    public HikariTemplate setMaximumPoolSize(Integer maximumPoolSize) {
        this.maximumPoolSize = maximumPoolSize;
        return this;
    }

    public HikariTemplate setValidationTimeout(Long validationTimeout) {
        this.validationTimeout = validationTimeout;
        return this;
    }

    public String getUrl() {
        return url;
    }

    public String getUsername() {
        return username;
    }

    public String getPassword() {
        return password;
    }

    public String getDriverClassName() {
        return driverClassName;
    }

    public Long getConnectionTimeout() {
        return connectionTimeout;
    }

    public Long getIdleTimeout() {
        return idleTimeout;
    }

    public Long getMaxLifetime() {
        return maxLifetime;
    }

    public String getConnectionTestQuery() {
        return connectionTestQuery;
    }

    public Long getMinimumIdle() {
        return minimumIdle;
    }

    public Integer getMaximumPoolSize() {
        return maximumPoolSize;
    }

    public Long getValidationTimeout() {
        return validationTimeout;
    }


    @Info(describe = "地址", modifier = Info.ModifierType.UNMODIFIABLE)
    private String url;
    @Info(describe = "用户名", modifier = Info.ModifierType.UNMODIFIABLE)
    private String username;
    @Info(describe = "密码", modifier = Info.ModifierType.UNMODIFIABLE)
    private String password;
    @Info(describe = "驱动", modifier = Info.ModifierType.UNMODIFIABLE)
    protected String driverClassName;
    protected Long connectionTimeout;
    protected Long idleTimeout;
    protected Long maxLifetime;
    protected String connectionTestQuery;
    protected Long minimumIdle;
    protected Integer maximumPoolSize;
    protected Long validationTimeout;

}
