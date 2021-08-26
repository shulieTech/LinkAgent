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
package com.pamirs.pradar.pressurement.datasource;

import java.io.StringWriter;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import com.pamirs.pradar.ConfigNames;
import com.pamirs.pradar.ErrorTypeEnum;
import com.pamirs.pradar.Pradar;
import com.pamirs.pradar.debug.DebugHelper;
import com.pamirs.pradar.internal.PradarInternalService;
import com.pamirs.pradar.internal.config.ShadowDatabaseConfig;
import com.pamirs.pradar.json.ResultSerializer;
import com.pamirs.pradar.pressurement.agent.shared.service.ErrorReporter;
import com.pamirs.pradar.pressurement.agent.shared.service.GlobalConfig;
import com.shulie.druid.DbType;
import com.shulie.druid.sql.SQLUtils;
import com.shulie.druid.sql.ast.SQLStatement;
import com.shulie.druid.sql.ast.statement.SQLAlterTableStatement;
import com.shulie.druid.sql.ast.statement.SQLCreateTableStatement;
import com.shulie.druid.sql.ast.statement.SQLDeleteStatement;
import com.shulie.druid.sql.ast.statement.SQLDropTableStatement;
import com.shulie.druid.sql.ast.statement.SQLInsertStatement;
import com.shulie.druid.sql.ast.statement.SQLSelectStatement;
import com.shulie.druid.sql.ast.statement.SQLUpdateStatement;
import com.shulie.druid.sql.dialect.mysql.ast.statement.MySqlRenameTableStatement;
import com.shulie.druid.sql.parser.SQLParserUtils;
import com.shulie.druid.sql.parser.SQLStatementParser;
import com.shulie.druid.sql.visitor.SQLASTOutputVisitor;
import com.shulie.druid.sql.visitor.SchemaStatVisitor;
import com.shulie.druid.stat.TableStat;
import com.shulie.druid.util.JdbcUtils;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * <p>{@code } instances should NOT be constructed in
 * standard programming. </p>
 * <p>This constructor is public to permit tools that require a JavaBean
 * instance to operate.</p>
 */
public class SqlParser {
    private final static Logger LOGGER = LoggerFactory.getLogger(SqlParser.class);
    public static String lowerCase;
    private static LoadingCache<String, TableParserResult> cacheSqlTablesBuilder = CacheBuilder.newBuilder()
            .maximumSize(300).expireAfterAccess(5 * 60, TimeUnit.SECONDS).build(

                    new CacheLoader<String, TableParserResult>() {
                        @Override
                        public TableParserResult load(String name) throws Exception {
                            try {
                                String[] args = StringUtils.split(name, "$$$$");
                                String sql = args[0];
                                String dbType = args[1];
                                return parseTables(sql, dbType);
                            } catch (SQLException e) {
                                return TableParserResult.EMPTY;
                            }
                        }
                    }

            );
    private static LoadingCache<String, String> cacheTableModeBuilder = CacheBuilder.newBuilder()
            .maximumSize(300).expireAfterAccess(5 * 60, TimeUnit.SECONDS).build(

                    new CacheLoader<String, String>() {
                        @Override
                        public String load(String name) throws Exception {
                            String[] args = StringUtils.split(name, "$$$$");
                            String sql = args[0];
                            String key = args[1];
                            String dbType = args[2];
                            return parseAndReplaceTableNames(sql, key, dbType);
                        }
                    }

            );
    private static LoadingCache<String, String> cacheSchemaModeBuilder = CacheBuilder.newBuilder()
            .maximumSize(300).expireAfterAccess(5 * 60, TimeUnit.SECONDS).build(

                    new CacheLoader<String, String>() {
                        @Override
                        public String load(String name) throws Exception {
                            String[] args = StringUtils.split(name, "$$$$");
                            String sql = args[0];
                            String key = args[1];
                            String dbType = args[2];
                            return parseAndReplaceSchema(sql, key, dbType);
                        }
                    }

            );

    public static void clear() {
        cacheSchemaModeBuilder.invalidateAll();
        cacheTableModeBuilder.invalidateAll();
    }

    public static void release() {
        cacheSchemaModeBuilder.invalidateAll();
        cacheTableModeBuilder.invalidateAll();
        cacheSqlTablesBuilder.invalidateAll();
    }

    public static TableParserResult getTables(String sql, String dbType) {
        String innerDbtype = dbType;
        //影子表压测
        try {
            return cacheSqlTablesBuilder.get(sql + "$$$$" + innerDbtype);
        } catch (Throwable e) {
            LOGGER.error("parse sql tables error. sql={}, dbType={}", sql, dbType, e);
            return TableParserResult.EMPTY;
        }
    }

    private static TableParserResult parseTables(String sql, String dbTypeName) throws SQLException {
        DbType dbType = DbType.of(dbTypeName);
        boolean isSelect = true;
        List<String> tables = new ArrayList<String>();
        SQLStatementParser parser = SQLParserUtils.createSQLStatementParser(sql, dbTypeName);
        if (parser == null) {
            if (GlobalConfig.getInstance().getWrongSqlDetail().size() < 10) {
                GlobalConfig.getInstance().addWrongSqlDetail(StringUtils.substring("dbType not support dbType" + dbTypeName + " sql" + sql, 0, 1995));
            }
            throw new SQLException("dbType not support dbType" + dbTypeName + " sql" + sql);
        }
        // 使用Parser解析生成AST，这里SQLStatement就是AST
        final StringWriter val = new StringWriter();
        try {
            final List<SQLStatement> sqlStatements = parser.parseStatementList();

            for (final SQLStatement sqlStatement : sqlStatements) {
                if (!(sqlStatement instanceof SQLSelectStatement)) {
                    isSelect = false;
                }
                SQLASTOutputVisitor visitor = SQLUtils.createOutputVisitor(val, dbType);
                SchemaStatVisitor visitor2 = SQLUtils.createSchemaStatVisitor(dbType);
                sqlStatement.accept(visitor2);
                final Map<TableStat.Name, TableStat> map2 = visitor2.getTables();
                for (final TableStat.Name name : map2.keySet()) {
                    /**
                     * 过滤掉函数
                     */
                    String tableName = name.getName();
                    if (StringUtils.indexOf(tableName, '(') != -1 && StringUtils.indexOf(tableName, ')') != -1) {
                        continue;
                    }
                    if ("DUAL".equalsIgnoreCase(tableName)) {
                        continue;
                    }
                    //这里的表名可能会带 schema，如 test.user
                    tableName = StringUtils.replace(tableName, "\"", "");
                    if (!tables.contains(tableName)) {
                        tables.add(tableName);
                    }
                }
                sqlStatement.accept(visitor);
            }
        } catch (Throwable e) {
            if (GlobalConfig.getInstance().getWrongSqlDetail().size() < 10) {
                GlobalConfig.getInstance().addWrongSqlDetail(StringUtils.substring(("Exception:" + e + " sql" + sql), 0, 1995));
            }
            throw new SQLException("Wrong sql:" + sql, e);
        }
        return new TableParserResult(tables, isSelect);
    }

    public static String replaceTable(String sql, String dbConnectionKey, String dbType) throws SQLException {
        if (!Pradar.isClusterTest()) {
            return sql;
        }
        String key = dbConnectionKey;
        String innerDbtype = dbType;
        //影子表压测
        try {
            return cacheTableModeBuilder.get(sql + "$$$$" + key + "$$$$" + innerDbtype);
        } catch (Throwable e) {
            LOGGER.error("replace table to shadow table error. sql={}, key={}, dbType={}", sql, dbConnectionKey, dbType, e);
            if (GlobalConfig.getInstance().getWrongSqlDetail().size() < 10) {
                GlobalConfig.getInstance().addWrongSqlDetail(StringUtils.substring(sql, 0, 1995));
            }
            SQLException exception = null;
            if (e instanceof SQLException) {
                exception = (SQLException) e;
            } else if (e.getCause() != null && e.getCause() instanceof SQLException) {
                exception = (SQLException) e.getCause();
            } else {
                exception = new SQLException(e);
            }
            recordDebugFlow(sql, e, "replaceTable");
            throw exception;
        }
    }

    public static String replaceSchema(String sql, String dbConnectionKey, String dbType) throws SQLException {
        if (!Pradar.isClusterTest()) {
            return sql;
        }
        String key = dbConnectionKey;
        String innerDbtype = dbType;
        //影子表压测
        try {
            return cacheSchemaModeBuilder.get(sql + "$$$$" + key + "$$$$" + innerDbtype);
        } catch (Throwable e) {
            LOGGER.error("replace schema to shadow schema error. sql={}, key={}, dbType={}", sql, dbConnectionKey, dbType, e);
            if (GlobalConfig.getInstance().getWrongSqlDetail().size() < 10) {
                GlobalConfig.getInstance().addWrongSqlDetail(StringUtils.substring(sql, 0, 1995));
            }
            SQLException exception = new SQLException(e);
            recordDebugFlow(sql, exception, "replaceSchema");
            throw exception;
        }
    }

    private static String serializeObject(Object target) {
        if (target == null) {
            return StringUtils.EMPTY;
        }
        try {
            return ResultSerializer.serializeObject(target, 2);
        } catch (Throwable e) {
            return StringUtils.EMPTY;
        }
    }

    private static void recordDebugFlow(final Object params, final Object returnObj, final String method) {
        if (!Pradar.isDebug()) {
            return;
        }
        String level;
        String pattern;
        Object return2log;
        String parameterArray = serializeObject(params);
        if (returnObj instanceof Throwable) {
            level = "ERROR";
            pattern = "%s, targetClass: %s, classLoader: %s, parameterArray: %s, throwable: %s";
            return2log = serializeObject(returnObj);
        } else {
            level = "INFO";
            pattern = "%s,targetClass: %s, classLoader: %s, parameterArray: %s, returnObj: %s";
            return2log = returnObj;
        }
        String content = String.format(pattern, method, SqlParser.class, SqlParser.class.getClassLoader().toString(),
            parameterArray, return2log);
        DebugHelper.addDebugInfo(level, content);
    }

    private static void resetMappingTables(String key, Map<String, String> mappings) {
        if (GlobalConfig.getInstance().containsShadowDatabaseConfig(key)) {
            ShadowDatabaseConfig shadowDatabaseConfig = GlobalConfig.getInstance().getShadowDatabaseConfig(key);
            shadowDatabaseConfig.setBusinessShadowTables(mappings);
        }
        if (StringUtils.isNotBlank(key)) {
            key = key.substring(0, key.lastIndexOf('|'));
        }
        if (GlobalConfig.getInstance().containsShadowDatabaseConfig(key)) {
            ShadowDatabaseConfig shadowDatabaseConfig = GlobalConfig.getInstance().getShadowDatabaseConfig(key);
            shadowDatabaseConfig.setBusinessShadowTables(mappings);
        }
    }

    private static Map<String, String> getMappingTables(String key) {
        if (GlobalConfig.getInstance().containsShadowDatabaseConfig(key)) {
            ShadowDatabaseConfig shadowDatabaseConfig = GlobalConfig.getInstance().getShadowDatabaseConfig(key);
            return shadowDatabaseConfig.getBusinessShadowTables();
        }
        if (StringUtils.isNotBlank(key)) {
            key = key.substring(0, key.lastIndexOf('|'));
        }
        if (GlobalConfig.getInstance().containsShadowDatabaseConfig(key)) {
            ShadowDatabaseConfig shadowDatabaseConfig = GlobalConfig.getInstance().getShadowDatabaseConfig(key);
            return shadowDatabaseConfig.getBusinessShadowTables();
        }
        return Collections.EMPTY_MAP;
    }

    private static String toShadowTable(String table) {
        if (!Pradar.isShadowDatabaseWithShadowTable()) {
            return table;
        }
        if (PradarInternalService.isClusterTestPrefix(table)) {
            return table;
        }
        return PradarInternalService.addClusterTestPrefix(table);
    }

    private static String toShadowSchema(String schema, ShadowDatabaseConfig config) {
        if (StringUtils.isBlank(schema)) {
            return null;
        }

        if (StringUtils.equals(schema, config.getSchema())) {
            return config.getShadowSchema();
        } else {
            //兼容 ORACLE
            if (StringUtils.startsWith(schema, "C##")) {
                final String oldSchema = StringUtils.substring(schema, StringUtils.indexOf(schema, "C##") + 3);
                if (!Pradar.isClusterTestPrefix(oldSchema)) {
                    return "C##" + Pradar.addClusterTestPrefix(oldSchema);
                }
            } else {
                if (!Pradar.isClusterTestPrefix(schema)) {
                    return Pradar.addClusterTestPrefix(schema);
                }
            }
        }
        return schema;
    }


    /**
     * 替换 schema
     *
     * @param sql
     * @param key
     * @param dbTypeName
     * @return
     * @throws SQLException
     */
    public static String parseAndReplaceSchema(String sql, String key, String dbTypeName) throws SQLException {
        ShadowDatabaseConfig config = GlobalConfig.getInstance().getShadowDatabaseConfig(key);
        if (config == null) {
            return sql;
        }
        DbType dbType = DbType.of(dbTypeName);
        // new MySQL Parser
        SQLStatementParser parser = SQLParserUtils.createSQLStatementParser(sql, dbTypeName);
        if (parser == null) {
            if (GlobalConfig.getInstance().getWrongSqlDetail().size() < 10) {
                GlobalConfig.getInstance().addWrongSqlDetail(StringUtils.substring("dbType not support" + key + " dbType" + dbTypeName + " sql" + sql, 0, 1995));
            }
            throw new SQLException("dbType not support" + key + " dbType" + dbTypeName + " sql" + sql);
        }
        // 使用Parser解析生成AST，这里SQLStatement就是AST
        final StringWriter val = new StringWriter();
        try {
            final List<SQLStatement> sqlStatements = parser.parseStatementList();

            for (final SQLStatement sqlStatement : sqlStatements) {
                SQLASTOutputVisitor visitor = SQLUtils.createOutputVisitor(val, dbType);
                SchemaStatVisitor visitor2 = SQLUtils.createSchemaStatVisitor(dbType);
                sqlStatement.accept(visitor2);
                final Map<TableStat.Name, TableStat> map2 = visitor2.getTables();
                Map<String, String> map = new HashMap<String, String>();
                for (Map.Entry<TableStat.Name, TableStat> entry : map2.entrySet()) {
                    String fullTable = StringUtils.replace(entry.getKey().getName(), "\"", "");
                    /**
                     * 过滤掉函数
                     */
                    if (StringUtils.indexOf(fullTable, '(') != -1 && StringUtils.indexOf(fullTable, ')') != -1) {
                        continue;
                    }
                    String table = null, schema = null;
                    final int indexOfDot = StringUtils.indexOf(fullTable, '.');
                    if (indexOfDot == -1) {
                        schema = null;
                        table = fullTable;
                    } else {
                        schema = StringUtils.substring(fullTable, 0, indexOfDot);
                        table = StringUtils.substring(fullTable, indexOfDot + 1);
                    }

                    String shadowSchema = toShadowSchema(schema, config);
                    String shadowTable = toShadowTable(table);
                    if (StringUtils.isBlank(shadowSchema)) {
                        map.put(fullTable, shadowTable);
                    } else {
                        map.put(fullTable, shadowSchema + '.' + shadowTable);
                        map.put(schema, shadowSchema);
                    }
                }
                visitor.setTableMapping(map);
                sqlStatement.accept(visitor);
            }
        } catch (Throwable e) {
            if (GlobalConfig.getInstance().getWrongSqlDetail().size() < 10) {
                GlobalConfig.getInstance().addWrongSqlDetail(StringUtils.substring(("Exception:" + e + " sql" + sql), 0, 1995));
            }
            throw new SQLException("Wrong sql:" + sql, e);
        }

        return val.toString();
    }

    public static String parseAndReplaceTableNames(String sql, String key, String dbTypeName) throws SQLException {
        DbType dbType = DbType.of(dbTypeName);
        Map<String, String> mappingTable = getMappingTables(key);
        if (SqlParser.lowerCase != null && "Y".equals(SqlParser.lowerCase)) {
            Map<String, String> mappingTableLower = new ConcurrentHashMap<String, String>();
            Set<String> keys = mappingTable.keySet();
            for (String tableName : keys) {
                String value = mappingTable.get(tableName);
                mappingTableLower.put(tableName.toLowerCase(), value.toLowerCase());
            }
            mappingTable = mappingTableLower;
            resetMappingTables(key, mappingTableLower);
        }

        // new MySQL Parser
        SQLStatementParser parser = SQLParserUtils.createSQLStatementParser(sql, dbTypeName);
        if (parser == null) {
            if (GlobalConfig.getInstance().getWrongSqlDetail().size() < 10) {
                GlobalConfig.getInstance().addWrongSqlDetail(StringUtils.substring("dbType not support" + key + " dbType" + dbTypeName + " sql" + sql, 0, 1995));
            }
            throw new SQLException("dbType not support" + key + " dbType" + dbTypeName + " sql" + sql);
        }

        // 使用Parser解析生成AST，这里SQLStatement就是AST
        final StringWriter val = new StringWriter();
        try {
            final List<SQLStatement> sqlStatements = parser.parseStatementList();

            for (final SQLStatement sqlStatement : sqlStatements) {
                SQLASTOutputVisitor visitor = SQLUtils.createOutputVisitor(val, dbType);
                SchemaStatVisitor visitor2 = SQLUtils.createSchemaStatVisitor(dbType);
                sqlStatement.accept(visitor2);
                final Map<TableStat.Name, TableStat> map2 = visitor2.getTables();

                //Set<String> tablesName = visitor.getTables();
                final Map<String, String> additionalTableNames = new HashMap();
                for (final TableStat.Name name : map2.keySet()) {
                    /**
                     * 过滤掉函数
                     */
                    String tableName = name.getName();
                    if (StringUtils.indexOf(tableName, '(') != -1 && StringUtils.indexOf(tableName, ')') != -1) {
                        continue;
                    }
                    for (String mappingName : mappingTable.keySet()) {
                        mappingName = StringUtils.replace(mappingName, "\"", "");
                        String nameTemp = name.getName();
                        String schema = "";
                        String fullTableName = StringUtils.replace(nameTemp, "\"", "");
                        if (nameTemp != null && StringUtils.contains(nameTemp, ".")) {
                            schema = StringUtils.substringBefore(nameTemp, ".");
                            nameTemp = StringUtils.substringAfter(nameTemp, ".");
                        }

                        if (StringUtils.indexOf(schema, "\"") != -1) {
                            schema = StringUtils.replace(schema, "\"", "");
                        }
                        if (StringUtils.indexOf(nameTemp, "\"") != -1) {
                            nameTemp = StringUtils.replace(nameTemp, "\"", "");
                        }

                        /**
                         * 如果配置的表名与获取到的表名相等，则如果 sql 中有 schema,则将映射表名也添加 schema
                         *   如sql 中的表名为 user或者是 test.user，但是配置的表名为 user
                         *
                         * 如果sql中的表名(带 schema)与映射表名相等，则直接添加映射表名
                         *
                         *   如 sql 中的表名为 test.user,配置的表名也为 test.user
                         */
                        if (StringUtils.equalsIgnoreCase(nameTemp, mappingName)) {
                            String value = mappingTable.get(mappingName);
                            if (StringUtils.isNotBlank(schema)) {
                                additionalTableNames.put(schema + "." + nameTemp, schema + "." + value);
                            } else {
                                additionalTableNames.put(nameTemp, value);
                            }
                        } else if (StringUtils.equalsIgnoreCase(fullTableName, mappingName)) {
                            additionalTableNames.put(fullTableName, mappingName);
                        }
                    }
                }

                if (additionalTableNames.size() > 0) {
                    mappingTable.putAll(additionalTableNames);
                }

                visitor.setTableMapping(mappingTable);
                sqlStatement.accept(visitor);
            }
        } catch (Throwable e) {
            if (GlobalConfig.getInstance().getWrongSqlDetail().size() < 10) {
                GlobalConfig.getInstance().addWrongSqlDetail(StringUtils.substring(("Exception:" + e + " sql" + sql), 0, 1995));
            }
            throw new SQLException("Wrong sql:" + sql, e);
        }

        SQLStatementParser parser2 = SQLParserUtils
                .createSQLStatementParser(val.toString(), dbTypeName);
        final List<SQLStatement> sqlStatements2 = parser2.parseStatementList();

        for (final SQLStatement sqlStatement : sqlStatements2) {
            if (sqlStatement instanceof SQLInsertStatement
                    || sqlStatement instanceof SQLUpdateStatement
                    || sqlStatement instanceof SQLDeleteStatement
                    || sqlStatement instanceof SQLAlterTableStatement
                    || sqlStatement instanceof SQLDropTableStatement
                    || sqlStatement instanceof SQLCreateTableStatement
                    || sqlStatement instanceof MySqlRenameTableStatement) {
                SchemaStatVisitor visitor = SQLUtils.createSchemaStatVisitor(dbType);
                sqlStatement.accept(visitor);

                final Map<TableStat.Name, TableStat> map = visitor.getTables();
                for (final TableStat.Name name : map.keySet()) {
                    boolean passThisTable = false;
                    String nameTemp = SQLUtils.normalize(name.getName(), dbType);
                    if (nameTemp != null && StringUtils.contains(nameTemp, ".")) {
                        nameTemp = StringUtils.substringAfter(nameTemp, ".");
                    }

                    if ("DUAL".equalsIgnoreCase(nameTemp)) {//dual table no need pt table
                        passThisTable = true;
                    } else {
                        for (final String mappingname : mappingTable.values()) {
                            if (StringUtils.equalsIgnoreCase(nameTemp, mappingname)) {
                                passThisTable = true;
                            }
                        }
                    }

                    if (!passThisTable) {
                        if (GlobalConfig.getInstance().getWrongSqlDetail().size() < 10) {
                            GlobalConfig.getInstance().addWrongSqlDetail(StringUtils.substring(sql, 0, 1995));
                        }
                        String url = key;
                        if (url.indexOf('|') != -1) {
                            url = url.substring(0, url.indexOf('|'));
                        }

                        ErrorReporter.buildError()
                            .setErrorType(ErrorTypeEnum.DataSource)
                            .setErrorCode("datasource-0004")
                            .setMessage(String
                                .format("没有配置对应的影子表! url:%s, table:%s, driverClassName:%s, dbType:%s", url, name.getName(),
                                    getDriverClassName(url), dbType))
                            .setDetail(
                                String.format(
                                    "The business table [%s] doesn't has shadow mapping table! url:%s, table:%s, "
                                        + "driverClassName:%s, dbType:%s, [sql] %s [new sql] %s",
                                    name.getName(), url, name.getName(), getDriverClassName(url), dbType, sql,
                                    val.toString()))
                            .closePradar(ConfigNames.SHADOW_DATABASE_CONFIGS)
                            .report();
                        throw new SQLException(String.format(
                            "The business table [%s] doesn't has shadow mapping table! url:%s, table:%s, "
                                + "driverClassName:%s, dbType:%s, [sql] %s [new sql] %s",
                            name.getName(), url, name.getName(), getDriverClassName(url), dbType, sql, val.toString()));
                    }
                }
            }
        }
        return val.toString();
    }

    private static String getDriverClassName(String url) {
        try {
            return JdbcUtils.getDriverClassName(url);
        } catch (SQLException e) {
            return "unknow";
        }
    }
}
