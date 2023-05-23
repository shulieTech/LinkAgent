package com.shulie.instrument.module.pradar.core.dependency;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.function.Function;
import java.util.stream.Collectors;

public class LocalRepositoryManager {

    /**
     * 直接读取当前用户目录下的/${user.home}/.m2
     *
     * @return
     * @throws IOException
     */
    public static String findLocalRepositoryByDefault() throws IOException {
        String property = System.getProperty("user.home");
        if (property == null) {
            return null;
        }
        File repositoryDir = Paths.get(property, ".m2", "repository").toFile();
        if (!repositoryDir.exists()) {
            return null;
        }
        return repositoryDir.getAbsolutePath();
    }

    /**
     * 从maven工程的settings.xml里读取maven仓库路径
     *
     * @return
     * @throws IOException
     */
    public static String findLocalRepositoryFromConfig() throws IOException {
        String m2_home = System.getenv("M2_HOME");
        String MAVEN_HOME = System.getenv("MAVEN_HOME");
        String maven_home = System.getenv("maven.home");
        if (m2_home == null && MAVEN_HOME == null && maven_home == null) {
            throw new IllegalStateException("[M2_HOME,MAVEN_HOME,maven.home]环境变量未配置,不能获取到maven工程地址");
        }
        String mavenHome = m2_home != null ? m2_home : MAVEN_HOME != null ? MAVEN_HOME : maven_home;
        Path settings = Paths.get(mavenHome, "conf", "settings.xml");
        List<String> lines = Files.readAllLines(settings);

        int index = -1;
        for (int i = 0; i < lines.size(); i++) {
            String line = lines.get(i);
            if (!line.contains("<localRepository>")) {
                continue;
            }
            index = i;
        }
        if (index == -1) {
            throw new IllegalStateException("maven工程的settings.xml里未配置本地仓库地址");
        }

        List<String> repositories = new ArrayList<>();
        repositories.add(lines.get(index));

        int j = index;
        while (true) {
            String s = lines.get(j);
            if (s.contains("</localRepository>")) {
                break;
            }
            repositories.add(lines.get(++j));
        }

        String repositoryLine = repositories.stream().map(s -> s.trim()).collect(Collectors.joining());
        int start = repositoryLine.indexOf("<localRepository>");
        int end = repositoryLine.indexOf("</localRepository>");
        return repositoryLine.substring(start + 17, end);
    }

    /**
     * 找到pom文件
     *
     * @param dependency
     * @return
     */
    public static File findDependencyJarFile(Dependency dependency) {
        String[] groupPaths = dependency.groupId.split("\\.");
        String[] artifactPaths = dependency.artifactId.split("\\.");
        Path path = Paths.get(DependencyRepositoryConfig.localRepositoryPath, groupPaths);
        File file = new File(path.toFile().getAbsolutePath());
        file = findFile(file.getAbsolutePath(), artifactPaths);
        file = findFile(file.getAbsolutePath(), dependency.version);
        return Paths.get(file.getAbsolutePath(), String.format("%s-%s.jar", dependency.artifactId, dependency.version)).toFile();
    }

    private static File findFile(String parent, String... children) {
        File file = new File(parent);
        for (String child : children) {
            file = new File(file.getAbsolutePath(), child);
        }
        return file;
    }

    /**
     * 把jar包拷贝到lib目录
     *
     * @param jarFiles
     */
    public static void copyDependencyJarFilesToLib(List<File> jarFiles) throws IOException {
        File linkAgent = new File(DependencyRepositoryConfig.currentModulePath);
        while (true) {
            if (linkAgent.getName().equals("LinkAgent") || linkAgent.getName().equals("agent-core")) {
                break;
            }
            linkAgent = linkAgent.getParentFile();
        }
        File lib = Paths.get(linkAgent.getAbsolutePath(), "deploy", "simulator-agent", "lib").toFile();
        if (!lib.exists()) {
            lib.mkdirs();
        }
        for (File jarFile : jarFiles) {
            Files.copy(jarFile.toPath(), new FileOutputStream(new File(lib, jarFile.getName())));
        }
    }

}
