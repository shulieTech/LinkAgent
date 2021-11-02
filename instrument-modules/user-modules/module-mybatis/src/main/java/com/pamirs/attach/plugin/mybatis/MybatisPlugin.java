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
package com.pamirs.attach.plugin.mybatis;

import com.pamirs.attach.plugin.mybatis.interceptor.*;
import com.pamirs.pradar.interceptor.Interceptors;
import com.shulie.instrument.simulator.api.ExtensionModule;
import com.shulie.instrument.simulator.api.ModuleInfo;
import com.shulie.instrument.simulator.api.ModuleLifecycleAdapter;
import com.shulie.instrument.simulator.api.instrument.EnhanceCallback;
import com.shulie.instrument.simulator.api.instrument.InstrumentClass;
import com.shulie.instrument.simulator.api.instrument.InstrumentMethod;
import com.shulie.instrument.simulator.api.listener.Listeners;
import com.shulie.instrument.simulator.api.scope.ExecutionPolicy;
import org.kohsuke.MetaInfServices;

/**
 * Created by xiaobin on 2017/2/16.
 */
@MetaInfServices(ExtensionModule.class)
@ModuleInfo(id = MybatisConstants.MODULE_NAME, version = "1.0.0", author = "xiaobin@shulie.io", description = "mybatis 支持，有对应的 trace 日志输出")
public class MybatisPlugin extends ModuleLifecycleAdapter implements ExtensionModule {

    @Override
    public boolean onActive() throws Throwable {

        /**
         * mybatis 二级缓存
         */
        addInterceptorsForMapperBuilderAssistant();
        addInterceptorsForMappedStatement();
        enhanceTemplate.enhance(this, "org.apache.ibatis.session.defaults.DefaultSqlSession", new EnhanceCallback() {
            @Override
            public void doEnhance(InstrumentClass target) {
                final InstrumentMethod method = target.getDeclaredMethods("selectOne", "selectList", "selectMap", "selectCursor", "select",
                        "insert", "update", "delete");
                method.addInterceptor(Listeners.of(SqlSessionOperationInterceptor.class, "MYBATIS_SCOPE", ExecutionPolicy.BOUNDARY, Interceptors.SCOPE_CALLBACK));
            }
        });

        enhanceTemplate.enhance(this, "org.mybatis.spring.SqlSessionTemplate", new EnhanceCallback() {
            @Override
            public void doEnhance(InstrumentClass target) {
                final InstrumentMethod method = target.getDeclaredMethods("selectOne", "selectList", "selectMap", "selectCursor", "select",
                        "insert", "update", "delete");
                method.addInterceptor(Listeners.of(SqlSessionOperationInterceptor.class, "MYBATIS_SCOPE", ExecutionPolicy.BOUNDARY, Interceptors.SCOPE_CALLBACK));
            }
        });
        return true;
    }

    private void addInterceptorsForMappedStatement() {
        enhanceTemplate.enhance(this, "org.apache.ibatis.mapping.MappedStatement", new EnhanceCallback() {

            @Override
            public void doEnhance(InstrumentClass target) {
                final InstrumentMethod instrumentMethod = target.getDeclaredMethod("getCache");
                instrumentMethod.addInterceptor(Listeners.of(MappedStatementGetCacheInterceptor.class));
            }
        });
    }

    private void addInterceptorsForMapperBuilderAssistant() {
        enhanceTemplate.enhance(this, "org.apache.ibatis.builder.MapperBuilderAssistant", new EnhanceCallback() {

            @Override
            public void doEnhance(InstrumentClass target) {
                final InstrumentMethod instrumentMethod = target.getDeclaredMethod("useNewCache",
                        "java.lang.Class", "java.lang.Class",
                        "java.lang.Long", "java.lang.Integer", "boolean", "boolean",
                        "java.util.Properties");
                instrumentMethod.addInterceptor(Listeners.of(MapperBuilderAssistantUseNewCacheInterceptor.class));

                final InstrumentMethod addMappedStatementMethod = target.getDeclaredMethod("addMappedStatement",
                        "java.lang.String", "org.apache.ibatis.mapping.SqlSource",
                        "org.apache.ibatis.mapping.StatementType", "org.apache.ibatis.mapping.SqlCommandType",
                        "java.lang.Integer", "java.lang.Integer", "java.lang.String",
                        "java.lang.Class", "java.lang.String", "java.lang.Class",
                        "org.apache.ibatis.mapping.ResultSetType", "boolean", "boolean", "boolean",
                        "org.apache.ibatis.executor.keygen.KeyGenerator", "java.lang.String",
                        "java.lang.String", "java.lang.String", "org.apache.ibatis.scripting.LanguageDriver",
                        "java.lang.String");
                addMappedStatementMethod.addInterceptor(Listeners.of(MapperBuilderAssistantAddMappedStatement.class));

            }
        });
    }
}
