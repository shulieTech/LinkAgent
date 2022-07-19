//
// Source code recreated from a .class file by IntelliJ IDEA
// (powered by FernFlower decompiler)
//

package swiftsdk.service;

import org.apache.http.client.methods.HttpRequestBase;
import swiftsdk.errors.SfOssException;
import swiftsdk.info.TokenInfo;

public interface SFOSSTokenService {
    TokenInfo getToken(HttpRequestBase var1, boolean var2) throws SfOssException;

    String getTokenV2(HttpRequestBase var1, boolean var2) throws SfOssException;

    String[] getTokenV2AndExpires(HttpRequestBase var1, boolean var2) throws SfOssException;
}
