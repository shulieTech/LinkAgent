package com.pamirs.attach.plugin.shadowjob.shadowRunnable;

import com.pamirs.pradar.Pradar;

/**
 * @Description
 * @Author ocean_wll
 * @Date 2022/10/18 15:18
 */
public class PradarShadowJobRunnable implements Runnable {

    private Runnable delegate;

    private String serviceName;

    private String methodName;

    public PradarShadowJobRunnable(Runnable delegate, String serviceName, String methodName) {
        this.delegate = delegate;
        this.serviceName = serviceName;
        this.methodName = methodName;
    }

    @Override
    public void run() {
        // 为了打压测标识
        Pradar.startTrace(null, serviceName, methodName);
        Pradar.setClusterTest(true);
        delegate.run();
        Pradar.setClusterTest(false);
        Pradar.endTrace();
    }

    public Runnable getDelegate() {
        return delegate;
    }

    public String getServiceName() {
        return serviceName;
    }

    public String getMethodName() {
        return methodName;
    }
}
