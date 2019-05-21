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

import org.apache.commons.rng.simple.RandomSource;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.Iterator;
import java.util.List;

/**
 * Default list of generators defined by the {@link RandomSource}. Any source that
 * requires arguments will have a default set of arguments.
 */
class StressTestDataList implements Iterable<StressTestData> {
    /**
     * The example arguments for RandomSource values that require them.
     */
    private static final EnumMap<RandomSource, Object[]> EXAMPLE_ARGUMENTS =
            new EnumMap<>(RandomSource.class);

    static {
        // Currently we cannot detect if the source requires arguments,
        // e.g. RandomSource.TWO_CMRES_SELECT. So example arguments must
        // be manually added here.
        EXAMPLE_ARGUMENTS.put(RandomSource.TWO_CMRES_SELECT, new Object[] {1, 2});
    }

    /** List of generators. */
    private final List<StressTestData> list = new ArrayList<>();

    /**
     * Creates the list with the number of trials set to 1.
     */
    StressTestDataList() {
        this("", 1);
    }

    /**
     * Creates the list. The number of trials will be set if the source does not require arguments.
     *
     * @param idPrefix The id prefix prepended to each ID.
     * @param numberOfTrials The number of trials (ignored if {@code <= 0}).
     */
    StressTestDataList(String idPrefix,
                       int numberOfTrials) {
        // Auto-generate using the order of the RandomSource enum
        for (final RandomSource source : RandomSource.values()) {
            final Object[] args = EXAMPLE_ARGUMENTS.get(source);
            StressTestData data = new StressTestData(source, args).withIDPrefix(idPrefix);
            if (args == null && numberOfTrials > 0) {
                data = data.withTrials(numberOfTrials);
            }
            list.add(data);
        }
    }

    /** {@inheritDoc} */
    @Override
    public Iterator<StressTestData> iterator() {
        return list.iterator();
    }
}
