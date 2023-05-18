package com.shulie.instrument.module.pradar.core.dependency;

import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.function.Predicate;
import java.util.stream.Collectors;

public class PradarCoreModuleDependenciesCollector {

    public static void main(String[] args) throws IOException {
        List<Dependency> dependencies = PomFileReader.extractPomDependencies(null, PomFileReader.findCurrentModulePomPath(), null, false, false).getValue();
        // 收集properties
        // 过滤框架依赖和非provide依赖
        dependencies = dependencies.stream().filter(new Predicate<Dependency>() {
            @Override
            public boolean test(Dependency dependency) {
                boolean agent = dependency.groupId.startsWith("io.shulie.instrument.simulator");
                boolean ttl = dependency.artifactId.equals("transmittable-thread-local");
                boolean spring = dependency.groupId.equals("org.springframework");
                boolean logback = dependency.groupId.equals("ch.qos.logback");
                boolean guava = dependency.artifactId.equals("guava");
                boolean provided = "provided".equals(dependency.scope);
                boolean system = "system".equals(dependency.scope);
                return guava || (!agent && !ttl && !spring && !system && provided && !logback);
            }
        }).collect(Collectors.toList());

        // 找到本地maven仓库
        String repositoryPath = LocalRepositoryManager.findLocalRepositoryByDefault();
        if (repositoryPath == null) {
            repositoryPath = LocalRepositoryManager.findLocalRepositoryFromConfig();
        }
        DependencyRepositoryConfig.localRepositoryPath = repositoryPath;

        List<Dependency> deepDependencies = PomFileReader.collectDependenciesDeeply(dependencies);
        dependencies.addAll(deepDependencies);

        Map<String, Dependency> dependencyMap = new HashMap<>();
        for (Dependency dependency : dependencies) {
            String key = dependency.groupId + ":" + dependency.artifactId;
            if (dependencyMap.containsKey(key)) {
                Dependency old = dependencyMap.get(key);
                boolean needRefresh = dependency.deep < old.deep || (dependency.deep == old.deep && dependency.time < old.time);
                if (needRefresh) {
                    dependencyMap.put(key, dependency);
                }
            } else {
                dependencyMap.put(key, dependency);
            }
        }

        Collection<Dependency> collectDependencies = dependencyMap.values();

        // 收集依赖jar包
        List<File> jarFiles = collectDependencies.stream().map(dependency -> LocalRepositoryManager.findDependencyJarFile(dependency)).collect(Collectors.toList());
        // 拷贝jar包到lib目录
        LocalRepositoryManager.copyDependencyJarFilesToLib(jarFiles);
    }


}
