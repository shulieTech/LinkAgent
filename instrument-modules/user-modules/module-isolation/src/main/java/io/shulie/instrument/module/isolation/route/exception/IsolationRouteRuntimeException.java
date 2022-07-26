package io.shulie.instrument.module.isolation.route.exception;

/**
 * @author Licey
 * @date 2022/7/26
 */
public class IsolationRouteRuntimeException extends RuntimeException{
    public IsolationRouteRuntimeException() {
        super();
    }

    public IsolationRouteRuntimeException(String message) {
        super(message);
    }

    public IsolationRouteRuntimeException(String message, Throwable cause) {
        super(message, cause);
    }

    public IsolationRouteRuntimeException(Throwable cause) {
        super(cause);
    }
}
