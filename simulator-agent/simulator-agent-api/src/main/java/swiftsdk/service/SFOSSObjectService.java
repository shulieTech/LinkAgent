//
// Source code recreated from a .class file by IntelliJ IDEA
// (powered by FernFlower decompiler)
//

package swiftsdk.service;

import org.apache.http.HttpEntity;
import org.json.JSONObject;
import swiftsdk.SwiftConfiguration;
import swiftsdk.errors.SfOssException;

import java.io.InputStream;
import java.util.List;
import java.util.Map;

public interface SFOSSObjectService {
    List<String> getObjectList(String var1, String var2, SwiftConfiguration var3) throws SfOssException;

    List<String> getObjectList(String var1, String var2, Integer var3, String var4, SwiftConfiguration var5) throws SfOssException;

    Map<String, String> headObjectMeta(String var1, String var2, SwiftConfiguration var3) throws SfOssException;

    boolean postObjectMeta(String var1, Map<String, String> var2, String var3, SwiftConfiguration var4) throws SfOssException;

    boolean deleteContent(String var1, String var2, SwiftConfiguration var3) throws SfOssException;

    boolean baseDeleteObjects(String var1, HttpEntity var2, String var3, SwiftConfiguration var4) throws SfOssException;

    JSONObject DeleteObjects(String var1, HttpEntity var2, String var3, SwiftConfiguration var4) throws SfOssException;

    InputStream getObjectsZip(String var1, List<String> var2, String var3, SwiftConfiguration var4, boolean var5) throws SfOssException;

    byte[] downloadContent(String var1, String var2, SwiftConfiguration var3, Map<String, String> var4) throws SfOssException;

    String getTempUrl(String var1, String var2, String var3, String var4, String var5, Integer var6, String var7, SwiftConfiguration var8) throws SfOssException;

    JSONObject checkObjects(String var1, List<String> var2, String var3, SwiftConfiguration var4) throws SfOssException;

    boolean deleteObject(String var1, String var2, String var3, SwiftConfiguration var4) throws SfOssException;

    boolean doUpload(String var1, HttpEntity var2, String var3, SwiftConfiguration var4, Map<String, String> var5) throws SfOssException;

    boolean doUploadTar(String var1, HttpEntity var2, String var3, SwiftConfiguration var4, Map<String, String> var5) throws SfOssException;
}
