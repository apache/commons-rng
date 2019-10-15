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

import org.apache.commons.rng.simple.RandomSource;

import picocli.CommandLine.Command;
import picocli.CommandLine.Mixin;
import picocli.CommandLine.Option;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Formatter;
import java.util.HashSet;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Scanner;
import java.util.concurrent.Callable;

/**
 * Specification for the "list" command.
 *
 * <p>This command prints a list of available random generators to the console.</p>
 */
@Command(name = "list",
         description = "List random generators.")
class ListCommand implements Callable<Void> {
    /** The standard options. */
    @Mixin
    private StandardOptions reusableOptions;

    /** The list format. */
    @Option(names = {"-f", "--format"},
            description = {"The list format (default: ${DEFAULT-VALUE}).",
                           "Valid values: ${COMPLETION-CANDIDATES}."},
            paramLabel = "<format>")
    private ListFormat listFormat = ListFormat.STRESS_TEST;

    /** The provider type. */
    @Option(names = {"--provider"},
            description = {"The provider type (default: ${DEFAULT-VALUE}).",
                           "Valid values: ${COMPLETION-CANDIDATES}."},
            paramLabel = "<provider>")
    private ProviderType providerType = ProviderType.ALL;

    /** The prefix for each ID in the template list of random generators. */
    @Option(names = {"-p", "--prefix"},
            description = {"The ID prefix.",
                           "Used for the stress test format."})
    private String idPrefix = "";

    /** The number of trials to put in the template list of random generators. */
    @Option(names = {"-t", "--trials"},
            description = {"The number of trials for each random generator.",
                           "Used for the stress test format."})
    private int trials = 1;

    /**
     * The list format.
     */
    enum ListFormat {
        /** The stress test format lists the data required for the stress test. */
        STRESS_TEST,
        /** The plain format lists only the name and optional arguments. */
        PLAIN
    }

    /**
     * The type of provider.
     */
    enum ProviderType {
        /** List all providers. */
        ALL,
        /** List int providers. */
        INT,
        /** List long providers. */
        LONG,
    }

    /**
     * Prints a template generators list to stdout.
     */
    @Override
    public Void call() throws Exception {
        LogUtils.setLogLevel(reusableOptions.logLevel);
        StressTestDataList list = new StressTestDataList(idPrefix, trials);
        if (providerType == ProviderType.INT) {
            list = list.subsetIntSource();
        } else if (providerType == ProviderType.LONG) {
            list = list.subsetLongSource();
        }
        // Write in one call to the output
        final StringBuilder sb = new StringBuilder();
        switch (listFormat) {
        case PLAIN:
            writePlainData(sb, list);
            break;
        case STRESS_TEST:
        default:
            writeStressTestData(sb, list);
            break;
        }
        // CHECKSTYLE: stop regexp
        System.out.append(sb);
        // CHECKSTYLE: resume regexp
        return null;
    }

    /**
     * Write the test data.
     *
     * <p>Note: If the {@link Appendable} implements {@link java.io.Closeable Closeable} it
     * is <strong>not</strong> closed by this method.
     *
     * @param appendable The appendable.
     * @param testData The test data.
     * @throws IOException Signals that an I/O exception has occurred.
     */
    static void writePlainData(Appendable appendable,
                               Iterable<StressTestData> testData) throws IOException {
        final String newLine = System.lineSeparator();
        for (final StressTestData data : testData) {
            appendable.append(data.getRandomSource().name());
            if (data.getArgs() != null) {
                appendable.append(' ');
                appendable.append(Arrays.toString(data.getArgs()));
            }
            appendable.append(newLine);
        }
    }

    /**
     * Write the test data.
     *
     * <p>Performs adjustment of the number of trials for each item:
     *
     * <ul>
     *   <li>Any item with trials {@code <= 0} will be written as zero.
     *   <li>Any item with trials {@code > 0} will be written as the maximum of the value and
     *       the input parameter {@code numberOfTrials}.
     * </ul>
     *
     * <p>This allows the output to contain a configurable number of trials for the
     * list of data.
     *
     * <p>Note: If the {@link Appendable} implements {@link java.io.Closeable Closeable} it
     * is <strong>not</strong> closed by this method.
     *
     * @param appendable The appendable.
     * @param testData The test data.
     * @throws IOException Signals that an I/O exception has occurred.
     */
    static void writeStressTestData(Appendable appendable,
                                    Iterable<StressTestData> testData) throws IOException {
        // Build the widths for each column
        int idWidth = 0;
        int randomSourceWidth = 15;
        for (final StressTestData data : testData) {
            idWidth = Math.max(idWidth, data.getId().length());
            randomSourceWidth = Math.max(randomSourceWidth, data.getRandomSource().name().length());
        }

        final String newLine = System.lineSeparator();

        appendable.append("# Random generators list.").append(newLine);
        appendable.append("# Any generator with no trials is ignored during testing.").append(newLine);
        appendable.append("#").append(newLine);

        String format = String.format("# %%-%ds   %%-%ds   trials   [constructor arguments ...]%%n",
                idWidth, randomSourceWidth);
        // Do not use try-with-resources or the Formatter will close the Appendable
        // if it implements Closeable. Just flush at the end.
        @SuppressWarnings("resource")
        final Formatter formatter = new Formatter(appendable);
        formatter.format(format, "ID", "RandomSource");
        format = String.format("%%-%ds   %%-%ds   ", idWidth + 2, randomSourceWidth);
        for (final StressTestData data : testData) {
            formatter.format(format, data.getId(), data.getRandomSource().name());
            if (data.getArgs() == null) {
                appendable.append(Integer.toString(data.getTrials()));
            } else {
                formatter.format("%-6d   %s", data.getTrials(), Arrays.toString(data.getArgs()));
            }
            appendable.append(newLine);
        }
        formatter.flush();
    }

    /**
     * Reads the test data. The {@link Readable} is not closed by this method.
     *
     * @param readable The readable.
     * @return The test data.
     * @throws IOException Signals that an I/O exception has occurred.
     * @throws ApplicationException If there was an error parsing the expected format.
     * @see java.io.Reader#close() Reader.close()
     */
    static Iterable<StressTestData> readStressTestData(Readable readable) throws IOException {
        final List<StressTestData> list = new ArrayList<>();

        // Validate that all IDs are unique.
        final HashSet<String> ids = new HashSet<>();

        // Do not use try-with-resources as the readable must not be closed
        @SuppressWarnings("resource")
        final Scanner scanner = new Scanner(readable);
        try {
            while (scanner.hasNextLine()) {
                // Expected format:
                //
                //# ID   RandomSource           trials    [constructor arguments ...]
                //12     TWO_CMRES              1
                //13     TWO_CMRES_SELECT       0         [1, 2]
                final String id = scanner.next();
                // Skip empty lines and comments
                if (id.isEmpty() || id.charAt(0) == '#') {
                    scanner.nextLine();
                    continue;
                }
                if (!ids.add(id)) {
                    throw new ApplicationException("Non-unique ID in strest test data: " + id);
                }
                final RandomSource randomSource = RandomSource.valueOf(scanner.next());
                final int trials = scanner.nextInt();
                // The arguments are the rest of the line
                final String arguments = scanner.nextLine().trim();
                final Object[] args = parseArguments(randomSource, arguments);
                list.add(new StressTestData(id, randomSource, args, trials));
            }
        } catch (NoSuchElementException | IllegalArgumentException ex) {
            if (scanner.ioException() != null) {
                throw scanner.ioException();
            }
            throw new ApplicationException("Failed to read stress test data", ex);
        }

        return list;
    }

    /**
     * Parses the arguments string into an array of {@link Object}.
     *
     * <p>Returns {@code null} if the string is empty.
     *
     * @param randomSource the random source
     * @param arguments the arguments
     * @return the arguments {@code Object[]}
     */
    static Object[] parseArguments(RandomSource randomSource,
                                   String arguments) {
        // Empty string is null arguments
        if (arguments.isEmpty()) {
            return null;
        }

        // Expected format:
        // [data1, data2, ...]
        final int len = arguments.length();
        if (len < 2 || arguments.charAt(0) != '[' || arguments.charAt(len - 1) != ']') {
            throw new ApplicationException("RandomSource arguments should be an [array]: " + arguments);
        }

        // Split the text between the [] on commas
        final String[] tokens = arguments.substring(1, len - 1).split(", *");
        final ArrayList<Object> args = new ArrayList<>();
        for (final String token : tokens) {
            args.add(RNGUtils.parseArgument(token));
        }
        return args.toArray();
    }
}
