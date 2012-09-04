/*
 * Copyright 2012 Mikhail Titov.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.raven.tree.impl;

import org.raven.log.LogLevel;
import org.raven.tree.Node;
import org.slf4j.Logger;
import org.slf4j.Marker;

/**
 *
 * @author Mikhail Titov
 */
public class LoggerHelper implements Logger {
    
    private final LogLevel logLevel;
    private final String name;
    private final String prefix;
    private final Logger logger;

    public LoggerHelper(Node node, String prefix) {
        this(node.getLogLevel(), node.getName(), prefix, node.getLogger());
    }

    public LoggerHelper(LogLevel logLevel, String name, String prefix, Logger logger) {
        this.logLevel = logLevel;
        this.name = name;
        this.prefix = prefix==null? "" : prefix;
        this.logger = logger;
    }
    
    public LoggerHelper(LoggerHelper logger, String prefix) {
        this(logger.getLogLevel(), logger.getName(), prefix, logger);
    }

    public LogLevel getLogLevel() {
        return logLevel;
    }

    public Logger getLogger() {
        return logger;
    }

    public String getName() {
        return name;
    }

    public String getPrefix() {
        return prefix;
    }
    
    public String logMess(String message, Object... args) {
        return prefix + String.format(message, args);
    }

    public boolean isTraceEnabled() {
        return logLevel.isLogLevelEnabled(LogLevel.TRACE);
    }

    public void trace(String msg) {
        logger.trace(prefix+msg);
    }

    public void trace(String format, Object arg) {
        logger.trace(prefix+format);
    }

    public void trace(String format, Object arg1, Object arg2) {
        logger.trace(prefix+format, arg1, arg2);
    }

    public void trace(String format, Object[] argArray) {
        logger.trace(prefix+format, argArray);
    }

    public void trace(String msg, Throwable t) {
        logger.trace(prefix+msg, t);
    }

    public boolean isTraceEnabled(Marker marker) {
        return isTraceEnabled();
    }

    public void trace(Marker marker, String msg) {
    }

    public void trace(Marker marker, String format, Object arg) {
    }

    public void trace(Marker marker, String format, Object arg1, Object arg2) {
    }

    public void trace(Marker marker, String format, Object[] argArray) {
    }

    public void trace(Marker marker, String msg, Throwable t) {
    }

    public boolean isDebugEnabled() {
        return logLevel.isLogLevelEnabled(LogLevel.DEBUG);
    }

    public void debug(String msg) {
        logger.debug(prefix+msg);
    }

    public void debug(String format, Object arg) {
        logger.debug(prefix+format, arg);
    }

    public void debug(String format, Object arg1, Object arg2) {
        logger.debug(prefix+format, arg1, arg2);
    }

    public void debug(String format, Object[] argArray) {
        logger.debug(prefix+format, argArray);
    }

    public void debug(String msg, Throwable t) {
        logger.debug(prefix+msg, t);
    }

    public boolean isDebugEnabled(Marker marker) {
        return isDebugEnabled();
    }

    public void debug(Marker marker, String msg) {
    }

    public void debug(Marker marker, String format, Object arg) {
    }

    public void debug(Marker marker, String format, Object arg1, Object arg2) {
    }

    public void debug(Marker marker, String format, Object[] argArray) {
    }

    public void debug(Marker marker, String msg, Throwable t) {
    }

    public boolean isInfoEnabled() {
        return logLevel.isLogLevelEnabled(LogLevel.INFO);
    }

    public void info(String msg) {
        logger.info(prefix+msg);
    }

    public void info(String format, Object arg) {
        logger.info(prefix+format, arg);
    }

    public void info(String format, Object arg1, Object arg2) {
        logger.info(prefix+format, arg1, arg2);
    }

    public void info(String format, Object[] argArray) {
        logger.info(prefix+format, argArray);
    }

    public void info(String msg, Throwable t) {
        logger.info(prefix+msg, t);
    }

    public boolean isInfoEnabled(Marker marker) {
        return isInfoEnabled();
    }

    public void info(Marker marker, String msg) {
    }

    public void info(Marker marker, String format, Object arg) {
    }

    public void info(Marker marker, String format, Object arg1, Object arg2) {
    }

    public void info(Marker marker, String format, Object[] argArray) {
    }

    public void info(Marker marker, String msg, Throwable t) {
    }

    public boolean isWarnEnabled() {
        return logLevel.isLogLevelEnabled(LogLevel.WARN);
    }

    public void warn(String msg) {
        logger.warn(prefix+msg);
    }

    public void warn(String format, Object arg) {
        logger.warn(prefix+format, arg);
    }

    public void warn(String format, Object[] argArray) {
        logger.warn(prefix+format, argArray);
    }

    public void warn(String format, Object arg1, Object arg2) {
        logger.warn(prefix+format, arg1, arg2);
    }

    public void warn(String msg, Throwable t) {
        logger.warn(prefix+msg, t);
    }

    public boolean isWarnEnabled(Marker marker) {
        return isWarnEnabled();
    }

    public void warn(Marker marker, String msg) {
    }

    public void warn(Marker marker, String format, Object arg) {
    }

    public void warn(Marker marker, String format, Object arg1, Object arg2) {
    }

    public void warn(Marker marker, String format, Object[] argArray) {
    }

    public void warn(Marker marker, String msg, Throwable t) {
    }

    public boolean isErrorEnabled() {
        return logLevel.isLogLevelEnabled(LogLevel.ERROR);
    }

    public void error(String msg) {
        logger.error(prefix+msg);
    }

    public void error(String format, Object arg) {
        logger.error(prefix+format, arg);
    }

    public void error(String format, Object arg1, Object arg2) {
        logger.error(prefix+format, arg1, arg2);
    }

    public void error(String format, Object[] argArray) {
        logger.error(prefix+format, argArray);
    }

    public void error(String msg, Throwable t) {
        logger.error(prefix+msg, t);
    }

    public boolean isErrorEnabled(Marker marker) {
        return isErrorEnabled();
    }

    public void error(Marker marker, String msg) {
    }

    public void error(Marker marker, String format, Object arg) {
    }

    public void error(Marker marker, String format, Object arg1, Object arg2) {
    }

    public void error(Marker marker, String format, Object[] argArray) {
    }

    public void error(Marker marker, String msg, Throwable t) {
    }
}
