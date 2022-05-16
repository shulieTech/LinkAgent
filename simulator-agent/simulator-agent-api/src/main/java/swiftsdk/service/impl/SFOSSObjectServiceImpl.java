//
// Source code recreated from a .class file by IntelliJ IDEA
// (powered by FernFlower decompiler)
//

package swiftsdk.service.impl;

import org.apache.http.Header;
import org.apache.http.HttpEntity;
import org.apache.http.client.methods.*;
import org.apache.http.entity.ByteArrayEntity;
import org.json.JSONObject;
import swiftsdk.SwiftConfiguration;
import swiftsdk.errors.ErrorCodeMessageEnum;
import swiftsdk.errors.SfOssException;
import swiftsdk.factory.SFOSSMasterService;
import swiftsdk.http.ResponseResult;
import swiftsdk.service.SFOSSObjectService;
import swiftsdk.util.SFOSSClientUtils;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.net.URLEncoder;
import java.util.*;

public class SFOSSObjectServiceImpl extends SFOSSMasterService implements SFOSSObjectService {
    private static volatile SFOSSObjectService sfossObjectService;

    private SFOSSObjectServiceImpl() {
    }

    public static SFOSSObjectService getInstance() {
        if (sfossObjectService == null) {
            Class var0 = SFOSSObjectServiceImpl.class;
            synchronized(SFOSSObjectServiceImpl.class) {
                if (sfossObjectService == null) {
                    sfossObjectService = new SFOSSObjectServiceImpl();
                }
            }
        }

        return sfossObjectService;
    }

    public List<String> getObjectList(String url, String token, SwiftConfiguration conf) throws SfOssException {
        HttpGet get = new HttpGet(url);

        try {
            ResponseResult response = super.doHttp(get, token, conf.getSecure(), conf.getTimeOutOption());
            int statusCode = response.getStatusCode();
            if (statusCode != 200 && statusCode != 204) {
                if (statusCode == 404) {
                    throw new SfOssException(ErrorCodeMessageEnum.RESOURCE_NOT_FIND.getIndex(), ErrorCodeMessageEnum.RESOURCE_NOT_FIND.getMessage());
                } else {
                    throw new SfOssException(statusCode, url);
                }
            } else {
                String content = response.getResponseStr();
                if (content == null) {
                    return new ArrayList();
                } else {
                    String[] strs = content.split("\n");
                    List<String> containerList = Arrays.asList(strs);
                    List<String> arrayList = new ArrayList(containerList);
                    if (arrayList.get(arrayList.size() - 1) == null || ((String)arrayList.get(arrayList.size() - 1)).equals("") || ((String)arrayList.get(arrayList.size() - 1)).equals("response empty content")) {
                        arrayList.remove(arrayList.size() - 1);
                    }

                    return arrayList;
                }
            }
        } catch (Exception var11) {
            throw new SfOssException(var11);
        }
    }

    public List<String> getObjectList(String url, String beginName, Integer limit, String token, SwiftConfiguration conf) throws SfOssException {
        if (limit == null || limit > 10000) {
            limit = 10000;
        }

        String resultUrl = url + "?format=plain&limit=" + limit;
        if (beginName != null && !beginName.isEmpty()) {
            resultUrl = resultUrl + "&marker=" + SFOSSClientUtils.pyURLEncoder(beginName);
        }

        return this.getObjectList(resultUrl, token, conf);
    }

    public Map<String, String> headObjectMeta(String url, String token, SwiftConfiguration conf) throws SfOssException {
        HttpHead head = new HttpHead(url);

        try {
            ResponseResult response = super.doHttp(head, token, conf.getSecure(), conf.getTimeOutOption());
            int statusCode = response.getStatusCode();
            Map<String, String> resultMap = new HashMap();
            if (statusCode != 200 && statusCode != 204) {
                if (statusCode == 404) {
                    throw new SfOssException(ErrorCodeMessageEnum.RESOURCE_NOT_FIND.getIndex(), ErrorCodeMessageEnum.RESOURCE_NOT_FIND.getMessage());
                } else {
                    throw new SfOssException(statusCode, url);
                }
            } else {
                Header[] headers = response.getResponse().getAllHeaders();
                Header[] var9 = response.getResponse().getAllHeaders();
                int var10 = var9.length;

                for(int var11 = 0; var11 < var10; ++var11) {
                    Header header = var9[var11];
                    String key = header.getName();
                    String value = header.getValue();
                    if (key.contains("X-Object-") || key.contains("Content-") || key.contains("X-Timestamp") || key.contains("X-Delete-") || key.contains("Last-Modified") || key.contains("Etag") || key.contains("X-Static-Large-Object")) {
                        resultMap.put(key, value);
                    }
                }

                return resultMap;
            }
        } catch (Exception var15) {
            throw new SfOssException(var15);
        }
    }

    public boolean postObjectMeta(String url, Map<String, String> headers, String token, SwiftConfiguration conf) throws SfOssException {
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
            ResponseResult response = super.doHttp(post, token, conf.getSecure(), conf.getTimeOutOption());
            int statusCode = response.getStatusCode();
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

    public boolean deleteContent(String url, String token, SwiftConfiguration conf) throws SfOssException {
        HttpDelete delete = new HttpDelete(url);

        try {
            ResponseResult response = super.doHttp(delete, token, conf.getSecure(), conf.getTimeOutOption());
            int statusCode = response.getStatusCode();
            if (statusCode != 204 && statusCode != 200 && statusCode != 404 && statusCode != 202) {
                throw new SfOssException(statusCode, url);
            } else {
                return true;
            }
        } catch (Exception var7) {
            throw new SfOssException(var7);
        }
    }

    public boolean baseDeleteObjects(String url, HttpEntity httpEntity, String token, SwiftConfiguration conf) throws SfOssException {
        try {
            HttpPost post = new HttpPost(url);
            post.setEntity(httpEntity);
            ResponseResult response = super.doHttp(post, token, conf.getSecure(), conf.getTimeOutOption());
            int statusCode = response.getStatusCode();
            if (statusCode == 200) {
                return true;
            } else if (statusCode == 404) {
                throw new SfOssException(ErrorCodeMessageEnum.RESOURCE_NOT_FIND.getIndex(), ErrorCodeMessageEnum.RESOURCE_NOT_FIND.getMessage());
            } else {
                throw new SfOssException(statusCode, url);
            }
        } catch (Exception var8) {
            throw new SfOssException(var8);
        }
    }

    public JSONObject DeleteObjects(String url, HttpEntity httpEntity, String token, SwiftConfiguration conf) throws SfOssException {
        try {
            HttpPost post = new HttpPost(url);
            post.setEntity(httpEntity);
            post.addHeader("Accept", "application/json");
            ResponseResult response = super.doHttp(post, token, conf.getSecure(), conf.getTimeOutOption());
            int statusCode = response.getStatusCode();
            if (statusCode == 200) {
                String s = response.getResponseStr();
                return new JSONObject(new String(s));
            } else if (statusCode == 404) {
                throw new SfOssException(ErrorCodeMessageEnum.RESOURCE_NOT_FIND.getIndex(), ErrorCodeMessageEnum.RESOURCE_NOT_FIND.getMessage());
            } else {
                throw new SfOssException(statusCode, url);
            }
        } catch (Exception var9) {
            throw new SfOssException(var9);
        }
    }

    public InputStream getObjectsZip(String url, List<String> objecList, String token, SwiftConfiguration conf, boolean UrlEnconde) throws SfOssException {
        try {
            JSONObject json = new JSONObject();

            for(int i = 0; i < objecList.size(); ++i) {
                json.put((String)objecList.get(i), "0");
            }

            HttpPost post = new HttpPost(url);
            post.addHeader("X-Object-Image-Processing-param", "NoOptions");
            if (UrlEnconde) {
                post.addHeader("X-Need-Encode", "true");
            }

            post.setEntity(new ByteArrayEntity(json.toString().getBytes("UTF-8")));
            ResponseResult response = super.doHttpForByteResult(post, token, conf.getSecure(), conf.getTimeOutOption());
            int statusCode = response.getStatusCode();
            byte[] res = response.getByteArray();
            if (statusCode != 200 && statusCode != 204) {
                if (statusCode == 404) {
                    throw new SfOssException(ErrorCodeMessageEnum.RESOURCE_NOT_FIND.getIndex(), ErrorCodeMessageEnum.RESOURCE_NOT_FIND.getMessage());
                } else {
                    throw new SfOssException(statusCode, url);
                }
            } else {
                return new ByteArrayInputStream(res);
            }
        } catch (Exception var11) {
            throw new SfOssException(var11);
        }
    }

    public byte[] downloadContent(String url, String token, SwiftConfiguration conf, Map<String, String> headers) throws SfOssException {
        HttpGet get = new HttpGet(url);

        String newkey;
        String value;
        for(Iterator var6 = headers.keySet().iterator(); var6.hasNext(); get.addHeader(newkey, value)) {
            String key = (String)var6.next();
            newkey = key.replace(" ", "");
            value = (String)headers.get(key);
            if (value == null) {
                value = "";
            }
        }

        try {
            ResponseResult response = super.doHttpForByteResult(get, token, conf.getSecure(), conf.getTimeOutOption());
            int statusCode = response.getStatusCode();
            if (statusCode != 200 && statusCode != 202 && statusCode != 204 && statusCode != 206) {
                if (statusCode == 404) {
                    throw new SfOssException(ErrorCodeMessageEnum.RESOURCE_NOT_FIND.getIndex(), ErrorCodeMessageEnum.RESOURCE_NOT_FIND.getMessage());
                } else {
                    throw new SfOssException(statusCode, url);
                }
            } else {
                byte[] byteArray = response.getByteArray();
                return byteArray;
            }
        } catch (Exception var10) {
            throw new SfOssException(var10);
        }
    }

    public String getTempUrl(String account, String container, String url, String object, String method, Integer durationTime, String token, SwiftConfiguration conf) throws SfOssException {
        try {
            Map<String, String> containerHeaders = SFOSSContainerServiceImpl.getInstance().headContainerMeta(url + "/" + container, token, conf);
            String key = (String)containerHeaders.get("X-Container-Meta-Temp-Url-Key");
            String key2 = (String)containerHeaders.get("X-Container-Meta-Temp-Url-Key-2");
            if (key == null && key2 == null) {
                Map<String, String> accountHeaders = SFOSSAccountServiceImpl.getInstance().headAccountMeta(url, token, conf);
                key = (String)accountHeaders.get("X-Account-Meta-Temp-Url-Key");
                key2 = (String)accountHeaders.get("X-Account-Meta-Temp-Url-Key-2");
                if (key == null && key2 == null) {
                    throw new SfOssException("没有设置Tempurl Key");
                }

                if (key == null) {
                    key = key2;
                }
            } else if (key == null) {
                key = key2;
            }

            String expiresTimeStamp = String.valueOf(System.currentTimeMillis() / 1000L + (long)durationTime);
            String inputStr = method.toUpperCase() + "\n" + expiresTimeStamp + "\n/v1/AUTH_" + account + "/" + container + "/" + object;
            byte[] inputData = inputStr.getBytes("UTF-8");

            String sig;
            try {
                sig = SFOSSClientUtils.byteArrayToHexString(SFOSSClientUtils.encryptHMAC(inputData, key));
            } catch (Exception var17) {
                throw new SfOssException(var17);
            }

            return "/v1/AUTH_" + account + "/" + container + "/" + object + "?temp_url_sig=" + sig + "&temp_url_expires=" + expiresTimeStamp;
        } catch (Exception var18) {
            throw new SfOssException(var18);
        }
    }

    public JSONObject checkObjects(String url, List<String> objecList, String token, SwiftConfiguration conf) throws SfOssException {
        List<String> exisetObject = new ArrayList();
        List<String> NotExisetObject = new ArrayList();
        if (objecList.size() > 20000) {
            throw new SfOssException("The number of objects exceeds the maximum limit");
        } else {
            for(int i = 0; i < objecList.size(); ++i) {
                try {
                    HttpHead head = new HttpHead(url + "/" + SFOSSClientUtils.pyURLEncoder((String)objecList.get(i)));

                    try {
                        ResponseResult response = super.doHttp(head, token, conf.getSecure(), conf.getTimeOutOption());
                        int statusCode = response.getStatusCode();
                        if (statusCode != 200 && statusCode != 204) {
                            if (statusCode != 404) {
                                throw new SfOssException(statusCode, url);
                            }

                            NotExisetObject.add(objecList.get(i));
                        } else {
                            exisetObject.add(objecList.get(i));
                        }
                    } catch (Exception var11) {
                        throw var11;
                    }
                } catch (SfOssException var12) {
                    throw var12;
                } catch (Exception var13) {
                    throw new SfOssException(var13);
                }
            }

            JSONObject obj = new JSONObject();
            obj.put("existObject", exisetObject);
            obj.put("NotExistObject", NotExisetObject);
            return obj;
        }
    }

    public boolean deleteObject(String url, String object, String token, SwiftConfiguration conf) throws SfOssException {
        String objectName = "";

        try {
            objectName = URLEncoder.encode(object, "UTF-8");
            objectName = objectName.replace("+", "%20");
        } catch (Exception var8) {
            throw new SfOssException("非法对象名");
        }

        url = url + "/" + objectName;
        Map<String, String> objectHeaders = this.headObjectMeta(url, token, conf);
        String mf = (String)objectHeaders.get("X-Static-Large-Object");
        if (mf != null && !mf.equals("")) {
            url = url + "?multipart-manifest=delete";
            return this.deleteContent(url, token, conf);
        } else {
            return this.deleteContent(url, token, conf);
        }
    }

    public boolean doUpload(String url, HttpEntity entity, String token, SwiftConfiguration conf, Map<String, String> headers) throws SfOssException {
        HttpPut put = new HttpPut(url);
        put.setEntity(entity);

        String newkey;
        String value;
        for(Iterator var7 = headers.keySet().iterator(); var7.hasNext(); put.addHeader(newkey, value)) {
            String key = (String)var7.next();
            newkey = key.replace(" ", "");
            value = (String)headers.get(key);
            if (value == null) {
                value = "";
            }
        }

        try {
            ResponseResult response = super.doHttp(put, token, conf.getSecure(), conf.getTimeOutOption());
            int statusCode = response.getStatusCode();
            if (statusCode != 200 && statusCode != 202 && statusCode != 201) {
                if (statusCode == 404) {
                    throw new SfOssException(ErrorCodeMessageEnum.RESOURCE_NOT_FIND.getIndex(), ErrorCodeMessageEnum.RESOURCE_NOT_FIND.getMessage());
                } else {
                    throw new SfOssException(statusCode, url);
                }
            } else {
                return true;
            }
        } catch (Exception var11) {
            throw new SfOssException(var11);
        }
    }

    public boolean doUploadTar(String url, HttpEntity entity, String token, SwiftConfiguration conf, Map<String, String> headers) throws SfOssException {
        HttpPut put = new HttpPut(url);
        put.setEntity(entity);

        String context;
        String value;
        for(Iterator var7 = headers.keySet().iterator(); var7.hasNext(); put.addHeader(context, value)) {
            String key = (String)var7.next();
            context = key.replace(" ", "");
            value = (String)headers.get(key);
            if (value == null) {
                value = "";
            }
        }

        put.addHeader("Accept", "application/json");

        try {
            ResponseResult response = super.doHttp(put, token, conf.getSecure(), conf.getTimeOutOption());
            int statusCode = response.getStatusCode();
            if (statusCode == 200) {
                context = response.getResponseStr();
                if (context != null && !context.equals("")) {
                    JSONObject os = new JSONObject(context);
                    return os.get("Response Status").equals("201 Created");
                } else {
                    return false;
                }
            } else if (statusCode == 404) {
                throw new SfOssException(ErrorCodeMessageEnum.RESOURCE_NOT_FIND.getIndex(), ErrorCodeMessageEnum.RESOURCE_NOT_FIND.getMessage());
            } else {
                throw new SfOssException(statusCode, url);
            }
        } catch (Exception var11) {
            throw new SfOssException(var11);
        }
    }
}
