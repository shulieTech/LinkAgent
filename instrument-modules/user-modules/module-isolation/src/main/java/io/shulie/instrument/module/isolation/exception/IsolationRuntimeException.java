package io.shulie.instrument.module.isolation.exception;

/**
 * @author Licey
 * @date 2022/7/26
 */
public class IsolationRuntimeException extends RuntimeException {
    public IsolationRuntimeException() {
        super();
    }

    public IsolationRuntimeException(String message) {
        super(message);
    }

    public IsolationRuntimeException(String message, Throwable cause) {
        super(message, cause);
    }

    public IsolationRuntimeException(Throwable cause) {
        super(cause);
    }
}
