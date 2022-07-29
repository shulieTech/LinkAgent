package io.shulie.instrument.module.messaging.exception;

/**
 * @author Licey
 * @date 2022/7/27
 */
public class MessagingRuntimeException extends RuntimeException{

    public MessagingRuntimeException() {
        super();
    }

    public MessagingRuntimeException(String message) {
        super(message);
    }

    public MessagingRuntimeException(String message, Throwable cause) {
        super(message, cause);
    }

    public MessagingRuntimeException(Throwable cause) {
        super(cause);
    }

}
