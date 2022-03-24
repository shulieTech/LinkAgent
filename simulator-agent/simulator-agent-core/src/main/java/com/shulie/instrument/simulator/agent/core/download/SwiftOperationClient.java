package com.shulie.instrument.simulator.agent.core.download;

import com.shulie.instrument.simulator.agent.core.util.UpgradeFileUtils;
import org.javaswift.joss.client.factory.AccountConfig;
import org.javaswift.joss.client.factory.AccountFactory;
import org.javaswift.joss.model.Account;
import org.javaswift.joss.model.Container;
import org.javaswift.joss.model.StoredObject;

import java.io.File;

/**
 * @author jiangjibo
 * @date 2022/3/24 3:15 下午
 * @description:
 */
public class SwiftOperationClient {


    public static void download(String username, String password, String authUrl, String tenantId, String tenantName, String container, String upgradeBatch) {
        Account account = buildSwiftAccount(username, password, authUrl, tenantId, tenantName);
        Container ctn = account.getContainer(container);
        StoredObject object = ctn.getObject(UpgradeFileUtils.getUpgradeFileTempFileName(upgradeBatch));
        if(object.exists()){
            object.delete();
        }
        object.downloadObject(new File(UpgradeFileUtils.getUpgradeFileTempSaveDir() + File.separator + UpgradeFileUtils.getUpgradeFileTempFileName(upgradeBatch)));
    }

    private static Account buildSwiftAccount(String username, String password, String url, String tenantId, String tenantName) {
        AccountConfig config = new AccountConfig();
        config.setUsername(username);
        config.setPassword(password);
        config.setAuthUrl(url);
        config.setTenantId(tenantId);
        config.setTenantName(tenantName);
        return new AccountFactory(config).createAccount();
    }

}
