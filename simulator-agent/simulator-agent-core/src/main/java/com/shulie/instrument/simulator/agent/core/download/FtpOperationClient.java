/*
 * *
 *  * Copyright 2021 Shulie Technology, Co.Ltd
 *  * Email: shulie@shulie.io
 *  * Licensed under the Apache License, Version 2.0 (the "License");
 *  * you may not use this file except in compliance with the License.
 *  * You may obtain a copy of the License at
 *  *
 *  * http://www.apache.org/licenses/LICENSE-2.0
 *  *
 *  * Unless required by applicable law or agreed to in writing, software
 *  * distributed under the License is distributed on an "AS IS" BASIS,
 *  * See the License for the specific language governing permissions and
 *  * limitations under the License.
 *
 */

package com.shulie.instrument.simulator.agent.core.download;

import org.apache.commons.lang.StringUtils;
import org.apache.commons.net.ftp.FTP;
import org.apache.commons.net.ftp.FTPClient;
import org.apache.commons.net.ftp.FTPReply;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.net.SocketException;

/**
 * @author angju
 * @date 2021/11/24 15:23
 */
public class FtpOperationClient{
    private static final Logger logger = LoggerFactory.getLogger(FtpOperationClient.class);

    /** 本地字符编码 */
    private static String LOCAL_CHARSET = "GBK";

    // FTP协议里面，规定文件名编码为iso-8859-1
    private String SERVER_CHARSET = "ISO-8859-1";



    private static FTPClient getFTPClient(String ftpHost, int ftpPort, String ftpUserName, String ftpPassword) {
        FTPClient ftpClient = null;
        try {
            ftpClient = new FTPClient();
            ftpClient.connect(ftpHost, ftpPort);// 连接FTP服务器
            ftpClient.login(ftpUserName, ftpPassword);// 登陆FTP服务器
            if (!FTPReply.isPositiveCompletion(ftpClient.getReplyCode())) {
                ftpClient.disconnect();
                throw new IllegalStateException("未连接到FTP，用户名或密码错误。");
            }
        } catch (SocketException e) {
            logger.error("FTP的IP地址可能错误，请正确配置", e);
            throw new IllegalStateException("FTP的IP地址可能错误，请正确配置。");
        } catch (IOException e) {
            logger.error("FTP的端口错误,请正确配置", e);
            throw new IllegalStateException("FTP的端口错误,请正确配置。");
        }
        return ftpClient;
    }

    /**
     * 从FTP服务器下载文件
     *
     * @param ftpHost FTP IP地址
     *
     * @param ftpUserName FTP 用户名
     *
     * @param ftpPassword FTP用户名密码
     *
     * @param ftpPort FTP端口
     *
     * @param ftpPath FTP服务器中文件所在路径 格式： ftptest/aa
     *
     * @param localPath 下载到本地的位置 格式：H:/download
     *
     * @param fileName 文件名称
     */
    public static boolean downloadFtpFile(String ftpHost, String ftpUserName, String ftpPassword, int ftpPort,
                                       String ftpPath, String localPath, String fileName) {

        FTPClient ftpClient = null;
        OutputStream os = null;
        try {
            ftpClient = getFTPClient(ftpHost, ftpPort, ftpUserName, ftpPassword);
            // 设置上传文件的类型为二进制类型
            if (FTPReply.isPositiveCompletion(ftpClient.sendCommand("OPTS UTF8", "ON"))) {// 开启服务器对UTF-8的支持，如果服务器支持就用UTF-8编码，否则就使用本地编码（GBK）.
                LOCAL_CHARSET = "UTF-8";
            }
            ftpClient.setControlEncoding(LOCAL_CHARSET);
            ftpClient.enterLocalPassiveMode();// 设置被动模式
            ftpClient.setFileType(FTP.BINARY_FILE_TYPE);// 设置传输的模式
            ftpClient.setBufferSize(1024 * 1024 * 5);
            // 上传文件
            //对中文文件名进行转码，否则中文名称的文件下载失败
            for (String s : ftpPath.split("/")){
                if (!StringUtils.isBlank(s)){
                    boolean r = ftpClient.changeWorkingDirectory(s);
                    if (!r){
                        throw new IllegalArgumentException("文件所在路径不正确,path is " + ftpPath);
                    }
                }
            }

            // 第一种方式下载文件(推荐)
            File localFile = new File(localPath + File.separatorChar + fileName);
            localFile.deleteOnExit();
            os = new FileOutputStream(localFile);
            boolean r = ftpClient.retrieveFile(fileName, os);
            return r;
        } catch (FileNotFoundException e) {
            logger.error("没有找到" + ftpPath + "文件", e);
            throw new IllegalArgumentException("没有找到" + ftpPath + "文件");
        } catch (SocketException e) {
            logger.error("连接FTP失败.", e);
            throw new IllegalArgumentException("连接FTP失败.");
        } catch (IOException e) {
            logger.error("文件读取错误.", e);
            throw new IllegalStateException("文件读取错误.");
        } finally {
            if (os != null){
                try {
                    os.close();
                } catch (IOException ignore) {
                }
            }
            if (ftpClient.isConnected()) {
                try {
                    //退出登录
                    ftpClient.logout();
                    //关闭连接
                    ftpClient.disconnect();
                } catch (IOException e) {
                }
            }
        }
    }
}
