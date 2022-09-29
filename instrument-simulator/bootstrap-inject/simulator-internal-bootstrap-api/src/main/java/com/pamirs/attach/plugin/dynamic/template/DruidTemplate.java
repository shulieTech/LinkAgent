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
 * @Date: 2021/8/19 19:32
 * @Description:
 */
public class DruidTemplate extends ConnectionPoolTemplate {

    @Override
    public String getName() {
        return "druid";
    }

    public String getUsername() {
        return username;
    }

    public DruidTemplate setUsername(String username) {
        this.username = username;
        return this;
    }

    public String getPassword() {
        return password;
    }

    public DruidTemplate setPassword(String password) {
        this.password = password;
        return this;
    }

    public String getUrl() {
        return url;
    }

    public DruidTemplate setUrl(String url) {
        this.url = url;
        return this;
    }

    public DruidTemplate setTestOnReturn(Boolean testOnReturn) {
        this.testOnReturn = testOnReturn;
        return this;
    }

    public DruidTemplate setInitialSize(Integer initialSize) {
        this.initialSize = initialSize;
        return this;
    }

    public DruidTemplate setMaxActive(Integer maxActive) {
        this.maxActive = maxActive;
        return this;
    }

    public DruidTemplate setRemoveAbandoned(Boolean removeAbandoned) {
        this.removeAbandoned = removeAbandoned;
        return this;
    }

    public DruidTemplate setRemoveAbandonedTimeout(Long removeAbandonedTimeout) {
        this.removeAbandonedTimeout = removeAbandonedTimeout;
        return this;
    }

    public DruidTemplate setTestWhileIdle(Boolean testWhileIdle) {
        this.testWhileIdle = testWhileIdle;
        return this;
    }

    public DruidTemplate setTestOnBorrow(Boolean testOnBorrow) {
        this.testOnBorrow = testOnBorrow;
        return this;
    }


    public DruidTemplate setValidationQuery(String validationQuery) {
        this.validationQuery = validationQuery;
        return this;
    }

    public DruidTemplate setDriverClassName(String driverClassName) {
        this.driverClassName = driverClassName;
        return this;
    }


    public Integer getInitialSize() {
        return initialSize;
    }

    public Integer getMaxActive() {
        return maxActive;
    }

    public Boolean getRemoveAbandoned() {
        return removeAbandoned;
    }

    public Long getRemoveAbandonedTimeout() {
        return removeAbandonedTimeout;
    }

    public Boolean getTestWhileIdle() {
        return testWhileIdle;
    }

    public Boolean getTestOnBorrow() {
        return testOnBorrow;
    }

    public Boolean getTestOnReturn() {
        return testOnReturn;
    }

    public String getValidationQuery() {
        return validationQuery;
    }

    public String getDriverClassName() {
        return driverClassName;
    }

    @Info(describe = "用户名", modifier = Info.ModifierType.UNMODIFIABLE)
    private String username;
    @Info(describe = "密码", modifier = Info.ModifierType.UNMODIFIABLE)
    private String password;
    @Info(describe = "地址", modifier = Info.ModifierType.UNMODIFIABLE)
    private String url;
    protected Integer initialSize;
    protected Integer maxActive;
    protected Boolean removeAbandoned;
    protected Long removeAbandonedTimeout;
    protected Boolean testWhileIdle;
    protected Boolean testOnBorrow;
    protected Boolean testOnReturn;
    protected String validationQuery;
    @Info(describe = "驱动")
    protected String driverClassName;


}
