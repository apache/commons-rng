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
package org.apache.commons.rng.core;

import java.util.Arrays;
import java.util.List;
import java.util.ArrayList;
import java.util.Collections;

import org.apache.commons.rng.core.source32.JDKRandom;
import org.apache.commons.rng.core.source32.Well512a;
import org.apache.commons.rng.core.source32.Well1024a;
import org.apache.commons.rng.core.source32.Well19937a;
import org.apache.commons.rng.core.source32.Well19937c;
import org.apache.commons.rng.core.source32.Well44497a;
import org.apache.commons.rng.core.source32.Well44497b;
import org.apache.commons.rng.core.source32.ISAACRandom;
import org.apache.commons.rng.core.source32.MersenneTwister;
import org.apache.commons.rng.core.source32.MultiplyWithCarry256;
import org.apache.commons.rng.core.source32.KISSRandom;
import org.apache.commons.rng.core.source64.SplitMix64;
import org.apache.commons.rng.core.source64.XorShift1024Star;
import org.apache.commons.rng.core.source64.TwoCmres;
import org.apache.commons.rng.core.source64.MersenneTwister64;
import org.apache.commons.rng.RestorableUniformRandomProvider;

/**
 * The purpose of this class is to provide the list of all generators
 * implemented in the library.
 * The list must be updated with each new RNG implementation.
 *
 * @see #list()
 * @see #list32()
 * @see #list64()
 */
public class ProvidersList {
    /** List of all RNGs implemented in the library. */
    private static final List<RestorableUniformRandomProvider[]> LIST =
        new ArrayList<RestorableUniformRandomProvider[]>();
    /** List of 32-bits based RNGs. */
    private static final List<RestorableUniformRandomProvider[]> LIST32 =
        new ArrayList<RestorableUniformRandomProvider[]>();
    /** List of 64-bits based RNGs. */
    private static final List<RestorableUniformRandomProvider[]> LIST64 =
        new ArrayList<RestorableUniformRandomProvider[]>();

    static {
        try {
            // "int"-based RNGs.
            add(LIST32, new JDKRandom(-122333444455555L));
            add(LIST32, new MersenneTwister(new int[] { -123, -234, -345 }));
            add(LIST32, new Well512a(new int[] { -23, -34, -45 }));
            add(LIST32, new Well1024a(new int[] { -1234, -2345, -3456 }));
            add(LIST32, new Well19937a(new int[] { -2123, -3234, -4345 }));
            add(LIST32, new Well19937c(new int[] { -123, -234, -345, -456 }));
            add(LIST32, new Well44497a(new int[] { -12345, -23456, -34567 }));
            add(LIST32, new Well44497b(new int[] { 123, 234, 345 }));
            add(LIST32, new ISAACRandom(new int[] { 123, -234, 345, -456 }));
            add(LIST32, new MultiplyWithCarry256(new int[] { 12, -1234, -3456, 45679 }));
            add(LIST32, new KISSRandom(new int[] { 12, 1234, 23456, 345678 }));
            // ... add more here.

            // "long"-based RNGs.
            add(LIST64, new SplitMix64(-98877766544333L));
            add(LIST64, new XorShift1024Star(new long[] { 123456L, 234567L, -345678L }));
            add(LIST64, new TwoCmres(55443322));
            add(LIST64, new TwoCmres(-987654321, 5, 8));
            add(LIST64, new MersenneTwister64(new long[] { 1234567L, 2345678L, -3456789L }));
            // ... add more here.

            // Do not modify the remaining statements.
            // Complete list.
            LIST.addAll(LIST32);
            LIST.addAll(LIST64);
        } catch (Exception e) {
            System.err.println("Unexpected exception while creating the list of generators: " + e);
            e.printStackTrace(System.err);
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
    private static void add(List<RestorableUniformRandomProvider[]> list,
                            RestorableUniformRandomProvider rng) {
        list.add(new RestorableUniformRandomProvider[] { rng });
    }

    /**
     * Subclasses that are "parametric" tests can forward the call to
     * the "@Parameters"-annotated method to this method.
     *
     * @return the list of all generators.
     */
    public static Iterable<RestorableUniformRandomProvider[]> list() {
        return Collections.unmodifiableList(LIST);
    }

    /**
     * Subclasses that are "parametric" tests can forward the call to
     * the "@Parameters"-annotated method to this method.
     *
     * @return the list of 32-bits based generators.
     */
    public static Iterable<RestorableUniformRandomProvider[]> list32() {
        return Collections.unmodifiableList(LIST32);
    }

    /**
     * Subclasses that are "parametric" tests can forward the call to
     * the "@Parameters"-annotated method to this method.
     *
     * @return the list of 64-bits based generators.
     */
    public static Iterable<RestorableUniformRandomProvider[]> list64() {
        return Collections.unmodifiableList(LIST64);
    }
}
