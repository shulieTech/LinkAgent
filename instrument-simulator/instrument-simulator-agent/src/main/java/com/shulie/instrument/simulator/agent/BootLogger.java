/**
 * Copyright 2021 Shulie Technology, Co.Ltd
 * Email: shulie@shulie.io
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.shulie.instrument.simulator.agent;

import java.io.*;
import java.text.MessageFormat;

/**
 * Created by xiaobin on 2017/1/19.
 */
public final class BootLogger {

    private final String messagePattern;
    private final PrintStream out;
    private final File file;

    public BootLogger(String loggerName) {
        this(loggerName, System.out);
    }

    BootLogger(String loggerName, PrintStream out) {
        if (loggerName == null) {
            throw new NullPointerException("loggerName must not be null");
        }
        this.messagePattern = "{0,date,yyyy-MM-dd HH:mm:ss} [{1}](" + loggerName + ") {2}{3}";
        this.out = out;
        String logPath = System.getProperty("SIMULATOR_LOG_PATH");
        if (logPath == null && "".equals(logPath.trim())) {
            logPath = System.getProperty("user.home") + File.separator + "pradarlogs";
        }
        file = new File(logPath, "simulator.log");
    }

    static BootLogger getLogger(String loggerName) {
        return new BootLogger(loggerName);
    }

    private PrintWriter getWriter() {
        try {
            return new PrintWriter(new OutputStreamWriter(new FileOutputStream(file, true), "UTF-8"));
        } catch (Throwable e) {
            this.out.println(e);
            return null;
        }
    }

    private String format(String logLevel, String msg, String exceptionMessage) {
        exceptionMessage = defaultString(exceptionMessage, "");

        MessageFormat messageFormat = new MessageFormat(messagePattern);
        final long date = System.currentTimeMillis();
        Object[] parameter = {date, logLevel, msg, exceptionMessage};
        return messageFormat.format(parameter);
    }

    public boolean isInfoEnabled() {
        return true;
    }

    public void info(String msg) {
        String formatMessage = format("INFO ", msg, "");
        PrintWriter writer = getWriter();
        if (writer != null) {
            try {
                writer.println(formatMessage);
            } finally {
                try {
                    writer.close();
                } catch (Exception e) {
                }
            }
        } else {
            this.out.println(formatMessage);
        }
    }


    public boolean isWarnEnabled() {
        return true;
    }

    public void warn(String msg) {
        warn(msg, null);
    }

    public void warn(String msg, Throwable throwable) {
        String exceptionMessage = toString(throwable);
        String formatMessage = format("WARN ", msg, exceptionMessage);
        PrintWriter writer = getWriter();
        if (writer != null) {
            try {
                writer.println(formatMessage);
            } finally {
                try {
                    writer.close();
                } catch (Exception e) {
                }
            }
        } else {
            this.out.println(formatMessage);
        }
    }

    public void error(String msg) {
        error(msg, null);
    }

    public void error(String msg, Throwable throwable) {
        String exceptionMessage = toString(throwable);
        String formatMessage = format("ERROR ", msg, exceptionMessage);
        PrintWriter writer = getWriter();
        if (writer != null) {
            try {
                writer.println(formatMessage);
            } finally {
                try {
                    writer.close();
                } catch (Exception e) {
                }
            }
        } else {
            this.out.println(formatMessage);
        }
    }

    private String toString(Throwable throwable) {
        if (throwable == null) {
            return "";
        }
        StringWriter sw = new StringWriter();
        PrintWriter pw = new PrintWriter(sw);
        pw.println();
        throwable.printStackTrace(pw);
        pw.close();
        return sw.toString();
    }

    private String defaultString(String exceptionMessage, String defaultValue) {
        if (exceptionMessage == null) {
            return defaultValue;
        }
        return exceptionMessage;
    }
}

