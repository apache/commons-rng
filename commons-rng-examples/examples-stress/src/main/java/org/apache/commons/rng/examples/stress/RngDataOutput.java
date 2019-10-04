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

import java.io.Closeable;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.ByteOrder;

/**
 * A specialised data output class that combines the functionality of
 * {@link java.io.DataOutputStream DataOutputStream} and
 * {@link java.io.BufferedOutputStream BufferedOutputStream} to write byte data from a RNG
 * to an OutputStream. Large blocks of byte data are written in a single operation for efficiency.
 * The byte data endianness can be configured.
 *
 * <p>This class is the functional equivalent of:</p>
 *
 * <pre>
 * <code>
 * OutputStream out = ...
 * UniformRandomProvider rng = ...
 * int size = 2048;
 * DataOutputStream sink = new DataOutputStream(new BufferedOutputStream(out, size * 4));
 * for (int i = 0; i < size; i++) {
 *    sink.writeInt(rng.nextInt());
 * }
 *
 * // Replaced with
 * RngDataOutput output = RngDataOutput.ofInt(out, size, ByteOrder.BIG_ENDIAN);
 * output.write(rng);
 * </code>
 * </pre>
 *
 * <p>Use of this class avoids the synchronized write operations in
 * {@link java.io.BufferedOutputStream BufferedOutputStream}. In particular it avoids the
 * 4 synchronized write operations to
 * {@link java.io.BufferedOutputStream#write(int) BufferedOutputStream#write(int)} that
 * occur for each {@code int} value that is written to
 * {@link java.io.DataOutputStream#writeInt(int) DataOutputStream#writeInt(int)}.</p>
 */
abstract class RngDataOutput implements Closeable {
    /** The data buffer. */
    protected final byte[] buffer;

    /** The underlying output stream. */
    private final OutputStream out;

    /**
     * Write big-endian {@code int} data.
     * <pre>
     * 3210  ->  3210
     * </pre>
     */
    private static class BIntRngDataOutput extends RngDataOutput {
        /**
         * @param out Output stream.
         * @param size Buffer size.
         */
        BIntRngDataOutput(OutputStream out, int size) {
            super(out, size);
        }

        @Override
        public void fillBuffer(UniformRandomProvider rng) {
            for (int i = 0; i < buffer.length; i += 4) {
                writeIntBE(i, rng.nextInt());
            }
        }
    }

    /**
     * Write little-endian {@code int} data.
     * <pre>
     * 3210  ->  0123
     * </pre>
     */
    private static class LIntRngDataOutput extends RngDataOutput {
        /**
         * @param out Output stream.
         * @param size Buffer size.
         */
        LIntRngDataOutput(OutputStream out, int size) {
            super(out, size);
        }

        @Override
        public void fillBuffer(UniformRandomProvider rng) {
            for (int i = 0; i < buffer.length; i += 4) {
                writeIntLE(i, rng.nextInt());
            }
        }
    }

    /**
     * Write big-endian {@code long} data.
     * <pre>
     * 76543210  ->  76543210
     * </pre>
     */
    private static class BLongRngDataOutput extends RngDataOutput {
        /**
         * @param out Output stream.
         * @param size Buffer size.
         */
        BLongRngDataOutput(OutputStream out, int size) {
            super(out, size);
        }

        @Override
        public void fillBuffer(UniformRandomProvider rng) {
            for (int i = 0; i < buffer.length; i += 8) {
                writeLongBE(i, rng.nextLong());
            }
        }
    }

    /**
     * Write little-endian {@code long} data.
     * <pre>
     * 76543210  ->  01234567
     * </pre>
     */
    private static class LLongRngDataOutput extends RngDataOutput {
        /**
         * @param out Output stream.
         * @param size Buffer size.
         */
        LLongRngDataOutput(OutputStream out, int size) {
            super(out, size);
        }

        @Override
        public void fillBuffer(UniformRandomProvider rng) {
            for (int i = 0; i < buffer.length; i += 8) {
                writeLongLE(i, rng.nextLong());
            }
        }
    }

    /**
     * Write {@code long} data as two little-endian {@code int} values.
     * <pre>
     * 76543210  ->  4567  0123
     * </pre>
     *
     * <p>This is a specialisation that allows the Java big-endian representation to be split
     * into two little-endian values in the original order of upper then lower bits. In
     * comparison the {@link LLongRngDataOutput} will output the same data as:
     *
     * <pre>
     * 76543210  ->  0123  4567
     * </pre>
     */
    private static class LLongAsIntRngDataOutput extends RngDataOutput {
        /**
         * @param out Output stream.
         * @param size Buffer size.
         */
        LLongAsIntRngDataOutput(OutputStream out, int size) {
            super(out, size);
        }

        @Override
        public void fillBuffer(UniformRandomProvider rng) {
            for (int i = 0; i < buffer.length; i += 8) {
                writeLongAsIntLE(i, rng.nextLong());
            }
        }
    }

    /**
     * Create a new instance.
     *
     * @param out Output stream.
     * @param size Buffer size.
     */
    RngDataOutput(OutputStream out, int size) {
        this.out = out;
        buffer = new byte[size];
    }

    /**
     * Write the configured amount of byte data from the specified RNG to the output.
     *
     * @param rng Source of randomness.
     * @exception IOException if an I/O error occurs.
     */
    public void write(UniformRandomProvider rng) throws IOException {
        fillBuffer(rng);
        out.write(buffer);
    }

    /**
     * Fill the buffer from the specified RNG.
     *
     * @param rng Source of randomness.
     */
    public abstract void fillBuffer(UniformRandomProvider rng);

    /**
     * Writes an {@code int} to the buffer as four bytes, high byte first (big-endian).
     *
     * @param index the index to start writing.
     * @param value an {@code int} to be written.
     */
    final void writeIntBE(int index, int value) {
        buffer[index    ] = (byte) (value >>> 24);
        buffer[index + 1] = (byte) (value >>> 16);
        buffer[index + 2] = (byte) (value >>> 8);
        buffer[index + 3] = (byte) value;
    }

    /**
     * Writes an {@code int} to the buffer as four bytes, low byte first (little-endian).
     *
     * @param index the index to start writing.
     * @param value an {@code int} to be written.
     */
    final void writeIntLE(int index, int value) {
        buffer[index    ] = (byte) value;
        buffer[index + 1] = (byte) (value >>> 8);
        buffer[index + 2] = (byte) (value >>> 16);
        buffer[index + 3] = (byte) (value >>> 24);
    }

    /**
     * Writes an {@code long} to the buffer as eight bytes, high byte first (big-endian).
     *
     * @param index the index to start writing.
     * @param value an {@code long} to be written.
     */
    final void writeLongBE(int index, long value) {
        buffer[index    ] = (byte) (value >>> 56);
        buffer[index + 1] = (byte) (value >>> 48);
        buffer[index + 2] = (byte) (value >>> 40);
        buffer[index + 3] = (byte) (value >>> 32);
        buffer[index + 4] = (byte) (value >>> 24);
        buffer[index + 5] = (byte) (value >>> 16);
        buffer[index + 6] = (byte) (value >>> 8);
        buffer[index + 7] = (byte) value;
    }

    /**
     * Writes an {@code long} to the buffer as eight bytes, low byte first (big-endian).
     *
     * @param index the index to start writing.
     * @param value an {@code long} to be written.
     */
    final void writeLongLE(int index, long value) {
        buffer[index    ] = (byte) value;
        buffer[index + 1] = (byte) (value >>> 8);
        buffer[index + 2] = (byte) (value >>> 16);
        buffer[index + 3] = (byte) (value >>> 24);
        buffer[index + 4] = (byte) (value >>> 32);
        buffer[index + 5] = (byte) (value >>> 40);
        buffer[index + 6] = (byte) (value >>> 48);
        buffer[index + 7] = (byte) (value >>> 56);
    }

    /**
     * Writes an {@code long} to the buffer as two integers of four bytes, each
     * low byte first (big-endian).
     *
     * @param index the index to start writing.
     * @param value an {@code long} to be written.
     */
    final void writeLongAsIntLE(int index, long value) {
        buffer[index    ] = (byte) (value >>> 32);
        buffer[index + 1] = (byte) (value >>> 40);
        buffer[index + 2] = (byte) (value >>> 48);
        buffer[index + 3] = (byte) (value >>> 56);
        buffer[index + 4] = (byte) value;
        buffer[index + 5] = (byte) (value >>> 8);
        buffer[index + 6] = (byte) (value >>> 16);
        buffer[index + 7] = (byte) (value >>> 24);
    }

    @Override
    public void close() throws IOException {
        try (OutputStream ostream = out) {
            ostream.flush();
        }
    }

    /**
     * Create a new instance to write batches of data from
     * {@link UniformRandomProvider#nextInt()} to the specified output.
     *
     * @param out Output stream.
     * @param size Number of values to write.
     * @param byteOrder Byte order.
     * @return the data output
     */
    @SuppressWarnings("resource")
    static RngDataOutput ofInt(OutputStream out, int size, ByteOrder byteOrder) {
        // Ensure the buffer is positive and a factor of 4
        final int bytes = Math.max(size * 4, 4);
        return byteOrder == ByteOrder.LITTLE_ENDIAN ?
            new LIntRngDataOutput(out, bytes) :
            new BIntRngDataOutput(out, bytes);
    }

    /**
     * Create a new instance to write batches of data from
     * {@link UniformRandomProvider#nextLong()} to the specified output.
     *
     * @param out Output stream.
     * @param size Number of values to write.
     * @param byteOrder Byte order.
     * @return the data output
     */
    @SuppressWarnings("resource")
    static RngDataOutput ofLong(OutputStream out, int size, ByteOrder byteOrder) {
        // Ensure the buffer is positive and a factor of 8
        final int bytes = Math.max(size * 8, 8);
        return byteOrder == ByteOrder.LITTLE_ENDIAN ?
            new LLongRngDataOutput(out, bytes) :
            new BLongRngDataOutput(out, bytes);
    }

    /**
     * Create a new instance to write batches of data from
     * {@link UniformRandomProvider#nextLong()} to the specified output as two sequential
     * {@code int} values.
     *
     * <p>This will output the following bytes:</p>
     *
     * <pre>
     * // Little-endian
     * 76543210  ->  4567  0123
     *
     * // Big-endian
     * 76543210  ->  7654  3210
     * </pre>
     *
     * <p>This ensures the output from the generator is the original upper then lower order bits
     * for each endianess.
     *
     * @param out Output stream.
     * @param size Number of values to write.
     * @param byteOrder Byte order.
     * @return the data output
     */
    @SuppressWarnings("resource")
    static RngDataOutput ofLongAsInt(OutputStream out, int size, ByteOrder byteOrder) {
        // Ensure the buffer is positive and a factor of 8
        final int bytes = Math.max(size * 8, 8);
        return byteOrder == ByteOrder.LITTLE_ENDIAN ?
            new LLongAsIntRngDataOutput(out, bytes) :
            new BLongRngDataOutput(out, bytes);
    }
}
