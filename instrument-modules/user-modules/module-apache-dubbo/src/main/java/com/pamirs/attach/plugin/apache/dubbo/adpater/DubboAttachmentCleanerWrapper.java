package com.pamirs.attach.plugin.apache.dubbo.adpater;

import com.pamirs.attach.plugin.apache.dubbo.adpater.impl.DubboHigherAttachmentCleaner;
import com.pamirs.attach.plugin.apache.dubbo.adpater.impl.DubboLowerAttachmentCleaner;
import org.apache.dubbo.rpc.RpcContext;
import org.apache.dubbo.rpc.RpcInvocation;

/**
 * @author jirenhe | jirenhe@shulie.io
 * @since 2022/08/15 8:37 PM
 */
public class DubboAttachmentCleanerWrapper {
    private static boolean isDubboHigher;

    static {
        try {
            RpcContext.class.getDeclaredMethod("removeAttachment", String.class);
            isDubboHigher = true;
        } catch (Throwable e) {
        }
    }

    public static void removeAttachment(String key, RpcInvocation invocation) {
        getAttachmentsCleaner().removeAttachment(key, invocation);
    }

    private static AttachmentCleaner getAttachmentsCleaner() {
        return isDubboHigher ? new DubboHigherAttachmentCleaner() : new DubboLowerAttachmentCleaner();
    }
}
