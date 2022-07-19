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
package com.shulie.instrument.module.log.data.pusher.utils;

import com.pamirs.pradar.common.Charsets;
import com.shulie.instrument.module.log.data.pusher.log.PullLogResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

/**
 * 文件读取工具类
 *
 * @author wangjian
 * @since 2021/1/5 20:48
 */
public class FileReaderUtils {

    private static final Logger LOGGER = LoggerFactory.getLogger(FileReaderUtils.class);

    /**
     * 倒序读取
     *
     * @param filePath 文件路径
     * @param lineSize 读取行数
     * @return 文件内容
     */
    public static String reverseReadLines(String filePath, int lineSize) {
        File file = new File(filePath);
        ReversedLinesFileReader object = null;
        try {
            object = new ReversedLinesFileReader(file, Charsets.UTF_8);
        } catch (IOException e) {
            LOGGER.error("reverseReadLines init file error:", e);
        }
        if (null == object) {
            return "";
        }
        int counter = 0;
        StringBuilder sb = new StringBuilder();
        List<String> reverseLines = new ArrayList<String>();
        while (counter < lineSize) {
            try {
                final String line = object.readLine();
                if (line == null) {
                    break;
                }
                reverseLines.add(line);
            } catch (IOException e) {
                LOGGER.error("reverseReadLines read file error:", e);
            }
            counter++;
        }
        for (int i = reverseLines.size() - 1; i >= 0; i--) {
            sb.append(reverseLines.get(i)).append('\n');
        }
        try {
            object.close();
        } catch (IOException e) {
            LOGGER.error("reverseReadLines close file error:", e);
        }
        if (sb.length() > 0) {
            sb.deleteCharAt(sb.length() - 1);
        }
        return sb.toString();
    }

    /**
     * 倒序读取
     *
     * @param filePath 文件路径
     * @param lineSize 读取行数
     * @return 文件内容
     */
    public static PullLogResponse.Log readLines(String filePath, int startLine, int lineSize) {
        PullLogResponse.Log log = new PullLogResponse.Log();
        File file = new File(filePath);
        StringBuilder sb = new StringBuilder();
        try {
            BufferedReader reader = new BufferedReader(new FileReader(file));
            int lines = 0;
            int count = 0;
            String line = reader.readLine();
            while (line != null) {
                lines++;
                if (lines >= startLine) {
                    sb.append(line).append("\n");
                    count++;
                }
                if (count >= lineSize) {
                    break;
                }
                line = reader.readLine();
            }
            log.setEndLine(lines);
            reader.close();
        } catch (FileNotFoundException e) {
            LOGGER.error("readLines file not found:", e);
        } catch (IOException e) {
            LOGGER.error("readLines file error:", e);
        }
        log.setLogContent(sb.toString());

        return log;
    }


    public static int countTotalLines(String filePath) {
        File file = new File(filePath);
        try {
            LineNumberReader lineNumberReader = new LineNumberReader(new FileReader(file));
            lineNumberReader.skip(Long.MAX_VALUE);
            int lines = lineNumberReader.getLineNumber();
            lineNumberReader.close();
            return lines;
        } catch (FileNotFoundException e) {
            LOGGER.error("countTotalLines file not found:", e);
        } catch (IOException e) {
            LOGGER.error("countTotalLines file error:", e);
        }
        return -1;
    }


    /**
     * 倒序读取
     *
     * @param file 文件路径
     * @param lineSize 读取行数
     * @Param grepParam 过滤参数
     * @return 文件内容
     */
    public static String reverseReadLinesWithGrep(File file, int lineSize, List<String> grepParam) {
        ReversedLinesFileReader reader = null;
        try {
            reader = new ReversedLinesFileReader(file, Charsets.UTF_8);
            int count = 0;
            StringBuilder builder = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null && count < lineSize) {
                if(grepParam.isEmpty() || contains(line,grepParam)){
                    builder.append(line).append("\n");
                    count++;
                }
            }
            return builder.toString();
        } catch (IOException e) {
            LOGGER.error("reverseReadLines init file error:", e);
        } finally {
            try {
                if (reader != null) {
                    reader.close();
                }
            } catch (Exception e) {
            }
        }
        return "";
    }


    /**
     * 获取指定路径下指定前缀的最新日志(日志拆分)
     * @param path 日志路径
     * @param fileRgex 文件名正则
     * @return
     */
    public static File findNewestFile(String path,String fileRgex){
        File logPath = new File(path);
        if(!logPath.isDirectory()){
            return null;
        }
        long lastModify = 0L;
        File latest = null;
        Pattern pattern = Pattern.compile(fileRgex);
        File[] files = logPath.listFiles();
        for (File file : files) {
            if(pattern.matcher(file.getName()).find() && file.lastModified() > lastModify){
                lastModify = file.lastModified();
                latest = file;
            }
        }
        return latest;
    }


    private static boolean contains(String line,List<String> grepParam){
        for (String s : grepParam) {
            if(!line.contains(s)){
                return false;
            }
        }
        return true;
    }

}
