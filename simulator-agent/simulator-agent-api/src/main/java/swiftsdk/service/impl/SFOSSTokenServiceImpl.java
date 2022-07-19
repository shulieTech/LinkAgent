//
// Source code recreated from a .class file by IntelliJ IDEA
// (powered by FernFlower decompiler)
//

package swiftsdk.service.impl;

import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpRequestBase;
import swiftsdk.errors.SfOssException;
import swiftsdk.factory.SFOSSMasterService;
import swiftsdk.http.ResponseResult;
import swiftsdk.info.TokenInfo;
import swiftsdk.service.SFOSSTokenService;

public class SFOSSTokenServiceImpl extends SFOSSMasterService implements SFOSSTokenService {
    private static volatile SFOSSTokenService sfossTokenService;

    private SFOSSTokenServiceImpl() {
    }

    public static SFOSSTokenService getInstance() {
        if (sfossTokenService == null) {
            Class var0 = SFOSSTokenServiceImpl.class;
            synchronized(SFOSSTokenServiceImpl.class) {
                if (sfossTokenService == null) {
                    sfossTokenService = new SFOSSTokenServiceImpl();
                }
            }
        }

        return sfossTokenService;
    }

    public TokenInfo getToken(HttpRequestBase method, boolean isHttps) throws SfOssException {
        TokenInfo tokenInfo = null;

        try {
            ResponseResult responseResult = super.doHttp(method, isHttps);
            CloseableHttpResponse response = responseResult.getResponse();
            int statusCode = responseResult.getStatusCode();
            if (200 == statusCode) {
                if (response.getFirstHeader("X-Auth-Token") != null) {
                    tokenInfo = new TokenInfo();
                    tokenInfo.setToken(response.getFirstHeader("X-Auth-Token").getValue());
                    String expires = response.getFirstHeader("X-Auth-Token-Expires").getValue();
                    tokenInfo.setExpiredTime(System.currentTimeMillis() + Long.parseLong(expires) * 1000L);
                }

                return tokenInfo;
            } else {
                throw new SfOssException(statusCode);
            }
        } catch (Exception var8) {
            throw new SfOssException(var8);
        }
    }

    public String getTokenV2(HttpRequestBase method, boolean isHttps) throws SfOssException {
        String tokenV2 = null;

        try {
            ResponseResult result = super.doHttp(method, isHttps);
            CloseableHttpResponse response = result.getResponse();
            int statusCode = response.getStatusLine().getStatusCode();
            if (200 == statusCode) {
                if (response.getFirstHeader("X-Auth-Token") != null) {
                    tokenV2 = response.getFirstHeader("X-Auth-Token").getValue();
                }

                return tokenV2;
            } else {
                throw new SfOssException(statusCode);
            }
        } catch (Exception var7) {
            throw new SfOssException(var7);
        }
    }

    public String[] getTokenV2AndExpires(HttpRequestBase method, boolean isHttps) throws SfOssException {
        String[] res = new String[2];

        try {
            ResponseResult result = super.doHttp(method, isHttps);
            CloseableHttpResponse response = result.getResponse();
            int statusCode = response.getStatusLine().getStatusCode();
            if (200 == statusCode) {
                if (response.getFirstHeader("X-Auth-Token") != null) {
                    res[0] = response.getFirstHeader("X-Auth-Token").getValue();
                    res[1] = response.getFirstHeader("X-Auth-Token-Expires").getValue();
                }

                return res;
            } else {
                throw new SfOssException(statusCode);
            }
        } catch (Exception var7) {
            throw new SfOssException(var7);
        }
    }
}
