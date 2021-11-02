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

import java.util.Map;

/**
 * @Auther: vernon
 * @Date: 2021/8/18 13:42
 * @Description:
 */
public class DruidTemplate implements Template {

    public boolean isSupport_shadowdb() {
        return support_shadowdb;
    }

    public DruidTemplate setSupport_shadowdb(boolean support_shadowdb) {
        this.support_shadowdb = support_shadowdb;
        return this;
    }

    public boolean isSupport_shadowtable() {
        return support_shadowtable;
    }

    public DruidTemplate setSupport_shadowtable(boolean support_shadowtable) {
        this.support_shadowtable = support_shadowtable;
        return this;
    }

    public boolean isSupport_shadowdb_and_shadowtable() {
        return support_shadowdb_and_shadowtable;
    }

    public DruidTemplate setSupport_shadowdb_and_shadowtable(boolean support_shadowdb_and_shadowtable) {
        this.support_shadowdb_and_shadowtable = support_shadowdb_and_shadowtable;
        return this;
    }

    public Map<String, Class<?>> getAttribute() {
        return attribute;
    }

    public DruidTemplate setAttribute(Map<String, Class<?>> attribute) {
        this.attribute = attribute;
        return this;
    }

    /**
     * 是否支持影子库
     */
    private boolean support_shadowdb;
    /**
     * 是否自持影子表
     */
    private boolean support_shadowtable;
    /**
     * 是否支持影子库和影子表
     */
    private boolean support_shadowdb_and_shadowtable;

    private Map<String, Class<?>> attribute;

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || o.getClass() != getClass()) {
            return false;
        }
        DruidTemplate that = (DruidTemplate) o;
        return support_shadowdb == that.support_shadowdb
                && support_shadowtable == that.support_shadowtable
                && support_shadowdb_and_shadowtable == that.support_shadowdb_and_shadowtable;
    }
}
