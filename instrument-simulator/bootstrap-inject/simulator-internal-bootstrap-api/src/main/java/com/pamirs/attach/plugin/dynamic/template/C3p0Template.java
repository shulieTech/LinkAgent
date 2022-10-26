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
 * @Date: 2021/8/19 15:03
 * @Description:
 */
public class C3p0Template extends ConnectionPoolTemplate {
    @Override
    public String getName() {
        return "c3p0";
    }

    @Info(describe = "用户名")
    private String username;
    @Info(describe = "密码")
    private String password;
    @Info(describe = "地址")
    private String url;
    protected String driverClassName;
    protected String datasourceName;
    protected Integer initialPoolSize;
    protected Integer maxIdleTime;
    protected Integer maxPoolSize;

    public String getDatasourceName() {
        return datasourceName;
    }

    public C3p0Template setDatasourceName(String datasourceName) {
        this.datasourceName = datasourceName;
        return this;
    }

    public Integer getInitialPoolSize() {
        return initialPoolSize;
    }

    public C3p0Template setInitialPoolSize(Integer initialPoolSize) {
        this.initialPoolSize = initialPoolSize;
        return this;
    }

    public Integer getMaxIdleTime() {
        return maxIdleTime;
    }

    public C3p0Template setMaxIdleTime(Integer maxIdleTime) {
        this.maxIdleTime = maxIdleTime;
        return this;
    }

    public Integer getMaxPoolSize() {
        return maxPoolSize;
    }

    public C3p0Template setMaxPoolSize(Integer maxPoolSize) {
        this.maxPoolSize = maxPoolSize;
        return this;
    }


    public String getDriverClassName() {
        return driverClassName;
    }

    public C3p0Template setDriverClassName(String driverClassName) {
        this.driverClassName = driverClassName;
        return this;
    }

    public String getUsername() {
        return username;
    }

    public String getPassword() {
        return password;
    }

    public String getUrl() {
        return url;
    }

    public C3p0Template setUrl(String url) {
        this.url = url;
        return this;
    }

    public C3p0Template setUsername(String username) {
        this.username = username;
        return this;
    }

    public C3p0Template setPassword(String password) {
        this.password = password;
        return this;
    }


}
