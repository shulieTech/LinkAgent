package com.pamirs.attach.plugin.jms.util;

import com.pamirs.pradar.Pradar;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.Constructor;
import java.util.concurrent.TimeUnit;

public class ChinaLifeWorker extends Thread {
    protected final static Logger LOGGER = LoggerFactory.getLogger(ChinaLifeWorker.class.getName());
    private String queueJNDI;
    private volatile boolean stop = false;

    private ClassLoader bizClassLoader;

    public ChinaLifeWorker(String queueJNDI, int index, ClassLoader classLoader) {
        this.queueJNDI = Pradar.addClusterTestPrefix(queueJNDI);
        super.setDaemon(true);
        this.bizClassLoader = classLoader;
        super.setName("pt-jms-" + this.queueJNDI + "-" + index);
        super.setUncaughtExceptionHandler(new UncaughtExceptionHandler() {
            @Override
            public void uncaughtException(Thread t, Throwable e) {
                LOGGER.error("pt-jms" + t.getName() + " error", e);
            }
        });
    }

    public void setStop(boolean stop) {
        this.stop = stop;
    }

    @Override
    public void run() {
        Class t = null;
        try {
            LOGGER.info("pt-jms module classloader" + Thread.currentThread().getContextClassLoader()
                + "... bizClassLoader" + bizClassLoader);
            t = Class.forName("com.chinalife.esp.scheduler.worker.TaskWorker", true, bizClassLoader);
        } catch (ClassNotFoundException e) {
            LOGGER.error("pt-jms error", e);
        }
        if(t == null){
            LOGGER.error("pt-jms not found TaskWorker");
            return;
        }
        Runnable runnable;
        final Object taskWorker;
        try {
            final Constructor constructor = t.getConstructor(String.class);
            taskWorker = constructor.newInstance(queueJNDI);
        } catch (Exception e) {
            LOGGER.error(String.format("pt-jms: %s new instance error", t.getName()));
            return;
        }
        if (taskWorker instanceof Runnable) {
            runnable = (Runnable)taskWorker;
        }else {
            LOGGER.error(String.format("pt-jms: %s not instance Runnable", t.getName()));
            return;
        }
        while (!stop) {
            try {
                runnable.run();
                TimeUnit.SECONDS.sleep(1);
            } catch (Throwable throwable) {
                if (stop) {
                    LOGGER.info("pt-jms stop thread:" + Thread.currentThread().getName());
                    break;
                } else {
                    LOGGER.error("pt-jms " + queueJNDI + " error", throwable);
                }
            }
        }
    }
}
