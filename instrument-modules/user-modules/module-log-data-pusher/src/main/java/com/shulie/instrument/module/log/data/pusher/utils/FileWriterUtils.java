package com.shulie.instrument.module.log.data.pusher.utils;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;

/**
 * @author guann1n9
 * @date 2022/4/14 9:48 AM
 */
public class FileWriterUtils {

    private static final Logger LOGGER = LoggerFactory.getLogger(FileWriterUtils.class);


    /**
     * 写入可执行文件
     * @param path  文件路径
     * @param content  内容
     * @return
     */
    public static boolean writeScript(String path, String content) {
        boolean write = write(path, content);
        if(! write ){
            return false;
        }
        File file = new File(path);
        file.setExecutable(true);
        return true;
    }


    /**
     * 文件写入
     * @param path  文件路径
     * @param content  内容
     * @return
     */
    public static boolean write(String path, String content) {
        BufferedWriter writer = null;
        try {
            writer = new BufferedWriter(new FileWriter(path));
            writer.write(content);
            return true;
        } catch (IOException e) {
            LOGGER.error("file write error",e);
        }finally {
            try {
                if (writer != null) {
                    writer.close();
                }
            } catch (Exception e) {
            }
        }
        return false;

    }


    /**
     * 删除文件
     * @param path
     * @return
     */
    public static boolean delete(String path) {
        File file = new File(path);
        try {
            return file.delete();
        } catch (Exception e) {
            LOGGER.error("delete file {} error",path);
        }
        return false;
    }

}
