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

package org.apache.commons.rng.examples.jmh;

import org.apache.commons.rng.UniformRandomProvider;
import org.apache.commons.rng.core.source64.SplitMix64;
import org.apache.commons.rng.simple.RandomSource;
import org.apache.commons.rng.simple.internal.SeedFactory;
import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.BenchmarkMode;
import org.openjdk.jmh.annotations.Fork;
import org.openjdk.jmh.annotations.Measurement;
import org.openjdk.jmh.annotations.Mode;
import org.openjdk.jmh.annotations.OutputTimeUnit;
import org.openjdk.jmh.annotations.Scope;
import org.openjdk.jmh.annotations.State;
import org.openjdk.jmh.annotations.Threads;
import org.openjdk.jmh.annotations.Warmup;

import java.util.Random;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

/**
 * Executes a benchmark to compare the speed of generating a single {@code int/long} value
 * in a thread-safe way.
 */
@BenchmarkMode(Mode.AverageTime)
@OutputTimeUnit(TimeUnit.NANOSECONDS)
@Warmup(iterations = 10, time = 1, timeUnit = TimeUnit.SECONDS)
@Measurement(iterations = 10, time = 1, timeUnit = TimeUnit.SECONDS)
@State(Scope.Benchmark)
@Fork(value = 1, jvmArgs = {"-server", "-Xms512M", "-Xmx512M"})
public class SeedGenerationPerformance {
    /**
     * The increment of the seed per new instance.
     * This is copied from ThreadLocalRandom.
     */
    static final long SEED_INCREMENT = 0xbb67ae8584caa73bL;

    /**
     * The lock to own when using the generator. This lock is unfair and there is no
     * particular access order for waiting threads.
     *
     * <p>This is used as an alternative to {@code synchronized} statements.</p>
     */
    private static final ReentrantLock UNFAIR_LOCK = new ReentrantLock(false);

    /**
     * The lock to own when using the generator. This lock is fair and the longest waiting
     * thread will be favoured.
     *
     * <p>This is used as an alternative to {@code synchronized} statements.</p>
     */
    private static final ReentrantLock FAIR_LOCK = new ReentrantLock(true);

    /**
     * A representation of the golden ratio converted to a long:
     * 2^64 * phi where phi = (sqrt(5) - 1) / 2.
     */
    private static final long GOLDEN_GAMMA = 0x9e3779b97f4a7c15L;

    /** The int value. Must NOT be final to prevent JVM optimisation! */
    private int intValue;

    /** The long value. Must NOT be final to prevent JVM optimisation! */
    private long longValue;

    /** The int value to use for 'randomness'. */
    private volatile int volatileIntValue;

    /** The long value to use for 'randomness'. */
    private volatile long volatileLongValue;

    /** The Atomic integer to use for 'randomness'. */
    private final AtomicInteger atomicInt = new AtomicInteger();

    /** The Atomic long to use for 'randomness'. */
    private final AtomicLong atomicLong = new AtomicLong();

    /** The state of the SplitMix generator. */
    private final AtomicLong state = new AtomicLong();

    /** The XoRoShiRo128Plus RNG. */
    private final UniformRandomProvider xoRoShiRo128Plus = RandomSource.create(RandomSource.XO_RO_SHI_RO_128_PLUS);

    /** The XorShift1024StarPhi RNG. */
    private final UniformRandomProvider xorShift1024StarPhi = RandomSource.create(RandomSource.XOR_SHIFT_1024_S_PHI);

    /** The Well44497b RNG. */
    private final UniformRandomProvider well44497b = RandomSource.create(RandomSource.WELL_44497_B);

    /** The JDK Random instance (the implementation is thread-safe). */
    private final Random random = new Random();

    /**
     * Extend the {@link ThreadLocal} to allow creation of a local generator.
     */
    private static final class ThreadLocalRNG extends ThreadLocal<UniformRandomProvider> {
        /**
         * The seed for constructors.
         */
        private static final AtomicLong SEED = new AtomicLong(0);

        /** Instance. */
        private static final ThreadLocalRNG INSTANCE = new ThreadLocalRNG();

        /** No public construction. */
        private ThreadLocalRNG() {
            // Do nothing. The seed could be initialised here.
        }

        @Override
        protected UniformRandomProvider initialValue() {
            return new SplitMix64(SEED.getAndAdd(SEED_INCREMENT));
        }

        /**
         * Get the current thread's RNG.
         *
         * @return the uniform random provider
         */
        public static UniformRandomProvider current() {
            return INSTANCE.get();
        }
    }

    /**
     * Extend the {@link ThreadLocal} to allow creation of a local generator.
     */
    private static final class ThreadLocalSplitMix extends ThreadLocal<SplitMix64> {
        /**
         * The seed for constructors.
         */
        private static final AtomicLong SEED = new AtomicLong(0);

        /** Instance. */
        private static final ThreadLocalSplitMix INSTANCE = new ThreadLocalSplitMix();

        /** No public construction. */
        private ThreadLocalSplitMix() {
            // Do nothing. The seed could be initialised here.
        }

        @Override
        protected SplitMix64 initialValue() {
            return new SplitMix64(SEED.getAndAdd(SEED_INCREMENT));
        }

        /**
         * Get the current thread's RNG.
         *
         * @return the uniform random provider
         */
        public static SplitMix64 current() {
            return INSTANCE.get();
        }
    }

    /**
     * Extend the {@link ThreadLocal} to allow creation of a local sequence.
     */
    private static final class ThreadLocalSequence extends ThreadLocal<long[]> {
        /**
         * The seed for constructors.
         */
        private static final AtomicLong SEED = new AtomicLong(0);

        /** Instance. */
        private static final ThreadLocalSequence INSTANCE = new ThreadLocalSequence();

        /** No public construction. */
        private ThreadLocalSequence() {
            // Do nothing. The seed could be initialised here.
        }

        @Override
        protected long[] initialValue() {
            return new long[] {SEED.getAndAdd(SEED_INCREMENT)};
        }

        /**
         * Get the current thread's next sequence value.
         *
         * @return the next value
         */
        public static long next() {
            final long[] value = INSTANCE.get();
            return value[0] += GOLDEN_GAMMA;
        }
    }

    /**
     * Get the next {@code int} from the RNG. This is synchronized on the generator.
     *
     * @param rng Random generator.
     * @return the int
     */
    private static int nextInt(UniformRandomProvider rng) {
        synchronized (rng) {
            return rng.nextInt();
        }
    }

    /**
     * Get the next {@code long} from the RNG. This is synchronized on the generator.
     *
     * @param rng Random generator.
     * @return the long
     */
    private static long nextLong(UniformRandomProvider rng) {
        synchronized (rng) {
            return rng.nextLong();
        }
    }

    /**
     * Get the next {@code int} from the RNG. The lock is used to guard access to the generator.
     *
     * @param lock Lock guarding access to the generator.
     * @param rng Random generator.
     * @return the int
     */
    private static int nextInt(Lock lock, UniformRandomProvider rng) {
        lock.lock();
        try {
            return rng.nextInt();
        } finally {
            lock.unlock();
        }
    }

    /**
     * Get the next {@code long} from the RNG. The lock is used to guard access to the generator.
     *
     * @param lock Lock guarding access to the generator.
     * @param rng Random generator.
     * @return the long
     */
    private static long nextLong(Lock lock, UniformRandomProvider rng) {
        lock.lock();
        try {
            return rng.nextLong();
        } finally {
            lock.unlock();
        }
    }

    /**
     * Perform the mixing step from the SplitMix algorithm. This is the Stafford variant
     * 13 mix64 function.
     *
     * @param z the input value
     * @return the output value
     * @see <a
     * href="http://zimbry.blogspot.com/2011/09/better-bit-mixing-improving-on.html">Better
     * Bit Mixing</a>
     */
    private static long mixLong(long z) {
        z = (z ^ (z >>> 30)) * 0xbf58476d1ce4e5b9L;
        z = (z ^ (z >>> 27)) * 0x94d049bb133111ebL;
        return z ^ (z >>> 31);
    }

    /**
     * Perform the 32-bit mixing step from the SplittableRandom algorithm. Note this is
     * the 32 high bits of Stafford variant 4 mix64 function as int.
     *
     * @param z the input value
     * @return the output value
     * @see <a
     * href="http://zimbry.blogspot.com/2011/09/better-bit-mixing-improving-on.html">Better
     * Bit Mixing</a>
     */
    private static int mixInt(long z) {
        z = (z ^ (z >>> 33)) * 0x62a9d9ed799705f5L;
        return (int) (((z ^ (z >>> 28)) * 0xcb24d0a5c88c35b3L) >>> 32);
    }

    /**
     * Baseline for a JMH method call with no return value.
     */
    @Benchmark
    public void baselineVoid() {
        // Do nothing, this is a baseline
    }

    /**
     * Baseline for a JMH method call returning an {@code int}.
     *
     * @return the value
     */
    @Benchmark
    public int baselineInt() {
        return intValue;
    }

    /**
     * Baseline for a JMH method call returning an {@code long}.
     *
     * @return the value
     */
    @Benchmark
    public long baselineLong() {
        return longValue;
    }

    // The following methods use underscores to make parsing the results output easier.
    // They are not documented as the names are self-documenting.

    // CHECKSTYLE: stop MethodName
    // CHECKSTYLE: stop JavadocMethod
    // CHECKSTYLE: stop DesignForExtension

    @Benchmark
    public int XoRoShiRo128Plus_nextInt() {
        return xoRoShiRo128Plus.nextInt();
    }

    @Benchmark
    public int XorShift1024StarPhi_nextInt() {
        return xorShift1024StarPhi.nextInt();
    }

    @Benchmark
    public int Well44497b_nextInt() {
        return well44497b.nextInt();
    }

    @Benchmark
    public long XoRoShiRo128Plus_nextLong() {
        return xoRoShiRo128Plus.nextLong();
    }

    @Benchmark
    public long XorShift1024StarPhi_nextLong() {
        return xorShift1024StarPhi.nextLong();
    }

    @Benchmark
    public long Well44497b_nextLong() {
        return well44497b.nextLong();
    }

    @Benchmark
    public int Threads1_SeedFactory_createInt() {
        return SeedFactory.createInt();
    }

    @Benchmark
    public long Threads1_SeedFactory_createLong() {
        return SeedFactory.createLong();
    }

    @Benchmark
    public long Threads1_System_currentTimeMillis() {
        // This may not be unique per call and is not random.
        return System.currentTimeMillis();
    }

    @Benchmark
    public long Threads1_System_nanoTime() {
        // This is not random.
        return System.nanoTime();
    }

    @Benchmark
    public int Threads1_System_identityHashCode() {
        return System.identityHashCode(new Object());
    }

    @Benchmark
    public long Threads1_ThreadLocalRandom_nextLong() {
        return ThreadLocalRandom.current().nextLong();
    }

    @Benchmark
    public int Threads1_ThreadLocalRandom_nextInt() {
        return ThreadLocalRandom.current().nextInt();
    }

    @Benchmark
    public long Threads1_ThreadLocalRNG_nextLong() {
        return ThreadLocalRNG.current().nextLong();
    }

    @Benchmark
    public int Threads1_ThreadLocalRNG_nextInt() {
        return ThreadLocalRNG.current().nextInt();
    }

    @Benchmark
    public long Threads1_ThreadLocalSplitMix_nextLong() {
        return ThreadLocalSplitMix.current().nextLong();
    }

    @Benchmark
    public int Threads1_ThreadLocalSplitMix_nextInt() {
        return ThreadLocalSplitMix.current().nextInt();
    }

    @Benchmark
    public long Threads1_ThreadLocalSequenceMix_nextLong() {
        return mixLong(ThreadLocalSequence.next());
    }

    @Benchmark
    public int Threads1_ThreadLocalSequenceMix_nextInt() {
        return mixInt(ThreadLocalSequence.next());
    }

    @Benchmark
    public long Threads1_Random_nextLong() {
        return random.nextLong();
    }

    @Benchmark
    public int Threads1_Random_nextInt() {
        return random.nextInt();
    }

    @Benchmark
    public long Threads1_SyncSplitMix_nextLong() {
        return mixLong(state.getAndAdd(GOLDEN_GAMMA));
    }

    @Benchmark
    public int Threads1_SyncSplitMix_nextInt() {
        return mixInt(state.getAndAdd(GOLDEN_GAMMA));
    }

    @Benchmark
    public int Threads1_Sync_XoRoShiRo128Plus_nextInt() {
        return nextInt(xoRoShiRo128Plus);
    }

    @Benchmark
    public int Threads1_Sync_XorShift1024StarPhi_nextInt() {
        return nextInt(xorShift1024StarPhi);
    }

    @Benchmark
    public int Threads1_Sync_Well44497b_nextInt() {
        return nextInt(well44497b);
    }

    @Benchmark
    public long Threads1_Sync_XoRoShiRo128Plus_nextLong() {
        return nextLong(xoRoShiRo128Plus);
    }

    @Benchmark
    public long Threads1_Sync_XorShift1024StarPhi_nextLong() {
        return nextLong(xorShift1024StarPhi);
    }

    @Benchmark
    public long Threads1_Sync_Well44497b_nextLong() {
        return nextLong(well44497b);
    }

    @Benchmark
    public int Threads1_UnfairLock_XoRoShiRo128Plus_nextInt() {
        return nextInt(UNFAIR_LOCK, xoRoShiRo128Plus);
    }

    @Benchmark
    public int Threads1_UnfairLock_XorShift1024StarPhi_nextInt() {
        return nextInt(UNFAIR_LOCK, xorShift1024StarPhi);
    }

    @Benchmark
    public int Threads1_UnfairLock_Well44497b_nextInt() {
        return nextInt(UNFAIR_LOCK, well44497b);
    }

    @Benchmark
    public long Threads1_UnfairLock_XoRoShiRo128Plus_nextLong() {
        return nextLong(UNFAIR_LOCK, xoRoShiRo128Plus);
    }

    @Benchmark
    public long Threads1_UnfairLock_XorShift1024StarPhi_nextLong() {
        return nextLong(UNFAIR_LOCK, xorShift1024StarPhi);
    }

    @Benchmark
    public long Threads1_UnfairLock_Well44497b_nextLong() {
        return nextLong(UNFAIR_LOCK, well44497b);
    }

    @Benchmark
    public int Threads1_FairLock_XoRoShiRo128Plus_nextInt() {
        return nextInt(FAIR_LOCK, xoRoShiRo128Plus);
    }

    @Benchmark
    public int Threads1_FairLock_XorShift1024StarPhi_nextInt() {
        return nextInt(FAIR_LOCK, xorShift1024StarPhi);
    }

    @Benchmark
    public int Threads1_FairLock_Well44497b_nextInt() {
        return nextInt(FAIR_LOCK, well44497b);
    }

    @Benchmark
    public long Threads1_FairLock_XoRoShiRo128Plus_nextLong() {
        return nextLong(FAIR_LOCK, xoRoShiRo128Plus);
    }

    @Benchmark
    public long Threads1_FairLock_XorShift1024StarPhi_nextLong() {
        return nextLong(FAIR_LOCK, xorShift1024StarPhi);
    }

    @Benchmark
    public long Threads1_FairLock_Well44497b_nextLong() {
        return nextLong(FAIR_LOCK, well44497b);
    }

    @Benchmark
    public int Threads1_volatileInt_increment() {
        return ++volatileIntValue;
    }

    @Benchmark
    public long Threads1_volatileLong_increment() {
        return ++volatileLongValue;
    }

    @Benchmark
    public int Threads1_AtomicInt_getAndIncrement() {
        return atomicInt.getAndIncrement();
    }

    @Benchmark
    public long Threads1_AtomicLong_getAndIncrement() {
        return atomicLong.getAndIncrement();
    }

    @Benchmark
    @Threads(4)
    public int Threads4_SeedFactory_createInt() {
        return SeedFactory.createInt();
    }

    @Benchmark
    @Threads(4)
    public long Threads4_SeedFactory_createLong() {
        return SeedFactory.createLong();
    }

    @Benchmark
    @Threads(4)
    public long Threads4_System_currentTimeMillis() {
        // This may not be unique per call and is not random.
        return System.currentTimeMillis();
    }

    @Benchmark
    @Threads(4)
    public long Threads4_System_nanoTime() {
        // This is not random.
        return System.nanoTime();
    }

    @Benchmark
    @Threads(4)
    public int Threads4_System_identityHashCode() {
        return System.identityHashCode(new Object());
    }

    @Benchmark
    @Threads(4)
    public long Threads4_ThreadLocalRandom_nextLong() {
        return ThreadLocalRandom.current().nextLong();
    }

    @Benchmark
    @Threads(4)
    public int Threads4_ThreadLocalRandom_nextInt() {
        return ThreadLocalRandom.current().nextInt();
    }

    @Benchmark
    @Threads(4)
    public long Threads4_ThreadLocalRNG_nextLong() {
        return ThreadLocalRNG.current().nextLong();
    }

    @Benchmark
    @Threads(4)
    public int Threads4_ThreadLocalRNG_nextInt() {
        return ThreadLocalRNG.current().nextInt();
    }

    @Benchmark
    @Threads(4)
    public long Threads4_ThreadLocalSplitMix_nextLong() {
        return ThreadLocalSplitMix.current().nextLong();
    }

    @Benchmark
    @Threads(4)
    public int Threads4_ThreadLocalSplitMix_nextInt() {
        return ThreadLocalSplitMix.current().nextInt();
    }

    @Benchmark
    @Threads(4)
    public long Threads4_ThreadLocalSequenceMix_nextLong() {
        return mixLong(ThreadLocalSequence.next());
    }

    @Benchmark
    @Threads(4)
    public int Threads4_ThreadLocalSequenceMix_nextInt() {
        return mixInt(ThreadLocalSequence.next());
    }

    @Benchmark
    @Threads(4)
    public long Threads4_Random_nextLong() {
        return random.nextLong();
    }

    @Benchmark
    @Threads(4)
    public int Threads4_Random_nextInt() {
        return random.nextInt();
    }

    @Benchmark
    @Threads(4)
    public long Threads4_SyncSplitMix_nextLong() {
        return mixLong(state.getAndAdd(GOLDEN_GAMMA));
    }

    @Benchmark
    @Threads(4)
    public int Threads4_SyncSplitMix_nextInt() {
        return mixInt(state.getAndAdd(GOLDEN_GAMMA));
    }

    @Benchmark
    @Threads(4)
    public int Threads4_Sync_XoRoShiRo128Plus_nextInt() {
        return nextInt(xoRoShiRo128Plus);
    }

    @Benchmark
    @Threads(4)
    public int Threads4_Sync_XorShift1024StarPhi_nextInt() {
        return nextInt(xorShift1024StarPhi);
    }

    @Benchmark
    @Threads(4)
    public int Threads4_Sync_Well44497b_nextInt() {
        return nextInt(well44497b);
    }

    @Benchmark
    @Threads(4)
    public long Threads4_Sync_XoRoShiRo128Plus_nextLong() {
        return nextLong(xoRoShiRo128Plus);
    }

    @Benchmark
    @Threads(4)
    public long Threads4_Sync_XorShift1024StarPhi_nextLong() {
        return nextLong(xorShift1024StarPhi);
    }

    @Benchmark
    @Threads(4)
    public long Threads4_Sync_Well44497b_nextLong() {
        return nextLong(well44497b);
    }

    @Benchmark
    @Threads(4)
    public int Threads4_UnfairLock_XoRoShiRo128Plus_nextInt() {
        return nextInt(UNFAIR_LOCK, xoRoShiRo128Plus);
    }

    @Benchmark
    @Threads(4)
    public int Threads4_UnfairLock_XorShift1024StarPhi_nextInt() {
        return nextInt(UNFAIR_LOCK, xorShift1024StarPhi);
    }

    @Benchmark
    @Threads(4)
    public int Threads4_UnfairLock_Well44497b_nextInt() {
        return nextInt(UNFAIR_LOCK, well44497b);
    }

    @Benchmark
    @Threads(4)
    public long Threads4_UnfairLock_XoRoShiRo128Plus_nextLong() {
        return nextLong(UNFAIR_LOCK, xoRoShiRo128Plus);
    }

    @Benchmark
    @Threads(4)
    public long Threads4_UnfairLock_XorShift1024StarPhi_nextLong() {
        return nextLong(UNFAIR_LOCK, xorShift1024StarPhi);
    }

    @Benchmark
    @Threads(4)
    public long Threads4_UnfairLock_Well44497b_nextLong() {
        return nextLong(UNFAIR_LOCK, well44497b);
    }

    @Benchmark
    @Threads(4)
    public int Threads4_FairLock_XoRoShiRo128Plus_nextInt() {
        return nextInt(FAIR_LOCK, xoRoShiRo128Plus);
    }

    @Benchmark
    @Threads(4)
    public int Threads4_FairLock_XorShift1024StarPhi_nextInt() {
        return nextInt(FAIR_LOCK, xorShift1024StarPhi);
    }

    @Benchmark
    @Threads(4)
    public int Threads4_FairLock_Well44497b_nextInt() {
        return nextInt(FAIR_LOCK, well44497b);
    }

    @Benchmark
    @Threads(4)
    public long Threads4_FairLock_XoRoShiRo128Plus_nextLong() {
        return nextLong(FAIR_LOCK, xoRoShiRo128Plus);
    }

    @Benchmark
    @Threads(4)
    public long Threads4_FairLock_XorShift1024StarPhi_nextLong() {
        return nextLong(FAIR_LOCK, xorShift1024StarPhi);
    }

    @Benchmark
    @Threads(4)
    public long Threads4_FairLock_Well44497b_nextLong() {
        return nextLong(FAIR_LOCK, well44497b);
    }

    @Benchmark
    @Threads(4)
    public int Threads4_volatileInt_increment() {
        return ++volatileIntValue;
    }

    @Benchmark
    @Threads(4)
    public long Threads4_volatileLong_increment() {
        return ++volatileLongValue;
    }

    @Benchmark
    @Threads(4)
    public int Threads4_AtomicInt_getAndIncrement() {
        return atomicInt.getAndIncrement();
    }

    @Benchmark
    @Threads(4)
    public long Threads4_AtomicLong_getAndIncrement() {
        return atomicLong.getAndIncrement();
    }
}
