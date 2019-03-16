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

import java.io.BufferedOutputStream;
import java.io.BufferedWriter;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

/**
 * Class that can be used for testing that {@code int} values can be piped to a
 * program that reads {@code int} values from its standard input. Only
 * a limited number of {@code int} values will be written.
 *
 * Example of command line, assuming that "examples.jar" specifies this
 * class as the "main" class (see {@link #main(String[]) main} method):
 * <pre><code>
 *  $ java -jar examples.jar \
 *    target/bridge \
 *    ./stdin2testu01 stdout
 * </code></pre>
 */
public class BridgeTester {
    /** Lookup table for binary representation of bytes. */
    private static final String[] BIT_REP = {
        "0000", "0001", "0010", "0011",
        "0100", "0101", "0110", "0111",
        "1000", "1001", "1010", "1011",
        "1100", "1101", "1110", "1111",
    };

    /** Command line. */
    private final List<String> cmdLine;
    /** Output prefix. */
    private final String fileOutputPrefix;

    /**
     * Creates the application.
     *
     * @param cmd Command line.
     * @param outputPrefix Output prefix for file reports.
     */
    private BridgeTester(List<String> cmd,
                         String outputPrefix) {
        final File exec = new File(cmd.get(0));
        if (!exec.exists() ||
            !exec.canExecute()) {
            throw new IllegalArgumentException("Program is not executable: " + exec);
        }

        cmdLine = new ArrayList<>(cmd);
        this.fileOutputPrefix = outputPrefix;

        final File reportDir = new File(fileOutputPrefix).getParentFile();
        if (!reportDir.exists() ||
            !reportDir.isDirectory() ||
            !reportDir.canWrite()) {
            throw new IllegalArgumentException("Invalid output directory: " + reportDir);
        }
    }

    /**
     * Program's entry point.
     *
     * @param args Application's arguments.
     * The order is as follows:
     * <ol>
     *  <li>Output prefix: Filename prefix where the output will written to.
     *   The appended suffix is [.data] for the Java native {@code int} values,
     *   and [.out] and [.err] for the stdout/stderr from the called executable.</li>
     *  <li>Path to the executable: this is the software that reads 32-bit
     *   integers from stdin.</li>
     *  <li>All remaining arguments are passed to the executable.</li>
     * </ol>
     */
    public static void main(String[] args) {
        final String outputPrefix = args[0];

        final List<String> cmdLine = new ArrayList<>();
        cmdLine.addAll(Arrays.asList(Arrays.copyOfRange(args, 1, args.length)));

        final BridgeTester app = new BridgeTester(cmdLine, outputPrefix);

        // Throws runtime exceptions
        app.run();
    }

    /**
     * Starts the executable process and writes {@code int} values to a data file and the stdin
     * of the executable. Captures stdout of the executable to a file.
     */
    private void run() {
        try {
            final File dataFile = new File(fileOutputPrefix + ".data");
            final File outputFile = new File(fileOutputPrefix + ".out");
            final File errorFile = new File(fileOutputPrefix + ".err");

            // Start the application.
            final ProcessBuilder builder = new ProcessBuilder(cmdLine);
            builder.redirectOutput(ProcessBuilder.Redirect.to(outputFile));
            builder.redirectError(ProcessBuilder.Redirect.to(errorFile));
            final Process testingProcess = builder.start();

            // Both resources will be closed automatically
            try (
                // Open the stdin of the process
                DataOutputStream output = new DataOutputStream(
                    new BufferedOutputStream(testingProcess.getOutputStream()));
                // Open the file for Java int data
                BufferedWriter data = new BufferedWriter(new FileWriter(dataFile))) {

                // Write int data using a single bit in all possible positions
                int value = 1;
                for (int i = 0; i < Integer.SIZE; i++) {
                    writeInt(data, output, value);
                    value <<= 1;
                }

                // Write random int data
                final ThreadLocalRandom rng = ThreadLocalRandom.current();
                for (int i = 0; i < Integer.SIZE; i++) {
                    writeInt(data, output, rng.nextInt());
                }
            }

        } catch (IOException e) {
            throw new RuntimeException("Failed to run process: " + e.getMessage());
        }
    }

    /**
     * Write an {@code int} value to the buffered writer and the output. The native
     * Java value will be written to the buffered writer on a single line using: a
     * binary string representation of the bytes; the unsigned integer; and the
     * signed integer.
     *
     * <pre>
     * 11001101 00100011 01101111 01110000   3441651568  -853315728
     * </pre>
     *
     * @param data the native data buffered writer.
     * @param output the output.
     * @param value the value.
     * @throws IOException Signals that an I/O exception has occurred.
     */
    private static void writeInt(BufferedWriter data,
                                 DataOutputStream output,
                                 int value) throws IOException {

        // Write out as 4 bytes with spaces between them, high byte first.
        writeByte(data, (value >>> 24) & 0xff);
        data.write(' ');
        writeByte(data, (value >>> 16) & 0xff);
        data.write(' ');
        writeByte(data, (value >>>  8) & 0xff);
        data.write(' ');
        writeByte(data, (value >>>  0) & 0xff);

        // Write the unsigned and signed int value
        data.write(String.format(" %11d %11d%n", value & 0xffffffffL, value));

        // Write the raw binary data to the output
        output.writeInt(value);
    }

    /**
     * Write the lower 8 bits of an {@code int} value to the buffered writer using a binary
     * string representation. This is left-filled with zeros if applicable.
     *
     * <pre>
     * 11001101
     * </pre>
     *
     * @param data the buffered writer.
     * @param value the value.
     * @throws IOException Signals that an I/O exception has occurred.
     */
    private static void writeByte(BufferedWriter data,
                                  int value) throws IOException {
        // This matches the functionality of:
        // data.write(String.format("%8s", Integer.toBinaryString(value & 0xff)).replace(' ', '0'));
        data.write(BIT_REP[value >>> 4]);
        data.write(BIT_REP[value & 0x0F]);
    }
}
