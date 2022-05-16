//
// Source code recreated from a .class file by IntelliJ IDEA
// (powered by FernFlower decompiler)
//

package swiftsdk.util;

import swiftsdk.SwiftConfiguration;
import swiftsdk.errors.ErrorCodeMessageEnum;
import swiftsdk.errors.SfOssException;

import javax.crypto.Mac;
import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import java.net.URLEncoder;

public class SFOSSClientUtils {
    public static final long MAX_SINGLEOBJECT_SIZE = 42949672960L;
    public static final int MAX_MULTIPART_COUNT = 1000;
    public static final int MIN_MULTIPART_SIZE = 5242880;
    public static final int MAX_MULTIPART_SIZE = 134217728;
    public static final int DEFAULT_MULTIPART_SIZE = 52428800;

    public SFOSSClientUtils() {
    }

    public static String getUrl(SwiftConfiguration config, String version) throws SfOssException {
        try {
            if (!version.equals("v1") && !version.equals("v1.2")) {
                throw new SfOssException(ErrorCodeMessageEnum.URL_ERROR.getIndex(), ErrorCodeMessageEnum.URL_ERROR.getMessage() + version);
            } else {
                return PropertiesUtil.getProperty("sfoss.server.url", config.getSfossServerUrl()) + "/" + version + "/AUTH_" + config.getAccount();
            }
        } catch (Exception var3) {
            throw new SfOssException(var3);
        }
    }

    public static String getUrl(SwiftConfiguration config, String version, String containerName) throws SfOssException {
        try {
            if ((version.equals("v1") || version.equals("v1.2")) && containerName != null && !containerName.equals("")) {
                String container = URLEncoder.encode(containerName, "UTF-8");
                container = container.replace("+", "%20");
                return PropertiesUtil.getProperty("sfoss.server.url", config.getSfossServerUrl()) + "/" + version + "/AUTH_" + config.getAccount() + "/" + container;
            } else {
                throw new SfOssException(ErrorCodeMessageEnum.URL_ERROR.getIndex(), ErrorCodeMessageEnum.URL_ERROR.getMessage() + version);
            }
        } catch (Exception var4) {
            throw new SfOssException(var4);
        }
    }

    public static String getUrl(SwiftConfiguration config, String version, String containerName, String objectName) throws SfOssException {
        try {
            if ((version.equals("v1") || version.equals("v1.2")) && containerName != null && !containerName.equals("") && objectName != null && !objectName.equals("")) {
                String container = URLEncoder.encode(containerName, "UTF-8");
                container = container.replace("+", "%20");
                String object = URLEncoder.encode(objectName, "UTF-8");
                object = object.replace("+", "%20");
                return PropertiesUtil.getProperty("sfoss.server.url", config.getSfossServerUrl()) + "/" + version + "/AUTH_" + config.getAccount() + "/" + container + "/" + object;
            } else {
                throw new SfOssException(ErrorCodeMessageEnum.URL_ERROR.getIndex(), ErrorCodeMessageEnum.URL_ERROR.getMessage() + version);
            }
        } catch (Exception var6) {
            throw new SfOssException(var6);
        }
    }

    public static String getUrlByPath(SwiftConfiguration config, String version, String path) throws SfOssException {
        try {
            return PropertiesUtil.getProperty("sfoss.server.url", config.getSfossServerUrl()) + "/" + version + "/" + path;
        } catch (Exception var4) {
            throw new SfOssException(var4);
        }
    }

    public static byte[] encryptHMAC(byte[] data, String key) throws Exception {
        SecretKey secretKey = new SecretKeySpec(key.getBytes(), "HmacSHA1");
        Mac mac = Mac.getInstance(secretKey.getAlgorithm());
        mac.init(secretKey);
        return mac.doFinal(data);
    }

    public static String pyURLEncoder(String string) throws SfOssException {
        try {
            string = URLEncoder.encode(string, "UTF-8");
            string = string.replace("+", "%20");
            string = string.replace("*", "%2A");
            string = string.replace("%2F", "/");
            return string;
        } catch (Exception var2) {
            throw new SfOssException(var2);
        }
    }

    protected static boolean ifChinese(String string) {
        int length = string.length();
        if (length > 1024) {
            return false;
        } else {
            for(int i = 0; i < length; ++i) {
                int r = string.charAt(i);
                if ((r < 'A' || r > 'Z') && (r < 'a' || r > 'z') && (r < '0' || r > '9') && r != '.' && r != '-' && r != '_' && r != '%') {
                    return true;
                }
            }

            return false;
        }
    }

    public static String byteArrayToHexString(byte[] b) {
        StringBuffer sb = new StringBuffer(b.length * 2);

        for(int i = 0; i < b.length; ++i) {
            int v = b[i] & 255;
            if (v < 16) {
                sb.append('0');
            }

            sb.append(Integer.toHexString(v));
        }

        return sb.toString();
    }

    public static int[] calculateMultipartSize(long size, int partSize) throws SfOssException {
        if (size > 42949672960L) {
            throw new SfOssException("size " + size + " is greater than allowed size 40GB");
        } else {
            double partCount = Math.ceil((double)size / (double)partSize);
            if (partCount > 1000.0D) {
                throw new SfOssException("partCount " + partCount + " is greater than allowed 1000");
            } else {
                double lastPartSize = (double)partSize - ((double)partSize * partCount - (double)size);
                if (lastPartSize == 0.0D) {
                    lastPartSize = (double)partSize;
                }

                return new int[]{(int)partCount, (int)lastPartSize};
            }
        }
    }
}
