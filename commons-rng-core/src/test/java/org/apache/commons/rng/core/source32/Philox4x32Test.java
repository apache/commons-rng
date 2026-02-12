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
package org.apache.commons.rng.core.source32;

import java.util.stream.Stream;
import org.apache.commons.rng.core.RandomAssert;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

public class Philox4x32Test {
    // Data from python randomgen.philox.Philox(key=1234,number=4,width=32) random_raw()
    // https://bashtage.github.io/randomgen/bit_generators/philox.html

    private static final int[] EXPECTED_SEQUENCE_1234 = {
        -1628512715, 482218876, -98078573, 343858512, 1070188760,
        -66651592, -870905049, -1994573039, -1238984130, 599211371,
        1926069095, -394512546, 346514135, -352142790, 196394741,
        -107436867, -903274039, 860026475, -1309487194, -1778049224,
        -49503714, -1441076994, -866074276, -1339523817, -1290919251,
        1857369626, -1839251177, -2041498882, -1956330288, 905306810,
        -2114083635, 200746399, 20291031, 214040874, -1628891823,
        -1958807646, 9198301, -1607720479, -1349496224, 1418271217
    };

    private static final int[] EXPECTED_SEQUENCE_DEFAULT = {
        623720234, -686991347, 358698524, 234508473, 1303720625,
        1235930736, -75297729, 110380616, 829652807, -1101240720,
        -1443748750, -1366075136, -1702811520, 232450464, 350957237,
        1425642103, 256542391, 1837662153, -448554748, 637025846,
        -902021712, 1085962074, -1391041963, 201580325, 1416828610,
        599210676, -628463662, -576572235, 457140358, -1026551805,
        -917125498, 529387774, 1254882949, 1278069784, 724938314,
        -4044975, -1211844829, -198846304, 286548119, 2085574084
    };

    private static final int[] EXPECTED_SEQUENCE_AFTER_JUMP = {
        -1941342745, 535234737, -1560986946, 1333403881, -467630828,
        -1212243215, 1924495835, 1889500660, 118588722, -444471278,
        -984974572, 2134204567, 620921081, -929199568, -44345645,
        -346841340, -557091335, 1023562906, -1544843001, 2014718360,
        -186712859, -874952234, -1016908504, 953606755, -1406346322,
        -1297454974, 1426742334, 1461035068, 206733349, 1606578263,
        -1354963004, -604654637, 782017623, 1501746828, 853947605,
        -1380277812, 1855551741, -1023933348, -635058958, 1752530776
    };

    private static final int[] EXPECTED_SEQUENCE_AFTER_LONG_JUMP = {
        -643973534, -1464631510, -1204127809, 380399830, 1336312468,
        862647039, -970571153, -1473390944, 811398823, -598244991,
        -1474151641, -1228756553, -166611808, -231601273, -2055417682,
        -1102476522, 1497124960, 438167652, -657449781, -404513325,
        -621271837, -10198296, -267651022, -296539606, -1564719261,
        -652626768, -973911394, 1388361366, 1675611708, -1270745165,
        -620748722, -1569788343, 831908952, 1873081673, -1058521087,
        -26171115, -1211556401, -65210719, -1194284085, 1579466740
    };

    /**
     * Gets a stream of reference data. Each argument consists of the seed as a long (first two ints),
     * and the int array of the expected output from the generator.
     *
     * @return the reference data
     */
    static Stream<Arguments> getReferenceData() {
        return Stream.of(
            Arguments.of(1234L, EXPECTED_SEQUENCE_1234),
            Arguments.of(67280421310721L, EXPECTED_SEQUENCE_DEFAULT)
        );
    }

    @ParameterizedTest
    @MethodSource(value = "getReferenceData")
    void testReferenceCode(long seed, int[] expected) {
        RandomAssert.assertEquals(expected, new Philox4x32(new int[]{(int) seed, (int) (seed >>> 32)}));
    }

    @Test
    void testConstructors() {
        Philox4x32[] rngs = new Philox4x32[]{
            new Philox4x32(),
            new Philox4x32(new int[]{(int) 67280421310721L, (int) (67280421310721L >>> 32), 0, 0, 0, 0})
        };
        int refValue = rngs[0].next();
        for (int i = 1; i < rngs.length; i++) {
            int value = rngs[i].next();
            Assertions.assertEquals(refValue, value, "Philox4x32 initialization for i=" + i);
        }
        rngs = new Philox4x32[]{
            new Philox4x32(new int[] {1234, 0, 1}),
            new Philox4x32(new int[] {1234, 0, 1, 0, 0, 0}),
        };
        refValue = rngs[0].next();
        for (int i = 1; i < rngs.length; i++) {
            int value = rngs[i].next();
            Assertions.assertEquals(refValue, value, "Philox4x32 initialization for i=" + i);
        }
        rngs = new Philox4x32[]{
            new Philox4x32(new int[] {1234}),
            new Philox4x32(new int[] {1234, 0}),
            new Philox4x32(new int[] {1234, 0, 0}),
            new Philox4x32(new int[] {1234, 0, 0, 0}),
            new Philox4x32(new int[] {1234, 0, 0, 0, 0}),
            new Philox4x32(new int[] {1234, 0, 0, 0, 0, 0}),
            new Philox4x32(new int[] {1234, 0, 0, 0, 0, 0, 0}),
        };
        refValue = rngs[0].next();
        for (int i = 1; i < rngs.length; i++) {
            int value = rngs[i].next();
            Assertions.assertEquals(refValue, value, "Philox4x32 initialization for i=" + i);
        }
    }

    @Test
    void testJump() {
        RandomAssert.assertJumpEquals(EXPECTED_SEQUENCE_DEFAULT, EXPECTED_SEQUENCE_AFTER_JUMP,
            new Philox4x32());
    }

    @Test
    void testLongJump() {
        RandomAssert.assertLongJumpEquals(EXPECTED_SEQUENCE_DEFAULT, EXPECTED_SEQUENCE_AFTER_LONG_JUMP,
            new Philox4x32());
    }

    @Test
    void testInternalCounter() {
        // Test of counter increment. Note that the value of -1 is all bits set and incrementing
        // will carry a 1-bit to the next counter up.
        final int key0 = (int) 67280421310721L;
        final int key1 = (int) (67280421310721L >>> 32);

        Philox4x32 rng = new Philox4x32(new int[] {key0, key1, -1, 0, 0, 0});
        for (int i = 0; i < 4; i++) {
            rng.next();
        }
        Philox4x32 rng2 = new Philox4x32(new int[] {key0, key1, 0, 1, 0, 0});
        RandomAssert.assertNextIntEquals(10, rng, rng2);

        rng = new Philox4x32(new int[] {key0, key1, -1, -1, 0, 0});
        for (int i = 0; i < 4; i++) {
            rng.next();
        }
        rng2 = new Philox4x32(new int[] {key0, key1, 0, 0, 1, 0});
        RandomAssert.assertNextIntEquals(10, rng, rng2);

        rng = new Philox4x32(new int[] {key0, key1, -1, -1, -1, 0});
        for (int i = 0; i < 4; i++) {
            rng.next();
        }
        rng2 = new Philox4x32(new int[] {key0, key1, 0, 0, 0, 1});
        RandomAssert.assertNextIntEquals(10, rng, rng2);
    }

    @Test
    void testJumpCounter() {
        Philox4x32 rng = new Philox4x32(new int[] {1234, 0, -1, 0, -1, 0});
        rng.jump();
        Philox4x32 rng2 = new Philox4x32(new int[] {1234, 0, -1, 0, 0, 1});
        RandomAssert.assertNextIntEquals(10, rng, rng2);

        rng = new Philox4x32(new int[] {1234, 0, -1, -1, -1, 0});
        rng.jump();
        rng2 = new Philox4x32(new int[] {1234, 0, -1, -1, 0, 1});
        RandomAssert.assertNextLongEquals(10, rng, rng2);
    }

    @Test
    void testLongJumpCounter() {
        Philox4x32 rng = new Philox4x32(new int[] {1234, 0, -1, -1, -1, 0});
        rng.longJump();
        Philox4x32 rng2 = new Philox4x32(new int[] {1234, 0, -1, -1, -1, 1});
        RandomAssert.assertNextIntEquals(10, rng, rng2);
    }
}
