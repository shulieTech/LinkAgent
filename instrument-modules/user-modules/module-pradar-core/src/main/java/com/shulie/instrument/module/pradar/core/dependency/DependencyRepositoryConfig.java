package com.shulie.instrument.module.pradar.core.dependency;

import java.util.Arrays;
import java.util.List;

public class DependencyRepositoryConfig {

    public static String currentModulePath;
    public static String localRepositoryPath;

    /**
     * 已经打进代码的依赖
     */
    public static List<String> hasPackagedDependencies = Arrays.asList("org.slf4j:slf4j-api");

}
