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
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.ListIterator;
import java.util.Map.Entry;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.function.Function;
import java.util.function.Supplier;
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
    /** The pattern to identify the test exit code in the stress test result footer. */
    private static final Pattern TEST_EXIT_PATTERN = Pattern.compile("^# Exit value: (\\d+)");
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
    /** The pattern to identify the Test U01 test starting entry. */
    private static final Pattern TESTU01_STARTING_PATTERN = Pattern.compile("^ *Starting (\\S*)");
    /** The pattern to identify the PractRand test format. */
    private static final Pattern PRACTRAND_PATTERN = Pattern.compile("PractRand version");
    /** The pattern to identify the PractRand output byte size. */
    private static final Pattern PRACTRAND_OUTPUT_SIZE_PATTERN = Pattern.compile("\\(2\\^(\\d+) bytes\\)");
    /** The pattern to identify a PractRand failed test result. */
    private static final Pattern PRACTRAND_FAILED_PATTERN = Pattern.compile("FAIL *!* *$");
    /** The name of the Dieharder sums test. */
    private static final String DIEHARDER_SUMS = "diehard_sums";
    /** The string identifying a bit-reversed generator. */
    private static final String BIT_REVERSED = "Bit-reversed";
    /** Character '\'. */
    private static final char FORWARD_SLASH = '\\';
    /** Character '/'. */
    private static final char BACK_SLASH = '\\';
    /** Character '|'. */
    private static final char PIPE = '|';
    /** The column name for the RNG. */
    private static final String COLUMN_RNG = "RNG";
    /** Constant for conversion of bytes to binary units, prefixed with a space. */
    private static final String[] BINARY_UNITS = {" B", " KiB", " MiB", " GiB", " TiB", " PiB", " EiB"};

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
            description = {"Output failed tests (support varies by format).",
                           "CSV: List individual test failures.",
                           "APT: Count of systematic test failures."})
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

    /** The flag to ignore partial results. */
    @Option(names = {"-i", "--ignore"},
            description = "Ignore partial results.")
    private boolean ignorePartialResults;

    /** The flag to delete partial results files. */
    @Option(names = {"--delete"},
            description = {"Delete partial results files.",
                           "This is not reversible!"})
    private boolean deletePartialResults;

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
        /** Systematic failures output. */
        FAILURES,
    }

    /**
     * The test application file format.
     */
    enum TestFormat {
        /** Dieharder. */
        DIEHARDER,
        /** Test U01. */
        TESTU01,
        /** PractRand. */
        PRACTRAND,
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
        private final List<String> failedTests = new ArrayList<>();
        /** The test application name. */
        private String testApplicationName;
        /**
         * Store the exit code.
         * Initialised to {@link Integer#MIN_VALUE}. Exit values are expected to be 8-bit numbers
         * with zero for success.
         */
        private int exitCode = Integer.MIN_VALUE;

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
        List<String> getFailedTests() {
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
         * Gets the a failure summary string.
         *
         * <p>The default implementation is the count of the number of failures.
         * This will be negative if the test is not complete.</p>
         *
         * @return the failure summary
         */
        String getFailureSummaryString() {
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
            return testApplicationName == null ? String.valueOf(getTestFormat()) : testApplicationName;
        }

        /**
         * Checks if the test result is complete.
         * This is {@code true} only if the exit code was found and is zero.
         *
         * @return true if complete
         */
        boolean isComplete() {
            return exitCode == 0;
        }

        /**
         * Sets the exit code flag.
         *
         * @param exitCode the new exit code
         */
        void setExitCode(int exitCode) {
            this.exitCode = exitCode;
        }
    }

    /**
     * Encapsulate the results of a PractRand test application. This is a specialisation that
     * allows handling PractRand results which are linked to the output length used by the RNG.
     */
    private static class PractRandTestResult extends TestResult {
        /** The length of the RNG output used to generate failed tests. */
        private int lengthExponent;

        /**
         * @param resultFile the result file
         * @param randomSource the random source
         * @param bitReversed the bit reversed flag
         * @param testFormat the test format
         */
        PractRandTestResult(File resultFile, RandomSource randomSource, boolean bitReversed, TestFormat testFormat) {
            super(resultFile, randomSource, bitReversed, testFormat);
        }

        /**
         * Gets the length of the RNG output used to generate failed tests.
         * If this is zero then no failures occurred.
         *
         * @return the length exponent
         */
        int getLengthExponent() {
            return lengthExponent;
        }

        /**
         * Sets the length of the RNG output used to generate failed tests.
         *
         * @param lengthExponent the length exponent
         */
        void setLengthExponent(int lengthExponent) {
            this.lengthExponent = lengthExponent;
        }

        /**
         * {@inheritDoc}
         *
         * <p>The PractRand summary is the exponent of the length of byte output from the RNG where
         * a failure occurred.
         */
        @Override
        String getFailureSummaryString() {
            return lengthExponent == 0 ? "-" : Integer.toString(lengthExponent);
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

        if (deletePartialResults) {
            deleteIfIncomplete(results);
            return null;
        }

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
            case FAILURES:
                writeFailures(out, results);
                break;
            default:
                throw new ApplicationException("Unknown output format: " + outputFormat);
            }
        } catch (final IOException ex) {
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
        for (final File resultFile : resultsFiles) {
            readResults(results, resultFile);
        }
        return results;
    }

    /**
     * Read the file and extract any test results.
     *
     * @param results Results.
     * @param resultFile Result file.
     * @throws ApplicationException If the results cannot be parsed.
     */
    private void readResults(List<TestResult> results,
                             File resultFile) {
        final List<String> contents = readFileContents(resultFile);
        // Files may have multiple test results per file (i.e. appended output)
        final List<List<String>> outputs = splitContents(contents);
        if (outputs.isEmpty()) {
            LogUtils.error("No test output in file: %s", resultFile);
        } else {
            for (final List<String> testOutput : outputs) {
                final TestResult result = readResult(resultFile, testOutput);
                if (!result.isComplete()) {
                    LogUtils.info("Partial results in file: %s", resultFile);
                    if (ignorePartialResults) {
                        continue;
                    }
                }
                results.add(result);
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
    private static List<String> readFileContents(File resultFile) {
        final ArrayList<String> contents = new ArrayList<>();
        try (BufferedReader reader = Files.newBufferedReader(resultFile.toPath())) {
            for (String line = reader.readLine(); line != null; line = reader.readLine()) {
                contents.add(line);
            }
        } catch (final IOException ex) {
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
     * @throws ApplicationException If the result cannot be parsed.
     */
    private TestResult readResult(File resultFile,
                                  List<String> testOutput) {
        // Use an iterator for a single pass over the test output
        final ListIterator<String> iter = testOutput.listIterator();

        // Identify the RandomSource and bit reversed flag from the header:
        final RandomSource randomSource = getRandomSource(resultFile, iter);
        final boolean bitReversed = getBitReversed(resultFile, iter);

        // Identify the test application format. This may return null.
        final TestFormat testFormat = getTestFormat(resultFile, iter);

        // Read the application results
        final TestResult testResult = createTestResult(resultFile, randomSource, bitReversed, testFormat);
        if (testFormat == TestFormat.DIEHARDER) {
            readDieharder(iter, testResult);
        } else if (testFormat == TestFormat.TESTU01) {
            readTestU01(resultFile, iter, testResult);
        } else {
            readPractRand(iter, (PractRandTestResult) testResult);
        }
        return testResult;
    }

    /**
     * Creates the test result.
     *
     * @param resultFile Result file.
     * @param randomSource Random source.
     * @param bitReversed True if the random source was bit reversed.
     * @param testFormat Test format.
     * @return the test result
     */
    private static TestResult createTestResult(File resultFile, RandomSource randomSource,
                                               boolean bitReversed, TestFormat testFormat) {
        return testFormat == TestFormat.PRACTRAND ?
            new PractRandTestResult(resultFile, randomSource, bitReversed, testFormat) :
            new TestResult(resultFile, randomSource, bitReversed, testFormat);
    }

    /**
     * Gets the random source from the output header.
     *
     * @param resultFile Result file (for the exception message).
     * @param iter Iterator of the test output.
     * @return the random source
     * @throws ApplicationException If the RandomSource header line cannot be found.
     */
    private static RandomSource getRandomSource(File resultFile, Iterator<String> iter) {
        while (iter.hasNext()) {
            final Matcher matcher = RANDOM_SOURCE_PATTERN.matcher(iter.next());
            if (matcher.matches()) {
                return RandomSource.valueOf(matcher.group(1));
            }
        }
        throw new ApplicationException("Failed to find RandomSource header line: " + resultFile);
    }

    /**
     * Gets the bit-reversed flag from the output header.
     *
     * @param resultFile Result file (for the exception message).
     * @param iter Iterator of the test output.
     * @return the bit-reversed flag
     * @throws ApplicationException If the RNG header line cannot be found.
     */
    private static boolean getBitReversed(File resultFile, Iterator<String> iter) {
        while (iter.hasNext()) {
            final Matcher matcher = RNG_PATTERN.matcher(iter.next());
            if (matcher.matches()) {
                return matcher.group(1).contains(BIT_REVERSED);
            }
        }
        throw new ApplicationException("Failed to find RNG header line: " + resultFile);
    }

    /**
     * Gets the test format from the output. This scans the stdout produced by a test application.
     * If it is not recognised this may be a valid partial result or an unknown result. Throw
     * an exception if not allowing partial results, otherwise log an error.
     *
     * @param resultFile Result file (for the exception message).
     * @param iter Iterator of the test output.
     * @return the test format (or null)
     * @throws ApplicationException If the test format cannot be found.
     */
    private TestFormat getTestFormat(File resultFile, Iterator<String> iter) {
        while (iter.hasNext()) {
            final String line = iter.next();
            if (DIEHARDER_PATTERN.matcher(line).find()) {
                return TestFormat.DIEHARDER;
            }
            if (TESTU01_PATTERN.matcher(line).find()) {
                return TestFormat.TESTU01;
            }
            if (PRACTRAND_PATTERN.matcher(line).find()) {
                return TestFormat.PRACTRAND;
            }
        }
        if (!ignorePartialResults) {
            throw new ApplicationException("Failed to identify the test application format: " + resultFile);
        }
        LogUtils.error("Failed to identify the test application format: %s", resultFile);
        return null;
    }

    /**
     * Read the result output from the Dieharder test application.
     *
     * @param iter Iterator of the test output.
     * @param testResult Test result.
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
            } else if (findExitCode(testResult, line)) {
                return;
            }
        }
    }

    /**
     * Find the exit code in the line. Update the test result with the code if found.
     *
     * @param testResult Test result.
     * @param line Line from the test result output.
     * @return true, if the exit code was found
     */
    private static boolean findExitCode(TestResult testResult, String line) {
        final Matcher matcher = TEST_EXIT_PATTERN.matcher(line);
        if (matcher.find()) {
            testResult.setExitCode(Integer.parseInt(matcher.group(1)));
            return true;
        }
        return false;
    }

    /**
     * Read the result output from the Test U01 test application.
     *
     * <p>Test U01 outputs a summary of results at the end of the test output. If this cannot
     * be found the method will throw an exception unless partial results are allowed.</p>
     *
     * @param resultFile Result file (for the exception message).
     * @param iter Iterator of the test output.
     * @param testResult Test result.
     * @throws ApplicationException If the TestU01 summary cannot be found.
     */
    private void readTestU01(File resultFile,
                             ListIterator<String> iter,
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
        final String testSuiteName = skipToTestU01Summary(resultFile, iter);

        // This may not be present if the results are not complete
        if (testSuiteName == null) {
            // Rewind
            while (iter.hasPrevious()) {
                iter.previous();
            }
            updateTestU01ApplicationName(iter, testResult);
            return;
        }

        setTestU01ApplicationName(testResult, testSuiteName);

        // Read test results using the entire line except the p-value for the test Id
        // Note:
        // This will count sub-parts of the same test as distinct failures.
        while (iter.hasNext()) {
            final String line = iter.next();
            final Matcher matcher = TESTU01_TEST_RESULT_PATTERN.matcher(line);
            if (matcher.find()) {
                testResult.addFailedTest(matcher.group(1).trim());
            } else if (findExitCode(testResult, line)) {
                return;
            }
        }
    }

    /**
     * Sets the Test U01 application name using the provide test suite name.
     *
     * @param testResult Test result.
     * @param testSuiteName Test suite name.
     */
    private static void setTestU01ApplicationName(TestResult testResult, String testSuiteName) {
        testResult.setTestApplicationName("TestU01 (" + testSuiteName + ")");
    }

    /**
     * Skip to the Test U01 result summary.
     *
     * <p>If this cannot be found the method will throw an exception unless partial results
     * are allowed.</p>
     *
     * @param resultFile Result file (for the exception message).
     * @param iter Iterator of the test output.
     * @return the name of the test suite
     * @throws ApplicationException If the TestU01 summary cannot be found.
     */
    private String skipToTestU01Summary(File resultFile, Iterator<String> iter) {
        final String testSuiteName = findMatcherGroup1(iter, TESTU01_SUMMARY_PATTERN);
        // Allow the partial result to be ignored
        if (testSuiteName != null || ignorePartialResults) {
            return testSuiteName;
        }
        throw new ApplicationException("Failed to identify the Test U01 result summary: " + resultFile);
    }

    /**
     * Update the test application name from the Test U01 results. This can be used to identify
     * the test suite from the start of the results in the event that the results summary has
     * not been output.
     *
     * @param iter Iterator of the test output.
     * @param testResult Test result.
     */
    private static void updateTestU01ApplicationName(Iterator<String> iter,
                                                     TestResult testResult) {
        final String testSuiteName = findMatcherGroup1(iter, TESTU01_STARTING_PATTERN);
        if (testSuiteName != null) {
            setTestU01ApplicationName(testResult, testSuiteName);
        }
    }

    /**
     * Create a matcher for each item in the iterator and if a match is identified return
     * group 1.
     *
     * @param iter Iterator of text to match.
     * @param pattern Pattern to match.
     * @return the string (or null)
     */
    private static String findMatcherGroup1(Iterator<String> iter,
                                            Pattern pattern) {
        while (iter.hasNext()) {
            final String line = iter.next();
            final Matcher matcher = pattern.matcher(line);
            if (matcher.find()) {
                return matcher.group(1);
            }
        }
        return null;
    }

    /**
     * Read the result output from the PractRand test application.
     *
     * @param iter Iterator of the test output.
     * @param testResult Test result.
     */
    private static void readPractRand(Iterator<String> iter,
                                      PractRandTestResult testResult) {
        // PractRand results are printed for blocks of byte output that double in size.
        // The results report 'unusual' and 'suspicious' output and the test stops
        // at the first failure.
        // Results are typically reported as the size of output that failed, e.g.
        // the JDK random fails at 256KiB.
        //
        // rng=RNG_stdin32, seed=0xfc6c7332
        // length= 128 kilobytes (2^17 bytes), time= 5.9 seconds
        //   no anomalies in 118 test result(s)
        //
        // rng=RNG_stdin32, seed=0xfc6c7332
        // length= 256 kilobytes (2^18 bytes), time= 7.6 seconds
        //   Test Name                         Raw       Processed     Evaluation
        //   [Low1/64]Gap-16:A                 R= +12.4  p =  8.7e-11   VERY SUSPICIOUS
        //   [Low1/64]Gap-16:B                 R= +13.4  p =  3.0e-11    FAIL
        //   [Low8/32]DC6-9x1Bytes-1           R=  +7.6  p =  5.7e-4   unusual
        //   ...and 136 test result(s) without anomalies

        testResult.setTestApplicationName("PractRand");

        // Store the exponent of the current output length.
        int exp = 0;

        // Identify any line containing FAIL and then get the test name using
        // the first token in the line.
        while (iter.hasNext()) {
            String line = iter.next();
            final Matcher matcher = PRACTRAND_OUTPUT_SIZE_PATTERN.matcher(line);
            if (matcher.find()) {
                // Store the current output length
                exp = Integer.parseInt(matcher.group(1));
            } else if (PRACTRAND_FAILED_PATTERN.matcher(line).find()) {
                // Record the output length where failures occurred.
                testResult.setLengthExponent(exp);
                // Add the failed test name. This does not include the length exponent
                // allowing meta-processing of systematic failures.
                // Remove initial whitespace
                line = line.trim();
                final int index = line.indexOf(' ');
                testResult.addFailedTest(line.substring(0, index));
            } else if (findExitCode(testResult, line)) {
                return;
            }
        }
    }

    /**
     * Delete any result file if incomplete.
     *
     * @param results Results.
     * @throws ApplicationException If a file could not be deleted.
     */
    private static void deleteIfIncomplete(List<TestResult> results) {
        results.forEach(ResultsCommand::deleteIfIncomplete);
    }

    /**
     * Delete the result file if incomplete.
     *
     * @param result Test result.
     * @throws ApplicationException If the file could not be deleted.
     */
    private static void deleteIfIncomplete(TestResult result) {
        if (!result.isComplete()) {
            try {
                Files.delete(result.getResultFile().toPath());
                LogUtils.info("Deleted file: %s", result.getResultFile());
            } catch (IOException ex) {
                throw new ApplicationException("Failed to delete file: " + result.getResultFile(), ex);
            }
        }
    }

    /**
     * Creates the output stream. This will not be buffered.
     *
     * @return the output stream
     */
    private OutputStream createOutputStream() {
        if (fileOutput != null) {
            try {
                return Files.newOutputStream(fileOutput.toPath());
            } catch (final IOException ex) {
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
                output.write(result.getFailureSummaryString());
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
                .append(result.getFailureSummaryString()).append("}}");
            // Convert to web-link name separators
            for (int i = 0; i < sb.length(); i++) {
                if (sb.charAt(i) == BACK_SLASH) {
                    sb.setCharAt(i, FORWARD_SLASH);
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

            final StringBuilder sb = new StringBuilder();

            // This will collate results for each combination of 'RandomSource + bitReversed'
            for (final RandomSource randomSource : randomSources) {
                for (final boolean reversed : bitReversed) {
                    // Highlight in bold a RNG with no systematic failures
                    boolean highlight = true;

                    // Buffer the column output
                    sb.setLength(0);

                    if (showBitReversedColumn) {
                        writeAPTColumn(sb, Boolean.toString(reversed), false);
                    }
                    for (final String testName : testNames) {
                        final List<TestResult> testResults = getTestResults(results, randomSource, reversed, testName);
                        String text = testResults.stream()
                                                 .map(toAPTString)
                                                 .collect(Collectors.joining(", "));
                        // Summarise the failures across all tests
                        final String summary = getFailuresSummary(testResults);
                        if (!summary.isEmpty()) {
                            // Identify RNGs with no systematic failures
                            highlight = false;
                            if (showFailedTests) {
                                // Add summary in brackets
                                text += " (" + summary + ")";
                            }
                        }
                        writeAPTColumn(sb, text, false);
                    }

                    output.write('|');
                    writeAPTColumn(output, randomSource.toString(), highlight);
                    output.write(sb.toString());
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
        for (final TestResult result : results) {
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
        if (results.isEmpty()) {
            // Default to no bit-reversed results
            list.add(Boolean.FALSE);
        } else {
            final boolean first = results.get(0).isBitReversed();
            list.add(first);
            for (final TestResult result : results) {
                if (first != result.isBitReversed()) {
                    list.add(!first);
                    break;
                }
            }
        }
        Collections.sort(list);
        return list;
    }

    /**
     * Gets the test names present in the results. These are returned in encountered order.
     *
     * @param results Results.
     * @return the test names
     */
    private static List<String> getTestNames(List<TestResult> results) {
        // Enforce encountered order with a linked hash set.
        final Set<String> set = new LinkedHashSet<>();
        for (final TestResult result : results) {
            set.add(result.getTestApplicationName());
        }
        return new ArrayList<>(set);
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
        return parent == null ? "" : parent;
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
        final StringBuilder sb = new StringBuilder(100).append("|| RNG identifier ||");
        if (showBitReversedColumn) {
            sb.append(" Bit-reversed ||");
        }
        for (final String name : testNames) {
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
            if (sb.charAt(i) == PIPE) {
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
     * Write the column text to the output.
     *
     * @param output Output.
     * @param text Text.
     * @param highlight If {@code true} highlight the text in bold.
     * @throws IOException Signals that an I/O exception has occurred.
     */
    private static void writeAPTColumn(Appendable output,
                                       String text,
                                       boolean highlight) throws IOException {
        output.append(' ');
        if (highlight) {
            output.append("<<");
        }
        output.append(text);
        if (highlight) {
            output.append(">>");
        }
        output.append(" |");
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
        for (final TestResult result : results) {
            if (result.getRandomSource() == randomSource &&
                result.bitReversed == bitReversed &&
                result.getTestApplicationName().equals(testName)) {
                list.add(result);
            }
        }
        return list;
    }

    /**
     * Gets the systematic failures (tests that fail in every test result).
     *
     * @param results Results.
     * @return the systematic failures
     */
    private static List<String> getSystematicFailures(List<TestResult> results) {
        final HashMap<String, Integer> map = new HashMap<>();
        for (final TestResult result : results) {
            // Ignore partial results
            if (!result.isComplete()) {
                continue;
            }
            // Some named tests can fail more than once on different statistics.
            // For example TestU01 BigCrush LongestHeadRun can output in the summary:
            // 86  LongestHeadRun, r = 0            eps
            // 86  LongestHeadRun, r = 0          1 - eps1
            // This will be counted as 2 failed tests. For the purpose of systematic
            // failures the name of the test is the same and should be counted once.
            final HashSet<String> unique = new HashSet<>(result.getFailedTests());
            for (final String test : unique) {
                map.merge(test, 1, (i, j) -> i + j);
            }
        }
        final int completeCount = (int) results.stream().filter(TestResult::isComplete).count();
        final List<String> list = map.entrySet().stream()
                                                .filter(e -> e.getValue() == completeCount)
                                                .map(Entry::getKey)
                                                .collect(Collectors.toCollection(
                                                    (Supplier<List<String>>) ArrayList::new));
        // Special case for PractRand. Add the maximum RNG output length before failure.
        // This is because some PractRand tests may not be counted as systematic failures
        // as they have not been run to the same output length due to earlier failure of
        // another test.
        final int max = getMaxLengthExponent(results);
        if (max != 0) {
            list.add(bytesToString(max));
        }
        return list;
    }

    /**
     * Gets the maximum length exponent from the PractRand results if <strong>all</strong> failed.
     * Otherwise return zero (i.e. some passed the full length of the test).
     *
     * <p>This method excludes those results that are not complete. It assumes all complete
     * tests are for the same length of RNG output. Thus if all failed then the max exponent
     * is the systematic failure length.</p>
     *
     * @param results Results.
     * @return the maximum length exponent (or zero)
     */
    private static int getMaxLengthExponent(List<TestResult> results) {
        if (results.isEmpty()) {
            return 0;
        }
        // [0] = count of zeros
        // [1] = max non-zero
        final int[] data = new int[2];
        results.stream()
               .filter(TestResult::isComplete)
               .filter(r -> r instanceof PractRandTestResult)
               .mapToInt(r -> ((PractRandTestResult) r).getLengthExponent())
               .forEach(i -> {
                   if (i == 0) {
                       // Count results that passed
                       data[0]++;
                   } else {
                       // Find the max of the failures
                       data[1] = Math.max(i, data[1]);
                   }
               });
        // If all failed (i.e. no zeros) then return the max, otherwise zero.
        return data[0] == 0 ? data[1] : 0;
    }

    /**
     * Gets a summary of the failures across all results. The text is empty if there are no
     * failures to report.
     *
     * <p>For Dieharder and TestU01 this is the number of systematic failures (tests that fail
     * in every test result). For PractRand it is the maximum byte output size that was reached
     * before failure.
     *
     * <p>It is assumed all the results are for the same test suite.</p>
     *
     * @param results Results.
     * @return the failures summary
     */
    private static String getFailuresSummary(List<TestResult> results) {
        if (results.isEmpty()) {
            return "";
        }
        if (results.get(0).getTestFormat() == TestFormat.PRACTRAND) {
            final int max = getMaxLengthExponent(results);
            return max == 0 ? "" : bytesToString(max);
        }
        final int count = getSystematicFailures(results).size();
        return count == 0 ? "" : Integer.toString(count);
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

        final List<List<String>> columns = createTXTColumns(testNames, showBitReversedColumn);

        // Add all data
        // This will collate results for each combination of 'RandomSource + bitReversed'
        for (final RandomSource randomSource : randomSources) {
            for (final boolean reversed : bitReversed) {
                int i = 0;
                columns.get(i++).add(randomSource.toString());
                if (showBitReversedColumn) {
                    columns.get(i++).add(Boolean.toString(reversed));
                }
                for (final String testName : testNames) {
                    final List<TestResult> testResults = getTestResults(results, randomSource,
                            reversed, testName);
                    columns.get(i++).add(testResults.stream()
                                                    .map(TestResult::getFailureSummaryString)
                                                    .collect(Collectors.joining(",")));
                    columns.get(i++).add(getFailuresSummary(testResults));
                }
            }
        }

        writeColumns(out, columns);
    }

    /**
     * Creates the columns for the text output.
     *
     * @param testNames the test names
     * @param showBitReversedColumn Set to true to show the bit reversed column
     * @return the list of columns
     */
    private static List<List<String>> createTXTColumns(final List<String> testNames,
        final boolean showBitReversedColumn) {
        final ArrayList<List<String>> columns = new ArrayList<>();
        columns.add(createColumn(COLUMN_RNG));
        if (showBitReversedColumn) {
            columns.add(createColumn(BIT_REVERSED));
        }
        for (final String testName : testNames) {
            columns.add(createColumn(testName));
            columns.add(createColumn("∩"));
        }
        return columns;
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
     * Creates the text format from column widths.
     *
     * @param columns Columns.
     * @return the text format string
     */
    private static String createTextFormatFromColumnWidths(final List<List<String>> columns) {
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
        return sb.toString();
    }

    /**
     * Gets the column width using the maximum length of the column items.
     *
     * @param column Column.
     * @return the column width
     */
    private static int getColumnWidth(List<String> column) {
        int width = 0;
        for (final String text : column) {
            width = Math.max(width, text.length());
        }
        return width;
    }

    /**
     * Write the columns as fixed width text to the output.
     *
     * @param out Output stream.
     * @param columns Columns
     * @throws IOException Signals that an I/O exception has occurred.
     */
    private static void writeColumns(OutputStream out,
                                     List<List<String>> columns) throws IOException {
        // Create format using the column widths
        final String format = createTextFormatFromColumnWidths(columns);

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
     * Write the systematic failures as a text table.
     *
     * @param out Output stream.
     * @param results Results.
     * @throws IOException Signals that an I/O exception has occurred.
     */
    private static void writeFailures(OutputStream out,
                                      List<TestResult> results) throws IOException {
        // Identify all:
        // RandomSources, bit-reversed, test names,
        final List<RandomSource> randomSources = getRandomSources(results);
        final List<Boolean> bitReversed = getBitReversed(results);
        final List<String> testNames = getTestNames(results);

        // Create columns for RandomSource, bit-reversed, each test name.
        // Make bit-reversed column optional if no generators are bit reversed.
        final boolean showBitReversedColumn = bitReversed.contains(Boolean.TRUE);

        final List<List<String>> columns = createFailuresColumns(testNames, showBitReversedColumn);

        final AlphaNumericComparator cmp = new AlphaNumericComparator();

        // Add all data for each combination of 'RandomSource + bitReversed'
        for (final RandomSource randomSource : randomSources) {
            for (final boolean reversed : bitReversed) {
                for (final String testName : testNames) {
                    final List<TestResult> testResults = getTestResults(results, randomSource,
                            reversed, testName);
                    final List<String> failures = getSystematicFailures(testResults);
                    if (failures.isEmpty()) {
                        continue;
                    }
                    Collections.sort(failures, cmp);
                    for (final String failed : failures) {
                        int i = 0;
                        columns.get(i++).add(randomSource.toString());
                        if (showBitReversedColumn) {
                            columns.get(i++).add(Boolean.toString(reversed));
                        }
                        columns.get(i++).add(testName);
                        columns.get(i).add(failed);
                    }
                }
            }
        }

        writeColumns(out, columns);
    }

    /**
     * Creates the columns for the failures output.
     *
     * @param testNames the test names
     * @param showBitReversedColumn Set to true to show the bit reversed column
     * @return the list of columns
     */
    private static List<List<String>> createFailuresColumns(final List<String> testNames,
        final boolean showBitReversedColumn) {
        final ArrayList<List<String>> columns = new ArrayList<>();
        columns.add(createColumn(COLUMN_RNG));
        if (showBitReversedColumn) {
            columns.add(createColumn(BIT_REVERSED));
        }
        columns.add(createColumn("Test Suite"));
        columns.add(createColumn("Test"));
        return columns;
    }

    /**
     * Convert bytes to a human readable string. The byte size is expressed in powers of 2.
     * The output units use the ISO binary prefix for increments of 2<sup>10</sup> or 1024.
     *
     * <p>This is a utility function used for reporting PractRand output sizes.
     * Example output:
     *
     * <pre>
     *            exponent       Binary
     *                   0          0 B
     *                  10        1 KiB
     *                  13        8 KiB
     *                  20        1 MiB
     *                  27      128 MiB
     *                  30        1 GiB
     *                  40        1 TiB
     *                  50        1 PiB
     *                  60        1 EiB
     * </pre>
     *
     * @param exponent Exponent of the byte size (i.e. 2^exponent).
     * @return the string
     */
    static String bytesToString(int exponent) {
        final int unit = exponent / 10;
        final int size = 1 << (exponent - 10 * unit);
        return size + BINARY_UNITS[unit];
    }
}
