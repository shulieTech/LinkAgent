//
// Source code recreated from a .class file by IntelliJ IDEA
// (powered by FernFlower decompiler)
//

package swiftsdk.service;

import swiftsdk.SwiftConfiguration;
import swiftsdk.errors.SfOssException;

import java.util.Map;

public interface SFOSSAccountService {
    Map<String, String> headAccountMeta(String var1, String var2, SwiftConfiguration var3) throws SfOssException;

    boolean postAccountMeta(String var1, Map<String, String> var2, String var3, SwiftConfiguration var4) throws SfOssException;
}
