package com.shulie.instrument.simulator.api.ignore;

public interface IgnoredTypesPredicate {

    /**
     * 是否该被忽略
     *
     * @param loader
     * @param internalClassName
     * @return
     */
    boolean test(ClassLoader loader, String internalClassName);

}
