//
// Source code recreated from a .class file by IntelliJ IDEA
// (powered by FernFlower decompiler)
//

package swiftsdk.http;

import org.apache.http.client.methods.HttpRequestBase;
import swiftsdk.errors.HttpException;

public interface HttpCaller {
    void setMaxConnectOption(int var1, int var2);

    ResponseResult doHttp(HttpRequestBase var1, boolean var2) throws HttpException;

    ResponseResult doHttp(HttpRequestBase var1, TimeOutOption var2, boolean var3) throws HttpException;

    ResponseResult doHttps(HttpRequestBase var1, boolean var2) throws HttpException;

    ResponseResult doHttps(HttpRequestBase var1, TimeOutOption var2, boolean var3) throws HttpException;

    void setConnectTimeOut(int var1);

    void setSocketTimeOut(int var1);
}
