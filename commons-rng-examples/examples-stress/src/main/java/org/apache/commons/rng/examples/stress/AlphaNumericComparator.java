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

import java.io.Serializable;
import java.util.Comparator;

/**
 * Provides number sensitive sorting for character sequences.
 *
 * <p>Extracts sub-sequences of either numeric ({@code [0, 9]}) or non-numeric characters
 * and compares them numerically or lexicographically. Leading zeros are ignored from
 * numbers. Negative numbers are not supported.
 *
 * <pre>
 * Traditional  AlphaNumeric
 * z0200.html   z2.html
 * z100.html    z100.html
 * z2.html      z0200.html
 * </pre>
 *
 * <p>This is based on ideas in the Alphanum algorithm by David Koelle.</p>
 *
 * <p>This implementation supports:</p>
 *
 * <ul>
 *  <li>{@link CharSequence} comparison
 *  <li>Direct use of input sequences for minimal memory consumption
 *  <li>Numbers with leading zeros
 * </ul>
 *
 * <p>Any null sequences are ordered before non-null sequences.</p>
 *
 * <p>Note: The comparator is thread-safe so can be used in a parallel sort.
 *
 * @see <a href="http://www.DaveKoelle.com">Alphanum Algorithm</a>
 */
class AlphaNumericComparator implements Comparator<CharSequence>, Serializable {
    /**
     * An instance.
     */
    public static final AlphaNumericComparator INSTANCE = new AlphaNumericComparator();

    /**
     * The serial version ID.
     * Note: Comparators are recommended to be Serializable to allow serialization of
     * collections constructed with a Comparator.
     */
    private static final long serialVersionUID = 1L;

    @Override
    public int compare(CharSequence seq1, CharSequence seq2) {
        // Null is less
        if (seq1 == null) {
            return -1;
        }
        if (seq2 == null) {
            return 1;
        }
        if (seq1.equals(seq2)) {
            return 0;
        }

        int pos1 = 0;
        int pos2 = 0;
        final int length1 = seq1.length();
        final int length2 = seq2.length();

        while (pos1 < length1 && pos2 < length2) {
            final int end1 = nextSubSequenceEnd(seq1, pos1, length1);
            final int end2 = nextSubSequenceEnd(seq2, pos2, length2);

            // If both sub-sequences contain numeric characters, sort them numerically
            int result = 0;
            if (isDigit(seq1.charAt(pos1)) && isDigit(seq2.charAt(pos2))) {
                result = compareNumerically(seq1, pos1, end1, seq2, pos2, end2);
            } else {
                result = compareLexicographically(seq1, pos1, end1, seq2, pos2, end2);
            }

            if (result != 0) {
                return result;
            }

            pos1 = end1;
            pos2 = end2;
        }

        return length1 - length2;
    }

    /**
     * Get the end position of the next sub-sequence of either digits or non-digit
     * characters starting from the start position.
     *
     * <p>The end position is exclusive such that the sub-sequence is the interval
     * {@code [start, end)}.
     *
     * @param seq the character sequence
     * @param start the start position
     * @param length the sequence length
     * @return the sub-sequence end position (exclusive)
     */
    private static int nextSubSequenceEnd(CharSequence seq, int start, int length) {
        int pos = start;
        // Set the sub-sequence type (digits or non-digits)
        final boolean seqType = isDigit(seq.charAt(pos++));
        while (pos < length && seqType == isDigit(seq.charAt(pos))) {
            // Extend the sub-sequence
            pos++;
        }
        return pos;
    }

    /**
     * Checks if the character is a digit.
     *
     * @param ch the character
     * @return true if a digit
     */
    private static boolean isDigit(char ch) {
        return ch >= 48 && ch <= 57;
    }

    /**
     * Compares two sub-sequences numerically. Ignores leading zeros. Assumes all
     * characters are digits.
     *
     * @param seq1 the first sequence
     * @param start1 the start of the first sub-sequence
     * @param end1 the end of the first sub-sequence
     * @param seq2 the second sequence
     * @param start2 the start of the second sub-sequence
     * @param end2 the end of the second sub-sequence sequence
     * @return the value {@code 0} if the sub-sequences are equal; a value less than
     * {@code 0} if sub-sequence 1 is numerically less than sub-sequence 2; and a value
     * greater than {@code 0} if sub-sequence 1 is numerically greater than sub-sequence
     * 2.
     */
    private static int compareNumerically(CharSequence seq1, int start1, int end1,
                                          CharSequence seq2, int start2, int end2) {
        // Ignore leading zeros in numbers
        int pos1 = advancePastLeadingZeros(seq1, start1, end1);
        int pos2 = advancePastLeadingZeros(seq2, start2, end2);

        // Simple comparison by length
        final int result = (end1 - pos1) - (end2 - pos2);
        // If equal, the first different number counts.
        if (result == 0) {
            while (pos1 < end1) {
                final char c1 = seq1.charAt(pos1++);
                final char c2 = seq2.charAt(pos2++);
                if (c1 != c2) {
                    return c1 - c2;
                }
            }
        }
        return result;
    }

    /**
     * Advances past leading zeros in the sub-sequence. Returns the index of the start
     * character of the number.
     *
     * @param seq the sequence
     * @param start the start of the sub-sequence
     * @param end the end of the sub-sequence
     * @return the start index of the number
     */
    private static int advancePastLeadingZeros(CharSequence seq, int start, int end) {
        int pos = start;
        // Ignore zeros only when there are further characters
        while (pos < end - 1 && seq.charAt(pos) == '0') {
            pos++;
        }
        return pos;
    }

    /**
     * Compares two sub-sequences lexicographically. This matches the compare function in
     * {@link String} using extracted sub-sequences.
     *
     * @param seq1 the first sequence
     * @param start1 the start of the first sub-sequence
     * @param end1 the end of the first sub-sequence
     * @param seq2 the second sequence
     * @param start2 the start of the second sub-sequence
     * @param end2 the end of the second sub-sequence sequence
     * @return the value {@code 0} if the sub-sequences are equal; a value less than
     * {@code 0} if sub-sequence 1 is lexicographically less than sub-sequence 2; and a
     * value greater than {@code 0} if sub-sequence 1 is lexicographically greater than
     * sub-sequence 2.
     * @see String#compareTo(String)
     */
    private static int compareLexicographically(CharSequence seq1, int start1, int end1,
                                                CharSequence seq2, int start2, int end2) {
        final int len1 = end1 - start1;
        final int len2 = end2 - start2;
        final int limit = Math.min(len1, len2);

        for (int i = 0; i < limit; i++) {
            final char c1 = seq1.charAt(i + start1);
            final char c2 = seq2.charAt(i + start2);
            if (c1 != c2) {
                return c1 - c2;
            }
        }
        return len1 - len2;
    }
}
