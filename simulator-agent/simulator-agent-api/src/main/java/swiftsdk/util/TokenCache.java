//
// Source code recreated from a .class file by IntelliJ IDEA
// (powered by FernFlower decompiler)
//

package swiftsdk.util;

import org.apache.http.client.methods.HttpGet;
import swiftsdk.SwiftConfiguration;
import swiftsdk.errors.SfOssException;
import swiftsdk.factory.SFOSSFactory;
import swiftsdk.factory.SFOSSFactoryImpl;
import swiftsdk.info.TokenInfo;

public class TokenCache {
    private volatile String token;
    private volatile String tokenV2;
    private volatile long expiredTime = 0L;
    private SwiftConfiguration config;
    private static SFOSSFactory serviceFactory = new SFOSSFactoryImpl();
    private String version = "swift-java-api-2.0.15";

    public TokenCache() {
    }

    public TokenCache(SwiftConfiguration config) {
        this.config = config;
    }

    public synchronized void refreshToken() throws SfOssException {
        if (this.expiredTime <= System.currentTimeMillis()) {
            this.getTokenFromOSS();
        }
    }

    public String getV2Token(String container, String obj) throws Exception {
        if (container != null && !container.equals("") && obj != null && !obj.equals("")) {
            HttpGet get = new HttpGet(PropertiesUtil.getProperty("sfoss.server.url", this.config.getSfossServerUrl()) + "/auth/v1.0");
            if (this.config.getAk() != null) {
                get.setHeader("X-Storage-User", PropertiesUtil.getProperty("sfoss.ak", this.config.getAk()));
            } else {
                get.setHeader("X-Storage-User", PropertiesUtil.getProperty("sfoss.account", this.config.getRealAccount()) + ":" + PropertiesUtil.getProperty("sfoss.username", this.config.getUserName()));
            }

            get.setHeader("X-Storage-Pass", PropertiesUtil.getProperty("sfoss.user.key", this.config.getUserKey()));
            get.setHeader("User-Agent", this.version);
            if (SFOSSClientUtils.ifChinese(container)) {
                container = SFOSSClientUtils.pyURLEncoder(container);
            }

            get.setHeader("X-Auth-Container", container);
            if (SFOSSClientUtils.ifChinese(obj)) {
                obj = SFOSSClientUtils.pyURLEncoder(obj);
            }

            get.setHeader("X-Auth-Object", obj);
            return serviceFactory.getTokenService().getTokenV2(get, this.config.getSecure());
        } else {
            return obj;
        }
    }

    public String[] getTokenV2AndExpires(String container, String obj) throws Exception {
        if (container != null && !container.equals("") && obj != null && !obj.equals("")) {
            HttpGet get = new HttpGet(PropertiesUtil.getProperty("sfoss.server.url", this.config.getSfossServerUrl()) + "/auth/v1.0");
            if (this.config.getAk() != null) {
                get.setHeader("X-Storage-User", PropertiesUtil.getProperty("sfoss.ak", this.config.getAk()));
            } else {
                get.setHeader("X-Storage-User", PropertiesUtil.getProperty("sfoss.account", this.config.getRealAccount()) + ":" + PropertiesUtil.getProperty("sfoss.username", this.config.getUserName()));
            }

            get.setHeader("X-Storage-Pass", PropertiesUtil.getProperty("sfoss.user.key", this.config.getUserKey()));
            get.setHeader("User-Agent", this.version);
            if (SFOSSClientUtils.ifChinese(container)) {
                container = SFOSSClientUtils.pyURLEncoder(container);
            }

            get.setHeader("X-Auth-Container", container);
            if (SFOSSClientUtils.ifChinese(obj)) {
                obj = SFOSSClientUtils.pyURLEncoder(obj);
            }

            get.setHeader("X-Auth-Object", obj);
            return serviceFactory.getTokenService().getTokenV2AndExpires(get, this.config.getSecure());
        } else {
            return null;
        }
    }

    public void setTokenV2(String tokenV2) {
        this.tokenV2 = tokenV2;
    }

    public String getTokenV2() {
        return this.tokenV2;
    }

    public String getToken() throws SfOssException {
        if (this.expiredTime <= System.currentTimeMillis()) {
            this.refreshToken();
            return this.token;
        } else {
            return this.token;
        }
    }

    public void getTokenFromOSS() throws SfOssException {
        HttpGet get = new HttpGet(PropertiesUtil.getProperty("sfoss.server.url", this.config.getSfossServerUrl()) + "/auth/v1.0");
        if (this.config.getAk() != null) {
            get.setHeader("X-Storage-User", PropertiesUtil.getProperty("sfoss.ak", this.config.getAk()));
        } else {
            get.setHeader("X-Storage-User", PropertiesUtil.getProperty("sfoss.account", this.config.getRealAccount()) + ":" + PropertiesUtil.getProperty("sfoss.username", this.config.getUserName()));
        }

        get.setHeader("X-Storage-Pass", PropertiesUtil.getProperty("sfoss.user.key", this.config.getUserKey()));
        get.setHeader("User-Agent", this.version);
        TokenInfo tokenInfo = serviceFactory.getTokenService().getToken(get, this.config.getSecure());
        if (null != tokenInfo) {
            this.token = tokenInfo.getToken();
            this.expiredTime = tokenInfo.getExpiredTime();
        }

    }

    public synchronized void reLoadToken() throws SfOssException {
        this.getTokenFromOSS();
    }
}
