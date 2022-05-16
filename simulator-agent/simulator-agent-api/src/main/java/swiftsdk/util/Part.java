//
// Source code recreated from a .class file by IntelliJ IDEA
// (powered by FernFlower decompiler)
//

package swiftsdk.util;

public class Part {
    private int partNumber;
    private String name;

    public Part() {
        this(0, (String)null);
    }

    public Part(int partNumber, String name) {
        this.partNumber = partNumber;
        this.name = name;
    }

    public int partNumber() {
        return this.partNumber;
    }

    public String name() {
        return this.name;
    }
}
