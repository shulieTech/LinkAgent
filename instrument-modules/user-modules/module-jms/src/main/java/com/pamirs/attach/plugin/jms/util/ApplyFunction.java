package com.pamirs.attach.plugin.jms.util;

public interface ApplyFunction<T> {
    T apply(Object... args) throws Throwable;
}
