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

import java.io.BufferedWriter;
import java.io.File;
import java.io.FilterOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.nio.ByteOrder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Formatter;
import java.util.List;
import java.util.concurrent.Callable;

/**
 * Specification for the "output" command.
 *
 * <p>This command creates a named random generator and outputs data in a specified format.</p>
 */
@Command(name = "output",
         description = {"Output data from a named random data generator."})
class OutputCommand implements Callable<Void> {
    /** The new line characters. */
    private static final String NEW_LINE = System.lineSeparator();
    /** Character '['. */
    private static final char LEFT_SQUARE_BRACKET = '[';
    /** Character ']'. */
    private static final char RIGHT_SQUARE_BRACKET = ']';

    /** Lookup table for binary representation of bytes. */
    private static final String[] BIT_REP = {
        "0000", "0001", "0010", "0011",
        "0100", "0101", "0110", "0111",
        "1000", "1001", "1010", "1011",
        "1100", "1101", "1110", "1111",
    };

    /** The standard options. */
    @Mixin
    private StandardOptions reusableOptions;

    /** The random source. */
    @Parameters(index = "0",
                description = "The random source.")
    private RandomSource randomSource;

    /** The executable arguments. */
    @Parameters(index = "1..*",
                description = "The arguments to pass to the constructor.",
                paramLabel = "<argument>")
    private List<String> arguments = new ArrayList<>();

    /** The file output prefix. */
    @Option(names = {"-o", "--out"},
            description = "The output file (default: stdout).")
    private File fileOutput;

    /** The output format. */
    @Option(names = {"-f", "--format"},
            description = {"Output format (default: ${DEFAULT-VALUE}).",
                           "Valid values: ${COMPLETION-CANDIDATES}."})
    private OutputCommand.OutputFormat outputFormat = OutputFormat.DIEHARDER;

    /** The random seed. */
    @Option(names = {"-s", "--seed"},
            description = {"The 64-bit number random seed (default: auto)."})
    private Long seed;

    /** The random seed as a byte[]. */
    @Option(names = {"-x", "--hex-seed"},
            description = {"The hex-encoded random seed.",
                           "Seed conversion for multi-byte primitives use little-endian format.",
                           "Over-rides the --seed parameter."})
    private String byteSeed;

    /** The count of numbers to output. */
    @Option(names = {"-n", "--count"},
            description = {"The count of numbers to output.",
                           "Use negative for an unlimited stream."})
    private long count = 10;

    /** The size of the byte buffer for the binary data. */
    @Option(names = {"--buffer-size"},
            description = {"Byte-buffer size for binary data (default: ${DEFAULT-VALUE}).",
                           "When outputing binary data the count parameter controls the " +
                           "number of buffers written."})
    private int bufferSize = 8192;

    /** The output byte order of the binary data. */
    @Option(names = {"-b", "--byte-order"},
            description = {"Byte-order of the output data (default: ${DEFAULT-VALUE}).",
                           "Uses the Java default of big-endian. This may not match the platform byte-order.",
                           "Valid values: BIG_ENDIAN, LITTLE_ENDIAN."})
    private ByteOrder byteOrder = ByteOrder.BIG_ENDIAN;

    /** The output byte order of the binary data. */
    @Option(names = {"-r", "--reverse-bits"},
            description = {"Reverse the bits in the data (default: ${DEFAULT-VALUE})."})
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
                           "This is ignored if not a native 64-bit generator.",
                           "In 32-bit mode the output uses the upper then lower bits of 64-bit " +
                           "generators sequentially."})
    private boolean raw64;

    /**
     * The output mode for existing files.
     */
    enum OutputFormat {
        /** Binary output. */
        BINARY,
        /** Use the Dieharder text format. */
        DIEHARDER,
        /** Output the bits in a text format. */
        BITS,
    }

    /**
     * Validates the command arguments, creates the generator and outputs numbers.
     */
    @Override
    public Void call() {
        LogUtils.setLogLevel(reusableOptions.logLevel);
        final Object objectSeed = createSeed();
        UniformRandomProvider rng = createRNG(objectSeed);

        // Upper or lower bits from 64-bit generators must be created first.
        // This will throw if not a 64-bit generator.
        if (longHighBits) {
            rng = RNGUtils.createLongUpperBitsIntProvider(rng);
        } else if (longLowBits) {
            rng = RNGUtils.createLongLowerBitsIntProvider(rng);
        }
        if (reverseBits) {
            rng = RNGUtils.createReverseBitsProvider(rng);
        }

        // -------
        // Note: Manipulation of the byte order for the platform is done during output
        // for the binary format. Otherwise do it in Java.
        // -------
        if (outputFormat != OutputFormat.BINARY) {
            rng = toOutputFormat(rng);
        }

        try (OutputStream out = createOutputStream()) {
            switch (outputFormat) {
            case BINARY:
                writeBinaryData(rng, out);
                break;
            case DIEHARDER:
                writeDieharder(rng, out);
                break;
            case BITS:
                writeBitData(rng, out);
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
     * Creates the seed.
     *
     * @return the seed
     */
    private Object createSeed() {
        if (byteSeed != null) {
            try {
                return Hex.decodeHex(byteSeed);
            } catch (IllegalArgumentException ex) {
                throw new ApplicationException("Invalid hex seed: " + ex.getMessage(), ex);
            }
        }
        if (seed != null) {
            return seed;
        }
        // Let the factory constructor create the native seed.
        return null;
    }

    /**
     * Creates the seed.
     *
     * @return the seed
     */
    private String createSeedString() {
        if (byteSeed != null) {
            return byteSeed;
        }
        if (seed != null) {
            return seed.toString();
        }
        return "auto";
    }

    /**
     * Creates the RNG.
     *
     * @param objectSeed Seed.
     * @return the uniform random provider
     * @throws ApplicationException If the RNG cannot be created
     */
    private UniformRandomProvider createRNG(Object objectSeed) {
        if (randomSource == null) {
            throw new ApplicationException("Random source is null");
        }
        final ArrayList<Object> data = new ArrayList<>();
        // Note: The list command outputs arguments as an array bracketed by [ and ]
        // Strip these for convenience.
        stripArrayFormatting(arguments);

        for (final String argument : arguments) {
            data.add(RNGUtils.parseArgument(argument));
        }
        try {
            return randomSource.create(objectSeed, data.toArray());
        } catch (IllegalStateException | IllegalArgumentException ex) {
            throw new ApplicationException("Failed to create RNG: " + randomSource + ". " + ex.getMessage(), ex);
        }
    }

    /**
     * Strip leading bracket from the first argument, trailing bracket from the last
     * argument, and any trailing commas from any argument.
     *
     * <p>This is used to remove the array formatting used by the list command.
     *
     * @param arguments the arguments
     */
    private static void stripArrayFormatting(List<String> arguments) {
        final int size = arguments.size();
        if (size > 1) {
            // These will not be empty as they were created from command-line args.
            final String first = arguments.get(0);
            if (first.charAt(0) == LEFT_SQUARE_BRACKET) {
                arguments.set(0, first.substring(1));
            }
            final String last = arguments.get(size - 1);
            if (last.charAt(last.length() - 1) == RIGHT_SQUARE_BRACKET) {
                arguments.set(size - 1, last.substring(0, last.length() - 1));
            }
        }
        for (int i = 0; i < size; i++) {
            final String argument = arguments.get(i);
            if (argument.endsWith(",")) {
                arguments.set(i, argument.substring(0, argument.length() - 1));
            }
        }
    }

    /**
     * Convert the native RNG to the requested output format. This will convert a 64-bit
     * generator to a 32-bit generator unless the 64-bit mode is active. It then optionally
     * reverses the byte order of the output.
     *
     * @param rng The random generator.
     * @return the uniform random provider
     */
    private UniformRandomProvider toOutputFormat(UniformRandomProvider rng) {
        UniformRandomProvider convertedRng = rng;
        if (rng instanceof RandomLongSource && !raw64) {
            // Convert to 32-bit generator
            convertedRng = RNGUtils.createIntProvider(rng);
        }
        if (byteOrder == ByteOrder.LITTLE_ENDIAN) {
            convertedRng = RNGUtils.createReverseBytesProvider(convertedRng);
        }
        return convertedRng;
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
            } catch (IOException ex) {
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
     * Check the count is positive, otherwise create an error message for the provided format.
     *
     * @param count The count of numbers to output.
     * @param format The format.
     * @throws ApplicationException If the count is not positive.
     */
    private static void checkCount(long count,
                                   OutputFormat format) {
        if (count <= 0) {
            throw new ApplicationException(format + " format requires a positive count: " + count);
        }
    }

    /**
     * Write int data to the specified output using the dieharder text format.
     *
     * @param rng The random generator.
     * @param out The output.
     * @throws IOException Signals that an I/O exception has occurred.
     * @throws ApplicationException If the count is not positive.
     */
    private void writeDieharder(final UniformRandomProvider rng,
                                final OutputStream out) throws IOException {
        checkCount(count, OutputFormat.DIEHARDER);

        // Use dieharder output, e.g.
        //#==================================================================
        //# generator mt19937  seed = 1
        //#==================================================================
        //type: d
        //count: 1
        //numbit: 32
        //1791095845
        try (BufferedWriter output = new BufferedWriter(new OutputStreamWriter(out, StandardCharsets.UTF_8))) {
            writeHeaderLine(output);
            output.write("# generator ");
            output.write(rng.toString());
            output.write("  seed = ");
            output.write(createSeedString());
            output.write(NEW_LINE);
            writeHeaderLine(output);
            output.write("type: d");
            output.write(NEW_LINE);
            output.write("count: ");
            output.write(Long.toString(count));
            output.write(NEW_LINE);
            output.write("numbit: 32");
            output.write(NEW_LINE);
            for (long c = 0; c < count; c++) {
                // Unsigned integers
                final String text = Long.toString(rng.nextInt() & 0xffffffffL);
                // Left pad with spaces
                for (int i = 10 - text.length(); i > 0; i--) {
                    output.write(' ');
                }
                output.write(text);
                output.write(NEW_LINE);
            }
        }
    }

    /**
     * Write a header line to the output.
     *
     * @param output the output
     * @throws IOException Signals that an I/O exception has occurred.
     */
    private static void writeHeaderLine(Writer output) throws IOException {
        output.write("#==================================================================");
        output.write(NEW_LINE);
    }

    /**
     * Write raw binary data to the output.
     *
     * @param rng The random generator.
     * @param out The output.
     * @throws IOException Signals that an I/O exception has occurred.
     */
    private void writeBinaryData(final UniformRandomProvider rng,
                                 final OutputStream out) throws IOException {
        // If count is not positive use max value.
        // This is effectively unlimited: program must be killed.
        final long limit = (count < 1) ? Long.MAX_VALUE : count;
        try (RngDataOutput data = RNGUtils.createDataOutput(rng, raw64, out, bufferSize, byteOrder)) {
            for (long c = 0; c < limit; c++) {
                data.write(rng);
            }
        }
    }

    /**
     * Write binary bit data to the specified file.
     *
     * @param rng The random generator.
     * @param out The output.
     * @throws IOException Signals that an I/O exception has occurred.
     * @throws ApplicationException If the count is not positive.
     */
    private void writeBitData(final UniformRandomProvider rng,
                              final OutputStream out) throws IOException {
        checkCount(count, OutputFormat.BITS);

        final boolean asLong = rng instanceof RandomLongSource;

        try (BufferedWriter output = new BufferedWriter(new OutputStreamWriter(out, StandardCharsets.UTF_8))) {
            for (long c = 0; c < count; c++) {
                if (asLong) {
                    writeLong(output, rng.nextLong());
                } else {
                    writeInt(output, rng.nextInt());
                }
            }
        }
    }

    /**
     * Write an {@code long} value to the the output. The native Java value will be
     * written to the writer on a single line using: a binary string representation
     * of the bytes; the unsigned integer; and the signed integer.
     *
     * <pre>
     * 10011010 01010011 01011010 11100100 01000111 00010000 01000011 11000101  11120331841399178181 -7326412232310373435
     * </pre>
     *
     * @param out The output.
     * @param value The value.
     * @throws IOException Signals that an I/O exception has occurred.
     */
    @SuppressWarnings("resource")
    static void writeLong(Writer out,
                          long value) throws IOException {

        // Write out as 8 bytes with spaces between them, high byte first.
        writeByte(out, (int)(value >>> 56) & 0xff);
        out.write(' ');
        writeByte(out, (int)(value >>> 48) & 0xff);
        out.write(' ');
        writeByte(out, (int)(value >>> 40) & 0xff);
        out.write(' ');
        writeByte(out, (int)(value >>> 32) & 0xff);
        out.write(' ');
        writeByte(out, (int)(value >>> 24) & 0xff);
        out.write(' ');
        writeByte(out, (int)(value >>> 16) & 0xff);
        out.write(' ');
        writeByte(out, (int)(value >>>  8) & 0xff);
        out.write(' ');
        writeByte(out, (int)(value >>>  0) & 0xff);

        // Write the unsigned and signed int value
        new Formatter(out).format("  %20s %20d%n", Long.toUnsignedString(value), value);
    }

    /**
     * Write an {@code int} value to the the output. The native Java value will be
     * written to the writer on a single line using: a binary string representation
     * of the bytes; the unsigned integer; and the signed integer.
     *
     * <pre>
     * 11001101 00100011 01101111 01110000   3441651568  -853315728
     * </pre>
     *
     * @param out The output.
     * @param value The value.
     * @throws IOException Signals that an I/O exception has occurred.
     */
    @SuppressWarnings("resource")
    static void writeInt(Writer out,
                         int value) throws IOException {

        // Write out as 4 bytes with spaces between them, high byte first.
        writeByte(out, (value >>> 24) & 0xff);
        out.write(' ');
        writeByte(out, (value >>> 16) & 0xff);
        out.write(' ');
        writeByte(out, (value >>>  8) & 0xff);
        out.write(' ');
        writeByte(out, (value >>>  0) & 0xff);

        // Write the unsigned and signed int value
        new Formatter(out).format("  %10d %11d%n", value & 0xffffffffL, value);
    }

    /**
     * Write the lower 8 bits of an {@code int} value to the buffered writer using a
     * binary string representation. This is left-filled with zeros if applicable.
     *
     * <pre>
     * 11001101
     * </pre>
     *
     * @param out The output.
     * @param value The value.
     * @throws IOException Signals that an I/O exception has occurred.
     */
    private static void writeByte(Writer out,
                                  int value) throws IOException {
        // This matches the functionality of:
        // data.write(String.format("%8s", Integer.toBinaryString(value & 0xff)).replace(' ', '0'))
        out.write(BIT_REP[value >>> 4]);
        out.write(BIT_REP[value & 0x0F]);
    }
}
