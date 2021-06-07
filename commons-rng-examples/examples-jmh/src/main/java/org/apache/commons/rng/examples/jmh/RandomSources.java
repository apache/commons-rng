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
import org.openjdk.jmh.annotations.Scope;
import org.openjdk.jmh.annotations.Setup;
import org.openjdk.jmh.annotations.State;

/**
 * A benchmark state that can retrieve the various generators defined by
 * {@link org.apache.commons.rng.simple.RandomSource RandomSource} values.
 *
 * <p>The state will include only those that do not require additional constructor arguments.</p>
 */
@State(Scope.Benchmark)
public class RandomSources extends RandomSourceValues {
    /** RNG. */
    private UniformRandomProvider generator;

    /**
     * Gets the generator.
     *
     * @return the RNG.
     */
    public UniformRandomProvider getGenerator() {
        return generator;
    }

    /**
     * Look-up the {@link org.apache.commons.rng.simple.RandomSource RandomSource} from the
     * name and instantiates the generator.
     */
    @Override
    @Setup
    public void setup() {
        super.setup();
        generator = getRandomSource().create();
    }
}
