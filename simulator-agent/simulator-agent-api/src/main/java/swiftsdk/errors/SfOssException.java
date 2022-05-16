//
// Source code recreated from a .class file by IntelliJ IDEA
// (powered by FernFlower decompiler)
//

package swiftsdk.errors;

public class SfOssException extends Exception {
    private static final long serialVersionUID = -1326807600935393549L;
    private static int errorCode;

    public int getErrorCode() {
        return errorCode;
    }

    private void setErrorCode(int errorCode) {
        SfOssException.errorCode = errorCode;
    }

    public SfOssException() {
    }

    public SfOssException(int statusCode) {
        super("http error statusCode=:" + statusCode);
        this.setErrorCode(statusCode);
    }

    public SfOssException(String error) {
        super(error);
    }

    public SfOssException(int code, String error) {
        super("http error statusCode=" + code + ",msg=" + error);
        this.setErrorCode(code);
    }

    public SfOssException(String error, Throwable cause) {
        super(error, cause);
    }

    public SfOssException(Throwable cause) {
        super(cause);
    }
}
