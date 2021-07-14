package com.pamirs.attach.plugin.common.datasource.hbaseserver;

/**
 * Create by xuyh at 2020/3/22 12:57.
 *
 * @author xuyh
 */
public class InvokeSwitcher {
    private static ThreadLocal<Boolean> SWITCHER = new ThreadLocal<Boolean>() {
        @Override
        protected Boolean initialValue() {
            return Boolean.FALSE;
        }
    };

    public static boolean status() {
        return SWITCHER.get();
    }

    public static void on() {
        SWITCHER.set(Boolean.TRUE);
    }


    public static void remove() {
        SWITCHER.set(Boolean.FALSE);
    }
}
