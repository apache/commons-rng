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
import org.apache.commons.rng.core.source64.RandomLongSource;
import org.apache.commons.rng.simple.RandomSource;

import picocli.CommandLine.Command;
import picocli.CommandLine.Mixin;
import picocli.CommandLine.Option;
import picocli.CommandLine.Parameters;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.IOException;
import java.nio.ByteOrder;
import java.nio.file.Files;
import java.nio.file.StandardOpenOption;
import java.text.SimpleDateFormat;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.Formatter;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.ReentrantLock;

/**
 * Specification for the "stress" command.
 *
 * <p>This command loads a list of random generators and tests each generator by
 * piping the values returned by its {@link UniformRandomProvider#nextInt()}
 * method to a program that reads {@code int} values from its standard input and
 * writes an analysis report to standard output.</p>
 */
@Command(name = "stress",
         description = {"Run repeat trials of random data generators using a provided test application.",
                        "Data is transferred to the application sub-process via standard input."})
class StressTestCommand implements Callable<Void> {
    /** 1000. Any value below this can be exactly represented to 3 significant figures. */
    private static final int ONE_THOUSAND = 1000;

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
    private File fileOutputPrefix = new File("test_");

    /** The stop file. */
    @Option(names = {"--stop-file"},
            description = {"Stop file (default: <Results file prefix>.stop).",
                           "When created it will prevent new tasks from starting " +
                           "but running tasks will complete."})
    private File stopFile;

    /** The output mode for existing files. */
    @Option(names = {"-o", "--output-mode"},
            description = {"Output mode for existing files (default: ${DEFAULT-VALUE}).",
                           "Valid values: ${COMPLETION-CANDIDATES}."})
    private StressTestCommand.OutputMode outputMode = OutputMode.ERROR;

    /** The list of random generators. */
    @Option(names = {"-l", "--list"},
            description = {"List of random generators.",
                           "The default list is all known generators."},
            paramLabel = "<genList>")
    private File generatorsListFile;

    /** The number of trials to put in the template list of random generators. */
    @Option(names = {"-t", "--trials"},
            description = {"The number of trials for each random generator.",
                           "Used only for the default list (default: ${DEFAULT-VALUE})."})
    private int trials = 1;

    /** The trial offset. */
    @Option(names = {"--trial-offset"},
            description = {"Offset to add to the trial number for output files (default: ${DEFAULT-VALUE}).",
                           "Use for parallel tests with the same output prefix."})
    private int trialOffset;

    /** The number of available processors. */
    @Option(names = {"-p", "--processors"},
            description = {"Number of available processors (default: ${DEFAULT-VALUE}).",
                           "Number of concurrent tasks = ceil(processors / threadsPerTask)",
                           "threadsPerTask = applicationThreads + (ignoreJavaThread ? 0 : 1)"})
    private int processors = Math.max(1, Runtime.getRuntime().availableProcessors());

    /** The number of threads to use for each test task. */
    @Option(names = {"--ignore-java-thread"},
            description = {"Ignore the java RNG thread when computing concurrent tasks."})
    private boolean ignoreJavaThread;

    /** The number of threads to use for each testing application. */
    @Option(names = {"--threads"},
            description = {"Number of threads to use for each application (default: ${DEFAULT-VALUE}).",
                           "Total threads per task includes an optional java thread."})
    private int applicationThreads = 1;

    /** The size of the byte buffer for the binary data. */
    @Option(names = {"--buffer-size"},
            description = {"Byte-buffer size for the transferred data (default: ${DEFAULT-VALUE})."})
    private int bufferSize = 8192;

    /** The output byte order of the binary data. */
    @Option(names = {"-b", "--byte-order"},
            description = {"Byte-order of the transferred data (default: ${DEFAULT-VALUE}).",
                           "Valid values: BIG_ENDIAN, LITTLE_ENDIAN."})
    private ByteOrder byteOrder = ByteOrder.nativeOrder();

    /** Flag to indicate the output should be bit-reversed. */
    @Option(names = {"-r", "--reverse-bits"},
            description = {"Reverse the bits in the data (default: ${DEFAULT-VALUE}).",
                           "Note: Generators may fail tests for a reverse sequence " +
                           "when passing using the standard sequence."})
    private boolean reverseBits;

    /** Flag to use the upper 32-bits from the 64-bit long output. */
    @Option(names = {"--high-bits"},
            description = {"Use the upper 32-bits from the 64-bit long output.",
                           "Takes precedent over --low-bits."})
    private boolean longHighBits;

    /** Flag to use the lower 32-bits from the 64-bit long output. */
    @Option(names = {"--low-bits"},
            description = {"Use the lower 32-bits from the 64-bit long output."})
    private boolean longLowBits;

    /** Flag to use 64-bit long output. */
    @Option(names = {"--raw64"},
            description = {"Use 64-bit output (default is 32-bit).",
                           "This requires a 64-bit testing application and native 64-bit generators.",
                           "In 32-bit mode the output uses the upper then lower bits of 64-bit " +
                           "generators sequentially, each appropriately byte reversed for the platform."})
    private boolean raw64;

    /** The random seed as a byte[]. */
    @Option(names = {"-x", "--hex-seed"},
            description = {"The hex-encoded random seed.",
                           "Seed conversion for multi-byte primitives use little-endian format.",
                           "Use to repeat tests. Not recommended for batch testing."})
    private String byteSeed;

    /**
     * Flag to indicate the output should be combined with a hashcode from a new object.
     * This is a method previously used in the
     * {@link org.apache.commons.rng.simple.internal.SeedFactory SeedFactory}.
     *
     * @see System#identityHashCode(Object)
     */
    @Option(names = {"--hashcode"},
            description = {"Combine the bits with a hashcode (default: ${DEFAULT-VALUE}).",
                           "System.identityHashCode(new Object()) ^ rng.nextInt()."})
    private boolean xorHashCode;

    /**
     * Flag to indicate the output should be combined with output from ThreadLocalRandom.
     */
    @Option(names = {"--local-random"},
            description = {"Combine the bits with ThreadLocalRandom (default: ${DEFAULT-VALUE}).",
                           "ThreadLocalRandom.current().nextInt() ^ rng.nextInt()."})
    private boolean xorThreadLocalRandom;

    /**
     * Optional second generator to be combined with the primary generator.
     */
    @Option(names = {"--xor-rng"},
            description = {"Combine the bits with a second generator.",
                           "xorRng.nextInt() ^ rng.nextInt().",
                           "Valid values: Any known RandomSource enum value."})
    private RandomSource xorRandomSource;

    /** The flag to indicate a dry run. */
    @Option(names = {"--dry-run"},
            description = "Perform a dry run where the generators and output files are created " +
                          "but the stress test is not executed.")
    private boolean dryRun;

    /** The locl to hold when checking the stop file. */
    private ReentrantLock stopFileLock = new ReentrantLock(false);
    /** The stop file exists flag. This should be read/updated when holding the lock. */
    private boolean stopFileExists;
    /**
     * The timestamp when the stop file was last checked.
     * This should be read/updated when holding the lock.
     */
    private long stopFileTimestamp;

    /**
     * The output mode for existing files.
     */
    enum OutputMode {
        /** Error if the files exists. */
        ERROR,
        /** Skip existing files. */
        SKIP,
        /** Append to existing files. */
        APPEND,
        /** Overwrite existing files. */
        OVERWRITE
    }

    /**
     * Validates the run command arguments, creates the list of generators and runs the
     * stress test tasks.
     */
    @Override
    public Void call() {
        LogUtils.setLogLevel(reusableOptions.logLevel);
        ProcessUtils.checkExecutable(executable);
        ProcessUtils.checkOutputDirectory(fileOutputPrefix);
        checkStopFileDoesNotExist();
        final Iterable<StressTestData> stressTestData = createStressTestData();
        printStressTestData(stressTestData);
        runStressTest(stressTestData);
        return null;
    }

    /**
     * Initialise the stop file to a default unless specified by the user, then check it
     * does not currently exist.
     *
     * @throws ApplicationException If the stop file exists
     */
    private void checkStopFileDoesNotExist() {
        if (stopFile == null) {
            stopFile = new File(fileOutputPrefix + ".stop");
        }
        if (stopFile.exists()) {
            throw new ApplicationException("Stop file exists: " + stopFile);
        }
    }

    /**
     * Check if the stop file exists.
     *
     * <p>This method is thread-safe. It will log a message if the file exists one time only.
     *
     * @return true if the stop file exists
     */
    private boolean isStopFileExists() {
        stopFileLock.lock();
        try {
            if (!stopFileExists) {
                // This should hit the filesystem each time it is called.
                // To prevent this happening a lot when all the first set of tasks run use
                // a timestamp to limit the check to 1 time each interval.
                final long timestamp = System.currentTimeMillis();
                if (timestamp > stopFileTimestamp) {
                    checkStopFile(timestamp);
                }
            }
            return stopFileExists;
        } finally {
            stopFileLock.unlock();
        }
    }

    /**
     * Check if the stop file exists. Update the timestamp for the next check. If the stop file
     * does exists then log a message.
     *
     * @param timestamp Timestamp of the last check.
     */
    private void checkStopFile(final long timestamp) {
        stopFileTimestamp = timestamp + TimeUnit.SECONDS.toMillis(2);
        stopFileExists = stopFile.exists();
        if (stopFileExists) {
            LogUtils.info("Stop file detected: %s", stopFile);
            LogUtils.info("No further tasks will start");
        }
    }

    /**
     * Creates the test data.
     *
     * <p>If the input file is null then a default list is created.
     *
     * @return the stress test data
     * @throws ApplicationException if an error occurred during the file read.
     */
    private Iterable<StressTestData> createStressTestData() {
        if (generatorsListFile == null) {
            return new StressTestDataList("", trials);
        }
        // Read data into a list
        try (BufferedReader reader = Files.newBufferedReader(generatorsListFile.toPath())) {
            return ListCommand.readStressTestData(reader);
        } catch (final IOException ex) {
            throw new ApplicationException("Failed to read generators list: " + generatorsListFile, ex);
        }
    }

    /**
     * Prints the stress test data if the verbosity allows. This is used to debug the list
     * of generators to be tested.
     *
     * @param stressTestData List of generators to be tested.
     */
    private static void printStressTestData(Iterable<StressTestData> stressTestData) {
        if (!LogUtils.isLoggable(LogUtils.LogLevel.DEBUG)) {
            return;
        }
        try {
            final StringBuilder sb = new StringBuilder("Testing generators").append(System.lineSeparator());
            ListCommand.writeStressTestData(sb, stressTestData);
            LogUtils.debug(sb.toString());
        } catch (final IOException ex) {
            throw new ApplicationException("Failed to show list of generators", ex);
        }
    }

    /**
     * Creates the tasks and starts the processes.
     *
     * @param stressTestData List of generators to be tested.
     */
    private void runStressTest(Iterable<StressTestData> stressTestData) {
        final List<String> command = ProcessUtils.buildSubProcessCommand(executable, executableArguments);

        LogUtils.info("Set-up stress test ...");

        // Check existing output files before starting the tasks.
        final String basePath = fileOutputPrefix.getAbsolutePath();
        checkExistingOutputFiles(basePath, stressTestData);

        final int parallelTasks = getParallelTasks();

        final ProgressTracker progressTracker = new ProgressTracker(parallelTasks);
        final List<Runnable> tasks = createTasks(command, basePath, stressTestData, progressTracker);

        // Run tasks with parallel execution.
        final ExecutorService service = Executors.newFixedThreadPool(parallelTasks);

        LogUtils.info("Running stress test ...");
        LogUtils.info("Shutdown by creating stop file: %s",  stopFile);
        progressTracker.setTotal(tasks.size());
        final List<Future<?>> taskList = submitTasks(service, tasks);

        // Wait for completion (ignoring return value).
        try {
            for (final Future<?> f : taskList) {
                try {
                    f.get();
                } catch (final ExecutionException ex) {
                    // Log the error. Do not re-throw as other tasks may be processing that
                    // can still complete successfully.
                    LogUtils.error(ex.getCause(), ex.getMessage());
                }
            }
        } catch (final InterruptedException ex) {
            // Restore interrupted state...
            Thread.currentThread().interrupt();
            throw new ApplicationException("Unexpected interruption: " + ex.getMessage(), ex);
        } finally {
            // Terminate all threads.
            service.shutdown();
        }

        LogUtils.info("Finished stress test");
    }

    /**
     * Check for existing output files.
     *
     * @param basePath The base path to the output results files.
     * @param stressTestData List of generators to be tested.
     * @throws ApplicationException If an output file exists and the output mode is error
     */
    private void checkExistingOutputFiles(String basePath,
                                          Iterable<StressTestData> stressTestData) {
        if (outputMode == StressTestCommand.OutputMode.ERROR) {
            for (final StressTestData testData : stressTestData) {
                for (int trial = 1; trial <= testData.getTrials(); trial++) {
                    // Create the output file
                    final File output = createOutputFile(basePath, testData, trial);
                    if (output.exists()) {
                        throw new ApplicationException(createExistingFileMessage(output));
                    }
                }
            }
        }
    }

    /**
     * Creates the named output file.
     *
     * <p>Note: The trial will be combined with the trial offset to create the file name.
     *
     * @param basePath The base path to the output results files.
     * @param testData The test data.
     * @param trial The trial.
     * @return the file
     */
    private File createOutputFile(String basePath,
                                  StressTestData testData,
                                  int trial) {
        return new File(String.format("%s%s_%d", basePath, testData.getId(), trial + trialOffset));
    }

    /**
     * Creates the existing file message.
     *
     * @param output The output file.
     * @return the message
     */
    private static String createExistingFileMessage(File output) {
        return "Existing output file: " + output;
    }

    /**
     * Gets the number of parallel tasks. This uses the number of available processors and
     * the number of threads to use per task.
     *
     * <pre>
     * threadsPerTask = applicationThreads + (ignoreJavaThread ? 0 : 1)
     * parallelTasks = ceil(processors / threadsPerTask)
     * </pre>
     *
     * @return the parallel tasks
     */
    private int getParallelTasks() {
        // Avoid zeros in the fraction numberator and denominator
        final int availableProcessors = Math.max(1, processors);
        final int threadsPerTask = Math.max(1, applicationThreads + (ignoreJavaThread ? 0 : 1));
        final int parallelTasks = (int) Math.ceil((double) availableProcessors / threadsPerTask);
        LogUtils.debug("Parallel tasks = %d (%d / %d)",
            parallelTasks, availableProcessors, threadsPerTask);
        return parallelTasks;
    }

    /**
     * Create the tasks for the test data. The output file for the sub-process will be
     * constructed using the base path, the test identifier and the trial number.
     *
     * @param command The command for the test application.
     * @param basePath The base path to the output results files.
     * @param stressTestData List of generators to be tested.
     * @param progressTracker Progress tracker.
     * @return the list of tasks
     */
    private List<Runnable> createTasks(List<String> command,
                                       String basePath,
                                       Iterable<StressTestData> stressTestData,
                                       ProgressTracker progressTracker) {
        final List<Runnable> tasks = new ArrayList<>();
        for (final StressTestData testData : stressTestData) {
            for (int trial = 1; trial <= testData.getTrials(); trial++) {
                // Create the output file
                final File output = createOutputFile(basePath, testData, trial);
                if (output.exists()) {
                    // In case the file was created since the last check
                    if (outputMode == StressTestCommand.OutputMode.ERROR) {
                        throw new ApplicationException(createExistingFileMessage(output));
                    }
                    // Log the decision
                    LogUtils.info("%s existing output file: %s", outputMode, output);
                    if (outputMode == StressTestCommand.OutputMode.SKIP) {
                        continue;
                    }
                }
                // Create the generator. Explicitly create a seed so it can be recorded.
                final byte[] seed = createSeed(testData.getRandomSource());
                UniformRandomProvider rng = testData.createRNG(seed);

                // Upper or lower bits from 64-bit generators must be created first.
                // This will throw if not a 64-bit generator.
                if (longHighBits) {
                    rng = RNGUtils.createLongUpperBitsIntProvider(rng);
                } else if (longLowBits) {
                    rng = RNGUtils.createLongLowerBitsIntProvider(rng);
                }

                // Combination generators. Mainly used for testing.
                // These operations maintain the native output type (int/long).
                if (xorHashCode) {
                    rng = RNGUtils.createHashCodeProvider(rng);
                }
                if (xorThreadLocalRandom) {
                    rng = RNGUtils.createThreadLocalRandomProvider(rng);
                }
                if (xorRandomSource != null) {
                    rng = RNGUtils.createXorProvider(
                            xorRandomSource.create(),
                            rng);
                }
                if (reverseBits) {
                    rng = RNGUtils.createReverseBitsProvider(rng);
                }

                // -------
                // Note: Manipulation of the byte order for the platform is done during output.
                // -------

                // Run the test
                final Runnable r = new StressTestTask(testData.getRandomSource(), rng, seed,
                                                      output, command, this, progressTracker);
                tasks.add(r);
            }
        }
        return tasks;
    }

    /**
     * Creates the seed. This will call {@link RandomSource#createSeed()} unless a hex seed has
     * been explicitly specified on the command line.
     *
     * @param randomSource Random source.
     * @return the seed
     */
    private byte[] createSeed(RandomSource randomSource) {
        if (byteSeed != null) {
            try {
                return Hex.decodeHex(byteSeed);
            } catch (IllegalArgumentException ex) {
                throw new ApplicationException("Invalid hex seed: " + ex.getMessage(), ex);
            }
        }
        return randomSource.createSeed();
    }

    /**
     * Submit the tasks to the executor service.
     *
     * @param service The executor service.
     * @param tasks The list of tasks.
     * @return the list of submitted tasks
     */
    private static List<Future<?>> submitTasks(ExecutorService service,
                                               List<Runnable> tasks) {
        final List<Future<?>> taskList = new ArrayList<>(tasks.size());
        tasks.forEach(r -> taskList.add(service.submit(r)));
        return taskList;
    }

    /**
     * Class for reporting total progress of tasks to the console.
     *
     * <p>This stores the start and end time of tasks to allow it to estimate the time remaining
     * for all the tests.
     */
    static class ProgressTracker {
        /** The interval at which to report progress (in milliseconds). */
        private static final long PROGRESS_INTERVAL = 500;

        /** The total. */
        private int total;
        /** The level of parallelisation. */
        private final int parallelTasks;
        /** The task id. */
        private int taskId;
        /** The start time of tasks (in milliseconds from the epoch). */
        private long[] startTimes;
        /** The durations of all completed tasks (in milliseconds). This is sorted. */
        private long[] sortedDurations;
        /** The number of completed tasks. */
        private int completed;
        /** The timestamp of the next progress report. */
        private long nextReportTimestamp;

        /**
         * Create a new instance. The total number of tasks must be initialized before use.
         *
         * @param parallelTasks The number of parallel tasks.
         */
        ProgressTracker(int parallelTasks) {
            this.parallelTasks = parallelTasks;
        }

        /**
         * Sets the total number of tasks to track.
         *
         * @param total The total tasks.
         */
        synchronized void setTotal(int total) {
            this.total = total;
            startTimes = new long[total];
            sortedDurations = new long[total];
        }

        /**
         * Submit a task for progress tracking. The task start time is recorded and the
         * task is allocated an identifier.
         *
         * @return the task Id
         */
        int submitTask() {
            int id;
            synchronized (this) {
                final long current = System.currentTimeMillis();
                id = taskId++;
                startTimes[id] = current;
                reportProgress(current);
            }
            return id;
        }

        /**
         * Signal that a task has completed. The task duration will be returned.
         *
         * @param id Task Id.
         * @return the task time in milliseconds
         */
        long endTask(int id) {
            long duration;
            synchronized (this) {
                final long current = System.currentTimeMillis();
                duration = current - startTimes[id];
                sortedDurations[completed++] = duration;
                reportProgress(current);
            }
            return duration;
        }

        /**
         * Report the progress. This uses the current state and should be done within a
         * synchronized block.
         *
         * @param current Current time (in milliseconds).
         */
        private void reportProgress(long current) {
            // Determine the current state of tasks
            final int pending = total - taskId;
            final int running = taskId - completed;

            // Report progress in the following conditions:
            // - All tasks have completed (i.e. the end); or
            // - The current timestamp is above the next reporting time and either:
            // -- The number of running tasks is equal to the level of parallel tasks
            //    (i.e. the system is running at capacity, so not the end of a task but the start
            //    of a new one)
            // -- There are no pending tasks (i.e. the final submission or the end of a final task)
            if (completed >= total ||
                (current >= nextReportTimestamp && (running == parallelTasks || pending == 0))) {
                // Report
                nextReportTimestamp = current + PROGRESS_INTERVAL;
                final StringBuilder sb = createStringBuilderWithTimestamp(current, pending, running, completed);
                try (Formatter formatter = new Formatter(sb)) {
                    formatter.format(" (%.2f%%)", 100.0 * completed / total);
                    appendRemaining(sb, current, pending, running);
                    LogUtils.info(sb.toString());
                }
            }
        }

        /**
         * Creates the string builder for the progress message with a timestamp prefix.
         *
         * <pre>
         * [HH:mm:ss] Pending [pending]. Running [running]. Completed [completed]
         * </pre>
         *
         * @param current Current time (in milliseconds)
         * @param pending Pending tasks.
         * @param running Running tasks.
         * @param completed Completed tasks.
         * @return the string builder
         */
        private static StringBuilder createStringBuilderWithTimestamp(long current,
            int pending, int running, int completed) {
            final StringBuilder sb = new StringBuilder(80);
            // Use local time to adjust for timezone
            final LocalDateTime time = LocalDateTime.ofInstant(
                Instant.ofEpochMilli(current), ZoneId.systemDefault());
            sb.append('[');
            append00(sb, time.getHour()).append(':');
            append00(sb, time.getMinute()).append(':');
            append00(sb, time.getSecond());
            return sb.append("] Pending ").append(pending)
                     .append(". Running ").append(running)
                     .append(". Completed ").append(completed);
        }

        /**
         * Compute an estimate of the time remaining and append to the progress. Updates
         * the estimated time of arrival (ETA).
         *
         * @param sb String Builder.
         * @param current Current time (in milliseconds)
         * @param pending Pending tasks.
         * @param running Running tasks.
         * @return the string builder
         */
        private StringBuilder appendRemaining(StringBuilder sb, long current, int pending, int running) {
            final long millis = getRemainingTime(current, pending, running);
            if (millis == 0) {
                // Unknown.
                return sb;
            }

            // HH:mm:ss format
            sb.append(". Remaining = ");
            hms(sb, millis);
            return sb;
        }

        /**
         * Gets the remaining time (in milliseconds).
         *
         * @param current Current time (in milliseconds)
         * @param pending Pending tasks.
         * @param running Running tasks.
         * @return the remaining time
         */
        private long getRemainingTime(long current, int pending, int running) {
            final long taskTime = getEstimatedTaskTime();
            if (taskTime == 0) {
                // No estimate possible
                return 0;
            }

            // The start times are sorted. This method assumes the most recent start times
            // are still running tasks.
            // If this is wrong (more recently submitted tasks finished early) the result
            // is the estimate is too high. This could be corrected by storing the tasks
            // that have finished and finding the times of only running tasks.

            // The remaining time is:
            //   The time for all running tasks to finish
            // + The time for pending tasks to run

            // The id of the most recently submitted task.
            // Guard with a minimum index of zero to get a valid index.
            final int id = Math.max(0, taskId - 1);

            // If there is a running task assume the youngest task is still running
            // and estimate the time left.
            long millis = (running == 0) ? 0 : getTimeRemaining(taskTime, current, startTimes[id]);

            // If additional tasks must also be submitted then the time must include
            // the estimated time for running tasks to finish before new submissions
            // in the batch can be made.
            //                   now
            // s1 --------------->|
            //      s2 -----------|-------->
            //          s3 -------|------------>
            //                    s4 -------------->
            //

            // Assume parallel batch execution.
            // E.g. 3 additional tasks with parallelisation 4 is 0 batches
            final int batches = pending / parallelTasks;
            millis += batches * taskTime;

            // Compute the expected end time of the final batch based on it starting when
            // a currently running task ends.
            // E.g. 3 remaining tasks requires the end time of the 3rd oldest running task.
            final int remainder = pending % parallelTasks;
            if (remainder != 0) {
                // Guard with a minimum index of zero to get a valid index.
                final int nthOldest = Math.max(0, id - parallelTasks + remainder);
                millis += getTimeRemaining(taskTime, current, startTimes[nthOldest]);
            }

            return millis;
        }

        /**
         * Gets the estimated task time.
         *
         * @return the estimated task time
         */
        private long getEstimatedTaskTime() {
            Arrays.sort(sortedDurations, 0, completed);

            // Return median of small lists. If no tasks have finished this returns zero.
            // as the durations is zero initialized.
            if (completed < 4) {
                return sortedDurations[completed / 2];
            }

            // Dieharder and BigCrush run in approximately constant time.
            // Speed varies with the speed of the RNG by about 2-fold, and
            // for Dieharder it may repeat suspicious tests.
            // PractRand may fail very fast for bad generators which skews
            // using the mean or even the median. So look at the longest
            // running tests.

            // Find long running tests (>50% of the max run-time)
            int upper = completed - 1;
            final long halfMax = sortedDurations[upper] / 2;
            // Binary search for the approximate cut-off
            int lower = 0;
            while (lower + 1 < upper) {
                final int mid = (lower + upper) >>> 1;
                if (sortedDurations[mid] < halfMax) {
                    lower = mid;
                } else {
                    upper = mid;
                }
            }
            // Use the median of all tasks within approximately 50% of the max.
            return sortedDurations[(upper + completed - 1) / 2];
        }

        /**
         * Gets the time remaining for the task.
         *
         * @param taskTime Estimated task time.
         * @param current Current time.
         * @param startTime Start time.
         * @return the time remaining
         */
        private static long getTimeRemaining(long taskTime, long current, long startTime) {
            final long endTime = startTime + taskTime;
            // Ensure the time is positive in the case where the estimate is too low.
            return Math.max(0, endTime - current);
        }

        /**
         * Append the milliseconds using {@code HH::mm:ss} format.
         *
         * @param sb String Builder.
         * @param millis Milliseconds.
         * @return the string builder
         */
        static StringBuilder hms(StringBuilder sb, final long millis) {
            final long hours = TimeUnit.MILLISECONDS.toHours(millis);
            long minutes = TimeUnit.MILLISECONDS.toMinutes(millis);
            long seconds = TimeUnit.MILLISECONDS.toSeconds(millis);
            // Truncate to interval [0,59]
            seconds -= TimeUnit.MINUTES.toSeconds(minutes);
            minutes -= TimeUnit.HOURS.toMinutes(hours);

            append00(sb, hours).append(':');
            append00(sb, minutes).append(':');
            return append00(sb, seconds);
        }

        /**
         * Append the ticks to the string builder in the format {@code %02d}.
         *
         * @param sb String Builder.
         * @param ticks Ticks.
         * @return the string builder
         */
        static StringBuilder append00(StringBuilder sb, long ticks) {
            if (ticks == 0) {
                sb.append("00");
            } else {
                if (ticks < 10) {
                    sb.append('0');
                }
                sb.append(ticks);
            }
            return sb;
        }
    }

    /**
     * Pipes random numbers to the standard input of an analyzer executable.
     */
    private static class StressTestTask implements Runnable {
        /** Comment prefix. */
        private static final String C = "# ";
        /** New line. */
        private static final String N = System.lineSeparator();
        /** The date format. */
        private static final String DATE_FORMAT = "yyyy-MM-dd HH:mm:ss";
        /** The SI units for bytes in increments of 10^3. */
        private static final String[] SI_UNITS = {"B", "kB", "MB", "GB", "TB", "PB", "EB"};
        /** The SI unit base for bytes (10^3). */
        private static final long SI_UNIT_BASE = 1000;

        /** The random source. */
        private final RandomSource randomSource;
        /** RNG to be tested. */
        private final UniformRandomProvider rng;
        /** The seed used to create the RNG. */
        private final byte[] seed;
        /** Output report file of the sub-process. */
        private final File output;
        /** The sub-process command to run. */
        private final List<String> command;
        /** The stress test command. */
        private final StressTestCommand cmd;
        /** The progress tracker. */
        private final ProgressTracker progressTracker;

        /** The count of bytes used by the sub-process. */
        private long bytesUsed;

        /**
         * Creates the task.
         *
         * @param randomSource The random source.
         * @param rng RNG to be tested.
         * @param seed The seed used to create the RNG.
         * @param output Output report file.
         * @param command The sub-process command to run.
         * @param cmd The run command.
         * @param progressTracker The progress tracker.
         */
        StressTestTask(RandomSource randomSource,
                       UniformRandomProvider rng,
                       byte[] seed,
                       File output,
                       List<String> command,
                       StressTestCommand cmd,
                       ProgressTracker progressTracker) {
            this.randomSource = randomSource;
            this.rng = rng;
            this.seed = seed;
            this.output = output;
            this.command = command;
            this.cmd = cmd;
            this.progressTracker = progressTracker;
        }

        /** {@inheritDoc} */
        @Override
        public void run() {
            if (cmd.isStopFileExists()) {
                // Do nothing
                return;
            }

            try {
                printHeader();

                Object exitValue;
                long millis;
                final int taskId = progressTracker.submitTask();
                if (cmd.dryRun) {
                    // Do not do anything. Ignore the runtime.
                    exitValue = "N/A";
                    progressTracker.endTask(taskId);
                    millis = 0;
                } else {
                    // Run the sub-process
                    exitValue = runSubProcess();
                    millis = progressTracker.endTask(taskId);
                }

                printFooter(millis, exitValue);

            } catch (final IOException ex) {
                throw new ApplicationException("Failed to run task: " + ex.getMessage(), ex);
            }
        }

        /**
         * Run the analyzer sub-process command.
         *
         * @return The exit value.
         * @throws IOException Signals that an I/O exception has occurred.
         */
        private Integer runSubProcess() throws IOException {
            // Start test suite.
            final ProcessBuilder builder = new ProcessBuilder(command);
            builder.redirectOutput(ProcessBuilder.Redirect.appendTo(output));
            builder.redirectErrorStream(true);
            final Process testingProcess = builder.start();

            // Use a custom data output to write the RNG.
            try (RngDataOutput sink = RNGUtils.createDataOutput(rng, cmd.raw64,
                testingProcess.getOutputStream(), cmd.bufferSize, cmd.byteOrder)) {
                for (;;) {
                    sink.write(rng);
                    bytesUsed++;
                }
            } catch (final IOException ignored) {
                // Hopefully getting here when the analyzing software terminates.
            }

            bytesUsed *= cmd.bufferSize;

            // Get the exit value.
            // Wait for up to 60 seconds.
            // If an application does not exit after this time then something is wrong.
            // Dieharder and TestU01 BigCrush exit within 1 second.
            // PractRand has been observed to take longer than 1 second. It calls std::exit(0)
            // when failing a test so the length of time may be related to freeing memory.
            return ProcessUtils.getExitValue(testingProcess, TimeUnit.SECONDS.toMillis(60));
        }

        /**
         * Prints the header.
         *
         * @throws IOException if there was a problem opening or writing to the
         * {@code output} file.
         */
        private void printHeader() throws IOException {
            final StringBuilder sb = new StringBuilder(200);
            sb.append(C).append(N)
                .append(C).append("RandomSource: ").append(randomSource.name()).append(N)
                .append(C).append("RNG: ").append(rng.toString()).append(N)
                .append(C).append("Seed: ").append(Hex.encodeHex(seed)).append(N)
                .append(C).append(N)

            // Match the output of 'java -version', e.g.
            // java version "1.8.0_131"
            // Java(TM) SE Runtime Environment (build 1.8.0_131-b11)
            // Java HotSpot(TM) 64-Bit Server VM (build 25.131-b11, mixed mode)
            .append(C).append("Java: ").append(System.getProperty("java.version")).append(N);
            appendNameAndVersion(sb, "Runtime", "java.runtime.name", "java.runtime.version");
            appendNameAndVersion(sb, "JVM", "java.vm.name", "java.vm.version", "java.vm.info");

            sb.append(C).append("OS: ").append(System.getProperty("os.name"))
                .append(' ').append(System.getProperty("os.version"))
                .append(' ').append(System.getProperty("os.arch")).append(N)
                .append(C).append("Native byte-order: ").append(ByteOrder.nativeOrder()).append(N)
                .append(C).append("Output byte-order: ").append(cmd.byteOrder).append(N);
            if (rng instanceof RandomLongSource) {
                sb.append(C).append("64-bit output: ").append(cmd.raw64).append(N);
            }
            sb.append(C).append(N)
                .append(C).append("Analyzer: ");
            for (final String s : command) {
                sb.append(s).append(' ');
            }
            sb.append(N)
              .append(C).append(N);

            appendDate(sb, "Start").append(C).append(N);

            write(sb, output, cmd.outputMode == StressTestCommand.OutputMode.APPEND);
        }

        /**
         * Prints the footer.
         *
         * @param millis Duration of the run (in milliseconds).
         * @param exitValue The process exit value.
         * @throws IOException if there was a problem opening or writing to the
         * {@code output} file.
         */
        private void printFooter(long millis,
                                 Object exitValue) throws IOException {
            final StringBuilder sb = new StringBuilder(200);
            sb.append(C).append(N);

            appendDate(sb, "End").append(C).append(N);

            sb.append(C).append("Exit value: ").append(exitValue).append(N)
                .append(C).append("Bytes used: ").append(bytesUsed)
                          .append(" >= 2^").append(log2(bytesUsed))
                          .append(" (").append(bytesToString(bytesUsed)).append(')').append(N)
                .append(C).append(N);

            final double duration = millis * 1e-3 / 60;
            sb.append(C).append("Test duration: ").append(duration).append(" minutes").append(N)
                .append(C).append(N);

            write(sb, output, true);
        }

        /**
         * Write the string builder to the output file.
         *
         * @param sb The string builder.
         * @param output The output file.
         * @param append Set to {@code true} to append to the file.
         * @throws IOException Signals that an I/O exception has occurred.
         */
        private static void write(StringBuilder sb,
                                  File output,
                                  boolean append) throws IOException {
            try (BufferedWriter w = append ?
                    Files.newBufferedWriter(output.toPath(), StandardOpenOption.APPEND) :
                    Files.newBufferedWriter(output.toPath())) {
                w.write(sb.toString());
            }
        }

        /**
         * Append prefix and then name and version from System properties, finished with
         * a new line. The format is:
         *
         * <pre>{@code # <prefix>: <name> (build <version>[, <info>, ...])}</pre>
         *
         * @param sb The string builder.
         * @param prefix The prefix.
         * @param nameKey The name key.
         * @param versionKey The version key.
         * @param infoKeys The additional information keys.
         * @return the StringBuilder.
         */
        private static StringBuilder appendNameAndVersion(StringBuilder sb,
                                                          String prefix,
                                                          String nameKey,
                                                          String versionKey,
                                                          String... infoKeys) {
            appendPrefix(sb, prefix)
                .append(System.getProperty(nameKey, "?"))
                .append(" (build ")
                .append(System.getProperty(versionKey, "?"));
            for (final String key : infoKeys) {
                final String value = System.getProperty(key, "");
                if (!value.isEmpty()) {
                    sb.append(", ").append(value);
                }
            }
            return sb.append(')').append(N);
        }

        /**
         * Append a comment with the current date to the {@link StringBuilder}, finished with
         * a new line. The format is:
         *
         * <pre>{@code # <prefix>: yyyy-MM-dd HH:mm:ss}</pre>
         *
         * @param sb The StringBuilder.
         * @param prefix The prefix used before the formatted date, e.g. "Start".
         * @return the StringBuilder.
         */
        private static StringBuilder appendDate(StringBuilder sb,
                                                String prefix) {
            // Use local date format. It is not thread safe.
            final SimpleDateFormat dateFormat = new SimpleDateFormat(DATE_FORMAT, Locale.US);
            return appendPrefix(sb, prefix).append(dateFormat.format(new Date())).append(N);
        }

        /**
         * Append a comment with the current date to the {@link StringBuilder}.
         *
         * <pre>
         * {@code # <prefix>: yyyy-MM-dd HH:mm:ss}
         * </pre>
         *
         * @param sb The StringBuilder.
         * @param prefix The prefix used before the formatted date, e.g. "Start".
         * @return the StringBuilder.
         */
        private static StringBuilder appendPrefix(StringBuilder sb,
                                                  String prefix) {
            return sb.append(C).append(prefix).append(": ");
        }

        /**
         * Convert bytes to a human readable string. Example output:
         *
         * <pre>
         *                              SI
         *                   0:        0 B
         *                  27:       27 B
         *                 999:      999 B
         *                1000:     1.0 kB
         *                1023:     1.0 kB
         *                1024:     1.0 kB
         *                1728:     1.7 kB
         *              110592:   110.6 kB
         *             7077888:     7.1 MB
         *           452984832:   453.0 MB
         *         28991029248:    29.0 GB
         *       1855425871872:     1.9 TB
         * 9223372036854775807:     9.2 EB   (Long.MAX_VALUE)
         * </pre>
         *
         * @param bytes the bytes
         * @return the string
         * @see <a
         * href="https://stackoverflow.com/questions/3758606/how-to-convert-byte-size-into-human-readable-format-in-java">How
         *      to convert byte size into human readable format in java?</a>
         */
        static String bytesToString(long bytes) {
            // When using the smallest unit no decimal point is needed, because it's the exact number.
            if (bytes < ONE_THOUSAND) {
                return bytes + " " + SI_UNITS[0];
            }

            final int exponent = (int) (Math.log(bytes) / Math.log(SI_UNIT_BASE));
            final String unit = SI_UNITS[exponent];
            return String.format(Locale.US, "%.1f %s", bytes / Math.pow(SI_UNIT_BASE, exponent), unit);
        }

        /**
         * Return the log2 of a {@code long} value rounded down to a power of 2.
         *
         * @param x the value
         * @return {@code floor(log2(x))}
         */
        static int log2(long x) {
            return 63 - Long.numberOfLeadingZeros(x);
        }
    }
}
