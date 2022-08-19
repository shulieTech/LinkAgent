package com.pamirs.attach.plugin.apache.dubbo.adpater;

import org.apache.dubbo.rpc.RpcInvocation;

/**
 * @author jirenhe | jirenhe@shulie.io
 * @since 2022/08/15 8:51 PM
 */
public interface AttachmentCleaner {
    void removeAttachment(String key, RpcInvocation invocation);
}
