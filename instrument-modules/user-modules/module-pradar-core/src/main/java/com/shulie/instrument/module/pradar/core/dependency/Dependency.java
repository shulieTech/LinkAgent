package com.shulie.instrument.module.pradar.core.dependency;

import java.util.List;

public class Dependency {

    Dependency importBy;
    String groupId;
    String artifactId;
    String version;
    String scope = "compile";
    String optional = "false";
    int deep = 0;
    long time = System.nanoTime();
    List<Exclusion> exclusions;

    public static class Exclusion {
        String groupId;
        String artifactId;
    }
}
