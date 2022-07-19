//
// Source code recreated from a .class file by IntelliJ IDEA
// (powered by FernFlower decompiler)
//

package swiftsdk.info;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

public class AccountInfo implements Serializable {
    private static final long serialVersionUID = 5339169924375955904L;
    private int containerCount;
    private int objectCount;
    private long bytes;
    private String name;
    private Map<String, String> headers = new HashMap();

    public AccountInfo() {
    }

    public int getContainerCount() {
        return this.containerCount;
    }

    public void setContainerCount(int containerCount) {
        this.containerCount = containerCount;
    }

    public int getObjectCount() {
        return this.objectCount;
    }

    public void setObjectCount(int objectCount) {
        this.objectCount = objectCount;
    }

    public long getBytes() {
        return this.bytes;
    }

    public void setBytes(long bytes) {
        this.bytes = bytes;
    }

    public String getName() {
        return this.name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Map<String, String> getHeaders() {
        return this.headers;
    }

    public void setHeaders(String key, String value) {
        this.headers.put(key, value);
    }

    public String toString() {
        return "AccountInfo [containerCount=" + this.containerCount + ", objectCount=" + this.objectCount + ", bytes=" + this.bytes + ", name=" + this.name + "]";
    }
}
