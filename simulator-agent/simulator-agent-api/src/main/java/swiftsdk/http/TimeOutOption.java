//
// Source code recreated from a .class file by IntelliJ IDEA
// (powered by FernFlower decompiler)
//

package swiftsdk.http;

public class TimeOutOption {
    private int connectTimeOut = 15000;
    private int socketTimeOut = 15000;

    public TimeOutOption(int connectTimeOut, int socketTimeOut) {
        this.connectTimeOut = connectTimeOut;
        this.socketTimeOut = socketTimeOut;
    }

    public int getConnectTimeOut() {
        return this.connectTimeOut;
    }

    public int getSocketTimeOut() {
        return this.socketTimeOut;
    }
}
