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

import org.apache.commons.rng.UniformRandomProvider;
import org.apache.commons.rng.core.source32.IntProvider;

import picocli.CommandLine.Command;
import picocli.CommandLine.Mixin;
import picocli.CommandLine.Option;
import picocli.CommandLine.Parameters;

import java.io.BufferedWriter;
import java.io.File;
import java.io.IOException;
import java.io.Writer;
import java.nio.ByteOrder;
import java.nio.IntBuffer;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ThreadLocalRandom;

/**
 * Specification for the "bridge" command.
 *
 * <p>This command tests that {@code int} values can be piped to a
 * program that reads {@code int} values from its standard input. Only
 * a limited number of {@code int} values will be written.</p>
 */
@Command(name = "bridge",
         description = {"Transfer test 32-bit data to a test application sub-process via standard input."})
class BridgeTestCommand implements Callable<Void> {
    /** The standard options. */
    @Mixin
    private StandardOptions reusableOptions;

    /** The executable. */
    @Parameters(index = "0",
                description = "The stress test executable.")
    private File executable;

    /** The executable arguments. */
    @Parameters(index = "1..*",
                description = "The arguments to pass to the executable.",
                paramLabel = "<argument>")
    private List<String> executableArguments = new ArrayList<>();

    /** The file output prefix. */
    @Option(names = {"--prefix"},
            description = "Results file prefix (default: ${DEFAULT-VALUE}).")
    private File fileOutputPrefix = new File("bridge");

    /** The output byte order of the binary data. */
    @Option(names = {"-b", "--byte-order"},
            description = {"Byte-order of the transferred data (default: ${DEFAULT-VALUE}).",
                           "Valid values: BIG_ENDIAN, LITTLE_ENDIAN."})
    private ByteOrder byteOrder = ByteOrder.nativeOrder();

    /**
     * Validates the run command arguments and and runs the bridge test.
     */
    @Override
    public Void call() {
        LogUtils.setLogLevel(reusableOptions.logLevel);
        ProcessUtils.checkExecutable(executable);
        ProcessUtils.checkOutputDirectory(fileOutputPrefix);
        runBridgeTest();
        return null;
    }

    /**
     * Starts the executable process and writes {@code int} values to a data file and the stdin
     * of the executable. Captures stdout of the executable to a file.
     */
    private void runBridgeTest() {
        final List<String> command = ProcessUtils.buildSubProcessCommand(executable, executableArguments);

        try {
            final File dataFile = new File(fileOutputPrefix + ".data");
            final File outputFile = new File(fileOutputPrefix + ".out");
            final File errorFile = new File(fileOutputPrefix + ".err");

            // Store int values
            final IntBuffer buffer = IntBuffer.allocate(Integer.SIZE * 2);

            try (BufferedWriter textOutput = Files.newBufferedWriter(dataFile.toPath())) {
                // Write int data using a single bit in all possible positions
                int value = 1;
                for (int i = 0; i < Integer.SIZE; i++) {
                    writeInt(textOutput, buffer, value);
                    value <<= 1;
                }

                // Write random int data
                while (buffer.remaining() != 0) {
                    writeInt(textOutput, buffer, ThreadLocalRandom.current().nextInt());
                }
            }

            // Pass the same values to the output application
            buffer.flip();
            final UniformRandomProvider rng = new IntProvider() {
                @Override
                public int next() {
                    return buffer.get();
                }
            };

            // Start the application.
            final ProcessBuilder builder = new ProcessBuilder(command);
            builder.redirectOutput(ProcessBuilder.Redirect.to(outputFile));
            builder.redirectError(ProcessBuilder.Redirect.to(errorFile));
            final Process testingProcess = builder.start();

            // Open the stdin of the process and write to a custom data sink.
            // Note: The 'bridge' command only supports 32-bit data in order to
            // demonstrate passing suitable data for TestU01 BigCrush.
            final boolean raw64 = false;
            try (RngDataOutput sink = RNGUtils.createDataOutput(rng, raw64,
                    testingProcess.getOutputStream(), buffer.capacity() * 4, byteOrder)) {
                sink.write(rng);
            }

            final Integer exitValue = ProcessUtils.getExitValue(testingProcess);
            if (exitValue == null) {
                LogUtils.error("%s did not exit. Process was killed.", command.get(0));
            } else {
                final int value = exitValue;
                if (value != 0) {
                    LogUtils.error("%s exit code = %d", command.get(0), value);
                }
            }

        } catch (IOException ex) {
            throw new ApplicationException("Failed to run process: " + ex.getMessage(), ex);
        }
    }

    /**
     * Write an {@code int} value to the writer and the buffer output. The native Java
     * value will be written to the writer using the debugging output of the
     * {@link OutputCommand}.
     *
     * @param textOutput the text data writer.
     * @param buffer the buffer.
     * @param value the value.
     * @throws IOException Signals that an I/O exception has occurred.
     * @see OutputCommand#writeInt(java.io.Writer, int)
     */
    private static void writeInt(Writer textOutput,
                                 IntBuffer buffer,
                                 int value) throws IOException {
        OutputCommand.writeInt(textOutput, value);
        buffer.put(value);
    }
}
