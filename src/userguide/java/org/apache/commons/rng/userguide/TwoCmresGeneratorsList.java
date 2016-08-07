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
package org.apache.commons.math4.userguide.rng;

import java.util.List;
import java.util.ArrayList;
import java.util.Iterator;

import org.apache.commons.math4.util.Combinations;
import org.apache.commons.rng.UniformRandomProvider;
import org.apache.commons.rng.RandomSource;
import org.apache.commons.rng.internal.source64.TwoCmres;

/**
 * All the {@link TwoCmres} generators.
 */
public class TwoCmresGeneratorsList implements Iterable<UniformRandomProvider> {
    /** List. */
    private final List<UniformRandomProvider> list = new ArrayList<>();

    /**
     * Creates list.
     */
    public TwoCmresGeneratorsList() {
        final int n = RandomSource.numberOfCmresGenerators();
        for (int[] indices : new Combinations(n, 2)) {
            // Combining the two subcycle generators in the opposite order
            // will result in a different RNG.
            list.add(RandomSource.create(RandomSource.TWO_CMRES_SELECT, null,
                                         indices[0], indices[1]));
            list.add(RandomSource.create(RandomSource.TWO_CMRES_SELECT, null,
                                         indices[1], indices[0]));
        }
    }

    /** {@inheritDoc} */
    @Override
    public Iterator<UniformRandomProvider> iterator() {
        return list.iterator();
    }
}
