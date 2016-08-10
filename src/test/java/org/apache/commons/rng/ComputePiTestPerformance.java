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
package org.apache.commons.rng;

import org.openjdk.jmh.annotations.*;
import java.util.concurrent.TimeUnit;

/**
 * Benchmark that compares the performance of the RNG implementations
 * by running a simple workload: computation of \( \pi \).
 *
 * The computation estimates the value by computing the probability that
 * a point \( p = (x, y) \) will lie in the circle of radius \( r = 1 \)
 * inscribed in the square of side \( r = 1 \).
 * The probability could be computed by \[ area_{circle} / area_{square} \],
 * where \( area_{circle} = \pi * r^2 \) and \( area_{square} = 4 r^2 \).
 * Hence, the probability is \( \frac{\pi}{4} \).
 *
 * The Monte Carlo simulation will produce \( N \) points.
 * Defining \( N_c \) as the number of point that satisfy \( x^2 + y^2 <= 1 \),
 * we will have \( \frac{N_c}{N} \approx \frac{\pi}{4} \).
 */
@BenchmarkMode(Mode.AverageTime)
@OutputTimeUnit(TimeUnit.MICROSECONDS)
@Warmup(iterations = 5, time = 1, timeUnit = TimeUnit.SECONDS)
@Measurement(iterations = 5, time = 1, timeUnit = TimeUnit.SECONDS)
@Fork(1)
@State(Scope.Benchmark)
public class ComputePiTestPerformance extends AbstractTestPerformance {
    /**
     * Number of 2D-points to generate.
     */
    @Param({"5000000"})
    long numPoints;

    @Benchmark
    public double computePi(Sources data) {
        long numPointsInCircle = 0;
        for (int i = 0; i < numPoints; i++) {
            final double x = data.provider.nextDouble();
            final double y = data.provider.nextDouble();
            final double r2 = x * x + y * y;
            if (r2 <= 1) {
                ++numPointsInCircle;
            }
        }

        final double pi = 4 * numPointsInCircle / (double) numPoints;
        System.out.println("pi=" + pi);
        return pi;
    }
}
