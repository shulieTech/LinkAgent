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

import com.pamirs.attach.plugin.dynamic.template.Template;

/**
 * @Auther: vernon
 * @Date: 2021/8/18 11:14
 * @Description:
 */
public class Attachment {
    /**
     * 需要attach的内容
     */
    Template ext;
    /**
     * 需要attach的索引对象
     */
    Object index;
    /**
     * 模块id
     */
    String moduleId;

    /**
     * 需要附着的trace类型数组
     */
    public String[] traceTypeList;


    public Attachment(Object index, String moduleId, String[] traceTypeList, Template ext) {
        this.index = index;
        this.moduleId = moduleId;
        this.traceTypeList = traceTypeList;
        this.ext = ext;
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
