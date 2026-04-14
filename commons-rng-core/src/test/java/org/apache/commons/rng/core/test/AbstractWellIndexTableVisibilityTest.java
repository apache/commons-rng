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

package org.apache.commons.rng.core.test;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.apache.commons.rng.core.source32.AbstractWell;
import org.junit.jupiter.api.Test;

/**
 * Checks that the visibility of the protected members of {@link AbstractWell} is correct.
 * <p>
 * See <a href="https://github.com/pmd/pmd/issues/6477">PMD issue 6477</a>.
 * </p>
 */
class AbstractWellIndexTableVisibilityTest {

    static class TestWell extends AbstractWell {

        private static final IndexTable TABLE = new IndexTable(1, 0, 0, 0);

        protected TestWell(final int k, final int[] seed) {
            super(k, seed);
        }

        @Override
        public int next() {
            return TABLE.getIndexPred(index) + TABLE.getIndexM1(index) + TABLE.getIndexM2(index) + TABLE.getIndexM3(index);
        }
    }

    @Test
    void test() {
        assertEquals(0, new TestWell(1, new int[1]).next());
    }
}
