package com.pamirs.attach.plugin.es.listener;

import com.pamirs.attach.plugin.es.shadowserver.rest.RestClientDefinitionStrategy;
import com.pamirs.attach.plugin.es.shadowserver.rest.definition.RestClientDefinition;
import com.pamirs.pradar.Pradar;
import com.pamirs.pradar.SyncObjectService;
import com.pamirs.pradar.bean.SyncObject;
import com.pamirs.pradar.bean.SyncObjectData;
import com.pamirs.pradar.internal.config.ShadowEsServerConfig;
import com.pamirs.pradar.pressurement.agent.event.IEvent;
import com.pamirs.pradar.pressurement.agent.event.impl.preparation.ShadowEsPreCheckEvent;
import com.pamirs.pradar.pressurement.agent.listener.EventResult;
import com.pamirs.pradar.pressurement.agent.listener.PradarEventListener;
import com.shulie.instrument.simulator.api.util.CollectionUtils;
import org.elasticsearch.client.Request;
import org.elasticsearch.client.Response;
import org.elasticsearch.client.RestClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.*;

public class EsShadowPreCheckEventListener implements PradarEventListener {

    private final static Logger LOGGER = LoggerFactory.getLogger(EsShadowPreCheckEventListener.class.getName());

    @Override
    public EventResult onEvent(IEvent iEvent) {
        if (!(iEvent instanceof ShadowEsPreCheckEvent)) {
            return EventResult.IGNORE;
        }
        ShadowEsPreCheckEvent event = (ShadowEsPreCheckEvent) iEvent;
        Thread.currentThread().setContextClassLoader(event.getRestClient().getClass().getClassLoader());

        RestClient restClient = (RestClient) event.getRestClient();

        String result;
        List<String> indices = event.getIndices();
        List<String> allIndices = new ArrayList<String>();
        List<String> allPtIndices = new ArrayList<String>();

        Integer shadowType = event.getShadowType();
        switch (shadowType) {
            // 影子库
            case 1:
                RestClientDefinition restClientDefinition = RestClientDefinitionStrategy.match(restClient);
                RestClient ptClient = restClientDefinition.solve(restClient, toConfig(event));
                // 取影子库所有索引
                result = fetchIndices(ptClient, allPtIndices);
                if (result != null) {
                    event.handlerResult("影子库," + result);
                    return EventResult.success(iEvent);
                }
                // 取业务索引
                result = fetchIndices(restClient, allIndices);
                if (result != null) {
                    event.handlerResult(result);
                    return EventResult.success(iEvent);
                }
                // 不校验索引
                if (CollectionUtils.isEmpty(indices)) {
                    event.handlerResult("success");
                    return EventResult.success(iEvent);
                }
                StringBuilder sb = new StringBuilder();
                for (String index : indices) {
                    if (!allIndices.contains(index)) {
                        sb.append("业务索引" + index + "不存在;");
                    }
                    if (!allPtIndices.contains(index)) {
                        sb.append("影子索引" + index + "不存在;");
                    }
                }

                closePreCheckClient(ptClient);

                result = sb.toString();
                if (result.length() > 0) {
                    event.handlerResult(result);
                    return EventResult.success(iEvent);
                }
                break;
            //影子表
            case 3:
                if (CollectionUtils.isEmpty(indices)) {
                    event.handlerResult("success");
                    return EventResult.success(iEvent);
                }
                result = fetchIndices(restClient, allIndices);
                if (result != null) {
                    event.handlerResult(result);
                    return EventResult.success(iEvent);
                }

                List<String> ptIndices = new ArrayList<String>();
                for (String index : indices) {
                    ptIndices.add(Pradar.addClusterTestPrefix(index).toLowerCase());
                }
                // 验证业务索引是否存在
                indices.removeAll(allIndices);
                if (!indices.isEmpty()) {
                    StringBuilder builder = new StringBuilder();
                    for (String index : indices) {
                        builder.append(index).append(",");
                    }
                    event.handlerResult(String.format("业务索引:%s不存在", builder));
                    return EventResult.success(iEvent);
                }
                // 验证影子索引是否存在
                ptIndices.removeAll(allIndices);
                if (!ptIndices.isEmpty()) {
                    StringBuilder builder = new StringBuilder();
                    for (String index : ptIndices) {
                        builder.append(index).append(",");
                    }
                    event.handlerResult(String.format("影子索引:%s不存在", builder));
                    return EventResult.success(iEvent);
                }
        }
        event.handlerResult("success");
        return EventResult.success(iEvent);
    }

    public String fetchIndices(RestClient restClient, List<String> indices) {
        Response response;
        try {
            Request request = new Request("GET", "/_cat/indices?v");
            response = restClient.performRequest(request);
        } catch (IOException e) {
            LOGGER.error("[es] get indices from server occur exception", e);
            return "从es服务器上读取索引信息时发生异常,异常信息:" + e.getMessage();
        }
        InputStream inputStream = null;
        if (response != null) {
            try {
                inputStream = response.getEntity().getContent();
            } catch (IOException e) {
                LOGGER.error("[es] read indices content failed", e);
                return "读取es索引信息返回内容时发生异常,异常信息:" + e.getMessage();
            }
        }
        String line;
        if (inputStream != null) {
            InputStreamReader inputStreamReader = null;
            BufferedReader bufferedReader = null;
            try {
                int idxIndex = -1;
                inputStreamReader = new InputStreamReader(inputStream);
                bufferedReader = new BufferedReader(inputStreamReader);
                while ((line = bufferedReader.readLine()) != null) {
                    String[] tokens = line.split("\\s+");
                    for (int i = 0; i < tokens.length; i++) {
                        String token = tokens[i];
                        if (idxIndex == -1 && token.equals("index")) {
                            idxIndex = i;
                            break;
                        }
                        if (idxIndex == -1) {
                            continue;
                        }
                        indices.add(tokens[idxIndex]);
                        break;
                    }
                }
                return null;
            } catch (Exception e) {
                LOGGER.error("[es] read indices content failed", e);
                return "读取es索引信息返回内容时发生异常,异常信息:" + e.getMessage();
            } finally {
                try {
                    if (bufferedReader != null) {
                        bufferedReader.close();
                    }
                    if (inputStreamReader != null) {
                        inputStreamReader.close();
                    }
                } catch (IOException e) {
                }

            }
        }
        return null;
    }

    private ShadowEsServerConfig toConfig(ShadowEsPreCheckEvent event) {
        return new ShadowEsServerConfig(Arrays.asList(event.getBusinessNodes().split(",")), Arrays.asList(event.getPerformanceTestNodes().split(",")),
                event.getBusinessClusterName(), event.getPerformanceClusterName(), event.getPtUserName(), event.getPtPassword());
    }

    private void closePreCheckClient(RestClient restClient){
        SyncObject syncObject = SyncObjectService.getSyncObject("org.elasticsearch.client.RestClient");
        Set<SyncObjectData> datas = syncObject.getDatas();
        Iterator<SyncObjectData> iterator = datas.iterator();
        while (iterator.hasNext()) {
            SyncObjectData next = iterator.next();
            if (next.getTarget().equals(restClient)) {
                iterator.remove();
            }
        }
        try {
            restClient.close();
        } catch (IOException e) {
            LOGGER.error("[es] close es pre check rest client occur exception", e);
        }
    }

    @Override
    public int order() {
        return 35;
    }
}
