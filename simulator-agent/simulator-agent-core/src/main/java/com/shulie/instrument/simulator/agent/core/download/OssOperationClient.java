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

import com.aliyun.oss.OSS;
import com.aliyun.oss.OSSClientBuilder;
import com.aliyun.oss.model.DownloadFileRequest;
import com.aliyun.oss.model.DownloadFileResult;

import java.io.File;

/**
 * @author angju
 * @date 2021/11/24 15:22
 */
public class OssOperationClient {

    private static OSS getOssClient(String endpoint, String accessKeyId, String accessKeySecret){
        return new OSSClientBuilder().build(endpoint, accessKeyId, accessKeySecret);
    }

    public static void download(String endpoint, String accessKeyId, String accessKeySecret,
                          String targetFilePath,String bucketName, String objectName) {
        OSS ossClient = null;
        try {
            ossClient = getOssClient(endpoint, accessKeyId, accessKeySecret);
            DownloadFileRequest downloadFileRequest = new DownloadFileRequest(bucketName, objectName);
            downloadFileRequest.setDownloadFile(targetFilePath + File.separator + objectName);
            DownloadFileResult downloadFileResult = ossClient.downloadFile(downloadFileRequest);
        }catch (Throwable e){
            throw new IllegalStateException("下载oss远程simulator包异常:" + e.getMessage());
        } finally {
            if (ossClient != null){
                ossClient.shutdown();
            }
        }
    }

}
