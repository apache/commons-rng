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

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * Utility methods for a {@link Process}.
 */
final class ProcessUtils {
    /** The timeout to wait for the process exit value in milliseconds. */
    private static final long DEFAULT_TIMEOUT_MILLIS = 1000L;

    /** No public construction. */
    private ProcessUtils() {}

    /**
     * Check the executable exists and has execute permissions.
     *
     * @param executable The executable.
     * @throws ApplicationException If the executable is invalid.
     */
    static void checkExecutable(File executable) {
        if (!executable.exists() ||
            !executable.canExecute()) {
            throw new ApplicationException("Program is not executable: " + executable);
        }
    }

    /**
     * Check the output directory exists and has write permissions.
     *
     * @param fileOutputPrefix The file output prefix.
     * @throws ApplicationException If the output directory is invalid.
     */
    static void checkOutputDirectory(File fileOutputPrefix) {
        final File reportDir = fileOutputPrefix.getAbsoluteFile().getParentFile();
        if (!reportDir.exists() ||
            !reportDir.isDirectory() ||
            !reportDir.canWrite()) {
            throw new ApplicationException("Invalid output directory: " + reportDir);
        }
    }

    /**
     * Builds the command for the sub-process.
     *
     * @param executable The executable file.
     * @param executableArguments The executable arguments.
     * @return the command
     * @throws ApplicationException If the executable path cannot be resolved
     */
    static List<String> buildSubProcessCommand(File executable,
                                               List<String> executableArguments) {
        final ArrayList<String> command = new ArrayList<>();
        try {
            command.add(executable.getCanonicalPath());
        } catch (final IOException ex) {
            // Not expected to happen as the file has been tested to exist
            throw new ApplicationException("Cannot resolve executable path: " + ex.getMessage(), ex);
        }
        command.addAll(executableArguments);
        return command;
    }

    /**
     * Get the exit value from the process, waiting at most for 1 second, otherwise kill the process
     * and return {@code null}.
     *
     * <p>This should be used when it is expected the process has completed.</p>
     *
     * @param process The process.
     * @return The exit value.
     * @see Process#destroy()
     */
    static Integer getExitValue(Process process) {
        return getExitValue(process, DEFAULT_TIMEOUT_MILLIS);
    }

    /**
     * Get the exit value from the process, waiting at most for the given time, otherwise
     * kill the process and return {@code null}.
     *
     * <p>This should be used when it is expected the process has completed. If the timeout
     * expires an error message is logged before the process is killed.</p>
     *
     * @param process The process.
     * @param timeoutMillis The timeout (in milliseconds).
     * @return The exit value.
     * @see Process#destroy()
     */
    static Integer getExitValue(Process process, long timeoutMillis) {
        final long startTime = System.currentTimeMillis();
        long remaining = timeoutMillis;

        while (remaining > 0) {
            try {
                return process.exitValue();
            } catch (final IllegalThreadStateException ex) {
                try {
                    Thread.sleep(Math.min(remaining + 1, 100));
                } catch (final InterruptedException e) {
                    // Reset interrupted status and stop waiting
                    Thread.currentThread().interrupt();
                    break;
                }
            }
            remaining = timeoutMillis - (System.currentTimeMillis() - startTime);
        }

        LogUtils.error("Failed to obtain exit value after %d ms, forcing termination", timeoutMillis);

        // Not finished so kill it
        process.destroy();

        return null;
    }
}
