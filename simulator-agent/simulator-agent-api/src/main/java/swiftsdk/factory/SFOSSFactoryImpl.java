//
// Source code recreated from a .class file by IntelliJ IDEA
// (powered by FernFlower decompiler)
//

package swiftsdk.factory;

import swiftsdk.service.SFOSSAccountService;
import swiftsdk.service.SFOSSContainerService;
import swiftsdk.service.SFOSSObjectService;
import swiftsdk.service.SFOSSTokenService;
import swiftsdk.service.impl.SFOSSAccountServiceImpl;
import swiftsdk.service.impl.SFOSSContainerServiceImpl;
import swiftsdk.service.impl.SFOSSObjectServiceImpl;
import swiftsdk.service.impl.SFOSSTokenServiceImpl;

public class SFOSSFactoryImpl implements SFOSSFactory {
    public SFOSSFactoryImpl() {
    }

    public SFOSSAccountService getAccountService() {
        return SFOSSAccountServiceImpl.getInstance();
    }

    public SFOSSContainerService getContainerService() {
        return SFOSSContainerServiceImpl.getInstance();
    }

    public SFOSSObjectService getObjectService() {
        return SFOSSObjectServiceImpl.getInstance();
    }

    public SFOSSTokenService getTokenService() {
        return SFOSSTokenServiceImpl.getInstance();
    }
}
