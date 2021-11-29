package com.shulie.instrument.module.config.fetcher.utils;

import org.apache.commons.net.ftp.FTP;
import org.apache.commons.net.ftp.FTPClient;
import org.apache.commons.net.ftp.FTPReply;

import java.io.IOException;
import java.io.InputStream;

/**
 * @author angju
 * @date 2021/11/22 19:40
 */
public class FtpUtils {
    /** 本地字符编码 */
    private static String LOCAL_CHARSET = "GBK";

    // FTP协议里面，规定文件名编码为iso-8859-1
    private static String SERVER_CHARSET = "ISO-8859-1";

    public static boolean checkFtp(String ftpHost, int ftpPort, String ftpUserName, String ftpPassword, String basePath){
        FTPClient ftpClient = null;
        try {
            int reply;
            ftpClient = new FTPClient();
            ftpClient.connect(ftpHost, ftpPort);// 连接FTP服务器
            ftpClient.login(ftpUserName, ftpPassword);// 登陆FTP服务器
            if (!FTPReply.isPositiveCompletion(ftpClient.getReplyCode())) {
                ftpClient.disconnect();
                throw new IllegalStateException("未连接到FTP，用户名或密码错误。");
            }
            reply = ftpClient.getReplyCode();
            if (!FTPReply.isPositiveCompletion(reply)) {
                ftpClient.disconnect();
                throw new IllegalStateException("FTPReply isPositiveCompletion false");
            }
            if (!ftpClient.changeWorkingDirectory(basePath)){
                throw new IllegalStateException("basePath:" + basePath + " invalid ");
            }
            return true;
        } catch (IOException e) {
            throw new IllegalStateException(e.getMessage());
        } finally {
            if (ftpClient.isConnected()) {
                try {
                    //退出登录
                    ftpClient.logout();
                    //关闭连接
                    ftpClient.disconnect();
                } catch (IOException ioe) {
                }
            }
        }
    }


    /**
     * Description: 向FTP服务器上传文件
     *
     * @return 成功返回true，否则返回false
     */
    public static boolean uploadFile(String ftpHost, int ftpPort, String ftpUserName, String ftpPassword,
                                     String basePath, String filePath, String filename, InputStream input) {
        boolean result = false;
        FTPClient ftpClient = null;
        try {
            int reply;
            ftpClient = getFTPClient(ftpHost, ftpPort, ftpUserName, ftpPassword);
            reply = ftpClient.getReplyCode();
            if (!FTPReply.isPositiveCompletion(reply)) {
                ftpClient.disconnect();
                return result;
            }
            // 切换到上传目录
            if (!ftpClient.changeWorkingDirectory(basePath + filePath)) {
                // 如果目录不存在创建目录
                String[] dirs = filePath.split("/");
                String tempPath = basePath;
                for (String dir : dirs) {
                    if (null == dir || "".equals(dir))
                        continue;
                    tempPath += "/" + dir;
                    if (!ftpClient.changeWorkingDirectory(tempPath)) {
                        if (!ftpClient.makeDirectory(tempPath)) {
                            return result;
                        } else {
                            ftpClient.changeWorkingDirectory(tempPath);
                        }
                    }
                }
            }
            // 设置上传文件的类型为二进制类型
            if (FTPReply.isPositiveCompletion(ftpClient.sendCommand("OPTS UTF8", "ON"))) {// 开启服务器对UTF-8的支持，如果服务器支持就用UTF-8编码，否则就使用本地编码（GBK）.
                LOCAL_CHARSET = "UTF-8";
            }
            ftpClient.setControlEncoding(LOCAL_CHARSET);
            ftpClient.enterLocalPassiveMode();// 设置被动模式
            ftpClient.setFileType(FTP.BINARY_FILE_TYPE);// 设置传输的模式
            // 上传文件
            filename = new String(filename.getBytes(LOCAL_CHARSET), SERVER_CHARSET);
            if (!ftpClient.storeFile(filename, input)) {
                return result;
            }

            if(null != input){
                input.close();
            }

            result = true;
        } catch (IOException e) {
            throw new IllegalStateException(e.getMessage());
        } finally {
            if (ftpClient.isConnected()) {
                try {
                    //退出登录
                    ftpClient.logout();
                    //关闭连接
                    ftpClient.disconnect();
                } catch (IOException ioe) {
                }
            }
        }
        return result;
    }


    /**
     * 获取FTPClient对象
     *
     * @param ftpHost
     *            FTP主机服务器
     *
     * @param ftpPassword
     *            FTP 登录密码
     *
     * @param ftpUserName
     *            FTP登录用户名
     *
     * @param ftpPort
     *            FTP端口 默认为21
     *
     * @return
     */
    public static FTPClient getFTPClient(String ftpHost, int ftpPort, String ftpUserName, String ftpPassword) throws IOException{
        FTPClient ftpClient = null;

        ftpClient = new FTPClient();
        ftpClient.connect(ftpHost, ftpPort);// 连接FTP服务器
        ftpClient.login(ftpUserName, ftpPassword);// 登陆FTP服务器
        if (!FTPReply.isPositiveCompletion(ftpClient.getReplyCode())) {
            ftpClient.disconnect();
            throw new IllegalStateException("未连接到FTP，用户名或密码错误。");
        }
        return ftpClient;
    }
}
