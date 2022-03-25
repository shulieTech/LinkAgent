//
// Source code recreated from a .class file by IntelliJ IDEA
// (powered by FernFlower decompiler)
//

package swiftsdk;

import org.apache.http.HttpEntity;
import org.apache.http.entity.ByteArrayEntity;
import org.apache.http.entity.FileEntity;
import org.json.JSONObject;
import swiftsdk.errors.ErrorCodeMessageEnum;
import swiftsdk.errors.SfOssException;
import swiftsdk.factory.SFOSSFactory;
import swiftsdk.factory.SFOSSFactoryImpl;
import swiftsdk.util.Part;
import swiftsdk.util.SFOSSClientUtils;
import swiftsdk.util.TokenCache;

import java.io.*;
import java.net.URLEncoder;
import java.nio.ByteBuffer;
import java.nio.channels.Channels;
import java.nio.channels.ReadableByteChannel;
import java.util.*;

public class SfOssClient {
    private static volatile SfOssClient sfOssClient;
    private SwiftConfiguration config;
    private TokenCache tokenCache;
    private SFOSSFactory serviceFactory = new SFOSSFactoryImpl();

    public static SfOssClient getInstance(SwiftConfiguration config) {
        if (sfOssClient == null) {
            Class var1 = SfOssClient.class;
            synchronized(SfOssClient.class) {
                if (sfOssClient == null) {
                    sfOssClient = new SfOssClient(config);
                }
            }
        }

        return sfOssClient;
    }

    public SfOssClient(SwiftConfiguration config) {
        this.config = config;
        this.tokenCache = new TokenCache(config);
        SFOSSObjectManager.getInstance(this.tokenCache);
    }

    public SwiftConfiguration getConfiguration() {
        return this.config;
    }

    public Map<String, String> headAccountMeta() throws SfOssException {
        String url = SFOSSClientUtils.getUrl(this.config, "v1");
        return this.serviceFactory.getAccountService().headAccountMeta(url, this.tokenCache.getToken(), this.config);
    }

    public boolean postAccountMeta(Map<String, String> headers) throws SfOssException {
        String url = SFOSSClientUtils.getUrl(this.config, "v1");
        return this.serviceFactory.getAccountService().postAccountMeta(url, headers, this.tokenCache.getToken(), this.config);
    }

    public List<String> getContainerList(String beginName, Integer limit) throws SfOssException {
        if (limit == null || limit > 10000 || limit <= 0) {
            limit = 10000;
        }

        String url = SFOSSClientUtils.getUrl(this.config, "v1") + "?format=plain&limit=" + limit;
        if (beginName != null && !beginName.isEmpty()) {
            try {
                beginName = URLEncoder.encode(beginName, "UTF-8");
                beginName = beginName.replace("+", "%20");
            } catch (Exception var5) {
                throw new SfOssException("Wrong beginName");
            }

            url = url + "&marker=" + beginName;
        }

        return this.serviceFactory.getContainerService().getContainerList(url, this.tokenCache.getToken(), this.config);
    }

    public List<String> getContainerList(String beginName, Integer limit, String prefix, String end_marker) throws SfOssException {
        if (limit == null || limit > 10000 || limit <= 0) {
            limit = 10000;
        }

        String url = SFOSSClientUtils.getUrl(this.config, "v1") + "?format=plain&limit=" + limit;
        if (beginName != null && !beginName.isEmpty()) {
            try {
                beginName = URLEncoder.encode(beginName, "UTF-8");
                beginName = beginName.replace("+", "%20");
            } catch (Exception var9) {
                throw new SfOssException("Wrong beginName");
            }

            url = url + "&marker=" + beginName;
        }

        if (prefix != null && !prefix.isEmpty()) {
            try {
                prefix = URLEncoder.encode(prefix, "UTF-8");
                prefix = prefix.replace("+", "%20");
            } catch (Exception var8) {
                throw new SfOssException("Wrong prefix");
            }

            url = url + "&prefix=" + prefix;
        }

        if (end_marker != null && !end_marker.isEmpty()) {
            try {
                end_marker = URLEncoder.encode(end_marker, "UTF-8");
                end_marker = end_marker.replace("+", "%20");
            } catch (Exception var7) {
                throw new SfOssException("Wrong end_marker");
            }

            url = url + "&end_marker=" + end_marker;
        }

        return this.serviceFactory.getContainerService().getContainerList(url, this.tokenCache.getToken(), this.config);
    }

    public boolean createContainer(String container) throws SfOssException {
        String url = SFOSSClientUtils.getUrl(this.config, "v1", container);
        return this.serviceFactory.getContainerService().createContainer(url, this.tokenCache.getToken(), this.config);
    }

    public boolean postContainerMeta(String container, Map<String, String> headers) throws SfOssException {
        String url = SFOSSClientUtils.getUrl(this.config, "v1", container);
        return this.serviceFactory.getContainerService().postContainerMeta(url, headers, this.tokenCache.getToken(), this.config);
    }

    public Map<String, String> headContainerMeta(String container) throws SfOssException {
        String url = SFOSSClientUtils.getUrl(this.config, "v1", container);
        return this.serviceFactory.getContainerService().headContainerMeta(url, this.tokenCache.getToken(), this.config);
    }

    public boolean deleteContainer(String container, boolean force) throws SfOssException {
        String url = SFOSSClientUtils.getUrl(this.config, "v1", container);
        return this.serviceFactory.getContainerService().deleteContainer(url, container, force, this.tokenCache.getToken(), this.config);
    }

    public boolean clearContainer(String container) throws SfOssException {
        String url = SFOSSClientUtils.getUrl(this.config, "v1", container);
        return this.serviceFactory.getContainerService().clearContainer(container, url, this.tokenCache.getToken(), this.config);
    }

    public List<String> getObjectList(String container, String beginName, Integer limit) throws SfOssException {
        return this.getObjectList(container, beginName, limit, "", "", "");
    }

    public List<String> getObjectList(String container, String beginName, Integer limit, String prefix, String end_marker, String delimiter) throws SfOssException {
        if (limit == null || limit > 10000) {
            limit = 10000;
        }

        String url = SFOSSClientUtils.getUrl(this.config, "v1", container) + "?format=plain&limit=" + limit;
        if (beginName != null && !beginName.isEmpty()) {
            try {
                beginName = URLEncoder.encode(beginName, "UTF-8");
                beginName = beginName.replace("+", "%20");
            } catch (Exception var12) {
                throw new SfOssException("Wrong beginName");
            }

            url = url + "&marker=" + beginName;
        }

        if (prefix != null && !prefix.isEmpty()) {
            try {
                prefix = URLEncoder.encode(prefix, "UTF-8");
                prefix = prefix.replace("+", "%20");
            } catch (Exception var11) {
                throw new SfOssException("Wrong prefix");
            }

            url = url + "&prefix=" + prefix;
        }

        if (delimiter != null && !delimiter.isEmpty()) {
            try {
                delimiter = URLEncoder.encode(delimiter, "UTF-8");
                delimiter = delimiter.replace("+", "%20");
            } catch (Exception var10) {
                throw new SfOssException("Wrong delimiter");
            }

            url = url + "&delimiter=" + delimiter;
        }

        if (end_marker != null && !end_marker.isEmpty()) {
            try {
                end_marker = URLEncoder.encode(end_marker, "UTF-8");
                end_marker = end_marker.replace("+", "%20");
            } catch (Exception var9) {
                throw new SfOssException("Wrong delimiter");
            }

            url = url + "&end_marker=" + end_marker;
        }

        return this.serviceFactory.getObjectService().getObjectList(url, this.tokenCache.getToken(), this.config);
    }

    public boolean postObjectMeta(String container, String objectName, Map<String, String> headers) throws SfOssException {
        String url = SFOSSClientUtils.getUrl(this.config, "v1", container, objectName);
        return this.serviceFactory.getObjectService().postObjectMeta(url, headers, this.tokenCache.getToken(), this.config);
    }

    public boolean uploadTar(String container, String filePath) throws SfOssException {
        return this.uploadTar(container, filePath, new HashMap());
    }

    public boolean uploadTar(String container, String filePath, Map<String, String> headers) throws SfOssException {
        if (container != null && !container.isEmpty()) {
            String url = SFOSSClientUtils.getUrl(this.config, "v1", container);
            String lower = filePath.toLowerCase();
            if (lower.endsWith("tar")) {
                url = url + "?extract-archive=tar";
            } else if (lower.endsWith(".tar.gz")) {
                url = url + "?extract-archive=tar.gz";
            } else {
                if (!lower.endsWith(".tar.bz2")) {
                    throw new SfOssException(ErrorCodeMessageEnum.VALIDATE_ZIP.getIndex(), ErrorCodeMessageEnum.VALIDATE_ZIP.getMessage());
                }

                url = url + "?extract-archive=tar.bz2";
            }

            try {
                return this.serviceFactory.getObjectService().doUploadTar(url, new FileEntity(new File(filePath)), this.tokenCache.getToken(), this.config, headers);
            } catch (SfOssException var10) {
                if (var10.getErrorCode() == ErrorCodeMessageEnum.RESOURCE_NOT_FIND.getIndex()) {
                    int cont = 0;

                    while(!this.checkContainer(container)) {
                        this.createContainer(container);
                        ++cont;

                        try {
                            Thread.sleep(1000L);
                        } catch (InterruptedException var9) {
                            continue;
                        }

                        if (cont > 5) {
                            return false;
                        }
                    }

                    return this.serviceFactory.getObjectService().doUploadTar(url, new FileEntity(new File(filePath)), this.tokenCache.getToken(), this.config, headers);
                } else {
                    throw var10;
                }
            }
        } else {
            return false;
        }
    }

    public boolean uploadFile(String container, String objectName, String filePath) throws SfOssException {
        return this.uploadFile(container, objectName, filePath, new HashMap());
    }

    public boolean uploadFile(String container, String objectName, String filePath, Map<String, String> headers) throws SfOssException {
        return this.uploadFile(container, objectName, filePath, new HashMap(), "");
    }

    public boolean uploadFile(String container, String objectName, String filePath, Map<String, String> headers, String query) throws SfOssException {
        if (container != null && !container.isEmpty()) {
            File f = new File(filePath);
            if (!f.isFile()) {
                throw new SfOssException(ErrorCodeMessageEnum.FILE_ERROR.getIndex(), ErrorCodeMessageEnum.FILE_ERROR.getMessage() + filePath);
            } else {
                String url = SFOSSClientUtils.getUrl(this.config, "v1", container, objectName);
                if (query != null && !query.isEmpty()) {
                    url = url + "?" + query;
                }

                try {
                    return this.serviceFactory.getObjectService().doUpload(url, new FileEntity(f), this.tokenCache.getToken(), this.config, headers);
                } catch (SfOssException var12) {
                    if (var12.getErrorCode() == ErrorCodeMessageEnum.RESOURCE_NOT_FIND.getIndex()) {
                        int cont = 0;

                        while(!this.checkContainer(container)) {
                            this.createContainer(container);
                            ++cont;

                            try {
                                Thread.sleep(1000L);
                            } catch (InterruptedException var11) {
                                continue;
                            }

                            if (cont > 5) {
                                return false;
                            }
                        }

                        return this.serviceFactory.getObjectService().doUpload(url, new FileEntity(f), this.tokenCache.getToken(), this.config, headers);
                    } else {
                        throw var12;
                    }
                }
            }
        } else {
            return false;
        }
    }

    public boolean uploadObject(String container, String objectName, InputStream objectStream) throws SfOssException, IOException {
        return this.uploadObject(container, objectName, objectStream, -1);
    }

    public boolean uploadObject(String container, String objectName, InputStream objectStream, int streamSize) throws SfOssException, IOException {
        return this.uploadObject(container, objectName, objectStream, streamSize, new HashMap(), "");
    }

    public boolean uploadObject(String container, String objectName, InputStream objectStream, int streamSize, Map<String, String> headers) throws SfOssException, IOException {
        return this.uploadObject(container, objectName, objectStream, streamSize, new HashMap(), "");
    }

    public boolean uploadObject(String container, String objectName, InputStream objectStream, int streamSize, Map<String, String> headers, String query) throws SfOssException, IOException {
        if (streamSize == -1) {
            streamSize = objectStream.available();
        }

        ReadableByteChannel channel = Channels.newChannel(objectStream);
        ByteBuffer buffer = ByteBuffer.allocate(streamSize);
        channel.read(buffer);
        return this.uploadObject(container, objectName, buffer.array(), headers, query);
    }

    public boolean uploadObject(String container, String objectName, byte[] objectBinary) throws SfOssException {
        return this.uploadObject(container, objectName, objectBinary, new HashMap(), "");
    }

    public boolean uploadObject(String container, String objectName, byte[] objectBinary, Map<String, String> headers) throws SfOssException {
        return this.uploadObject(container, objectName, objectBinary, headers, "");
    }

    public boolean uploadObject(String container, String objectName, byte[] objectBinary, Map<String, String> headers, String query) throws SfOssException {
        if (container != null && !container.isEmpty()) {
            String url = SFOSSClientUtils.getUrl(this.config, "v1", container, objectName);
            if (query != null && !query.isEmpty()) {
                url = url + "?" + query;
            }

            try {
                return this.serviceFactory.getObjectService().doUpload(url, new ByteArrayEntity(objectBinary), this.tokenCache.getToken(), this.config, headers);
            } catch (SfOssException var11) {
                if (var11.getErrorCode() == ErrorCodeMessageEnum.RESOURCE_NOT_FIND.getIndex()) {
                    int cont = 0;

                    while(!this.checkContainer(container)) {
                        this.createContainer(container);
                        ++cont;

                        try {
                            Thread.sleep(1000L);
                        } catch (InterruptedException var10) {
                            continue;
                        }

                        if (cont > 5) {
                            return false;
                        }
                    }

                    return this.serviceFactory.getObjectService().doUpload(url, new ByteArrayEntity(objectBinary), this.tokenCache.getToken(), this.config, headers);
                } else {
                    throw var11;
                }
            }
        } else {
            return false;
        }
    }

    public boolean uploadManifast(String container, String objectName, byte[] objectBinary, Map<String, String> headers) throws SfOssException {
        if (container != null && !container.isEmpty()) {
            if (headers == null) {
                headers = new HashMap();
            }

            String url = SFOSSClientUtils.getUrl(this.config, "v1", container, objectName) + "?multipart-manifest=put";

            try {
                return this.serviceFactory.getObjectService().doUpload(url, new ByteArrayEntity(objectBinary), this.tokenCache.getToken(), this.config, (Map)headers);
            } catch (SfOssException var10) {
                if (var10.getErrorCode() == ErrorCodeMessageEnum.RESOURCE_NOT_FIND.getIndex()) {
                    int cont = 0;

                    while(!this.checkContainer(container)) {
                        this.createContainer(container);
                        ++cont;

                        try {
                            Thread.sleep(1000L);
                        } catch (InterruptedException var9) {
                            continue;
                        }

                        if (cont > 5) {
                            return false;
                        }
                    }

                    return this.serviceFactory.getObjectService().doUpload(url, new ByteArrayEntity(objectBinary), this.tokenCache.getToken(), this.config, (Map)headers);
                } else {
                    throw var10;
                }
            }
        } else {
            return false;
        }
    }

    public boolean uploadBigObject(String container, String objectName, String fileName, int partSize) throws SfOssException, IOException {
        InputStream file = new FileInputStream(fileName);
        long size = (long)file.available();
        String segmentsContainer = container + "+segments";
        if (size < (long)partSize) {
            throw new SfOssException(ErrorCodeMessageEnum.NOT_GE_FILE_SIZE.getIndex(), ErrorCodeMessageEnum.NOT_GE_FILE_SIZE.getMessage());
        } else {
            int[] rv = SFOSSClientUtils.calculateMultipartSize(size, partSize);
            int partCount = rv[0];
            int lastPartSize = rv[1];
            int expectedReadSize = partSize;
            boolean cleanPart = false;
            Part[] parts = new Part[partCount];
            int cont = 0;

            while(!this.checkContainer(segmentsContainer)) {
                this.createContainer(segmentsContainer);
                ++cont;

                try {
                    Thread.sleep(1000L);
                } catch (InterruptedException var26) {
                    continue;
                }

                if (cont > 5) {
                    return false;
                }
            }

            while(!this.checkContainer(container)) {
                this.createContainer(container);
                ++cont;

                try {
                    Thread.sleep(1000L);
                } catch (InterruptedException var25) {
                    continue;
                }

                if (cont > 5) {
                    return false;
                }
            }

            int partNumber;
            byte[] b;
            for(partNumber = 0; partNumber < partCount; ++partNumber) {
                if (partNumber == partCount - 1) {
                    expectedReadSize = lastPartSize;
                }

                b = new byte[expectedReadSize];
                int hasRead = file.read(b);
                if (hasRead > 0) {
                    try {
                        String url = SFOSSClientUtils.getUrl(this.config, "v1", segmentsContainer, objectName) + "/" + size + "/" + partSize + "/" + partNumber;
                        boolean ret = this.serviceFactory.getObjectService().doUpload(url, new ByteArrayEntity(b), this.tokenCache.getToken(), this.config, new HashMap());
                        parts[partNumber] = new Part(partNumber, objectName + "/" + size + "/" + partSize + "/" + partNumber);
                        if (!ret) {
                            cleanPart = true;
                        }
                    } catch (Exception var22) {
                        throw new SfOssException(var22);
                    }
                }
            }

            if (!cleanPart) {
                List<JSONObject> maniFestList = new ArrayList();

                for(int i = 0; i < partCount; ++i) {
                    Part part = parts[i];
                    JSONObject segment = new JSONObject();
                    segment.put("path", "/" + segmentsContainer + "/" + part.name());
                    maniFestList.add(segment);
                }

                b = maniFestList.toString().getBytes("UTF-8");
                boolean ret = false;

                try {
                    ret = this.uploadManifast(container, objectName, b, new HashMap());
                } catch (SfOssException var24) {
                    if (var24.getErrorCode() != ErrorCodeMessageEnum.RESOURCE_NOT_FIND.getIndex()) {
                        throw var24;
                    }

                    int cont1 = 0;

                    while(!this.checkContainer(container)) {
                        this.createContainer(container);
                        ++cont1;

                        try {
                            Thread.sleep(1000L);
                        } catch (InterruptedException var23) {
                            continue;
                        }

                        if (cont1 > 5) {
                            return false;
                        }
                    }

                    ret = this.uploadManifast(container, objectName, b, new HashMap());
                }

                if (!ret) {
                    cleanPart = true;
                }
            }

            if (!cleanPart) {
                return true;
            } else {
                for(partNumber = 0; partNumber < partCount; ++partNumber) {
                    this.deleteContent(segmentsContainer, parts[partNumber].name());
                }

                this.deleteContent(container, objectName);
                return false;
            }
        }
    }

    private boolean abortMultipartUpload(String segmentsContainer, String object, int partCount, Part[] parts) throws SfOssException {
        for(int partNumber = 0; partNumber < partCount; ++partNumber) {
            this.deleteContent(segmentsContainer, parts[partNumber].name());
        }

        this.deleteContent(segmentsContainer, object);
        return false;
    }

    private boolean completeMultipart(String container, String obj, Part[] totalParts, int partCount, Map<String, String> header) throws SfOssException {
        try {
            String segmentsContainer = container + "+segments";
            List<JSONObject> maniFestList = new ArrayList();
            int l = totalParts.length;
            String tmp = "";

            for(int partNumber = 0; partNumber < l; ++partNumber) {
                Part part = totalParts[partNumber];
                if (part == null) {
                    break;
                }

                JSONObject segment = new JSONObject();
                tmp = segmentsContainer + "/" + part.name();
                segment.put("path", "/" + tmp);
                maniFestList.add(segment);
            }

            byte[] b = maniFestList.toString().getBytes("UTF-8");
            return this.uploadManifast(container, obj, b, header);
        } catch (Exception var13) {
            throw new SfOssException(var13);
        }
    }

    public boolean putObject(String containerName, String objectName, String fileName, Long size, int partSzie, Map<String, String> headerMap) throws SfOssException {
        if (fileName != null && !"".equals(fileName)) {
            File file1 = new File(fileName);
            if (!file1.isFile()) {
                throw new SfOssException("'" + fileName + "': not a regular file");
            } else {
                if (size == null) {
                    size = file1.length();
                }

                if (partSzie < 5242880) {
                    partSzie = 52428800;
                }

                if (partSzie > 134217728) {
                    partSzie = 134217728;
                }

                if (partSzie % 1024 != 0) {
                    throw new SfOssException("PartSize must be a multiple of 1024!");
                } else {
                    try {
                        RandomAccessFile file = new RandomAccessFile(file1, "r");

                        boolean var9;
                        try {
                            var9 = this.putObject(containerName, objectName, size, partSzie, file, headerMap);
                        } finally {
                            file.close();
                        }

                        return var9;
                    } catch (Exception var14) {
                        throw new SfOssException(var14);
                    }
                }
            }
        } else {
            throw new SfOssException("empty file name is not allowed");
        }
    }

    public boolean putObject(String containerName, String objectName, InputStream data, Long size, int partSzie, Map<String, String> headerMap) throws SfOssException {
        try {
            boolean var8;
            try {
                if (partSzie < 5242880) {
                    partSzie = 52428800;
                }

                if (partSzie > 134217728) {
                    partSzie = 134217728;
                }

                BufferedInputStream data1 = new BufferedInputStream(data);
                var8 = this.putObject(containerName, objectName, size, partSzie, data1, headerMap);
            } finally {
                data.close();
            }

            return var8;
        } catch (Exception var13) {
            throw new SfOssException(var13);
        }
    }

    private boolean putObject(String containerName, String objectName, Long size, int partSize, Object data, Map<String, String> headerMap) throws SfOssException {
        boolean unknownSize = false;
        if (headerMap == null) {
            headerMap = new HashMap();
        }

        if (size == null) {
            unknownSize = true;
            size = 42949672960L;
        }

        if (size > 5242880L && size > (long)partSize) {
            int[] rv = SFOSSClientUtils.calculateMultipartSize(size, partSize);
            int partCount = rv[0];
            int lastPartSize = rv[1];
            Part[] totalParts = new Part[partCount];
            String segmentsContainer = containerName + "+segments";
            boolean t;
            if (!this.checkContainer(segmentsContainer)) {
                t = this.createContainer(segmentsContainer);
                if (!t) {
                    throw new SfOssException("Can't create segments container");
                }
            }

            if (!this.checkContainer(containerName)) {
                t = this.createContainer(containerName);
                if (!t) {
                    throw new SfOssException("Can't create  container");
                }
            }

            try {
                int expectedReadSize = partSize;

                for(int partNumber = 0; partNumber < partCount; ++partNumber) {
                    if (partNumber == partCount - 1) {
                        expectedReadSize = lastPartSize;
                    }

                    String objName;
                    if (unknownSize) {
                        int availableSize = this.getAvailableSize(data, expectedReadSize + 1);
                        if (availableSize <= expectedReadSize) {
                            if (partNumber == 0) {
                                objName = this.putObject(containerName, objectName, availableSize, data, (String)null, 0, (Map)headerMap);
                                if (!objName.equals("") && objectName != null) {
                                    return true;
                                }

                                return false;
                            }

                            expectedReadSize = availableSize;
                            partCount = partNumber;
                        }
                    }

                    objName = this.putObject(containerName, objectName, expectedReadSize, data, segmentsContainer, partNumber, (Map)headerMap);
                    totalParts[partNumber] = new Part(partNumber, objName);
                }

                if (!this.completeMultipart(containerName, objectName, totalParts, partCount, (Map)headerMap)) {
                    throw new SfOssException("Can't create Manifast!");
                } else {
                    return true;
                }
            } catch (Exception var18) {
                try {
                    return this.abortMultipartUpload(segmentsContainer, objectName, partCount, totalParts);
                } catch (Exception var17) {
                    throw new SfOssException(var18);
                }
            }
        } else {
            String objName = this.putObject(containerName, objectName, size.intValue(), data, (String)null, 0, (Map)headerMap);
            return !objName.equals("") && objectName != null;
        }
    }

    private String putObject(String containerName, String objectName, int length, Object data, String segmentsContainer, int partNumber, Map<String, String> headerMap) throws SfOssException {
        Map<String, String> queryParamMap = null;
        String url = "";
        if (partNumber >= 0 && segmentsContainer != null && !"".equals(segmentsContainer)) {
            url = SFOSSClientUtils.getUrl(this.config, "v1", segmentsContainer, objectName) + "/" + partNumber;
        } else {
            url = SFOSSClientUtils.getUrl(this.config, "v1", containerName, objectName);
        }

        try {
            byte[] bytes = null;
            if (data != null && !(data instanceof InputStream) && !(data instanceof RandomAccessFile) && !(data instanceof byte[])) {
                bytes = data.toString().getBytes("UTF-8");
            } else {
                int len;
                ByteArrayOutputStream outSteam;
                byte[] buffer;
                if (data instanceof InputStream) {
                    InputStream input = (InputStream)data;
                    outSteam = new ByteArrayOutputStream();
                    buffer = new byte[length];
                    len = input.read(buffer);
                    outSteam.write(buffer, 0, len);
                    bytes = outSteam.toByteArray();
                    outSteam.close();
                } else {
                    if (!(data instanceof RandomAccessFile)) {
                        throw new SfOssException("Error input");
                    }

                    RandomAccessFile input = (RandomAccessFile)data;
                    outSteam = new ByteArrayOutputStream();
                    buffer = new byte[length];
                    len = input.read(buffer);
                    outSteam.write(buffer, 0, len);
                    bytes = outSteam.toByteArray();
                    outSteam.close();
                }
            }

            return this.serviceFactory.getObjectService().doUpload(url, new ByteArrayEntity(bytes), this.tokenCache.getToken(), this.config, headerMap) ? objectName + '/' + partNumber : "";
        } catch (Exception var15) {
            throw new SfOssException(var15);
        }
    }

    public boolean deleteObject(String container, String object) throws SfOssException {
        return this.deleteContent(container, object);
    }

    public boolean deleteLargeObject(String container, String object) throws SfOssException {
        return this.deleteContent(container, object, "?multipart-manifest=delete");
    }

    public boolean deleteVariousObject(String container, String object) throws SfOssException {
        Map<String, String> objectHeaders = this.headObjectMeta(container, object);
        String mf = (String)objectHeaders.get("X-Static-Large-Object");
        return mf != null && !mf.equals("") ? this.deleteContent(container, object, "?multipart-manifest=delete") : this.deleteContent(container, object);
    }

    private boolean deleteContent(String container, String contentName) throws SfOssException {
        String url = SFOSSClientUtils.getUrl(this.config, "v1", container, contentName);
        return this.serviceFactory.getObjectService().deleteContent(url, this.tokenCache.getToken(), this.config);
    }

    private boolean deleteContent(String container, String contentName, String query) throws SfOssException {
        String url = SFOSSClientUtils.getUrl(this.config, "v1", container, contentName) + query;
        return this.serviceFactory.getObjectService().deleteContent(url, this.tokenCache.getToken(), this.config);
    }

    public boolean deleteObjects(String filePath) throws SfOssException {
        return this.baseDeleteObjects(new FileEntity(new File(filePath)));
    }

    public boolean deleteObjects(InputStream deleteStream) throws SfOssException, IOException {
        int streamSize = deleteStream.available();
        ReadableByteChannel channel = Channels.newChannel(deleteStream);
        ByteBuffer buffer = ByteBuffer.allocate(streamSize);
        channel.read(buffer);
        return this.deleteObjects(buffer.array());
    }

    public boolean deleteObjects(byte[] deleteBinary) throws SfOssException {
        return this.baseDeleteObjects(new ByteArrayEntity(deleteBinary));
    }

    public JSONObject deleteObjects(List<String> objectList) throws SfOssException {
        StringBuilder jString = new StringBuilder();
        if (objectList.size() > 10000) {
            throw new SfOssException("Delete up to 10,000 objects in a single batch delete");
        } else {
            Iterator var3 = objectList.iterator();

            while(var3.hasNext()) {
                String obj = (String)var3.next();
                jString.append(obj);
                jString.append("\n");
            }

            byte[] b = jString.toString().getBytes();
            return this.DeleteObjects(new ByteArrayEntity(b));
        }
    }

    private JSONObject DeleteObjects(HttpEntity entity) throws SfOssException {
        String url = SFOSSClientUtils.getUrl(this.config, "v1") + "?bulk-delete";
        return this.serviceFactory.getObjectService().DeleteObjects(url, entity, this.tokenCache.getToken(), this.config);
    }

    private boolean baseDeleteObjects(HttpEntity entity) throws SfOssException {
        String url = SFOSSClientUtils.getUrl(this.config, "v1") + "?bulk-delete";
        return this.serviceFactory.getObjectService().baseDeleteObjects(url, entity, this.tokenCache.getToken(), this.config);
    }

    public InputStream getObjectsZip(List<String> objecList) throws SfOssException {
        String url = SFOSSClientUtils.getUrl(this.config, "v1") + "?package=" + UUID.randomUUID().toString();
        return this.serviceFactory.getObjectService().getObjectsZip(url, objecList, this.tokenCache.getToken(), this.config, false);
    }

    public InputStream getObjectsZip(List<String> objecList, boolean UrlEncode) throws SfOssException {
        String url = SFOSSClientUtils.getUrl(this.config, "v1") + "?package=" + UUID.randomUUID().toString();
        return this.serviceFactory.getObjectService().getObjectsZip(url, objecList, this.tokenCache.getToken(), this.config, UrlEncode);
    }

    public JSONObject CheckObjects(List<String> objecList) throws SfOssException {
        String url = SFOSSClientUtils.getUrl(this.config, "v1");
        return this.serviceFactory.getObjectService().checkObjects(url, objecList, this.tokenCache.getToken(), this.config);
    }

    public InputStream getObject(String container, String objectName) throws SfOssException {
        byte[] res = this.downloadContent(container, objectName, "", new HashMap());

        try {
            return new ByteArrayInputStream(res);
        } catch (Exception var5) {
            return null;
        }
    }

    public InputStream getObject(String container, String objectName, String query) throws SfOssException {
        query = query.replace("|", "%7C");
        byte[] res = this.downloadContent(container, objectName, query, new HashMap());

        try {
            return new ByteArrayInputStream(res);
        } catch (Exception var6) {
            return null;
        }
    }

    public InputStream getObject(String container, String objectName, String query, Map<String, String> headers) throws SfOssException {
        query = query.replace("|", "%7C");
        byte[] res = this.downloadContent(container, objectName, query, headers);

        try {
            return new ByteArrayInputStream(res);
        } catch (Exception var7) {
            return null;
        }
    }

    private byte[] downloadContent(String container, String objectName, String style, Map<String, String> headers) throws SfOssException {
        String url = SFOSSClientUtils.getUrl(this.config, "v1", container, objectName);
        if (!style.equals("")) {
            url = url + "?" + style;
        }

        return this.serviceFactory.getObjectService().downloadContent(url, this.tokenCache.getToken(), this.config, headers);
    }

    public InputStream getByPath(String path) throws SfOssException {
        byte[] res = this.downloadContentByPath(path, "", new HashMap());

        try {
            return new ByteArrayInputStream(res);
        } catch (Exception var4) {
            return null;
        }
    }

    public InputStream getByPath(String path, String query) throws SfOssException {
        query = query.replace("|", "%7C");
        byte[] res = this.downloadContentByPath(path, query, new HashMap());

        try {
            return new ByteArrayInputStream(res);
        } catch (Exception var5) {
            return null;
        }
    }

    public InputStream getByPath(String path, String query, Map<String, String> headers) throws SfOssException {
        query = query.replace("|", "%7C");
        byte[] res = this.downloadContentByPath(path, query, headers);

        try {
            return new ByteArrayInputStream(res);
        } catch (Exception var6) {
            return null;
        }
    }

    private byte[] downloadContentByPath(String path, String style, Map<String, String> headers) throws SfOssException {
        String url = SFOSSClientUtils.getUrlByPath(this.config, "v1", path);
        if (!style.equals("")) {
            url = url + "?" + style;
        }

        return this.serviceFactory.getObjectService().downloadContent(url, this.tokenCache.getToken(), this.config, headers);
    }

    public Map<String, String> headObjectMeta(String container, String object) throws SfOssException {
        String url = SFOSSClientUtils.getUrl(this.config, "v1", container, object);
        return this.serviceFactory.getObjectService().headObjectMeta(url, this.tokenCache.getToken(), this.config);
    }

    public String getTempUrl(String container, String object, String method, Integer durationTime) throws SfOssException {
        String url = SFOSSClientUtils.getUrl(this.config, "v1");
        return this.serviceFactory.getObjectService().getTempUrl(this.config.getAccount(), container, url, object, method, durationTime, this.tokenCache.getToken(), this.config);
    }

    public boolean checkContainer(String container) throws SfOssException {
        String url = SFOSSClientUtils.getUrl(this.config, "v1", container);
        return this.serviceFactory.getContainerService().checkContainer(url, this.tokenCache.getToken(), this.config);
    }

    public void setV2token(String token) {
        this.tokenCache.setTokenV2(token);
    }

    public String getV2token(String container, String obj) {
        try {
            return this.tokenCache.getV2Token(container, obj);
        } catch (Exception var4) {
            return "";
        }
    }

    public String[] getTokenV2AndExpires(String container, String obj) {
        try {
            return this.tokenCache.getTokenV2AndExpires(container, obj);
        } catch (Exception var4) {
            return null;
        }
    }

    public Map<String, String> headObjectMetaV2(String container, String object) throws SfOssException {
        String url = SFOSSClientUtils.getUrl(this.config, "v1.2", container, object);
        return this.serviceFactory.getObjectService().headObjectMeta(url, this.tokenCache.getTokenV2(), this.config);
    }

    public InputStream getObjectV2(String container, String objectName) throws SfOssException {
        byte[] res = this.downloadContentV2(container, objectName, "", new HashMap());

        try {
            return new ByteArrayInputStream(res);
        } catch (Exception var5) {
            return null;
        }
    }

    public InputStream getObjectV2(String container, String objectName, String query) throws SfOssException {
        query = query.replace("|", "%7C");
        byte[] res = this.downloadContentV2(container, objectName, query, new HashMap());

        try {
            return new ByteArrayInputStream(res);
        } catch (Exception var6) {
            return null;
        }
    }

    public InputStream getObjectV2(String container, String objectName, String query, Map<String, String> headers) throws SfOssException {
        query = query.replace("|", "%7C");
        byte[] res = this.downloadContentV2(container, objectName, query, headers);

        try {
            return new ByteArrayInputStream(res);
        } catch (Exception var7) {
            return null;
        }
    }

    private byte[] downloadContentV2(String container, String objectName, String style, Map<String, String> headers) throws SfOssException {
        String url = SFOSSClientUtils.getUrl(this.config, "v1.2", container, objectName);
        if (!style.equals("")) {
            url = url + "?" + style;
        }

        return this.serviceFactory.getObjectService().downloadContent(url, this.tokenCache.getTokenV2(), this.config, headers);
    }

    public boolean uploadFileV2(String container, String objectName, String filePath) throws SfOssException {
        if (container != null && !container.isEmpty()) {
            String url = SFOSSClientUtils.getUrl(this.config, "v1.2", container, objectName);
            return this.serviceFactory.getObjectService().doUpload(url, new FileEntity(new File(filePath)), this.tokenCache.getTokenV2(), this.config, new HashMap());
        } else {
            return false;
        }
    }

    public boolean uploadObjectV2(String container, String objectName, InputStream objectStream) throws SfOssException, IOException {
        return this.uploadObjectV2(container, objectName, objectStream, -1);
    }

    public boolean uploadObjectV2(String container, String objectName, InputStream objectStream, int streamSize) throws SfOssException, IOException {
        if (streamSize == -1) {
            streamSize = objectStream.available();
        }

        ReadableByteChannel channel = Channels.newChannel(objectStream);
        ByteBuffer buffer = ByteBuffer.allocate(streamSize);
        channel.read(buffer);
        return this.uploadObjectV2(container, objectName, buffer.array(), new HashMap());
    }

    public boolean uploadObjectV2(String container, String objectName, InputStream objectStream, int streamSize, Map<String, String> headers) throws SfOssException, IOException {
        if (streamSize == -1) {
            streamSize = objectStream.available();
        }

        ReadableByteChannel channel = Channels.newChannel(objectStream);
        ByteBuffer buffer = ByteBuffer.allocate(streamSize);
        channel.read(buffer);
        return this.uploadObjectV2(container, objectName, buffer.array(), headers);
    }

    public boolean uploadObjectV2(String container, String objectName, byte[] objectBinary, Map<String, String> headers) throws SfOssException {
        if (container != null && !container.isEmpty()) {
            String url = SFOSSClientUtils.getUrl(this.config, "v1.2", container, objectName);
            return this.serviceFactory.getObjectService().doUpload(url, new ByteArrayEntity(objectBinary), this.tokenCache.getTokenV2(), this.config, headers);
        } else {
            return false;
        }
    }

    public boolean deleteObjectV2(String container, String object) throws SfOssException {
        return this.deleteContentV2(container, object, false);
    }

    public boolean deleteLargeObjectV2(String container, String object) throws SfOssException {
        return this.deleteContentV2(container, object, true);
    }

    public boolean deleteVariousObjectV2(String container, String object) throws SfOssException {
        Map<String, String> objectHeaders = this.headObjectMetaV2(container, object);
        String mf = (String)objectHeaders.get("X-Static-Large-Object");
        return mf != null && !mf.equals("") ? this.deleteContentV2(container, object, true) : this.deleteContentV2(container, object, false);
    }

    private boolean deleteContentV2(String container, String contentName, boolean deleteAll) throws SfOssException {
        String url = SFOSSClientUtils.getUrl(this.config, "v1.2", container, contentName);
        if (deleteAll) {
            url = url + "?multipart-manifest=delete";
        }

        return this.serviceFactory.getObjectService().deleteContent(url, this.tokenCache.getTokenV2(), this.config);
    }

    public boolean postObjectMetaV2(String container, String objectName, Map<String, String> headers) throws SfOssException {
        String url = SFOSSClientUtils.getUrl(this.config, "v1.2", container, objectName);
        return this.serviceFactory.getObjectService().postObjectMeta(url, headers, this.tokenCache.getTokenV2(), this.config);
    }

    public BufferedReader getObjectReader(String container, String objectName) throws SfOssException {
        byte[] res = this.downloadContent(container, objectName, "", new HashMap());

        try {
            InputStream is = new ByteArrayInputStream(res);
            InputStreamReader isr = new InputStreamReader(is, "UTF-8");
            return new BufferedReader(isr);
        } catch (Exception var6) {
            return null;
        }
    }

    public BufferedReader getObjectReader(String container, String objectName, String query) throws SfOssException {
        query = query.replace("|", "%7C");
        byte[] res = this.downloadContent(container, objectName, query, new HashMap());

        try {
            InputStream is = new ByteArrayInputStream(res);
            InputStreamReader isr = new InputStreamReader(is, "UTF-8");
            return new BufferedReader(isr);
        } catch (Exception var7) {
            return null;
        }
    }

    public BufferedReader getObjectReader(String container, String objectName, String query, Map<String, String> headers) throws SfOssException {
        query = query.replace("|", "%7C");
        byte[] res = this.downloadContent(container, objectName, query, headers);

        try {
            InputStream is = new ByteArrayInputStream(res);
            InputStreamReader isr = new InputStreamReader(is, "UTF-8");
            return new BufferedReader(isr);
        } catch (Exception var8) {
            return null;
        }
    }

    public BufferedReader getReaderByPath(String path) throws SfOssException {
        byte[] res = this.downloadContentByPath(path, "", new HashMap());

        try {
            InputStream is = new ByteArrayInputStream(res);
            InputStreamReader isr = new InputStreamReader(is, "UTF-8");
            return new BufferedReader(isr);
        } catch (Exception var5) {
            return null;
        }
    }

    public BufferedReader getReaderByPath(String path, String query) throws SfOssException {
        query = query.replace("|", "%7C");
        byte[] res = this.downloadContentByPath(path, query, new HashMap());

        try {
            InputStream is = new ByteArrayInputStream(res);
            InputStreamReader isr = new InputStreamReader(is, "UTF-8");
            return new BufferedReader(isr);
        } catch (Exception var6) {
            return null;
        }
    }

    private static String initMultipartUpload(String container, String obj, HashMap<String, String> headers) {
        return "";
    }

    private int getAvailableSize(Object inputStream, int expectedReadSize) throws IOException, SfOssException {
        RandomAccessFile file = null;
        BufferedInputStream stream = null;
        if (inputStream instanceof RandomAccessFile) {
            file = (RandomAccessFile)inputStream;
        } else {
            if (!(inputStream instanceof BufferedInputStream)) {
                throw new SfOssException("Unknown input stream. This should not happen.");
            }

            stream = (BufferedInputStream)inputStream;
        }

        long pos = 0L;
        if (file != null) {
            pos = file.getFilePointer();
        } else {
            stream.mark(expectedReadSize);
        }

        byte[] buf = new byte[16384];
        int bytesToRead = buf.length;

        int totalBytesRead;
        int bytesRead;
        for(totalBytesRead = 0; totalBytesRead < expectedReadSize; totalBytesRead += bytesRead) {
            if (expectedReadSize - totalBytesRead < bytesToRead) {
                bytesToRead = expectedReadSize - totalBytesRead;
            }

            if (file != null) {
                bytesRead = file.read(buf, 0, bytesToRead);
            } else {
                bytesRead = stream.read(buf, 0, bytesToRead);
            }

            if (bytesRead < 0) {
                break;
            }
        }

        if (file != null) {
            file.seek(pos);
        } else {
            stream.reset();
        }

        return totalBytesRead;
    }
}
