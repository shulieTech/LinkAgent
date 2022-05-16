//
// Source code recreated from a .class file by IntelliJ IDEA
// (powered by FernFlower decompiler)
//

package swiftsdk.service;

import swiftsdk.SwiftConfiguration;
import swiftsdk.errors.SfOssException;

import java.util.List;
import java.util.Map;

public interface SFOSSContainerService {
    List<String> getContainerList(String var1, String var2, SwiftConfiguration var3) throws SfOssException;

    boolean createContainer(String var1, String var2, SwiftConfiguration var3) throws SfOssException;

    boolean postContainerMeta(String var1, Map<String, String> var2, String var3, SwiftConfiguration var4) throws SfOssException;

    Map<String, String> headContainerMeta(String var1, String var2, SwiftConfiguration var3) throws SfOssException;

    boolean deleteContainer(String var1, String var2, boolean var3, String var4, SwiftConfiguration var5) throws SfOssException;

    boolean clearContainer(String var1, String var2, String var3, SwiftConfiguration var4) throws SfOssException;

    boolean checkContainer(String var1, String var2, SwiftConfiguration var3) throws SfOssException;
}
