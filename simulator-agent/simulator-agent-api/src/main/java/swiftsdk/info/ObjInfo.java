//
// Source code recreated from a .class file by IntelliJ IDEA
// (powered by FernFlower decompiler)
//

package swiftsdk.info;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

public class ObjInfo implements Serializable {
    private static final long serialVersionUID = 5339169924375955904L;
    private long bytes;
    private String name;
    private String last_modified;
    private String etag;
    private Map<String, String> headers = new HashMap();

    public ObjInfo() {
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

    public String getLast_modified() {
        return this.last_modified;
    }

    public void setLast_modified(String last_modified) {
        this.last_modified = last_modified;
    }

    public Map<String, String> getHeaders() {
        return this.headers;
    }

    public void setHeaders(String key, String value) {
        this.headers.put(key, value);
    }

    public String getEtag() {
        return this.etag;
    }

    public void setEtag(String etag) {
        this.etag = etag;
    }

    public String toString() {
        return "ObjectaInfo [bytes=" + this.bytes + ", name=" + this.name + ", last_modified=" + this.last_modified + ", etag=" + this.etag + "]";
    }
}
