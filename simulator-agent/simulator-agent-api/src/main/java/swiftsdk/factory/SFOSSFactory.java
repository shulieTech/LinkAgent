//
// Source code recreated from a .class file by IntelliJ IDEA
// (powered by FernFlower decompiler)
//

package swiftsdk.factory;

import swiftsdk.service.SFOSSAccountService;
import swiftsdk.service.SFOSSContainerService;
import swiftsdk.service.SFOSSObjectService;
import swiftsdk.service.SFOSSTokenService;

public interface SFOSSFactory {
    SFOSSAccountService getAccountService();

    SFOSSContainerService getContainerService();

    SFOSSObjectService getObjectService();

    SFOSSTokenService getTokenService();
}
