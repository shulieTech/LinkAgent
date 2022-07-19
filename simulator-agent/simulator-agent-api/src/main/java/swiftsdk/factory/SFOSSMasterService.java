//
// Source code recreated from a .class file by IntelliJ IDEA
// (powered by FernFlower decompiler)
//

package swiftsdk.factory;

import org.apache.http.client.methods.HttpRequestBase;
import swiftsdk.SFOSSObjectManager;
import swiftsdk.errors.ErrorCodeMessageEnum;
import swiftsdk.errors.HttpException;
import swiftsdk.errors.SfOssException;
import swiftsdk.http.HttpCaller;
import swiftsdk.http.ResponseResult;
import swiftsdk.http.SFOSSHttpClientFactory;
import swiftsdk.http.TimeOutOption;

public class SFOSSMasterService {
    private String version = "swift-java-api-2.0.15";
    private static HttpCaller httpFactory = new SFOSSHttpClientFactory();

    public SFOSSMasterService() {
    }

    protected ResponseResult doHttpForByteResult(HttpRequestBase method, String token, boolean isHttps) throws SfOssException, HttpException {
        ResponseResult result = null;
        if (null != token && !"".equalsIgnoreCase(token)) {
            method.addHeader("X-Auth-Token", token);
            method.addHeader("User-Agent", this.version);
            if (isHttps) {
                result = httpFactory.doHttps(method, true);
            } else {
                result = httpFactory.doHttp(method, true);
            }

            if (!token.startsWith("AUTH_tkv12_") && result.getStatusCode() == 401) {
                this.reloadToken(result);
                token = SFOSSObjectManager.getTokenCache().getToken();
                method.setHeader("X-Auth-Token", token);
                if (isHttps) {
                    result = httpFactory.doHttps(method, true);
                } else {
                    result = httpFactory.doHttp(method, true);
                }
            }

            return result;
        } else {
            throw new SfOssException(ErrorCodeMessageEnum.TOKEN_IS_NULL.getIndex(), ErrorCodeMessageEnum.TOKEN_IS_NULL.getMessage() + method.getURI());
        }
    }

    protected ResponseResult doHttp(HttpRequestBase method, String token, boolean isHttps) throws SfOssException, HttpException {
        ResponseResult result = null;
        if (null != token && !"".equalsIgnoreCase(token)) {
            method.addHeader("X-Auth-Token", token);
            method.addHeader("User-Agent", this.version);
            if (isHttps) {
                result = httpFactory.doHttps(method, false);
            } else {
                result = httpFactory.doHttp(method, false);
            }

            if (!token.startsWith("AUTH_tkv12_") && result.getStatusCode() == 401) {
                this.reloadToken(result);
                token = SFOSSObjectManager.getTokenCache().getToken();
                method.setHeader("X-Auth-Token", token);
                if (isHttps) {
                    result = httpFactory.doHttps(method, false);
                } else {
                    result = httpFactory.doHttp(method, false);
                }
            }

            return result;
        } else {
            throw new SfOssException(ErrorCodeMessageEnum.TOKEN_IS_NULL.getIndex(), ErrorCodeMessageEnum.TOKEN_IS_NULL.getMessage() + method.getURI());
        }
    }

    protected ResponseResult doHttp(HttpRequestBase method, boolean isHttps) throws SfOssException, HttpException {
        ResponseResult result = null;
        if (isHttps) {
            result = httpFactory.doHttps(method, false);
        } else {
            result = httpFactory.doHttp(method, false);
        }

        return result;
    }

    protected ResponseResult doHttpForByteResult(HttpRequestBase method, String token, boolean isHttps, TimeOutOption t) throws SfOssException, HttpException {
        ResponseResult result = null;
        if (null != token && !"".equalsIgnoreCase(token)) {
            method.addHeader("X-Auth-Token", token);
            method.addHeader("User-Agent", this.version);
            if (isHttps) {
                if (t != null) {
                    result = httpFactory.doHttps(method, t, true);
                } else {
                    result = httpFactory.doHttps(method, true);
                }
            } else if (t != null) {
                result = httpFactory.doHttp(method, t, true);
            } else {
                result = httpFactory.doHttp(method, true);
            }

            if (!token.startsWith("AUTH_tkv12_") && result.getStatusCode() == 401) {
                this.reloadToken(result);
                token = SFOSSObjectManager.getTokenCache().getToken();
                method.setHeader("X-Auth-Token", token);
                if (isHttps) {
                    result = httpFactory.doHttps(method, true);
                } else {
                    result = httpFactory.doHttp(method, true);
                }
            }

            return result;
        } else {
            throw new SfOssException(ErrorCodeMessageEnum.TOKEN_IS_NULL.getIndex(), ErrorCodeMessageEnum.TOKEN_IS_NULL.getMessage() + method.getURI());
        }
    }

    protected ResponseResult doHttp(HttpRequestBase method, String token, boolean isHttps, TimeOutOption t) throws SfOssException, HttpException {
        ResponseResult result = null;
        if (null != token && !"".equalsIgnoreCase(token)) {
            method.addHeader("X-Auth-Token", token);
            method.addHeader("User-Agent", this.version);
            if (isHttps) {
                if (t != null) {
                    result = httpFactory.doHttps(method, t, false);
                } else {
                    result = httpFactory.doHttps(method, false);
                }
            } else if (t != null) {
                result = httpFactory.doHttp(method, t, false);
            } else {
                result = httpFactory.doHttp(method, false);
            }

            if (!token.startsWith("AUTH_tkv12_") && result.getStatusCode() == 401) {
                this.reloadToken(result);
                token = SFOSSObjectManager.getTokenCache().getToken();
                method.setHeader("X-Auth-Token", token);
                if (isHttps) {
                    result = httpFactory.doHttps(method, false);
                } else {
                    result = httpFactory.doHttp(method, false);
                }
            }

            return result;
        } else {
            throw new SfOssException(ErrorCodeMessageEnum.TOKEN_IS_NULL.getIndex(), ErrorCodeMessageEnum.TOKEN_IS_NULL.getMessage() + method.getURI());
        }
    }

    protected ResponseResult doHttp(HttpRequestBase method, boolean isHttps, TimeOutOption t) throws SfOssException, HttpException {
        ResponseResult result = null;
        if (isHttps) {
            if (t != null) {
                result = httpFactory.doHttps(method, t, false);
            } else {
                result = httpFactory.doHttps(method, false);
            }
        } else if (t != null) {
            result = httpFactory.doHttp(method, t, false);
        } else {
            result = httpFactory.doHttp(method, false);
        }

        return result;
    }

    private void reloadToken(ResponseResult result) {
        try {
            SFOSSObjectManager.getTokenCache().reLoadToken();
        } catch (Exception var4) {
            var4.printStackTrace();
        }

    }
}
