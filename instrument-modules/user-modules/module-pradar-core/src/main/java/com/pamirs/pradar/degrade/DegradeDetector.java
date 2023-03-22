package com.pamirs.pradar.degrade;


import com.pamirs.pradar.degrade.action.DegradeAction;

/**
 * @author jirenhe | jirenhe@shulie.io
 * @since 2022/08/31 2:41 PM
 */
public interface DegradeDetector {

    void startDetect(DegradeAction degradeAction);

    void stopDetect();

}
