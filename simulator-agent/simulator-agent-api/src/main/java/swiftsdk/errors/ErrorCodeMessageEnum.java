//
// Source code recreated from a .class file by IntelliJ IDEA
// (powered by FernFlower decompiler)
//

package swiftsdk.errors;

public enum ErrorCodeMessageEnum {
    SUCCESS(0, "SUCCESS", "SUCCESS"),
    FAILURE(1, "FAILURE", "FAILURE"),
    RESOURCE_NOT_FIND(404, "资源未找到", "Resource does not find"),
    TOKEN_IS_NULL(200001, "token为空,无法登录Sfoss", "token is null,not login sfoss"),
    CONFIG_IS_NULL(200002, "configp配置为空,无法初始化", "config is null,not inint sfoss"),
    VALIDATE_ZIP(200003, "只支持tar, tar.gz, and tar.bz2格式的文件做批量上传.", "只支持tar, tar.gz, and tar.bz2格式的文件做批量上传."),
    NOT_GE_FILE_SIZE(200004, "设定的片段大小大于文件大小", "设定的片段大小大于文件大小"),
    URL_ERROR(200005, "请求路径的参数错误", "请求路径的参数错误"),
    FILE_ERROR(200006, "不是文件类型", "不是文件类型");

    private int index;
    private String message;
    private String enMessage;

    private ErrorCodeMessageEnum(int index, String message, String enMessage) {
        this.index = index;
        this.message = message;
        this.enMessage = enMessage;
    }

    public static String getMessage(int index) {
        ErrorCodeMessageEnum[] var1 = values();
        int var2 = var1.length;

        for(int var3 = 0; var3 < var2; ++var3) {
            ErrorCodeMessageEnum c = var1[var3];
            if (c.getIndex() == index) {
                return c.message;
            }
        }

        return "";
    }

    public static String getEnMessage(int index) {
        ErrorCodeMessageEnum[] var1 = values();
        int var2 = var1.length;

        for(int var3 = 0; var3 < var2; ++var3) {
            ErrorCodeMessageEnum c = var1[var3];
            if (c.getIndex() == index) {
                return c.enMessage;
            }
        }

        return "";
    }

    public int getIndex() {
        return this.index;
    }

    public void setIndex(int index) {
        this.index = index;
    }

    public String getMessage() {
        return this.message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public String getEnMessage() {
        return this.enMessage;
    }

    public void setEnMessage(String enMessage) {
        this.enMessage = enMessage;
    }
}
