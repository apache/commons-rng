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
package org.apache.commons.rng.simple;

import java.util.Arrays;
import java.util.List;
import java.util.ArrayList;
import java.util.Collections;

/**
 * The purpose of this class is to provide the list of all generators
 * implemented in the library.
 * The list must be updated with each new RNG implementation.
 *
 * @see #list()
 * @see #list32()
 * @see #list64()
 */
public final class ProvidersList {
    /** List of all RNGs implemented in the library. */
    private static final List<Data> LIST = new ArrayList<>();
    /** List of 32-bits based RNGs. */
    private static final List<Data> LIST32 = new ArrayList<>();
    /** List of 64-bits based RNGs. */
    private static final List<Data> LIST64 = new ArrayList<>();

    static {
        try {
            // "int"-based RNGs.
            add(LIST32, RandomSource.JDK, -122333444455555L);
            add(LIST32, RandomSource.MT, new int[] {-123, -234, -345});
            add(LIST32, RandomSource.WELL_512_A, new int[] {-23, -34, -45});
            add(LIST32, RandomSource.WELL_1024_A, new int[] {-1234, -2345, -3456});
            add(LIST32, RandomSource.WELL_19937_A, new int[] {-2123, -3234, -4345});
            add(LIST32, RandomSource.WELL_19937_C, new int[] {-123, -234, -345, -456});
            add(LIST32, RandomSource.WELL_44497_A, new int[] {-12345, -23456, -34567});
            add(LIST32, RandomSource.WELL_44497_B, new int[] {123, 234, 345});
            add(LIST32, RandomSource.ISAAC, new int[] {123, -234, 345, -456});
            add(LIST32, RandomSource.MWC_256, new int[] {12, -1234, -3456, 45678});
            add(LIST32, RandomSource.KISS, new int[] {12, 1234, 23456, 345678});
            add(LIST32, RandomSource.XO_RO_SHI_RO_64_S, new int[] {42, 12345});
            add(LIST32, RandomSource.XO_RO_SHI_RO_64_SS, new int[] {78942, 134});
            add(LIST32, RandomSource.XO_SHI_RO_128_PLUS, new int[] {565642, 1234, 4534});
            add(LIST32, RandomSource.XO_SHI_RO_128_SS, new int[] {89, 1234, 6787});
            add(LIST32, RandomSource.PCG_XSH_RR_32, new long[] {1738L, 1234L});
            add(LIST32, RandomSource.PCG_XSH_RS_32, new long[] {259L, 2861L});
            add(LIST32, RandomSource.PCG_MCG_XSH_RS_32, 9678L);
            add(LIST32, RandomSource.PCG_MCG_XSH_RR_32, 2578291L);
            // Ensure a high complexity increment is used for the Weyl sequence otherwise
            // it will not output random data.
            add(LIST32, RandomSource.MSWS, new long[] {687233648L, 678656564562300L, 0xb5ad4eceda1ce2a9L});
            add(LIST32, RandomSource.SFC_32, new int[] {-23574234, 7654343});
            add(LIST32, RandomSource.XO_SHI_RO_128_PP, new int[] {8796823, -3244890, -263842});
            add(LIST32, RandomSource.PCG_XSH_RR_32_OS, 72346247L);
            add(LIST32, RandomSource.PCG_XSH_RS_32_OS, -5340832872354L);
            add(LIST32, RandomSource.L32_X64_MIX, new int[] {2134678128, -162788128});
            // ... add more here.

            // "long"-based RNGs.
            add(LIST64, RandomSource.SPLIT_MIX_64, -988777666655555L);
            add(LIST64, RandomSource.XOR_SHIFT_1024_S, new long[] {123456L, 234567L, -345678L});
            add(LIST64, RandomSource.XOR_SHIFT_1024_S_PHI, new long[] {-234567L, -345678L, 3456789L});
            add(LIST64, RandomSource.TWO_CMRES, 55443322);
            add(LIST64, RandomSource.TWO_CMRES_SELECT, -987654321, 5, 8);
            add(LIST64, RandomSource.MT_64, new long[] {1234567L, 2345678L, -3456789L});
            add(LIST64, RandomSource.XO_RO_SHI_RO_128_PLUS, new long[] {55646L, -456659L, 565656L});
            add(LIST64, RandomSource.XO_RO_SHI_RO_128_SS, new long[] {45655L, 5454544L, 4564659L});
            add(LIST64, RandomSource.XO_SHI_RO_256_PLUS, new long[] {11222L, -568989L, -456789L});
            add(LIST64, RandomSource.XO_SHI_RO_256_SS, new long[] {98765L, -2345678L, -3456789L});
            add(LIST64, RandomSource.XO_SHI_RO_512_PLUS, new long[] {89932L, -545669L, 4564689L});
            add(LIST64, RandomSource.XO_SHI_RO_512_SS, new long[] {123L, -654654L, 45646789L});
            add(LIST64, RandomSource.PCG_RXS_M_XS_64, new long[] {42088L, 69271L});
            add(LIST64, RandomSource.SFC_64, new long[] {-2357423478979842L, 76543434515L});
            add(LIST64, RandomSource.XO_RO_SHI_RO_128_PP, new long[] {789741321465L, -461321684612L, -12301654794L});
            add(LIST64, RandomSource.XO_SHI_RO_256_PP, new long[] {2374243L, -8097397345383L, -223479293943L});
            add(LIST64, RandomSource.XO_SHI_RO_512_PP, new long[] {-1210684761321465L, -485132198745L, 89942134798523L});
            add(LIST64, RandomSource.XO_RO_SHI_RO_1024_PP, new long[] {236424345654L, 781544546164721L, -85235476312346L});
            add(LIST64, RandomSource.XO_RO_SHI_RO_1024_S, new long[] {-1574314L, 7879874453221215L, -7894343883216L});
            add(LIST64, RandomSource.XO_RO_SHI_RO_1024_SS, new long[] {-41514541234654321L, -12146412316546L, 7984134134L});
            add(LIST64, RandomSource.PCG_RXS_M_XS_64_OS, -34657834534L);
            add(LIST64, RandomSource.L64_X128_SS, new long[] {-2379479823783L, -235642384324L, 123678172804389L});
            add(LIST64, RandomSource.L64_X128_MIX, new long[] {-9723846672394L, 623748567398002L, -23678792345897934L});
            add(LIST64, RandomSource.L64_X256_MIX, new long[] {236784568279L, 237894579279L, -2378945793L});
            add(LIST64, RandomSource.L64_X1024_MIX, new long[] {279834579232345L, -2374689578237L, -2347895789327L});
            add(LIST64, RandomSource.L128_X128_MIX, new long[] {236748567823789L, 237485792375L, 2374895789324L});
            add(LIST64, RandomSource.L128_X256_MIX, new long[] {-829345782324L, -92304897238673245L, 28974785792345L});
            add(LIST64, RandomSource.L128_X1024_MIX, new long[] {-6563745678920234L, 7348578274523L, 234523455234L});
            // ... add more here.

            // Do not modify the remaining statements.
            // Complete list.
            LIST.addAll(LIST32);
            LIST.addAll(LIST64);
        } catch (Exception e) {
            // CHECKSTYLE: stop Regexp
            System.err.println("Unexpected exception while creating the list of generators: " + e);
            e.printStackTrace(System.err);
            // CHECKSTYLE: resume Regexp
            throw new RuntimeException(e);
        }
    }

    /**
     * Class contains only static methods.
     */
    private ProvidersList() {}

    /**
     * Helper to statisfy Junit requirement that each parameter set contains
     * the same number of objects.
     */
    private static void add(List<Data> list,
                            RandomSource source,
                            Object... data) {
        final RandomSource rng = source;
        final Object seed = data.length > 0 ? data[0] : null;
        final Object[] args = data.length > 1 ? Arrays.copyOfRange(data, 1, data.length) : null;

        list.add(new Data(rng, seed, args));
    }

    /**
     * Subclasses that are "parametric" tests can forward the call to
     * the "@Parameters"-annotated method to this method.
     *
     * @return the list of all generators.
     */
    public static Iterable<Data> list() {
        return Collections.unmodifiableList(LIST);
    }

    /**
     * Subclasses that are "parametric" tests can forward the call to
     * the "@Parameters"-annotated method to this method.
     *
     * @return the list of 32-bits based generators.
     */
    public static Iterable<Data> list32() {
        return Collections.unmodifiableList(LIST32);
    }

    /**
     * Subclasses that are "parametric" tests can forward the call to
     * the "@Parameters"-annotated method to this method.
     *
     * @return the list of 64-bits based generators.
     */
    public static Iterable<Data> list64() {
        return Collections.unmodifiableList(LIST64);
    }

    /**
     * Helper.
     * Better not to mix Junit assumptions of the usage of "Object[]".
     */
    public static class Data {
        /** RNG specifier. */
        private final RandomSource source;
        /** Seed (constructor's first parameter). */
        private final Object seed;
        /** Constructor's additional parameters. */
        private final Object[] args;

        /**
         * @param source RNG specifier.
         * @param seed Seed (constructor's first parameter).
         * @param args Constructor's additional parameters.
         */
        public Data(RandomSource source,
                    Object seed,
                    Object[] args) {
            this.source = source;
            this.seed = seed;
            this.args = args;
        }

        /**
         * Gets the RNG specifier.
         *
         * @return RNG specifier.
         */
        public RandomSource getSource() {
            return source;
        }

        /**
         * Gets the seed (constructor's first parameter).
         *
         * @return Seed
         */
        public Object getSeed() {
            return seed;
        }

        /**
         * Gets the constructor's additional parameters.
         *
         * @return Additional parameters.
         */
        public Object[] getArgs() {
            return args == null ? null : Arrays.copyOf(args, args.length);
        }

        @Override
        public String toString() {
            return source.toString() + " seed=" + seed + " args=" + Arrays.toString(args);
        }
    }
}
