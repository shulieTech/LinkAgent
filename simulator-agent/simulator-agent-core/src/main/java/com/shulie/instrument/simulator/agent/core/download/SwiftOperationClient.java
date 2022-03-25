package com.shulie.instrument.simulator.agent.core.download;

import com.shulie.instrument.simulator.agent.core.util.UpgradeFileUtils;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import swiftsdk.SfOssClient;
import swiftsdk.SwiftConfiguration;

import java.io.File;
import java.io.InputStream;
import java.util.HashMap;

/**
 * @author jiangjibo
 * @date 2022/3/24 3:15 下午
 * @description:
 */
public class SwiftOperationClient {

    private static final Logger logger = LoggerFactory.getLogger(SwiftOperationClient.class);

    public static void download(String username, String account, String userKey, String ak, String url, String container, String upgradeBatch) {
        SfOssClient client = buildSwiftAccount(username, account, userKey, ak, url);
        if (client == null) {
            return;
        }
        try {
            client.createContainer(container);
            InputStream inputStream = client.getObject(container, UpgradeFileUtils.getUpgradeFileTempFileName(upgradeBatch), "", new HashMap<String, String>());
            File file = new File(UpgradeFileUtils.getUpgradeFileTempSaveDir() + File.separator + UpgradeFileUtils.getUpgradeFileTempFileName(upgradeBatch));
            byte[] bytes = IOUtils.toByteArray(inputStream);
            FileUtils.writeByteArrayToFile(file, bytes);
        } catch (Exception e) {
            logger.error("failed to download agent file", e);
        }

    }

    public static SfOssClient buildSwiftAccount(String username, String account, String userKey, String ak, String url) {
        try {
            SwiftConfiguration f = new SwiftConfiguration();
            f.setUserName(username);
            f.setAccount(account);
            f.setUserKey(userKey);
            f.setAk(ak);
            f.setSfossServerUrl(url);
            return new SfOssClient(f);
        } catch (Exception e) {
            logger.error("failed init SfOssClient", e);
            return null;
        }
    }

}
