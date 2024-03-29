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
package org.apache.commons.rng.core.util;

import org.apache.commons.rng.UniformRandomProvider;

/**
 * Test helper class to expose package-private functionality for tests in other packages.
 */
public final class RandomStreamsTestHelper {

    /** No instances. */
    private RandomStreamsTestHelper() {}

    /**
     * Creates a seed to prepend to a counter. This method makes public the package-private
     * seed generation method used in {@link RandomStreams} for test classes in other packages.
     *
     * @param rng Source of randomness.
     * @return the seed
     */
    public static long createSeed(UniformRandomProvider rng) {
        return RandomStreams.createSeed(rng);
    }
}
