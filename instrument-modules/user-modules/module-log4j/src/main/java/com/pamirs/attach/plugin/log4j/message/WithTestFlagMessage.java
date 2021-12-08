package com.pamirs.attach.plugin.log4j.message;

import org.apache.logging.log4j.message.Message;

/**
 * @author jirenhe | jirenhe@shulie.io
 * @since 2021/12/06 3:54 下午
 */
public class WithTestFlagMessage implements Message {

    private final Message message;

    public WithTestFlagMessage(Message message) {
        this.message = message;
    }

    @Override
    public String getFormattedMessage() {
        return message.getFormattedMessage();
    }

    @Override
    public String getFormat() {
        return message.getFormat();
    }

    @Override
    public Object[] getParameters() {
        return message.getParameters();
    }

    @Override
    public Throwable getThrowable() {
        return message.getThrowable();
    }

}
