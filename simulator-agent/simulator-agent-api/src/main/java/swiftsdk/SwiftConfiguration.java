//
// Source code recreated from a .class file by IntelliJ IDEA
// (powered by FernFlower decompiler)
//

package swiftsdk;

import sun.misc.BASE64Decoder;
import swiftsdk.http.TimeOutOption;
import swiftsdk.util.PropertiesUtil;

public class SwiftConfiguration {
    private String sfossServerUrl;
    private String userName;
    private String userKey;
    private String account;
    private String ak = null;
    private boolean secure = false;
    private String defaultContainer;
    private TimeOutOption timeOutOption = null;
    private String useeAccount = null;

    public SwiftConfiguration() {
    }

    public void setUseAccount(String useeAccount) {
        this.useeAccount = useeAccount;
    }

    public String getUseAccount() {
        return this.useeAccount;
    }

    public String getSfossServerUrl() {
        return this.sfossServerUrl;
    }

    public void setTimeOutOption(TimeOutOption t) {
        this.timeOutOption = t;
    }

    public TimeOutOption getTimeOutOption() {
        return this.timeOutOption;
    }

    public void setSfossServerUrl(String sfossServerUrl) {
        if (PropertiesUtil.getProperty("sfoss.server.url", sfossServerUrl).contains("http://")) {
            this.secure = false;
            this.sfossServerUrl = sfossServerUrl;
        } else if (PropertiesUtil.getProperty("sfoss.server.url", sfossServerUrl).contains("https://")) {
            this.secure = true;
            this.sfossServerUrl = sfossServerUrl;
        } else {
            this.secure = false;
            this.sfossServerUrl = "http://" + PropertiesUtil.getProperty("sfoss.server.url", sfossServerUrl);
        }

    }

    public String getUserName() {
        return this.userName;
    }

    public void setUserName(String userName) {
        this.userName = userName.replace(" ", "");
    }

    public String getUserKey() {
        return this.userKey;
    }

    public void setUserKey(String userkey) {
        this.userKey = userkey.replace(" ", "");
    }

    public String getAccount() {
        if (this.useeAccount != null) {
            return this.useeAccount;
        } else {
            return !PropertiesUtil.getProperty("sfoss.account", "").equals("") ? PropertiesUtil.getProperty("sfoss.account", "") : this.account;
        }
    }

    public String getRealAccount() {
        return this.account;
    }

    public void setAccount(String account) {
        this.account = account.replace(" ", "");
    }

    public String getDefaultContainer() {
        return this.defaultContainer;
    }

    public void setDefaultContainer(String defaultContainer) {
        this.defaultContainer = defaultContainer;
    }

    public String getAk() {
        return this.ak;
    }

    public void setAk(String ak) throws Exception {
        ak = ak.replace(" ", "");
        if (ak.contains(":")) {
            String[] ccs = ak.split(":");
            this.account = ccs[0];
            this.userName = ccs[1];
        } else {
            BASE64Decoder decoder = new BASE64Decoder();

            try {
                byte[] b = decoder.decodeBuffer(ak);
                String str = new String(b, "utf-8");
                if (!str.contains(":")) {
                    throw new Exception("Error Ak");
                }

                String[] ccs = str.split(":");
                this.account = ccs[0];
                this.userName = ccs[1];
            } catch (Exception var6) {
                throw var6;
            }
        }

        this.ak = ak;
    }

    public boolean getSecure() {
        return this.secure;
    }

    public String toString() {
        return "Configuration [sfossServerUrl=" + this.sfossServerUrl + ", userName=" + this.userName + ", UserKey=" + this.userKey + ", account=" + this.account + ", ak=" + this.ak + ", rootContainer=" + this.defaultContainer + ", secure=" + this.secure + "]";
    }
}
