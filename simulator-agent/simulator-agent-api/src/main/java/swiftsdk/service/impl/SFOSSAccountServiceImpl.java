//
// Source code recreated from a .class file by IntelliJ IDEA
// (powered by FernFlower decompiler)
//

package swiftsdk.service.impl;

import org.apache.http.Header;
import org.apache.http.client.methods.HttpHead;
import org.apache.http.client.methods.HttpPost;
import swiftsdk.SwiftConfiguration;
import swiftsdk.errors.ErrorCodeMessageEnum;
import swiftsdk.errors.SfOssException;
import swiftsdk.factory.SFOSSMasterService;
import swiftsdk.http.ResponseResult;
import swiftsdk.service.SFOSSAccountService;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

public class SFOSSAccountServiceImpl extends SFOSSMasterService implements SFOSSAccountService {
    private static volatile SFOSSAccountService sfossAccountService;

    private SFOSSAccountServiceImpl() {
    }

    public static SFOSSAccountService getInstance() {
        if (sfossAccountService == null) {
            Class var0 = SFOSSAccountServiceImpl.class;
            synchronized(SFOSSAccountServiceImpl.class) {
                if (sfossAccountService == null) {
                    sfossAccountService = new SFOSSAccountServiceImpl();
                }
            }
        }

        return sfossAccountService;
    }

    public Map<String, String> headAccountMeta(String url, String token, SwiftConfiguration conf) throws SfOssException {
        HttpHead head = new HttpHead(url);
        HashMap resultMap = new HashMap();
        try {
            ResponseResult responseResult = super.doHttp(head, token, conf.getSecure(), conf.getTimeOutOption());
            int statusCode = responseResult.getStatusCode();
            if (200 != statusCode && 204 != statusCode) {
                throw new SfOssException(statusCode, url);
            } else {
                Header[] headers = responseResult.getResponse().getAllHeaders();
                Header[] var9 = headers;
                int var10 = headers.length;

                for(int var11 = 0; var11 < var10; ++var11) {
                    Header header = var9[var11];
                    String key = header.getName();
                    if (key.contains("X-Account-") || key.contains("X-Timestamp") || key.contains("Last-Modified")) {
                        resultMap.put(key, header.getValue());
                    }
                }

                return resultMap;
            }
        } catch (Exception var14) {
            throw new SfOssException(var14);
        }
    }

    public boolean postAccountMeta(String url, Map<String, String> headers, String token, SwiftConfiguration conf) throws SfOssException {
        HttpPost post = new HttpPost(url);

        String newkey;
        String value;
        for(Iterator var6 = headers.keySet().iterator(); var6.hasNext(); post.addHeader(newkey, value)) {
            String key = (String)var6.next();
            newkey = key.replace(" ", "");
            value = (String)headers.get(key);
            if (value == null) {
                value = "";
            }
        }

        try {
            ResponseResult responseResult = super.doHttp(post, token, conf.getSecure(), conf.getTimeOutOption());
            int statusCode = responseResult.getStatusCode();
            if (statusCode != 204 && statusCode != 202 && statusCode != 200) {
                if (statusCode == 404) {
                    throw new SfOssException(ErrorCodeMessageEnum.RESOURCE_NOT_FIND.getIndex(), ErrorCodeMessageEnum.RESOURCE_NOT_FIND.getMessage());
                } else {
                    throw new SfOssException(statusCode, url);
                }
            } else {
                return true;
            }
        } catch (Exception var10) {
            throw new SfOssException(var10);
        }
    }
}
