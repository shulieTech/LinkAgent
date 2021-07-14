package org.eclipse.jetty.continuation;

/**
 * @author xiaobin.zfb|xiaobin@shulie.io
 * @since 2020/12/28 1:54 下午
 */
public class ContinuationThrowable extends Throwable {
    public ContinuationThrowable() {
        super();
    }

    public ContinuationThrowable(String message) {
        super(message);
    }

    public ContinuationThrowable(String message, Throwable cause) {
        super(message, cause);
    }

    public ContinuationThrowable(Throwable cause) {
        super(cause);
    }
}
