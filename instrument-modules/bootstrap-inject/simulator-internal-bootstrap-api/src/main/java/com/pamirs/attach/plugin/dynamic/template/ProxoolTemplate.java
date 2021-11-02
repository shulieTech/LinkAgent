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
 * @Date: 2021/9/9 16:25
 * @Description:
 */
public class ProxoolTemplate extends ConnectionPoolTemplate {
    @Override
    public String getName() {
        return "proxool";
    }

    public String getUsername() {
        return username;
    }

    public ProxoolTemplate setUsername(String username) {
        this.username = username;
        return this;
    }

    public String getPassword() {
        return password;
    }

    public ProxoolTemplate setPassword(String password) {
        this.password = password;
        return this;
    }

    public String getUrl() {
        return url;
    }

    public ProxoolTemplate setUrl(String url) {
        this.url = url;
        return this;
    }

    public String getDriverClassName() {
        return driverClassName;
    }

    public ProxoolTemplate setDriverClassName(String driverClassName) {
        this.driverClassName = driverClassName;
        return this;
    }

    public String getAlias() {
        return alias;
    }

    public ProxoolTemplate setAlias(String alias) {
        this.alias = alias;
        return this;
    }

    @Info(describe = "别名")

    private String alias;

    @Info(describe = "用户名", modifier = Info.ModifierType.UNMODIFIABLE)
    private String username;
    @Info(describe = "密码", modifier = Info.ModifierType.UNMODIFIABLE)
    private String password;
    @Info(describe = "地址", modifier = Info.ModifierType.UNMODIFIABLE)
    private String url;

    @Info(describe = "驱动")
    protected String driverClassName;

}
