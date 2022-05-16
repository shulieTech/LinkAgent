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
package com.pamirs.attach.plugin.hbase.util;

import com.pamirs.pradar.Pradar;
import org.apache.commons.lang.StringUtils;

import java.util.ArrayList;
import java.util.List;

/**
 * @author: Hengyu
 * @className: HBaseUtils
 * @date: 2020/12/1 下午5:48
 * @description:
 */
public class HBaseTableNameUtils {

    public static final String NAMESPACE_SPLIT = ":";
    public static final String HBASE_METASPACE = "hbase";
    public static final String HBASE_NAMESPACE = "namespace";
    public static final String HBASE_META = "meta";

    private static final List<String> excludeReplace = new ArrayList<String>(2);

    static {
        excludeReplace.add("namespace");
        excludeReplace.add("meta");
    }

    /**
     * 替换影子表
     *
     * @param tableName 表名
     * @return
     */
    public static String replaceShadowTableName(String tableName) {
        int index = StringUtils.indexOf(tableName, NAMESPACE_SPLIT);
        if (index != -1) {
            String namespace = StringUtils.substring(tableName, 0, index);
            String table = StringUtils.substring(tableName, index + 1);
            if (!excludeReplace.contains(table) && !table.startsWith(HBASE_METASPACE) && !Pradar.isClusterTestPrefix(table)) {
                table = Pradar.addClusterTestPrefix(table);
                tableName = namespace + NAMESPACE_SPLIT + table;
                return tableName;
            }
        } else {
            if (!excludeReplace.contains(tableName) && !tableName.startsWith(HBASE_METASPACE) && !Pradar.isClusterTestPrefix(tableName)) {
                tableName = Pradar.addClusterTestPrefix(tableName);
                return tableName;
            }
        }
        return tableName;
    }


    public static String getTableNameNoContainsNameSpace(String tableName) {
        int index = StringUtils.indexOf(tableName, NAMESPACE_SPLIT);
        if (index != -1) {
            return StringUtils.substring(tableName, index + 1);
        }
        return tableName;
    }
}
