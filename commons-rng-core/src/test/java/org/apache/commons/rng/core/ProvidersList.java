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

import java.util.List;
import java.util.ArrayList;
import java.util.Collections;
import java.security.SecureRandom;

import org.apache.commons.rng.core.source32.JDKRandom;
import org.apache.commons.rng.core.source32.Well512a;
import org.apache.commons.rng.core.source32.XoRoShiRo64Star;
import org.apache.commons.rng.core.source32.XoRoShiRo64StarStar;
import org.apache.commons.rng.core.source32.XoShiRo128Plus;
import org.apache.commons.rng.core.source32.XoShiRo128StarStar;
import org.apache.commons.rng.core.source32.Well1024a;
import org.apache.commons.rng.core.source32.Well19937a;
import org.apache.commons.rng.core.source32.Well19937c;
import org.apache.commons.rng.core.source32.Well44497a;
import org.apache.commons.rng.core.source32.Well44497b;
import org.apache.commons.rng.core.source32.ISAACRandom;
import org.apache.commons.rng.core.source32.MersenneTwister;
import org.apache.commons.rng.core.source32.MultiplyWithCarry256;
import org.apache.commons.rng.core.source32.KISSRandom;
import org.apache.commons.rng.core.source32.PcgXshRr32;
import org.apache.commons.rng.core.source32.PcgXshRs32;
import org.apache.commons.rng.core.source32.PcgMcgXshRr32;
import org.apache.commons.rng.core.source32.PcgMcgXshRs32;
import org.apache.commons.rng.core.source64.SplitMix64;
import org.apache.commons.rng.core.source64.XorShift1024Star;
import org.apache.commons.rng.core.source64.XorShift1024StarPhi;
import org.apache.commons.rng.core.source64.TwoCmres;
import org.apache.commons.rng.core.source64.XoRoShiRo128Plus;
import org.apache.commons.rng.core.source64.XoRoShiRo128StarStar;
import org.apache.commons.rng.core.source64.XoShiRo256Plus;
import org.apache.commons.rng.core.source64.XoShiRo256StarStar;
import org.apache.commons.rng.core.source64.XoShiRo512Plus;
import org.apache.commons.rng.core.source64.XoShiRo512StarStar;
import org.apache.commons.rng.core.source64.MersenneTwister64;
import org.apache.commons.rng.core.source64.PcgRxsMXs64;
import org.apache.commons.rng.JumpableUniformRandomProvider;
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
public final class ProvidersList {
    /** List of all RNGs implemented in the library. */
    private static final List<RestorableUniformRandomProvider[]> LIST =
        new ArrayList<RestorableUniformRandomProvider[]>();
    /** List of 32-bits based RNGs. */
    private static final List<RestorableUniformRandomProvider[]> LIST32 =
        new ArrayList<RestorableUniformRandomProvider[]>();
    /** List of 64-bits based RNGs. */
    private static final List<RestorableUniformRandomProvider[]> LIST64 =
        new ArrayList<RestorableUniformRandomProvider[]>();
    /** List of {@link JumpableUniformRandomProvider} RNGs. */
    private static final List<JumpableUniformRandomProvider[]> LIST_JUMP =
        new ArrayList<JumpableUniformRandomProvider[]>();

    static {
        // External generator for creating a random seed.
        final SecureRandom g = new SecureRandom();

        try {
            // "int"-based RNGs.
            add(LIST32, new JDKRandom(g.nextLong()));
            add(LIST32, new MersenneTwister(new int[] {g.nextInt(), g.nextInt(), g.nextInt()}));
            add(LIST32, new Well512a(new int[] {g.nextInt(), g.nextInt(), g.nextInt()}));
            add(LIST32, new Well1024a(new int[] {g.nextInt(), g.nextInt(), g.nextInt()}));
            add(LIST32, new Well19937a(new int[] {g.nextInt(), g.nextInt(), g.nextInt()}));
            add(LIST32, new Well19937c(new int[] {g.nextInt(), g.nextInt(), g.nextInt()}));
            add(LIST32, new Well44497a(new int[] {g.nextInt(), g.nextInt(), g.nextInt()}));
            add(LIST32, new Well44497b(new int[] {g.nextInt(), g.nextInt(), g.nextInt()}));
            add(LIST32, new ISAACRandom(new int[] {g.nextInt(), g.nextInt(), g.nextInt()}));
            add(LIST32, new MultiplyWithCarry256(new int[] {g.nextInt(), g.nextInt(), g.nextInt()}));
            add(LIST32, new KISSRandom(new int[] {g.nextInt(), g.nextInt(), g.nextInt()}));
            add(LIST32, new XoRoShiRo64Star(new int[] {g.nextInt(), g.nextInt()}));
            add(LIST32, new XoRoShiRo64StarStar(new int[] {g.nextInt(), g.nextInt(), g.nextInt()}));
            add(LIST32, new XoShiRo128Plus(new int[] {g.nextInt(), g.nextInt(), g.nextInt()}));
            add(LIST32, new XoShiRo128StarStar(new int[] {g.nextInt(), g.nextInt(), g.nextInt()}));
            add(LIST32, new PcgXshRr32(new long[] {g.nextLong()}));
            add(LIST32, new PcgXshRs32(new long[] {g.nextLong()}));
            add(LIST32, new PcgMcgXshRr32(g.nextLong()));
            add(LIST32, new PcgMcgXshRs32(g.nextLong()));
            // ... add more here.

            // "long"-based RNGs.
            add(LIST64, new SplitMix64(g.nextLong()));
            add(LIST64, new XorShift1024Star(new long[] {g.nextLong(), g.nextLong(), g.nextLong(), g.nextLong()}));
            add(LIST64, new XorShift1024StarPhi(new long[] {g.nextLong(), g.nextLong(), g.nextLong(), g.nextLong()}));
            add(LIST64, new TwoCmres(g.nextInt()));
            add(LIST64, new TwoCmres(g.nextInt(), 5, 8));
            add(LIST64, new MersenneTwister64(new long[] {g.nextLong(), g.nextLong(), g.nextLong(), g.nextLong()}));
            add(LIST64, new XoRoShiRo128Plus(new long[] {g.nextLong(), g.nextLong()}));
            add(LIST64, new XoRoShiRo128StarStar(new long[] {g.nextLong(), g.nextLong()}));
            add(LIST64, new XoShiRo256Plus(new long[] {g.nextLong(), g.nextLong(), g.nextLong(), g.nextLong()}));
            add(LIST64, new XoShiRo256StarStar(new long[] {g.nextLong(), g.nextLong(), g.nextLong(), g.nextLong()}));
            add(LIST64, new XoShiRo512Plus(new long[] {g.nextLong(), g.nextLong(), g.nextLong(), g.nextLong()}));
            add(LIST64, new XoShiRo512StarStar(new long[] {g.nextLong(), g.nextLong(), g.nextLong(), g.nextLong()}));
            add(LIST64, new PcgRxsMXs64(new long[] {g.nextLong()}));
            // ... add more here.

            // Do not modify the remaining statements.
            // Complete list.
            LIST.addAll(LIST32);
            LIST.addAll(LIST64);
            // Dynamically identify the Jumpable RNGs
            for (RestorableUniformRandomProvider[] rng : LIST) {
                if (rng[0] instanceof JumpableUniformRandomProvider) {
                    add(LIST_JUMP, (JumpableUniformRandomProvider) rng[0]);
                }
            }
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
    private static void add(List<RestorableUniformRandomProvider[]> list,
                            RestorableUniformRandomProvider rng) {
        list.add(new RestorableUniformRandomProvider[] {rng});
    }

    /**
     * Helper to statisfy Junit requirement that each parameter set contains
     * the same number of objects.
     */
    private static void add(List<JumpableUniformRandomProvider[]> list,
                            JumpableUniformRandomProvider rng) {
        list.add(new JumpableUniformRandomProvider[] {rng});
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

    /**
     * Subclasses that are "parametric" tests can forward the call to
     * the "@Parameters"-annotated method to this method.
     *
     * @return the list of {@link JumpableUniformRandomProvider} generators.
     */
    public static Iterable<JumpableUniformRandomProvider[]> listJumpable() {
        return Collections.unmodifiableList(LIST_JUMP);
    }
}
