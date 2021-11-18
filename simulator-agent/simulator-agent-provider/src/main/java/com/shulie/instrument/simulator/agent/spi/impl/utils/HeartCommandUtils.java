package com.shulie.instrument.simulator.agent.spi.impl.utils;

import com.shulie.instrument.simulator.agent.api.model.CommandExecuteKey;
import com.shulie.instrument.simulator.agent.api.model.CommandExecuteResponse;

import java.util.Map;
import java.util.concurrent.*;

/**
 * @author angju
 * @date 2021/11/18 15:27
 */
public class HeartCommandUtils {
    /**
     * 在线升级的指令，只有这个指令由当前agent操作
     */
    public static final long onlineUpgradeCommandId = 110000;

    /**
     * 一批指令中包涵这个和升级的指令，需要优先处理这个指令，忽略升级指令
     */
    public static final long checkStorageCommandId = 100100;

    /**
     * 启动命令，有该指令时直接只执行这个指令
     */
    public static final long startCommandId = 200000;



    public static final ExecutorService commandExecutorService = new ThreadPoolExecutor(1, 3,
            0L, TimeUnit.MILLISECONDS,
            new LinkedBlockingQueue<Runnable>(10), new ThreadFactory() {
        @Override
        public Thread newThread(Runnable r) {
            Thread t = new Thread(r);
            t.setName("command-executor-" + t.getId());
            return t;
        }
    });


    public static final Map<CommandExecuteKey, FutureTask<CommandExecuteResponse>> futureTaskMap = new ConcurrentHashMap<CommandExecuteKey, FutureTask<CommandExecuteResponse>>(16, 1);
}
