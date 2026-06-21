package com.aidentx.logging.service;

import com.aidentx.logging.model.UnifiedLogEntry;

public interface IUnifiedLogger {

    LogEventBuilder info();
    LogEventBuilder warn();
    LogEventBuilder error();
    LogEventBuilder error(Throwable throwable);
    LogEventBuilder debug();
    public LogEventBuilder trace();
    /**
     * Internal method to dispatch the log entry. Called by LogEventBuilder.
     */
    void log(UnifiedLogEntry logEntry);

    /**
     * Helper utility to get a stack trace as a string.
     */
    static String getStackTraceAsString(Throwable throwable) {
        if (throwable == null) return null;
        java.io.StringWriter sw = new java.io.StringWriter();
        java.io.PrintWriter pw = new java.io.PrintWriter(sw);
        throwable.printStackTrace(pw);
        return sw.toString();
    }
}