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
import picocli.CommandLine.Parameters;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FilterOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumSet;
import java.util.Formatter;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.function.Function;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * Specification for the "results" command.
 *
 * <p>This command creates a summary of the results from known test applications.</p>
 *
 * <ul>
 *   <li>Dieharder
 *   <li>Test U01 (BigCrush, Crush, SmallCrush)
 * </ul>
 */
@Command(name = "results",
         description = {"Collate results from stress test applications."})
class ResultsCommand implements Callable<Void> {
    /** The pattern to identify the RandomSource in the stress test result header. */
    private static final Pattern RANDOM_SOURCE_PATTERN = Pattern.compile("^# RandomSource: (.*)");
    /** The pattern to identify the RNG in the stress test result header. */
    private static final Pattern RNG_PATTERN = Pattern.compile("^# RNG: (.*)");
    /** The pattern to identify the test duration in the stress test result footer. */
    private static final Pattern TEST_DURATION_PATTERN = Pattern.compile("^# Test duration:");
    /** The pattern to identify the Dieharder test format. */
    private static final Pattern DIEHARDER_PATTERN = Pattern.compile("^# *dieharder version");
    /** The pattern to identify a Dieharder failed test result. */
    private static final Pattern DIEHARDER_FAILED_PATTERN = Pattern.compile("FAILED *$");
    /** The pattern to identify the Test U01 test format. */
    private static final Pattern TESTU01_PATTERN = Pattern.compile("^ *Version: TestU01");
    /** The pattern to identify the Test U01 summary header. */
    private static final Pattern TESTU01_SUMMARY_PATTERN = Pattern.compile("^========= Summary results of (\\S*) ");
    /** The pattern to identify the Test U01 failed test result. */
    private static final Pattern TESTU01_TEST_RESULT_PATTERN = Pattern.compile("^  ?(\\d+  .*)    ");
    /** The name of the Dieharder sums test. */
    private static final String DIEHARDER_SUMS = "diehard_sums";
    /** The string identifying a bit-reversed generator. */
    private static final String BIT_REVERSED = "Bit-reversed";

    /** The standard options. */
    @Mixin
    private StandardOptions reusableOptions;

    /** The results files. */
    @Parameters(arity = "1..*",
                description = "The results files.",
                paramLabel = "<file>")
    private List<File> resultsFiles = new ArrayList<>();

    /** The file output prefix. */
    @Option(names = {"-o", "--out"},
            description = "The output file (default: stdout).")
    private File fileOutput;

    /** The output format. */
    @Option(names = {"-f", "--format"},
            description = {"Output format (default: ${DEFAULT-VALUE}).",
                           "Valid values: ${COMPLETION-CANDIDATES}."})
    private ResultsCommand.OutputFormat outputFormat = OutputFormat.TXT;

    /** The flag to show failed tests. */
    @Option(names = {"--failed"},
            description = "Output failed tests (not all formats).")
    private boolean showFailedTests;

    /** The flag to include the Dieharder sums test. */
    @Option(names = {"--include-sums"},
            description = "Include Dieharder sums test.")
    private boolean includeDiehardSums;

    /** The common file path prefix used when outputting file paths. */
    @Option(names = {"--path-prefix"},
            description = {"Common path prefix.",
                           "If specified this will replace the common prefix from all " +
                           "files when the path is output, e.g. for the APT report."})
    private String pathPrefix = "";

    /**
     * The output mode for the results.
     */
    enum OutputFormat {
        /** Comma-Separated Values (CSV) output. */
        CSV,
        /** Almost Plain Text (APT) output. */
        APT,
        /** Text table output. */
        TXT,
    }

    /**
     * The test application file format.
     */
    enum TestFormat {
        /** Dieharder. */
        DIEHARDER,
        /** Test U01. */
        TESTU01,
    }

    /**
     * Encapsulate the results of a test application.
     */
    private static class TestResult {
        /** The result file. */
        private final File resultFile;
        /** The random source. */
        private final RandomSource randomSource;
        /** Flag indicating the generator was bit reversed. */
        private final boolean bitReversed;
        /** The test application format. */
        private final TestFormat testFormat;
        /** The names of the failed tests. */
        private final ArrayList<String> failedTests = new ArrayList<>();
        /** The test application name. */
        private String testApplicationName;
        /** Flag to indicate results are complete (i.e. not still in progress). */
        private boolean complete;

        /**
         * @param resultFile the result file
         * @param randomSource the random source
         * @param bitReversed the bit reversed flag
         * @param testFormat the test format
         */
        TestResult(File resultFile,
                   RandomSource randomSource,
                   boolean bitReversed,
                   TestFormat testFormat) {
            this.resultFile = resultFile;
            this.randomSource = randomSource;
            this.bitReversed = bitReversed;
            this.testFormat = testFormat;
        }

        /**
         * Adds the failed test.
         *
         * @param testId the test id
         */
        void addFailedTest(String testId) {
            failedTests.add(testId);
        }

        /**
         * Gets the result file.
         *
         * @return the result file
         */
        File getResultFile() {
            return resultFile;
        }

        /**
         * Gets the random source.
         *
         * @return the random source
         */
        RandomSource getRandomSource() {
            return randomSource;
        }

        /**
         * Checks if the generator was bit reversed.
         *
         * @return true if bit reversed
         */
        boolean isBitReversed() {
            return bitReversed;
        }

        /**
         * Gets the test format.
         *
         * @return the test format
         */
        TestFormat getTestFormat() {
            return testFormat;
        }

        /**
         * Gets the failed tests.
         *
         * @return the failed tests
         */
        ArrayList<String> getFailedTests() {
            return failedTests;
        }

        /**
         * Gets the failure count.
         *
         * @return the failure count
         */
        int getFailureCount() {
            return failedTests.size();
        }

        /**
         * Gets the failure count as a string. This will be negative if the test is not complete.
         *
         * @return the failure count
         */
        String getFailureCountString() {
            return (isComplete()) ? Integer.toString(failedTests.size()) : "-" + failedTests.size();
        }

        /**
         * Sets the test application name.
         *
         * @param testApplicationName the new test application name
         */
        void setTestApplicationName(String testApplicationName) {
            this.testApplicationName = testApplicationName;
        }

        /**
         * Gets the test application name.
         *
         * @return the test application name
         */
        String getTestApplicationName() {
            return testApplicationName != null ? testApplicationName : getTestFormat().toString();
        }

        /**
         * Checks if the test result is complete.
         *
         * @return true if complete
         */
        boolean isComplete() {
            return complete;
        }

        /**
         * Sets the complete flag.
         *
         * @param complete the new complete
         */
        void setComplete(boolean complete) {
            this.complete = complete;
        }
    }

    /**
     * Reads the results files and outputs in the chosen format.
     */
    @Override
    public Void call() {
        LogUtils.setLogLevel(reusableOptions.logLevel);

        // Read the results
        final List<TestResult> results = readResults();

        try (OutputStream out = createOutputStream()) {
            switch (outputFormat) {
            case CSV:
                writeCSVData(out, results);
                break;
            case APT:
                writeAPT(out, results);
                break;
            case TXT:
                writeTXT(out, results);
                break;
            default:
                throw new ApplicationException("Unknown output format: " + outputFormat);
            }
        } catch (IOException ex) {
            throw new ApplicationException("IO error: " + ex.getMessage(), ex);
        }
        return null;
    }

    /**
     * Read the results.
     *
     * @return the results
     */
    private List<TestResult> readResults() {
        final ArrayList<TestResult> results = new ArrayList<>();
        for (File resultFile : resultsFiles) {
            readResults(results, resultFile);
        }
        return results;
    }

    /**
     * Read the file and extract any test results.
     *
     * @param results Results.
     * @param resultFile Result file.
     */
    private void readResults(List<TestResult> results,
                             File resultFile) {
        final ArrayList<String> contents = readFileContents(resultFile);
        // Files may have multiple test results per file (i.e. appended output)
        final List<List<String>> outputs = splitContents(contents);
        if (outputs.isEmpty()) {
            LogUtils.error("No test output in file: " + resultFile);
        } else {
            for (List<String> testOutput : outputs) {
                results.add(readResult(resultFile, testOutput));
            }
        }
    }

    /**
     * Read the file contents.
     *
     * @param resultFile Result file.
     * @return the file contents
     * @throws ApplicationException If the file cannot be read.
     */
    private static ArrayList<String> readFileContents(File resultFile) {
        final ArrayList<String> contents = new ArrayList<>();
        try (BufferedReader reader = Files.newBufferedReader(resultFile.toPath())) {
            String line;
            while ((line = reader.readLine()) != null) {
                contents.add(line);
            }
        } catch (IOException ex) {
            throw new ApplicationException("Failed to read file contents: " + resultFile, ex);
        }
        return contents;
    }

    /**
     * Split the file contents into separate test output. This is used in the event
     * that results have been appended to the same file.
     *
     * @param contents File contents.
     * @return the test output
     */
    private static List<List<String>> splitContents(List<String> contents) {
        final ArrayList<List<String>> testOutputs = new ArrayList<>();
        // Assume each output begins with e.g. # RandomSource: SPLIT_MIX_64
        // Note each beginning.
        int begin = -1;
        for (int i = 0; i < contents.size(); i++) {
            if (RANDOM_SOURCE_PATTERN.matcher(contents.get(i)).matches()) {
                if (begin >= 0) {
                    testOutputs.add(contents.subList(begin, i));
                }
                begin = i;
            }
        }
        if (begin >= 0) {
            testOutputs.add(contents.subList(begin, contents.size()));
        }
        return testOutputs;
    }

    /**
     * Read the file into a test result.
     *
     * @param resultFile Result file.
     * @param testOutput Test output.
     * @return the test result
     */
    private TestResult readResult(File resultFile,
                                  List<String> testOutput) {
        // Use an iterator for a single pass over the test output
        final Iterator<String> iter = testOutput.iterator();

        // Identify the RandomSource and bit reversed flag from the header:
        final RandomSource randomSource = getRandomSource(iter);
        final boolean bitReversed = getBitReversed(iter);
        // Identify the test application format
        final TestFormat testFormat = getTestFormat(iter);

        // Read the application results
        final TestResult testResult = new TestResult(resultFile, randomSource, bitReversed, testFormat);
        if (testFormat == TestFormat.DIEHARDER) {
            readDieharder(iter, testResult);
        } else {
            readTestU01(iter, testResult);
        }
        return testResult;
    }

    /**
     * Gets the random source from the output header.
     *
     * @param iter Iterator of the test output.
     * @return the random source
     */
    private static RandomSource getRandomSource(Iterator<String> iter) {
        while (iter.hasNext()) {
            final Matcher matcher = RANDOM_SOURCE_PATTERN.matcher(iter.next());
            if (matcher.matches()) {
                return RandomSource.valueOf(matcher.group(1));
            }
        }
        throw new ApplicationException("Failed to find RandomSource header line");
    }

    /**
     * Gets the bit-reversed flag from the output header.
     *
     * @param iter Iterator of the test output.
     * @return the bit-reversed flag
     */
    private static boolean getBitReversed(Iterator<String> iter) {
        while (iter.hasNext()) {
            final Matcher matcher = RNG_PATTERN.matcher(iter.next());
            if (matcher.matches()) {
                return matcher.group(1).contains(BIT_REVERSED);
            }
        }
        throw new ApplicationException("Failed to find RNG header line");
    }

    /**
     * Gets the test format from the output.
     *
     * @param iter Iterator of the test output.
     * @return the test format
     */
    private static TestFormat getTestFormat(Iterator<String> iter) {
        while (iter.hasNext()) {
            final String line = iter.next();
            if (DIEHARDER_PATTERN.matcher(line).find()) {
                return TestFormat.DIEHARDER;
            }
            if (TESTU01_PATTERN.matcher(line).find()) {
                return TestFormat.TESTU01;
            }
        }
        throw new ApplicationException("Failed to identify the test application format");
    }

    /**
     * Read the result output from the Dieharder test application.
     *
     * @param iter Iterator of the test output.
     * @param testResult the test result
     */
    private void readDieharder(Iterator<String> iter,
                               TestResult testResult) {
        // Dieharder results are printed in-line using the following format:
        //#=============================================================================#
        //        test_name   |ntup| tsamples |psamples|  p-value |Assessment
        //#=============================================================================#
        //   diehard_birthdays|   0|       100|     100|0.97484422|  PASSED
        //   ...
        //        diehard_oqso|   0|   2097152|     100|0.00000000|  FAILED

        testResult.setTestApplicationName("Dieharder");

        // Identify any line containing FAILED and then get the test ID using
        // the first two columns (test_name + ntup).
        while (iter.hasNext()) {
            final String line = iter.next();
            if (DIEHARDER_FAILED_PATTERN.matcher(line).find()) {
                // Optionally include the flawed Dieharder sums test
                if (!includeDiehardSums && line.contains(DIEHARDER_SUMS)) {
                    continue;
                }
                final int index1 = line.indexOf('|');
                final int index2 = line.indexOf('|', index1 + 1);
                testResult.addFailedTest(line.substring(0, index1).trim() + ":" +
                                         line.substring(index1 + 1, index2).trim());
            } else if (TEST_DURATION_PATTERN.matcher(line).find()) {
                testResult.setComplete(true);
                break;
            }
        }
    }

    /**
     * Read the result output from the Test U01 test application.
     *
     * @param iter Iterator of the test output.
     * @param testResult the test result
     */
    private static void readTestU01(Iterator<String> iter,
                                    TestResult testResult) {
        // Results are summarised at the end of the file:
        //========= Summary results of BigCrush =========
        //
        //Version:          TestU01 1.2.3
        //Generator:        stdin
        //Number of statistics:  155
        //Total CPU time:   06:14:50.43
        //The following tests gave p-values outside [0.001, 0.9990]:
        //(eps  means a value < 1.0e-300):
        //(eps1 means a value < 1.0e-15):
        //
        //      Test                          p-value
        //----------------------------------------------
        // 1  SerialOver, r = 0                eps
        // 3  CollisionOver, t = 2           1 - eps1
        // 5  CollisionOver, t = 3           1 - eps1
        // 7  CollisionOver, t = 7             eps

        // Identify the summary line
        testResult.setTestApplicationName("TestU01 (" + skipToTestU01Summary(iter) + ")");

        // Read test results using the entire line except the p-value for the test Id
        // Note:
        // This will count sub-parts of the same test as distinct failures.
        while (iter.hasNext()) {
            String line = iter.next();
            Matcher matcher = TESTU01_TEST_RESULT_PATTERN.matcher(line);
            if (matcher.find()) {
                testResult.addFailedTest(matcher.group(1).trim());
            } else if (TEST_DURATION_PATTERN.matcher(line).find()) {
                testResult.setComplete(true);
                break;
            }
        }
    }

    /**
     * Skip to the Test U01 result summary.
     *
     * @param iter Iterator of the test output.
     * @return the name of the test suite
     */
    private static String skipToTestU01Summary(Iterator<String> iter) {
        while (iter.hasNext()) {
            final String line = iter.next();
            final Matcher matcher = TESTU01_SUMMARY_PATTERN.matcher(line);
            if (matcher.find()) {
                return matcher.group(1);
            }
        }
        throw new ApplicationException("Failed to identify the Test U01 result summary");
    }

    /**
     * Creates the output stream. This will not be buffered.
     *
     * @return the output stream
     */
    private OutputStream createOutputStream() {
        if (fileOutput != null) {
            try {
                return new FileOutputStream(fileOutput);
            } catch (FileNotFoundException ex) {
                throw new ApplicationException("Failed to create output: " + fileOutput, ex);
            }
        }
        return new FilterOutputStream(System.out) {
            @Override
            public void close() {
                // Do not close stdout
            }
        };
    }

    /**
     * Write the results to a table in Comma Separated Value (CSV) format.
     *
     * @param out Output stream.
     * @param results Results.
     * @throws IOException Signals that an I/O exception has occurred.
     */
    private void writeCSVData(OutputStream out,
                              List<TestResult> results) throws IOException {
        // Sort by columns
        Collections.sort(results, (o1, o2) -> {
            int result = Integer.compare(o1.getRandomSource().ordinal(), o2.getRandomSource().ordinal());
            if (result != 0) {
                return result;
            }
            result = Boolean.compare(o1.isBitReversed(), o2.isBitReversed());
            if (result != 0) {
                return result;
            }
            result = o1.getTestApplicationName().compareTo(o2.getTestApplicationName());
            if (result != 0) {
                return result;
            }
            return Integer.compare(o1.getFailureCount(), o2.getFailureCount());
        });

        try (BufferedWriter output = new BufferedWriter(new OutputStreamWriter(out, StandardCharsets.UTF_8))) {
            output.write("RandomSource,Bit-reversed,Test,Failures");
            if (showFailedTests) {
                output.write(",Failed");
            }
            output.newLine();
            for (final TestResult result : results) {
                output.write(result.getRandomSource().toString());
                output.write(',');
                output.write(Boolean.toString(result.isBitReversed()));
                output.write(',');
                output.write(result.getTestApplicationName());
                output.write(',');
                output.write(result.getFailureCountString());
                // Optionally write out failed test names.
                if (showFailedTests) {
                    output.write(',');
                    output.write(result.getFailedTests().stream().collect(Collectors.joining("|")));
                }
                output.newLine();
            }
        }
    }

    /**
     * Write the results using the Almost Plain Text (APT) format for a table. This
     * table can be included in the documentation for the Commons RNG site.
     *
     * @param out Output stream.
     * @param results Results.
     * @throws IOException Signals that an I/O exception has occurred.
     */
    private void writeAPT(OutputStream out,
                          List<TestResult> results) throws IOException {
        // Identify all:
        // RandomSources, bit-reversed, test names,
        final List<RandomSource> randomSources = getRandomSources(results);
        final List<Boolean> bitReversed = getBitReversed(results);
        final List<String> testNames = getTestNames(results);

        // Identify the common path prefix to be replaced
        final int prefixLength = (pathPrefix.isEmpty()) ? 0 : findCommonPathPrefixLength(results);

        // Create a function to update the file path and then output the failure count
        // as a link to the file using the APT format.
        final Function<TestResult, String> toAPTString = result -> {
            String path = result.getResultFile().getPath();
            // Remove common path prefix
            path = path.substring(prefixLength);
            // Build the APT relative link
            final StringBuilder sb = new StringBuilder()
                .append("{{{").append(pathPrefix).append(path).append('}')
                .append(result.getFailureCountString()).append("}}");
            // Convert to web-link name separators
            for (int i = 0; i < sb.length(); i++) {
                if (sb.charAt(i) == '\\') {
                    sb.setCharAt(i, '/');
                }
            }
            return sb.toString();
        };

        // Create columns for RandomSource, bit-reversed, each test name.
        // Make bit-reversed column optional if no generators are bit reversed.
        final boolean showBitReversedColumn = bitReversed.contains(Boolean.TRUE);

        final String header = createAPTHeader(showBitReversedColumn, testNames);
        final String separator = createAPTSeparator(header);

        // Output
        try (BufferedWriter output = new BufferedWriter(new OutputStreamWriter(out, StandardCharsets.UTF_8))) {
            // For the first line using '*' (centre) character instead of '+' (left align)
            output.write(separator.replace('+', '*'));
            output.write(header);
            output.newLine();
            output.write(separator);

            // This will collate results for each combination of 'RandomSource + bitReversed'
            for (RandomSource randomSource : randomSources) {
                for (boolean reversed : bitReversed) {
                    output.write('|');
                    writeAPTColumn(output, randomSource.toString());
                    if (showBitReversedColumn) {
                        writeAPTColumn(output, Boolean.toString(reversed));
                    }
                    for (String testName : testNames) {
                        final List<TestResult> testResults = getTestResults(results, randomSource, reversed, testName);
                        writeAPTColumn(output, testResults.stream()
                                                          .map(toAPTString)
                                                          .collect(Collectors.joining(", ")));
                    }
                    output.newLine();
                    output.write(separator);
                }
            }
        }
    }

    /**
     * Gets the random sources present in the results.
     *
     * @param results Results.
     * @return the random sources
     */
    private static List<RandomSource> getRandomSources(List<TestResult> results) {
        final EnumSet<RandomSource> set = EnumSet.noneOf(RandomSource.class);
        for (TestResult result : results) {
            set.add(result.getRandomSource());
        }
        final ArrayList<RandomSource> list = new ArrayList<>(set);
        Collections.sort(list);
        return list;
    }

    /**
     * Gets the bit-reversed options present in the results.
     *
     * @param results Results.
     * @return the bit-reversed options
     */
    private static List<Boolean> getBitReversed(List<TestResult> results) {
        final ArrayList<Boolean> list = new ArrayList<>(2);
        if (!results.isEmpty()) {
            final boolean first = results.get(0).isBitReversed();
            list.add(first);
            for (TestResult result : results) {
                if (first != result.isBitReversed()) {
                    list.add(!first);
                    break;
                }
            }
        } else {
            // Default to no bit-reversed results
            list.add(Boolean.FALSE);
        }
        Collections.sort(list);
        return list;
    }

    /**
     * Gets the test names present in the results.
     *
     * @param results Results.
     * @return the test names
     */
    private static List<String> getTestNames(List<TestResult> results) {
        final HashSet<String> set = new HashSet<>();
        for (TestResult result : results) {
            set.add(result.getTestApplicationName());
        }
        final ArrayList<String> list = new ArrayList<>(set);
        Collections.sort(list);
        return list;
    }

    /**
     * Find the common path prefix for all result files. This is returned as the length of the
     * common prefix.
     *
     * @param results Results.
     * @return the length
     */
    private static int findCommonPathPrefixLength(List<TestResult> results) {
        if (results.isEmpty()) {
            return 0;
        }
        // Find the first prefix
        final String prefix1 = getPathPrefix(results.get(0));
        int length = prefix1.length();
        for (int i = 1; i < results.size() && length != 0; i++) {
            final String prefix2 = getPathPrefix(results.get(i));
            // Update
            final int size = Math.min(prefix2.length(), length);
            length = 0;
            while (length < size && prefix1.charAt(length) == prefix2.charAt(length)) {
                length++;
            }
        }
        return length;
    }

    /**
     * Gets the path prefix.
     *
     * @param testResult Test result.
     * @return the path prefix (or the empty string)
     */
    private static String getPathPrefix(TestResult testResult) {
        final String parent = testResult.getResultFile().getParent();
        return parent != null ? parent : "";
    }

    /**
     * Creates the APT header.
     *
     * @param showBitReversedColumn Set to true to the show bit reversed column.
     * @param testNames Test names.
     * @return the header
     */
    private static String createAPTHeader(boolean showBitReversedColumn,
                                          List<String> testNames) {
        final StringBuilder sb = new StringBuilder("|| RNG identifier ||");
        if (showBitReversedColumn) {
            sb.append(' ').append("Bit-reversed ||");
        }
        for (String name : testNames) {
            sb.append(' ').append(name).append(" ||");
        }
        return sb.toString();
    }

    /**
     * Creates the APT separator for each table row.
     *
     * <p>The separator is created using the '+' character to left align the columns.
     *
     * @param header Header.
     * @return the separator
     */
    private static String createAPTSeparator(String header) {
        // Replace everything with '-' except '|' which is replaced with "*-" for the first
        // character, "+-" for all other occurrences except "-+" at the end
        final StringBuilder sb = new StringBuilder(header);
        for (int i = 0; i < header.length(); i++) {
            if (sb.charAt(i) == '|') {
                sb.setCharAt(i, i == 0 ? '*' : '+');
                sb.setCharAt(i + 1,  '-');
            } else {
                sb.setCharAt(i,  '-');
            }
        }
        // Fix the end
        sb.setCharAt(header.length() - 2, '-');
        sb.setCharAt(header.length() - 1, '+');
        sb.append(System.lineSeparator());
        return sb.toString();
    }

    /**
     * Write the column name to the output.
     *
     * @param output Output.
     * @param name Name.
     * @throws IOException Signals that an I/O exception has occurred.
     */
    private static void writeAPTColumn(BufferedWriter output,
                                       String name) throws IOException {
        output.write(' ');
        output.write(name);
        output.write(" |");
    }

    /**
     * Gets the test results that match the arguments.
     *
     * @param results Results.
     * @param randomSource Random source.
     * @param bitReversed Bit reversed flag.
     * @param testName Test name.
     * @return the matching results
     */
    private static List<TestResult> getTestResults(List<TestResult> results,
                                                   RandomSource randomSource,
                                                   boolean bitReversed,
                                                   String testName) {
        final ArrayList<TestResult> list = new ArrayList<>();
        for (TestResult result : results) {
            if (result.getRandomSource() == randomSource &&
                result.bitReversed == bitReversed &&
                result.getTestApplicationName().equals(testName)) {
                list.add(result);
            }
        }
        return list;
    }

    /**
     * Write the results as a text table.
     *
     * @param out Output stream.
     * @param results Results.
     * @throws IOException Signals that an I/O exception has occurred.
     */
    private static void writeTXT(OutputStream out,
                                 List<TestResult> results) throws IOException {
        // Identify all:
        // RandomSources, bit-reversed, test names,
        final List<RandomSource> randomSources = getRandomSources(results);
        final List<Boolean> bitReversed = getBitReversed(results);
        final List<String> testNames = getTestNames(results);

        // Create columns for RandomSource, bit-reversed, each test name.
        // Make bit-reversed column optional if no generators are bit reversed.
        final boolean showBitReversedColumn = bitReversed.contains(Boolean.TRUE);

        final ArrayList<List<String>> columns = new ArrayList<>();
        columns.add(createColumn("RNG"));
        if (showBitReversedColumn) {
            columns.add(createColumn(BIT_REVERSED));
        }
        for (String testName : testNames) {
            columns.add(createColumn(testName));
        }

        // Add all data
        // This will collate results for each combination of 'RandomSource + bitReversed'
        for (final RandomSource randomSource : randomSources) {
            for (final boolean reversed : bitReversed) {
                int i = 0;
                columns.get(i++).add(randomSource.toString());
                if (showBitReversedColumn) {
                    columns.get(i++).add(Boolean.toString(reversed));
                }
                for (String testName : testNames) {
                    final List<TestResult> testResults = getTestResults(results, randomSource,
                            reversed, testName);
                    columns.get(i++).add(testResults.stream()
                                                    .map(TestResult::getFailureCountString)
                                                    .collect(Collectors.joining(",")));
                }
            }
        }

        // Create format using the column widths
        final StringBuilder sb = new StringBuilder();
        try (Formatter formatter = new Formatter(sb)) {
            for (int i = 0; i < columns.size(); i++) {
                if (i != 0) {
                    sb.append('\t');
                }
                formatter.format("%%-%ds", getColumnWidth(columns.get(i)));
            }
        }
        sb.append(System.lineSeparator());
        final String format = sb.toString();

        // Output
        try (BufferedWriter output = new BufferedWriter(new OutputStreamWriter(out, StandardCharsets.UTF_8));
             Formatter formatter = new Formatter(output)) {
            final int rows = columns.get(0).size();
            final Object[] args = new Object[columns.size()];
            for (int row = 0; row < rows; row++) {
                for (int i = 0; i < args.length; i++) {
                    args[i] = columns.get(i).get(row);
                }
                formatter.format(format, args);
            }
        }
    }

    /**
     * Creates the column.
     *
     * @param columnName Column name.
     * @return the list
     */
    private static List<String> createColumn(String columnName) {
        final ArrayList<String> list = new ArrayList<>();
        list.add(columnName);
        return list;
    }

    /**
     * Gets the column width using the maximum length of the column items.
     *
     * @param column Column.
     * @return the column width
     */
    private static int getColumnWidth(List<String> column) {
        int width = 0;
        for (String text : column) {
            width = Math.max(width, text.length());
        }
        return width;
    }
}
