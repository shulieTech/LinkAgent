/**
 * Copyright 2021 Shulie Technology, Co.Ltd
 * Email: shulie@shulie.io
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.pamirs.pradar;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.Arrays;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.Set;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * jar包信息扫描工具类
 *
 * @author liqiyu
 */
public class ScanJarPomUtils {
    public final static Logger LOGGER = LoggerFactory.getLogger(ScanJarPomUtils.class);

    public static Set<String> scanByJarPaths(Set<String> jarPaths) {
        final Set<String> result = new HashSet<String>(jarPaths.size());
        try {
            for (String jarPath : jarPaths) {
                String jarName = jarPath.substring(jarPath.lastIndexOf(File.separator) + 1);
                if (jarPath.endsWith(".jar") && jarName.matches(".*-[0-9].*\\.jar")) {
                    JarFile jarFile = null;
                    try {
                        jarFile = new JarFile(jarPath);
                        final String pomInfo = readPomPropertiesFromJarFile(jarFile);
                        if (pomInfo != null && pomInfo.split(":").length == 3) {
                            result.add(pomInfo);
                        }
                    } catch (Exception e) {
                        LOGGER.error("处理jar包版本信息失败", e);
                    } finally {
                        if (jarFile != null) {
                            try {
                                jarFile.close();
                            } catch (IOException e) {
                                e.printStackTrace();
                            }
                        }
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return result;
    }

    public static Set<String> scan() {
        final String property = System.getProperty("java.class.path");
        final String[] jarPaths = property.split(File.pathSeparator);
        return scanByJarPaths(new HashSet<String>(Arrays.asList(jarPaths)));
    }

    /**
     * 从jar文件中读取pom信息
     *
     * @param jarFile jar文件
     * @return pom信息
     */
    private static String readPomPropertiesFromJarFile(JarFile jarFile) {
        final Enumeration<JarEntry> entries = jarFile.entries();
        while (entries.hasMoreElements()) {
            final JarEntry jarEntry = entries.nextElement();
            if (jarEntry.getName().endsWith("pom.properties")) {
                InputStream inputStream = null;
                try {
                    inputStream = jarFile.getInputStream(jarEntry);
                    return readPomName(inputStream);
                } catch (Exception e) {
                    e.printStackTrace();
                    // 如果找到 pom.properties但是读取处理报错了。就从jar包名字中获取。
                    LOGGER.error("读取JAR失败", e);
                    break;
                } finally {
                    if (inputStream != null) {
                        try {
                            inputStream.close();
                        } catch (IOException ioException) {
                            ioException.printStackTrace();
                            LOGGER.error("文件关闭失败", ioException);
                        }
                    }
                }
            }
        }
        final String name = jarFile.getName();
        final String jarName = name.substring(name.lastIndexOf(File.separator) + 1, name.lastIndexOf("."));
        return analysisPomInfoWithoutPomInfo(jarName);
    }

    private static final Pattern VERSION_PATTERN = Pattern.compile("(-\\d+.?)");

    /**
     * 分析jarname
     *
     * @param jarName jar包名字。带.jar后缀
     * @return "%s:%s:%s 格式化后的pom信息
     */
    private static String analysisPomInfoWithoutPomInfo(String jarName) {

        String pomInfo = jarName;
        final Matcher matcher = VERSION_PATTERN.matcher(pomInfo);
        int versionIndex;
        if (matcher.find()) {
            versionIndex = pomInfo.indexOf(matcher.group(0)) + 1;
        } else {
            versionIndex = pomInfo.lastIndexOf("-") + 1;
        }
        if (versionIndex < 1) {
            return null;
        }
        try {
            return String.format("%s:%s:%s", "", StringUtils.trimToEmpty(pomInfo.substring(0, versionIndex - 1)),
                StringUtils.trimToEmpty(pomInfo.substring(versionIndex)));
        } catch (Exception e) {
            LOGGER.error(String.format("解析版本信息出错：%s", jarName), e);
            return String.format("%s:%s:%s", "", StringUtils.trimToEmpty(pomInfo), "");
        }
    }

    /**
     * 从properties文件中读取pom字符串信息
     *
     * @param inputStream properties文件流
     * @return "%s:%s:%s 格式化后的pom信息
     * @throws Exception
     */
    private static String readPomName(InputStream inputStream) throws Exception {
        String pomName = null;
        if (inputStream != null) {
            BufferedReader br = new BufferedReader(new InputStreamReader(inputStream));
            String groupId = null;
            String artifactId = null;
            String version = null;
            String line;
            while ((line = br.readLine()) != null) {
                if (line.startsWith("groupId")) {
                    groupId = line.substring(8);
                }
                if (line.startsWith("artifactId")) {
                    artifactId = line.substring(11);
                }
                if (line.startsWith("version")) {
                    version = line.substring(8);
                }
            }
            pomName = StringUtils.trimToEmpty(groupId) + ":" + StringUtils.trimToEmpty(artifactId) + ":" + StringUtils
                .trimToEmpty(version);
        }
        return pomName;
    }
}
