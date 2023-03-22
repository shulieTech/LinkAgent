package com.pamirs.pradar.degrade.action;

import com.pamirs.pradar.ErrorTypeEnum;
import com.pamirs.pradar.Pradar;
import com.pamirs.pradar.PradarService;
import com.pamirs.pradar.PradarSwitcher;
import com.pamirs.pradar.pressurement.agent.event.impl.SilenceSwitchOffEvent;
import com.pamirs.pradar.pressurement.agent.event.impl.SilenceSwitchOnEvent;
import com.pamirs.pradar.pressurement.agent.shared.service.ErrorReporter;
import com.pamirs.pradar.pressurement.agent.shared.service.EventRouter;
import com.pamirs.pradar.pressurement.agent.shared.service.SimulatorDynamicConfig;

public enum SilenceDegradeAction implements DegradeAction {

    INSTANCE;

    @Override
    public void degrade(String message) {
        SilenceSwitchOnEvent event = new SilenceSwitchOnEvent(this);
        EventRouter.router().publish(event);
        PradarSwitcher.turnSilenceSwitchOn();

        Pradar.setSilenceDegradeStatus(true);

        ErrorReporter.buildError()
                .setErrorType(ErrorTypeEnum.DEGRADE)
                .setErrorCode("degrade-0001")
                .setMessage("资源紧张触发降级,静默探针")
                .setDetail(message)
                .report();
    }

    @Override
    public void unDegrade() {
        SilenceSwitchOffEvent event = new SilenceSwitchOffEvent(this);
        EventRouter.router().publish(event);
        PradarSwitcher.turnSilenceSwitchOff();

        Pradar.setSilenceDegradeStatus(false);

        ErrorReporter.buildError()
                .setErrorType(ErrorTypeEnum.DEGRADE)
                .setErrorCode("degrade-0002")
                .setMessage("资源充足降级恢复，静默恢复")
                .setDetail("")
                .report();
    }

    @Override
    public boolean isDegraded() {
        return Pradar.isSilenceDegraded();
    }

    @Override
    public boolean canDegrade() {
        return !PradarService.isSilence();
    }
}
