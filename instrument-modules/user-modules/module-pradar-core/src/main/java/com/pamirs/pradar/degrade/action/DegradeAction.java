package com.pamirs.pradar.degrade.action;

/**
 * @author jirenhe | jirenhe@shulie.io
 * @since 2022/08/31 3:08 PM
 */
public interface DegradeAction {

    void degrade(String format);

    void unDegrade();

    boolean isDegraded();

    boolean canDegrade();
}
