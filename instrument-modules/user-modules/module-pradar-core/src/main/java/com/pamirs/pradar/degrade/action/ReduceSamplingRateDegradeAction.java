package com.pamirs.pradar.degrade.action;

import com.pamirs.pradar.ErrorTypeEnum;
import com.pamirs.pradar.pressurement.agent.shared.service.ErrorReporter;
import com.pamirs.pradar.pressurement.agent.shared.service.SimulatorDynamicConfig;

/**
 * @author jirenhe | jirenhe@shulie.io
 * @since 2022/08/31 4:30 PM
 */
public enum ReduceSamplingRateDegradeAction implements DegradeAction {

    INSTANCE;

    @Override
    public void degrade(String msg) {
        SimulatorDynamicConfig.triggerDegrade(msg);
        ErrorReporter.buildError()
                .setErrorType(ErrorTypeEnum.DEGRADE)
                .setErrorCode("degrade-0001")
                .setMessage("资源紧张触发降级,采样率降低为9999")
                .setDetail(msg)
                .report();
    }

    @Override
    public void unDegrade() {
        SimulatorDynamicConfig.resetDegradeStatus();
        ErrorReporter.buildError()
                .setErrorType(ErrorTypeEnum.DEGRADE)
                .setErrorCode("degrade-0002")
                .setMessage("资源充足降级恢复，采样率恢复")
                .setDetail("")
                .report();
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
