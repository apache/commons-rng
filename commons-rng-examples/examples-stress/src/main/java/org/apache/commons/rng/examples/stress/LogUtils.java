/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.commons.rng.examples.stress;

import java.io.PrintStream;
import java.io.PrintWriter;
import java.io.StringWriter;

/**
 * Simple utility for logging messages to the console.
 *
 * <p>This is not a replacement for a logging framework and may be replaced in the future.
 */
final class LogUtils {
    /** The logging level. */
    private static LogLevel logLevel = LogLevel.INFO;

    /**
     * The log level.
     */
    enum LogLevel {
        /** The error log level. */
        ERROR(0),
        /** The information log level. */
        INFO(1),
        /** The debug log level. */
        DEBUG(2);

        /** The prefix for log messages. */
        private final String prefix;

        /** The logging level. */
        private final int level;

        /**
         * Create a new instance.
         *
         * @param level The level.
         */
        LogLevel(int level) {
            // Just use the name
            prefix = "[" + name() + "] ";
            this.level = level;
        }

        /**
         * Gets the message prefix.
         *
         * @return the prefix
         */
        String getPrefix() {
            return prefix;
        }

        /**
         * Gets the level.
         *
         * @return the level
         */
        int getLevel() {
            return level;
        }
    }

    /**
     * No public construction.
     */
    private LogUtils() {}

    /**
     * Sets the log level.
     *
     * @param logLevel The new log level.
     */
    static void setLogLevel(LogLevel logLevel) {
        LogUtils.logLevel = logLevel;
    }

    /**
     * Checks if the given level is loggable.
     *
     * @param level The level.
     * @return true if loggable
     */
    static boolean isLoggable(LogLevel level) {
        return level.getLevel() <= LogUtils.logLevel.getLevel();
    }

    /**
     * Log a debug message to {@link System#out}.
     *
     * @param message The message.
     */
    static void debug(String message) {
        if (isLoggable(LogLevel.DEBUG)) {
            println(System.out, LogLevel.DEBUG.getPrefix() + message);
        }
    }

    /**
     * Log a debug message to {@link System#out}.
     *
     * @param format The format.
     * @param args The arguments.
     */
    static void debug(String format, Object... args) {
        if (isLoggable(LogLevel.DEBUG)) {
            printf(System.out, LogLevel.DEBUG.getPrefix() + format, args);
        }
    }
    /**
     * Log an info message to {@link System#out}.
     *
     * @param message The message.
     */
    static void info(String message) {
        if (isLoggable(LogLevel.INFO)) {
            println(System.out, LogLevel.INFO.getPrefix() + message);
        }
    }

    /**
     * Log an info message to {@link System#out}.
     *
     * @param format The format.
     * @param args The arguments.
     */
    static void info(String format, Object... args) {
        if (isLoggable(LogLevel.INFO)) {
            printf(System.out, LogLevel.INFO.getPrefix() + format, args);
        }
    }

    /**
     * Log an error message to {@link System#err}.
     *
     * @param message The message.
     */
    static void error(String message) {
        if (isLoggable(LogLevel.ERROR)) {
            println(System.err, LogLevel.ERROR.getPrefix() + message);
        }
    }

    /**
     * Log an error message to {@link System#err}.
     *
     * @param format The format.
     * @param args The arguments.
     */
    static void error(String format, Object... args) {
        if (isLoggable(LogLevel.ERROR)) {
            printf(System.err, LogLevel.ERROR.getPrefix() + format, args);
        }
    }

    /**
     * Log an error message to {@link System#err}. The stack trace of the thrown is added.
     *
     * @param thrown The thrown.
     * @param message The message.
     */
    static void error(Throwable thrown, String message) {
        if (isLoggable(LogLevel.ERROR)) {
            final StringWriter sw = new StringWriter();
            try (PrintWriter pw = createErrorPrintWriter(sw)) {
                pw.print(message);
                addStackTrace(pw, thrown);
            }
            println(System.err, sw.toString());
        }
    }

    /**
     * Log an error message to {@link System#err}. The stack trace of the thrown is
     * added.
     *
     * @param thrown The thrown.
     * @param format The format.
     * @param args The arguments.
     */
    static void error(Throwable thrown, String format, Object... args) {
        if (isLoggable(LogLevel.ERROR)) {
            final StringWriter sw = new StringWriter();
            try (PrintWriter pw = createErrorPrintWriter(sw)) {
                pw.printf(format, args);
                addStackTrace(pw, thrown);
            }
            printf(System.err, sw.toString());
        }
    }

    /**
     * Creates the error print writer. It will contain the prefix for error
     * messages.
     *
     * @param sw The string writer.
     * @return the print writer
     */
    private static PrintWriter createErrorPrintWriter(StringWriter sw) {
        final PrintWriter pw = new PrintWriter(sw);
        pw.print(LogLevel.ERROR.getPrefix());
        return pw;
    }

    /**
     * Adds the stack trace to the print writer.
     *
     * @param pw The print writer.
     * @param thrown The thrown.
     */
    private static void addStackTrace(PrintWriter pw, Throwable thrown) {
        // Complete the current line
        pw.println();
        thrown.printStackTrace(pw);
    }

    /**
     * Print a message to the provided {@link PrintStream}.
     *
     * @param out The output.
     * @param message The message.
     * @see PrintStream#println(String)
     */
    private static void println(PrintStream out, String message) {
        out.println(message);
    }

    /**
     * Print a message to the provided {@link PrintStream}.
     *
     * @param out The output.
     * @param format The format.
     * @param args The arguments.
     * @see PrintStream#printf(String, Object...)
     */
    private static void printf(PrintStream out, String format, Object... args) {
        // Ensure a new line is added
        out.printf(format + "%n", args);
    }
}
