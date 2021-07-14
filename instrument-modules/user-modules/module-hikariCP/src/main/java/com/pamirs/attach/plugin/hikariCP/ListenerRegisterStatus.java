package com.pamirs.attach.plugin.hikariCP;

import java.util.concurrent.atomic.AtomicBoolean;

/**
 * 监听器注册状态
 *
 * @author xiaobin.zfb|xiaobin@shulie.io
 * @since 2020/12/22 4:47 下午
 */
public class ListenerRegisterStatus {

    private static ListenerRegisterStatus INSTANCE;

    private final AtomicBoolean isInited = new AtomicBoolean(false);

    public static ListenerRegisterStatus getInstance() {
        if (INSTANCE == null) {
            synchronized (ListenerRegisterStatus.class) {
                if (INSTANCE == null) {
                    INSTANCE = new ListenerRegisterStatus();
                }
            }
        }
        return INSTANCE;
    }

    public static void release() {
        INSTANCE = null;
    }

    /**
     * 判断是否已经初始化
     *
     * @return 返回初始化状态, true|false
     */
    public boolean isInited() {
        return isInited.get();
    }

    /**
     * 进行初始化操作，如果失败则返回 false
     *
     * @return 如果初始化失败则返回 false
     */
    public boolean init() {
        return isInited.compareAndSet(false, true);
    }
}
