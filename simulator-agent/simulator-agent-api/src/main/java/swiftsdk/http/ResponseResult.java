//
// Source code recreated from a .class file by IntelliJ IDEA
// (powered by FernFlower decompiler)
//

package swiftsdk.http;

import org.apache.http.client.methods.CloseableHttpResponse;

public class ResponseResult {
    private CloseableHttpResponse response;
    private String responseStr;
    private byte[] byteArray;
    private int statusCode;

    public ResponseResult(CloseableHttpResponse response, String responseStr, byte[] byteArray, int statusCode) {
        this.response = response;
        this.responseStr = responseStr;
        this.statusCode = statusCode;
        this.byteArray = byteArray;
    }

    public String getResponseStr() {
        return this.responseStr;
    }

    public int getStatusCode() {
        return this.statusCode;
    }

    public CloseableHttpResponse getResponse() {
        return this.response;
    }

    public byte[] getByteArray() {
        return this.byteArray;
    }
}
