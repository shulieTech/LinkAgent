//
// Source code recreated from a .class file by IntelliJ IDEA
// (powered by FernFlower decompiler)
//

package swiftsdk.service.impl;

import org.apache.http.Header;
import org.apache.http.client.methods.*;
import swiftsdk.SwiftConfiguration;
import swiftsdk.errors.ErrorCodeMessageEnum;
import swiftsdk.errors.SfOssException;
import swiftsdk.factory.SFOSSMasterService;
import swiftsdk.http.ResponseResult;
import swiftsdk.service.SFOSSContainerService;

import java.util.*;

public class SFOSSContainerServiceImpl extends SFOSSMasterService implements SFOSSContainerService {
    private static volatile SFOSSContainerService sfossContainerService;

    private SFOSSContainerServiceImpl() {
    }

    public static SFOSSContainerService getInstance() {
        if (sfossContainerService == null) {
            Class var0 = SFOSSContainerServiceImpl.class;
            synchronized(SFOSSContainerServiceImpl.class) {
                if (sfossContainerService == null) {
                    sfossContainerService = new SFOSSContainerServiceImpl();
                }
            }
        }

        return sfossContainerService;
    }

    public List<String> getContainerList(String url, String token, SwiftConfiguration conf) throws SfOssException {
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

    public boolean createContainer(String url, String token, SwiftConfiguration conf) throws SfOssException {
        HttpPut put = new HttpPut(url);

        try {
            ResponseResult response = super.doHttp(put, token, conf.getSecure(), conf.getTimeOutOption());
            int statusCode = response.getStatusCode();
            if (statusCode != 202 && statusCode != 201) {
                if (statusCode == 404) {
                    throw new SfOssException(ErrorCodeMessageEnum.RESOURCE_NOT_FIND.getIndex(), ErrorCodeMessageEnum.RESOURCE_NOT_FIND.getMessage());
                } else {
                    throw new SfOssException(statusCode, url);
                }
            } else {
                return true;
            }
        } catch (Exception var7) {
            throw new SfOssException(var7);
        }
    }

    public boolean postContainerMeta(String url, Map<String, String> headers, String token, SwiftConfiguration conf) throws SfOssException {
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

    public Map<String, String> headContainerMeta(String url, String token, SwiftConfiguration conf) throws SfOssException {
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
                    if (key.contains("X-Container-") || key.contains("X-Timestamp") || key.contains("Last-Modified")) {
                        resultMap.put(key, value);
                    }
                }

                return resultMap;
            }
        } catch (Exception var15) {
            throw new SfOssException(var15);
        }
    }

    public boolean deleteContainer(String url, String container, boolean force, String token, SwiftConfiguration conf) throws SfOssException {
        new HashMap();

        Map containerHeaders;
        try {
            containerHeaders = this.headContainerMeta(url, token, conf);
        } catch (SfOssException var11) {
            if (var11.getErrorCode() == ErrorCodeMessageEnum.RESOURCE_NOT_FIND.getIndex()) {
                return true;
            }

            throw var11;
        }

        if (Long.valueOf((String)containerHeaders.get("X-Container-Object-Count")) > 0L) {
            if (!force) {
                throw new SfOssException("容器" + container + "非空");
            }

            boolean t = this.clearContainer(container, url, token, conf);
            if (!t) {
                return false;
            }
        }

        HttpDelete delete = new HttpDelete(url);

        try {
            ResponseResult response = super.doHttp(delete, token, conf.getSecure(), conf.getTimeOutOption());
            int statusCode = response.getStatusCode();
            if (statusCode != 200 && statusCode != 202 && statusCode != 204 && statusCode != 404) {
                throw new SfOssException(statusCode, url);
            } else {
                return true;
            }
        } catch (Exception var10) {
            throw new SfOssException(var10);
        }
    }

    public boolean clearContainer(String container, String url, String token, SwiftConfiguration conf) throws SfOssException {
        String beginName = null;

        try {
            while(true) {
                List<String> contentList = SFOSSObjectServiceImpl.getInstance().getObjectList(url, beginName, 10000, token, conf);
                if (contentList == null) {
                    return false;
                }

                if (contentList.isEmpty()) {
                    return true;
                }

                Iterator var7 = contentList.iterator();

                while(var7.hasNext()) {
                    String content = (String)var7.next();
                    SFOSSObjectServiceImpl.getInstance().deleteObject(url, content, token, conf);
                }

                if (contentList.size() < 10000) {
                    return true;
                }

                beginName = (String)contentList.get(contentList.size() - 1);
            }
        } catch (SfOssException var9) {
            if (var9.getErrorCode() == ErrorCodeMessageEnum.RESOURCE_NOT_FIND.getIndex()) {
                return true;
            } else {
                throw var9;
            }
        }
    }

    public boolean checkContainer(String url, String token, SwiftConfiguration conf) throws SfOssException {
        HttpHead head = new HttpHead(url);

        try {
            ResponseResult response = super.doHttp(head, token, conf.getSecure(), conf.getTimeOutOption());
            int statusCode = response.getStatusCode();
            return statusCode == 200 || statusCode == 204 || statusCode == 202;
        } catch (Exception var7) {
            throw new SfOssException(var7);
        }
    }
}
