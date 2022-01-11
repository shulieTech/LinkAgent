package com.pamirs.attach.plugin.mongodb.interceptor;

import com.mongodb.Mongo;
import com.mongodb.MongoClient;
import com.mongodb.ReadPreference;
import com.mongodb.ServerAddress;
import com.mongodb.operation.ReadOperation;
import com.mongodb.operation.WriteOperation;
import com.pamirs.attach.plugin.mongodb.utils.mongo343.ClientManagerUtils;
import com.pamirs.pradar.ConfigNames;
import com.pamirs.pradar.CutOffResult;
import com.pamirs.pradar.ErrorTypeEnum;
import com.pamirs.pradar.Pradar;
import com.pamirs.pradar.exception.PressureMeasureError;
import com.pamirs.pradar.interceptor.CutoffInterceptorAdaptor;
import com.pamirs.pradar.internal.config.ShadowDatabaseConfig;
import com.pamirs.pradar.pressurement.agent.shared.service.ErrorReporter;
import com.pamirs.pradar.pressurement.agent.shared.service.GlobalConfig;
import com.shulie.instrument.simulator.api.listener.ext.Advice;

import java.lang.reflect.Method;
import java.util.List;

/**
 * @author angju
 * @date 2021/9/22 20:23
 */
public class MongoExecuteCutoffInterceptor extends CutoffInterceptorAdaptor {

    private Method writeMethod = null;
    private Method readMethod = null;


    {
        try {
            writeMethod = Mongo.class.getDeclaredMethod("execute", WriteOperation.class);
            readMethod = Mongo.class.getDeclaredMethod("execute", ReadOperation.class, ReadPreference.class);
            writeMethod.setAccessible(true);
            readMethod.setAccessible(true);
        } catch (NoSuchMethodException e) {
            LOGGER.error("MongoExecuteCutoffInterceptor error ", e);
        }
    }

    @Override
    public CutOffResult cutoff0(Advice advice) throws Throwable {
        if (!Pradar.isClusterTest()) {
            return CutOffResult.passed();
        }
        if (ClientManagerUtils.getBusClient2ptClientMapping().containsValue(advice.getTarget())){
            return CutOffResult.passed();
        }
        List<ServerAddress> serverAddresses = ((MongoClient) advice.getTarget()).getAllAddress();
        ShadowDatabaseConfig shadowDatabaseConfig = getShadowDatabaseConfig(serverAddresses);

        if (shadowDatabaseConfig == null) {
            ErrorReporter.Error error = ErrorReporter.buildError()
                    .setErrorType(ErrorTypeEnum.DataSource)
                    .setErrorCode("datasource-0005")
                    .setMessage("mongo 未配置对应影子表")
                    .setDetail("mongo 未配置对应影子表");
            error.closePradar(ConfigNames.SHADOW_DATABASE_CONFIGS);
            error.report();
            throw new PressureMeasureError("mongo 未配置对应影子表");
        }

        if (!shadowDatabaseConfig.isShadowDatabase()){
            return CutOffResult.passed();
        }

        Mongo ptMongo = ClientManagerUtils.getBusClient2ptClientMapping().get(advice.getTarget());
        if (ptMongo == null){
            return CutOffResult.passed();
        }
        if (advice.getParameterArray().length == 1){
            return CutOffResult.cutoff(writeMethod.invoke(ptMongo, advice.getParameterArray()));
        } else {
            return CutOffResult.cutoff(readMethod.invoke(ptMongo, advice.getParameterArray()));
        }
    }



    private ShadowDatabaseConfig getShadowDatabaseConfig(List<ServerAddress> serverAddresses) {
        ShadowDatabaseConfig shadowDatabaseConfig = null;
        for (ShadowDatabaseConfig config : GlobalConfig.getInstance().getShadowDatasourceConfigs().values()) {
            for (ServerAddress serverAddress : serverAddresses) {
                if (config.getUrl().contains(serverAddress.toString())) {
                    shadowDatabaseConfig = config;
                    break;
                }
            }
        }
        return shadowDatabaseConfig;
    }
}
