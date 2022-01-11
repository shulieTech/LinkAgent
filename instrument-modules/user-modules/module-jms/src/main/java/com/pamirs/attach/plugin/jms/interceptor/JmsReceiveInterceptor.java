package com.pamirs.attach.plugin.jms.interceptor;

import com.pamirs.attach.plugin.jms.JmsConstants;
import com.pamirs.attach.plugin.jms.util.ChinaLifeWorker;
import com.pamirs.attach.plugin.jms.util.PtJmsThreadDestroy;
import com.pamirs.pradar.Pradar;
import com.pamirs.pradar.interceptor.AroundInterceptor;
import com.pamirs.pradar.pressurement.agent.event.IEvent;
import com.pamirs.pradar.pressurement.agent.event.impl.MqWhiteListConfigEvent;
import com.pamirs.pradar.pressurement.agent.listener.EventResult;
import com.pamirs.pradar.pressurement.agent.listener.PradarEventListener;
import com.pamirs.pradar.pressurement.agent.shared.service.EventRouter;
import com.pamirs.pradar.pressurement.agent.shared.service.GlobalConfig;
import com.shulie.instrument.simulator.api.annotation.Destroyable;
import com.shulie.instrument.simulator.api.listener.ext.Advice;

import javax.jms.ObjectMessage;
import java.util.*;
import java.util.Map.Entry;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicBoolean;

@Destroyable(PtJmsThreadDestroy.class)
public class JmsReceiveInterceptor extends AroundInterceptor {
    public static final ConcurrentHashMap<String, List<ChinaLifeWorker>> RUNNING_JNDI_THREAD_MAP
        = new ConcurrentHashMap<String, List<ChinaLifeWorker>>();
    private final static AtomicBoolean FIRST = new AtomicBoolean(true);

    @Override
    public void doBefore(Advice advice) throws Throwable {
        super.doBefore(advice);
        if (FIRST.compareAndSet(true, false)) {
            final ClassLoader contextClassLoader = Thread.currentThread().getContextClassLoader();
            registerListener(contextClassLoader);
        }
    }

    @Override
    public void doAfter(Advice advice) throws Throwable {
        final ObjectMessage returnObj = (ObjectMessage)advice.getReturnObj();
        final Map<String, String> map = new HashMap<String, String>();
        for (String invokeContextTransformKey : Pradar.getInvokeContextTransformKeys()) {
            final String stringProperty = returnObj.getStringProperty(
                invokeContextTransformKey.replaceAll("-", "_"));
            map.put(invokeContextTransformKey, stringProperty);
        }
        Pradar.startServerInvoke(LookupInterceptor.JNDI_NAME.get(), null, map);
        Pradar.getInvokeContext().setResponse(returnObj.getObject());
        Pradar.middlewareName(JmsConstants.PLUGIN_NAME);
    }

    private void registerListener(final ClassLoader classLoader) {
        EventRouter.router().addListener(new PradarEventListener() {
            @Override
            public EventResult onEvent(IEvent event) {
                if (!(event instanceof MqWhiteListConfigEvent)) {
                    return EventResult.IGNORE;
                }
                final Set<String> mqWhiteList = GlobalConfig.getInstance().getMqWhiteList();
                LOGGER.info("pt-jms get mqWhiteList:" + Arrays.toString(mqWhiteList.toArray()));
                // remove no need pt queue
                final Set<String> allQueue = new HashSet<String>(mqWhiteList.size());
                for (String mq : mqWhiteList) {
                    final String[] split = mq.split("#");
                    if (split.length != 2) {
                        continue;
                    }
                    String queue = split[0];
                    int threadNum;
                    try {
                        threadNum = Integer.parseInt(split[1]);
                    } catch (NumberFormatException e) {
                        continue;
                    }
                    allQueue.add(queue);
                }
                for (Entry<String, List<ChinaLifeWorker>> queueThreadEntity : RUNNING_JNDI_THREAD_MAP.entrySet()) {
                    // if pulled config not contains running pt queue,remove all threads of this queue.
                    if (!allQueue.contains(queueThreadEntity.getKey())) {
                        for (ChinaLifeWorker chinaLifeWorker : queueThreadEntity.getValue()) {
                            chinaLifeWorker.setStop(true);
                            try {
                                chinaLifeWorker.interrupt();
                            } catch (Exception e) {
                                LOGGER.info(
                                    String.format("pt-jms thread %s have bean interrupt", chinaLifeWorker.getName()));
                            }
                        }
                        queueThreadEntity.getValue().clear();
                        LOGGER.info(String.format("pt-jms remove all pt queue %s", queueThreadEntity.getKey()));
                        RUNNING_JNDI_THREAD_MAP.remove(queueThreadEntity.getKey());
                    }
                }

                // calc nedd add or sub pt queue
                for (String mq : mqWhiteList) {
                    final String[] split = mq.split("#");
                    if (split.length != 2) {
                        continue;
                    }
                    String queue = split[0];
                    int threadNum;
                    try {
                        threadNum = Integer.parseInt(split[1]);
                    } catch (NumberFormatException e) {
                        continue;
                    }
                    List<ChinaLifeWorker> threads = RUNNING_JNDI_THREAD_MAP.get(queue);
                    if (threads == null) {
                        threads = new CopyOnWriteArrayList<ChinaLifeWorker>();
                        RUNNING_JNDI_THREAD_MAP.put(queue, threads);
                    }
                    final int offset = threadNum - threads.size();
                    if (offset == 0) {
                        LOGGER.info("pt-jms queue : " + queue + "  count not change");
                        continue;
                    }
                    if (offset > 0) {
                        LOGGER.info("pt-jms queue : " + queue + " need add thread count:" + offset);
                        for (int i = 1; i <= offset; i++) {
                            final ChinaLifeWorker worker = new ChinaLifeWorker(queue, threads.size() - 1 + i,
                                classLoader);
                            worker.setContextClassLoader(classLoader);
                            threads.add(worker);
                            worker.start();
                        }
                    } else {
                        final int absOffset = Math.abs(offset);
                        LOGGER.info("pt-jms queue : " + queue + "  need sub count:" + absOffset);
                        for (int i = threads.size() - 1; i >= absOffset; i--) {
                            final ChinaLifeWorker remove = threads.remove(i);
                            remove.setStop(true);
                            try {
                                remove.interrupt();
                            } catch (Exception e) {
                                LOGGER.info(String.format("pt-jms thread %s have bean interrupt", remove.getName()));
                            }
                        }
                    }
                }
                return EventResult.success("jms-plugin");
            }

            @Override
            public int order() {
                return 9;
            }
        });
    }
}
