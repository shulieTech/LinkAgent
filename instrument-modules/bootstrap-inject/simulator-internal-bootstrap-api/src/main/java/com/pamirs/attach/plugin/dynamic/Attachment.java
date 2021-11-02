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
package com.pamirs.attach.plugin.dynamic;

import java.io.Serializable;

/**
 * @Auther: vernon
 * @Date: 2021/8/18 11:14
 * @Description:
 */
public class Attachment<T> implements Serializable {


    /**
     * 需要attach的内容
     */
    T ext;
    /**
     * 需要attach的索引对象
     */
    transient Object index;
    /**
     * 模块id
     */
    String moduleId;
    /**
     * 需要附着的trace类型数组
     */
    transient String[] traceTypeList;

    public String getModuleId() {
        return moduleId;
    }

    public Attachment setModuleId(String moduleId) {
        this.moduleId = moduleId;
        return this;
    }

    public T getExt() {
        return ext;
    }

    public void setExt(T ext) {
        this.ext = ext;
    }

    public Attachment(Object index, String moduleId, String[] traceTypeList, T ext) {
        this.index = index;
        this.moduleId = moduleId;
        this.traceTypeList = traceTypeList;
        this.ext = ext;
    }

    public Attachment() {

    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        Attachment that = (Attachment) o;

        return that.index == index
                && that.moduleId == moduleId
                && that.traceTypeList == traceTypeList
                && that.ext == ext;
    }

    @Override
    public int hashCode() {
        return super.hashCode();
    }
}
