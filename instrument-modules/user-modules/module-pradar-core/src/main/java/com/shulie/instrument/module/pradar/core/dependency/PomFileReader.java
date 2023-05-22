package com.shulie.instrument.module.pradar.core.dependency;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.function.Predicate;
import java.util.stream.Collectors;

public class PomFileReader {


    /**
     * 读取当前模块的pom文件路径
     *
     * @return
     * @throws IOException
     */
    public static String findCurrentModulePomPath() throws IOException {
        URL resource = PradarCoreModuleDependenciesCollector.class.getResource("/");
        File file = new File(Paths.get(resource != null ? "instrument-modules/user-modules/module-pradar-core" : "").toFile().getAbsolutePath());

        File modulePath;
        while (true) {
            String fileName = file.getName();
            if (fileName.equals("module-pradar-core")) {
                modulePath = file;
                break;
            }
            if (fileName.equals("LinkAgent")) {
                modulePath = Paths.get(file.getAbsolutePath(), "instrument-modules", "user-modules", "module-pradar-core").toFile();
                break;
            } else {
                file = file.getParentFile();
            }
        }
        DependencyRepositoryConfig.currentModulePath = modulePath.getAbsolutePath();
        File pom = new File(modulePath, "pom.xml");
        return pom.getAbsolutePath();
    }

    /**
     * 收集pom.xml里的依赖信息
     *
     * @return
     */
    public static Map.Entry<Map<String, String>, List<Dependency>> extractPomDependencies(Dependency importBy, String pom, List<Dependency.Exclusion> excludes, boolean filterDep, boolean includeManagement) throws IOException {
        List<String> lines = Files.readAllLines(Paths.get(pom));
        List<Dependency> dependencies = new ArrayList<>();
        String line;
        Dependency dep = null;
        List<Dependency.Exclusion> exclusions = null;
        boolean hasDep = false;
        boolean exclusion = false;
        boolean build = false;

        String groupId = "<groupId>", artifactId = "<artifactId>", version = "<version>", scope = "<scope>", optional = "<optional>";

        Iterator<String> iterator = lines.iterator();
        while (iterator.hasNext()) {
            line = iterator.next().trim();
            if (line.startsWith("<!-- ")) {
                continue;
            }
            if (includeManagement ? line.contains("</dependencyManagement>") : line.contains("</dependencies>")) {
                break;
            }
            if (line.contains("<build>")) {
                build = true;
                continue;
            }
            if (line.contains("</build>")) {
                build = false;
                continue;
            }
            if (!build && line.contains("<dependency>")) {
                dep = new Dependency();
                dep.importBy = importBy;
                dep.deep = importBy != null ? importBy.deep + 1 : 0;
                exclusions = new ArrayList<>();
                hasDep = true;
                continue;
            }
            if (!build && line.contains("</dependency>")) {
                dep.exclusions = exclusions;
                dependencies.add(dep);
                dep = null;
                hasDep = false;
                continue;
            }
            if (line.contains("<exclusions>")) {
                exclusion = true;
                continue;
            }
            if (line.contains("</exclusions>")) {
                exclusion = false;
                continue;
            }
            if (line.contains("<exclusion>")) {
                exclusions.add(new Dependency.Exclusion());
                continue;
            }
            if (!hasDep) {
                continue;
            }
            if (!line.startsWith(groupId) && !line.startsWith(artifactId) && !line.startsWith(version) && !line.startsWith(scope) && !line.startsWith(optional)) {
                continue;
            }
            if (line.startsWith(groupId)) {
                String group = line.substring(line.indexOf(groupId) + 9, line.indexOf("</groupId>")).trim();
                if (exclusion) {
                    exclusions.get(exclusions.size() - 1).groupId = group;
                } else {
                    dep.groupId = group;
                }
            }
            if (line.startsWith(artifactId)) {
                String artifact = line.substring(line.indexOf(artifactId) + 12, line.indexOf("</artifactId>")).trim();
                if (exclusion) {
                    exclusions.get(exclusions.size() - 1).artifactId = artifact;
                } else {
                    dep.artifactId = artifact;
                }
            }
            if (line.startsWith(version)) {
                dep.version = line.substring(line.indexOf(version) + 9, line.indexOf("</version>")).trim();
            }
            if (line.startsWith(scope)) {
                dep.scope = line.substring(line.indexOf(scope) + 7, line.indexOf("</scope>")).trim();
            }
            if (line.startsWith(optional)) {
                dep.optional = line.substring(line.indexOf(optional) + 10, line.indexOf("</optional>")).trim();
            }
        }

        Map<String, String> properties = extractProperties(pom);

        for (Dependency dependency : dependencies) {
            replaceVariable(dependency, properties);
            if ("${project.version}".equals(dependency.version)) {
                dependency.version = importBy.version;
            }
            if ("${project.groupId}".equals(dependency.groupId)) {
                dependency.groupId = importBy.groupId;
            }

        }

        // 过滤exclusion
        if (excludes != null && !excludes.isEmpty()) {
            Set<String> excludeDeps = excludes.stream().map(dependency -> dependency.groupId + ":" + dependency.artifactId).collect(Collectors.toSet());
            dependencies = dependencies.stream().filter(dependency -> !excludeDeps.contains(dependency.groupId + ":" + dependency.artifactId)).collect(Collectors.toList());
        }
        // 过滤结果
        if (filterDep) {
            dependencies = filterDependencies(dependencies);
        }
        return new AbstractMap.SimpleEntry<>(properties, dependencies);
    }

    /**
     * 读取properties配置
     *
     * @return
     * @throws IOException
     */
    private static Map<String, String> extractProperties(String pom) throws IOException {
        List<String> lines = Files.readAllLines(Paths.get(pom));
        Map<String, String> properties = new HashMap<>();
        Iterator<String> iterator = lines.iterator();

        String propertyContent = "";
        String line;
        boolean start = false;
        boolean comment = false;
        boolean build = false;
        boolean profiles = false;
        try {
            while (iterator.hasNext()) {
                line = iterator.next().trim();
                if (line.startsWith("-->")) {
                    line = line.substring(3);
                    comment = false;
                }
                if (line.isEmpty()) {
                    continue;
                }
                if (line.startsWith("<!--")) {
                    comment = true;
                    if (line.endsWith("-->")) {
                        comment = false;
                    }
                    continue;
                }
                if (line.endsWith("-->")) {
                    comment = false;
                    continue;
                }
                if (comment) {
                    continue;
                }
                if (line.contains("<build>")) {
                    build = true;
                    continue;
                }
                if (line.contains("</build>")) {
                    build = false;
                    continue;
                }
                if (line.contains("<profiles>")) {
                    profiles = true;
                    continue;
                }
                if (line.contains("</profiles>")) {
                    profiles = false;
                    continue;
                }
                if (!build && !profiles && line.contains("</properties>")) {
                    break;
                }
                if (!build && !profiles && line.contains("<properties>")) {
                    start = true;
                    continue;
                }
                if (!start || line.contains("<!--")) {
                    continue;
                }
                if (!line.contains("</")) {
                    propertyContent = propertyContent + line;
                    continue;
                }
                if (propertyContent.length() > 0) {
                    line = propertyContent + line;
                }
                int index = line.indexOf(">");
                String key = line.substring(1, index);
                int index1 = line.indexOf("<", index);
                String value = line.substring(index + 1, index1);
                properties.put(key, value);
                propertyContent = "";
            }
        } catch (Exception e) {
            System.out.println();
        }

        return properties;
    }

    /**
     * 深入的收集依赖信息
     *
     * @param dependencies
     * @return
     */
    public static List<Dependency> collectDependenciesDeeply(List<Dependency> dependencies) throws IOException {
        List<Dependency> allDependencies = new ArrayList<>();
        for (Dependency dependency : dependencies) {
            collectDependenciesDeeply(dependency, allDependencies, 1);
        }
        return allDependencies;
    }

    private static void collectDependenciesDeeply(Dependency dependency, List<Dependency> dependencies, int deep) throws IOException {
        File pom = findPomFile(dependency);
        List<Dependency> extractDeps = extractPomDependencies(dependency, pom.getAbsolutePath(), dependency.exclusions, true, false).getValue();

        if (extractDeps.isEmpty()) {
            return;
        }

        for (Dependency extractDep : extractDeps) {
            if ("${project.groupId}".equals(extractDep.groupId)) {
                extractDep.groupId = dependency.groupId;
            }
            if ("${project.version}".equals(extractDep.version)) {
                extractDep.version = dependency.version;
            }
        }

        // 是否有变量型的依赖
        List<Dependency> hasNoVersionDeps = extractDeps.stream().filter(dp -> dp.groupId == null || dp.groupId.startsWith("${") || dp.artifactId == null || dp.artifactId.startsWith("${") || dp.version == null || dp.version.startsWith("${")).collect(Collectors.toList());
        // 从parent里读取依赖
        if (!hasNoVersionDeps.isEmpty()) {
            Dependency parent = extractParentDependency(pom);
            File parentPom = findPomFile(parent);
            // 读取parent pom
            Map.Entry<Map<String, String>, List<Dependency>> parentContent = new AbstractMap.SimpleEntry<>(new HashMap<>(), new ArrayList<>());
            extractParentDependencies(pom.getAbsolutePath(), parentContent);

            Map<String, String> parentProperties = parentContent.getKey();
            List<Dependency> parentDependencies = parentContent.getValue();

            Map<String, Dependency> depMaps = new HashMap<>();
            for (Dependency dep : parentDependencies) {
                depMaps.put(dep.groupId + ":" + dep.artifactId, dep);
            }

            for (Dependency dep : hasNoVersionDeps) {
                replaceVariable(dep, parentProperties);
                if (dep.version == null) {
                    String key = dep.groupId + ":" + dep.artifactId;
                    Dependency parentDep = depMaps.get(key);
                    if (parentDep == null) {
                        throw new IllegalStateException(String.format("从[%s]文件里读取依赖[%s]失败", parentPom.getAbsolutePath(), key));
                    }
                    // 使用parent里的scope
                    dep.version = parentDep.version != null ? parentDep.version : dependency.version;
                    dep.scope = parentDep.scope;
                    dep.optional = parentDep.optional;
                }
            }
        }

        extractDeps = filterDependencies(extractDeps);


        dependencies.addAll(extractDeps);

        if (extractDeps != null && !extractDeps.isEmpty()) {
            for (Dependency extractDep : extractDeps) {
                try {
                    collectDependenciesDeeply(extractDep, dependencies, deep++);
                } catch (Throwable throwable) {
                    System.out.println(throwable);
                }
            }
        }
    }

    private static void extractParentDependencies(String childPom, Map.Entry<Map<String, String>, List<Dependency>> result) throws IOException {
        Dependency parentDep = extractParentDependency(new File(childPom));
        while (parentDep.artifactId != null) {
            File parentPom = findPomFile(parentDep);
            Map.Entry<Map<String, String>, List<Dependency>> innerResult = extractPomDependencies(parentDep, parentPom.getAbsolutePath(), null, false, true);
            result.getKey().putAll(innerResult.getKey());
            result.getValue().addAll(innerResult.getValue());
            parentDep = extractParentDependency(parentPom);
        }
    }

    private static List<Dependency> filterDependencies(List<Dependency> dependencies) {
        return dependencies.stream().filter(new Predicate<Dependency>() {
            @Override
            public boolean test(Dependency dependency) {
                if ("true".equals(dependency.optional)) {
                    return false;
                }
                if (dependency.artifactId.contains("999") || (dependency.version != null && dependency.version.contains("999"))) {
                    return false;
                }
                List<String> scopes = Arrays.asList("system", "provided", "test", "import");
                if (scopes.contains(dependency.scope)) {
                    return false;
                }
                String groupArtifact = dependency.groupId + ":" + dependency.artifactId;
                return !DependencyRepositoryConfig.hasPackagedDependencies.contains(groupArtifact);
            }
        }).collect(Collectors.toList());
    }

    /**
     * 替换变量
     *
     * @param dep
     * @param properties
     */
    private static void replaceVariable(Dependency dep, Map<String, String> properties) {
        if (dep.groupId != null && dep.groupId.startsWith("${")) {
            String key = dep.groupId.substring(2, dep.groupId.length() - 1);
            String groupId = findPropertyValue(key, properties);
            if (groupId != null) {
                dep.groupId = groupId;
            }
        }
        if (dep.artifactId != null && dep.artifactId.startsWith("${")) {
            String key = dep.artifactId.substring(2, dep.artifactId.length() - 1);
            String artifactId = findPropertyValue(key, properties);
            if (artifactId != null) {
                dep.artifactId = artifactId;
            }
        }
        if (dep.version != null && dep.version.startsWith("${")) {
            String key = dep.version.substring(2, dep.version.length() - 1);
            String version = findPropertyValue(key, properties);
            if (version != null) {
                dep.version = version;
            }
        }
        if (dep.scope != null && dep.scope.startsWith("${")) {
            String key = dep.scope.substring(2, dep.scope.length() - 1);
            String scope = findPropertyValue(key, properties);
            if (scope != null) {
                dep.scope = scope;
            }
        }
    }

    private static String findPropertyValue(String key, Map<String, String> properties) {
        String value = properties.get(key);
        while (value != null && value.startsWith("${")) {
            key = value.substring(2, value.length() - 1);
            value = properties.get(key);
        }
        return value;
    }

    /**
     * 找到parent依赖
     *
     * @param pom
     * @return
     * @throws IOException
     */
    private static Dependency extractParentDependency(File pom) throws IOException {
        List<String> lines = Files.readAllLines(pom.toPath());
        Iterator<String> iterator = lines.iterator();
        String line;
        boolean parent = false;
        Dependency dep = new Dependency();

        String groupId = "<groupId>", artifactId = "<artifactId>", version = "<version>";

        while (iterator.hasNext()) {
            line = iterator.next().trim();
            if (line.isEmpty()) {
                continue;
            }
            if (line.contains("<parent>")) {
                parent = true;
                continue;
            }
            if (line.contains("</parent>")) {
                break;
            }
            if (!parent) {
                continue;
            }
            if (!line.startsWith(groupId) && !line.startsWith(artifactId) && !line.startsWith(version)) {
                continue;
            }
            if (line.startsWith(groupId)) {
                dep.groupId = line.substring(line.indexOf(groupId) + 9, line.indexOf("</groupId>")).trim();
            }
            if (line.startsWith(artifactId)) {
                dep.artifactId = line.substring(line.indexOf(artifactId) + 12, line.indexOf("</artifactId>")).trim();
            }
            if (line.startsWith(version)) {
                dep.version = line.substring(line.indexOf(version) + 9, line.indexOf("</version>")).trim();
            }
        }
        return dep;
    }

    /**
     * 找到pom文件
     *
     * @param dependency
     * @return
     */
    private static File findPomFile(Dependency dependency) {
        String[] groupPaths = dependency.groupId.split("\\.");
        String[] artifactPaths = dependency.artifactId.split("\\.");
        Path path = Paths.get(DependencyRepositoryConfig.localRepositoryPath, groupPaths);
        path = Paths.get(path.toUri().getPath(), artifactPaths);
        path = Paths.get(path.toUri().getPath(), dependency.version);
        return Paths.get(path.toUri().getPath(), String.format("%s-%s.pom", dependency.artifactId, dependency.version)).toFile();
    }

}


