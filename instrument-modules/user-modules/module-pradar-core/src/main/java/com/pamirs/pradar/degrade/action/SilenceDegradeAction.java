package com.pamirs.pradar.degrade.action;

import com.pamirs.pradar.pressurement.agent.shared.service.SimulatorDynamicConfig;

/**
 * @author jirenhe | jirenhe@shulie.io
 * @since 2022/08/31 4:30 PM
 */
public enum SilenceDegradeAction implements DegradeAction {

    INSTANCE;

    @Override
    public void degrade(String msg) {
        SimulatorDynamicConfig.triggerDegrade(msg);
    }

    @Override
    public void unDegrade() {
        SimulatorDynamicConfig.resetDegradeStatus();
    }

    @Override
    public boolean isDegraded() {
        return SimulatorDynamicConfig.isDegraded();
    }

    @Override
    public boolean canDegrade() {
        return !SimulatorDynamicConfig.isDegraded();
    }
}
