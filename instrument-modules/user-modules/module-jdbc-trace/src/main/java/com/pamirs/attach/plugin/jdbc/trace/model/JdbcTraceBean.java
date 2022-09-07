/*
 * Copyright 2021 Shulie Technology, Co.Ltd
 * Email: shulie@shulie.io
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.pamirs.attach.plugin.jdbc.trace.model;

import com.pamirs.attach.plugin.common.datasource.trace.SqlTraceMetaData;

/**
 * @Description
 * @Author ocean_wll
 * @Date 2022/8/31 13:45
 */
public class JdbcTraceBean {

    private SqlTraceMetaData sqlTraceMetaData;

    private boolean isStartSuccess = false;

    public JdbcTraceBean(SqlTraceMetaData sqlTraceMetaData, boolean isStartSuccess) {
        this.sqlTraceMetaData = sqlTraceMetaData;
        this.isStartSuccess = isStartSuccess;
    }

    public SqlTraceMetaData getSqlTraceMetaData() {
        return sqlTraceMetaData;
    }

    public void setSqlTraceMetaData(SqlTraceMetaData sqlTraceMetaData) {
        this.sqlTraceMetaData = sqlTraceMetaData;
    }

    public boolean isStartSuccess() {
        return isStartSuccess;
    }

    public void setStartSuccess(boolean startSuccess) {
        isStartSuccess = startSuccess;
    }
}
