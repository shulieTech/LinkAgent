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
package com.pamirs.attach.plugin.shadowjob.common;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.net.URI;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import javax.tools.JavaCompiler;
import javax.tools.JavaFileObject;
import javax.tools.SimpleJavaFileObject;
import javax.tools.StandardJavaFileManager;
import javax.tools.ToolProvider;

public class ClassGeneratorManager {


    private static String classQuartzString = "package %s;\n" +
        "\n" +
        "import org.quartz.Job;\n" +
        "import org.quartz.JobExecutionContext;\n" +
        "import org.quartz.JobExecutionException;\n" +
        "import com.pamirs.pradar.internal.PradarInternalService;\n" +
        "\n" +
        "import java.lang.reflect.Method;\n" +
        "\n" +
        "public class %s extends %s {\n" +
        "\n" +
        "\n" +
        "    @Override\n" +
        "    public void #{method}(JobExecutionContext jobExecutionContext) {\n" +
        "        try{\n" +
        "            PradarInternalService.setClusterTest(true);\n" +
        "            super.#{method}(jobExecutionContext);\n" +
        "            PradarInternalService.setClusterTest(false);\n" +
        "        }catch(Exception e){\n" +
        "        }\n" +
        "    }\n" +
        "}\n";


    private static Class createClassWriteFile(String className, String parentClassName, String packageName, String classString, Integer type) throws Exception {
        String classesPath = Thread.currentThread().getContextClassLoader().loadClass(packageName + "." + parentClassName).getResource("").getPath();
        String sourcePath = classesPath.replace("target/classes", "src/main/java");
        String rootPath = classesPath.substring(0, classesPath.indexOf("/target"));
        String fileName = sourcePath + className;
        File file = new File(fileName + ".java");
        FileWriter fileWriter = new FileWriter(file);
        fileWriter.write(String.format(classString, packageName, className, parentClassName));
        fileWriter.flush();
        fileWriter.close();

        JavaCompiler compiler = ToolProvider.getSystemJavaCompiler();
        StandardJavaFileManager standardFileManager = compiler.getStandardFileManager(null, null, null);

        StringObject stringObject = new StringObject(fileName, String.format(classString, packageName, className, parentClassName));

        Iterable javaFileObjects = Arrays.asList(stringObject);

        String dest = rootPath + "/target/classes";
        List<String> options = new ArrayList<String>();
        options.add("-d");
        options.add(dest);
        JavaCompiler.CompilationTask task = compiler.getTask(null, standardFileManager, null, options, null, javaFileObjects);

        task.call();
        standardFileManager.close();
        URL[] urls = new URL[]{new URL("file:/" + rootPath + "/target/classes")};

        ClassLoader classLoader = new URLClassLoader(urls);
        return classLoader.loadClass(packageName + "." + className);
    }


    public static Class createQuartzClass(String className, String parentClassName, String packageName, Integer quartzType) throws Exception {
        if (1 == quartzType) {
            classQuartzString = classQuartzString.replaceAll("#\\{method}", "executeInternal");
        } else {
            classQuartzString = classQuartzString.replaceAll("#\\{method}", "execute");
        }
        return createClassWriteFile(className, parentClassName, packageName, classQuartzString, quartzType);
    }

    static class StringObject extends SimpleJavaFileObject {

        private String content;

        public StringObject(URI uri, JavaFileObject.Kind kind, String content) {
            super(uri, kind);
            this.content = content;
        }

        public StringObject(String name, String content) {
            super(URI.create("string:///" + name.replace('.', '/') + Kind.SOURCE.extension), Kind.SOURCE);
            System.out.println(URI.create("string:///" + name.replace('.', '/') + Kind.SOURCE.extension));
            System.out.println(Kind.SOURCE);
            this.content = content;
        }

        @Override
        public CharSequence getCharContent(boolean ignoreEncodingErrors) throws IOException {
            return this.content;
        }
    }
}
