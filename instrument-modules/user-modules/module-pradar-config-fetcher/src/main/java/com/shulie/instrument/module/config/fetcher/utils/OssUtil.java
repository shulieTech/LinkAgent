package com.shulie.instrument.module.config.fetcher.utils;

import com.aliyun.oss.OSS;
import com.aliyun.oss.OSSClientBuilder;
import com.aliyun.oss.model.AppendObjectRequest;
import com.aliyun.oss.model.AppendObjectResult;
import com.aliyun.oss.model.BucketInfo;
import com.aliyun.oss.model.ObjectMetadata;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;

/**
 * @author angju
 * @date 2021/11/22 19:39
 */
public class OssUtil {
    public static boolean checkOss(String endpoint, String accessKeyId, String accessKeySecret, String buckName){
        OSS ossClient = null;
        try {
            ossClient = new OSSClientBuilder().build(endpoint, accessKeyId, accessKeySecret);
            BucketInfo bucketInfo = ossClient.getBucketInfo(buckName);
            return true;
        } catch (Throwable e){
            throw new IllegalStateException(e.getMessage());
        } finally {
            if (ossClient != null){
                ossClient.shutdown();
            }
        }
    }


    public static boolean upgrade(String endpoint, String accessKeyId, String accessKeySecret, String buckName,
                    String objectName) {

        OSS ossClient = null;
        try {
            ossClient = new OSSClientBuilder().build(endpoint, accessKeyId, accessKeySecret);
            String sourcePath = null;
            URL url = new URL(sourcePath);
            InputStream is = url.openStream();
            writeOss(is, buckName, objectName, ossClient);
        } catch (Throwable e){
            throw new IllegalStateException(e.getMessage());
        }finally {
            if (ossClient != null){
                ossClient.shutdown();
            }
        }
        return true;
    }


    private static  void writeOss(InputStream inputStream, String bucketName, String objectName, OSS ossClient) throws IOException {
        byte[] buffer = new byte[1024];
        int len = 0;
        long pos = 0L;
        while((len = inputStream.read(buffer)) != -1) {
//            bos.write(buffer, 0, len);
            pos = writeOss(bucketName, objectName, pos, new ByteArrayInputStream(buffer), ossClient);
            if (inputStream.available() < 1024 && inputStream.available() > 0){
                buffer = new byte[inputStream.available()];
            }
        }
    }


    public static long writeOss(String bucketName, String objectName, long pos, ByteArrayInputStream byteArrayInputStream,
                                OSS ossClient){
        ObjectMetadata meta = new ObjectMetadata();
        // 指定上传的内容类型。
        meta.setContentType("text/plain");

        // 通过AppendObjectRequest设置多个参数。
        AppendObjectRequest appendObjectRequest = new AppendObjectRequest(bucketName, objectName, byteArrayInputStream,meta);


        // 第一次追加。
        // 设置文件的追加位置。
        appendObjectRequest.setPosition(pos);
        AppendObjectResult appendObjectResult = ossClient.appendObject(appendObjectRequest);

        return appendObjectResult.getNextPosition();
    }
}
