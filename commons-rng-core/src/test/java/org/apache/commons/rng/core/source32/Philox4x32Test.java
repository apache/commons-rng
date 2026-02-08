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

import org.apache.commons.rng.UniformRandomProvider;
import org.apache.commons.rng.core.RandomAssert;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class Philox4x32Test {

    /*
     * Data from python randomgen.philox.Philox(key=1234,number=4,width=32) random_raw()
     * https://bashtage.github.io/randomgen/bit_generators/philox.html
     */

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
        -851136091, 1309410510, -1085986073, -2011015294, 1141542412,
        1418494107, -1747451871, -1055627323, -146194734, 1282520890,
        -1352853386, 1006181297, -1439198278, 1236883457, 492325190,
        1314792982, 532544947, 1080385192, -2075089806, 1956500098,
        -1226283606, 1100040044, -1227122850, -565515005, -851592399,
        -2140061922, 138067050, 1387279196, 1163016478, -26858470,
        -1462800132, 484042867, -1872158237, -300782320, -1425836673,
        830088801, 1808637392, 1353273018, 660570244, -1956528645
    };

    private static final int[] EXPECTED_SEQUENCE_AFTER_LONG_JUMP = {
        -1941342745, 535234737, -1560986946, 1333403881, -467630828,
        -1212243215, 1924495835, 1889500660, 118588722, -444471278,
        -984974572, 2134204567, 620921081, -929199568, -44345645,
        -346841340, -557091335, 1023562906, -1544843001, 2014718360,
        -186712859, -874952234, -1016908504, 953606755, -1406346322,
        -1297454974, 1426742334, 1461035068, 206733349, 1606578263,
        -1354963004, -604654637, 782017623, 1501746828, 853947605,
        -1380277812, 1855551741, -1023933348, -635058958, 1752530776
    };

    @Test
    void testReferenceCode() {
        RandomAssert.assertEquals(EXPECTED_SEQUENCE_1234, new Philox4x32(1234L, 0, 0));
    }

    @Test
    void testReferenceCodeDefaultSeed() {
        RandomAssert.assertEquals(EXPECTED_SEQUENCE_DEFAULT, new Philox4x32(67280421310721L, 0, 0));
    }

    @Test
    void testConstructors() {
        Philox4x32[] rngs = new Philox4x32[]{
            new Philox4x32(),
            new Philox4x32(67280421310721L),
            new Philox4x32(67280421310721L, 0, 0),
            new Philox4x32(new int[]{(int) 67280421310721L, (int) (67280421310721L >>> 32), 0, 0, 0, 0})
        };
        int refValue = rngs[0].next();
        for (int i = 1; i < rngs.length; i++) {
            int value = rngs[i].next();
            assertEquals(refValue, value, "Philox4x32 initialization for i=" + i);
        }
        rngs = new Philox4x32[]{
            new Philox4x32(1234L, 1, 1),
            new Philox4x32(1234, 0, 1, 0, 1),
            new Philox4x32(1234, 0, 1, 0, 1, 0),
        };
        refValue = rngs[0].next();
        for (int i = 1; i < rngs.length; i++) {
            int value = rngs[i].next();
            assertEquals(refValue, value, "Philox4x32 initialization for i=" + i);
        }
        rngs = new Philox4x32[]{
            new Philox4x32(1234L),
            new Philox4x32(1234L, 0, 0),
            new Philox4x32(1234),
            new Philox4x32(new int[]{1234}),
            new Philox4x32(1234, 0),
            new Philox4x32(1234, 0, 0),
            new Philox4x32(1234, 0, 0, 0),
            new Philox4x32(1234, 0, 0, 0, 0),
            new Philox4x32(1234, 0, 0, 0, 0, 0),
            new Philox4x32(1234, 0, 0, 0, 0, 0, 0),
        };
        refValue = rngs[0].next();
        for (int i = 1; i < rngs.length; i++) {
            int value = rngs[i].next();
            assertEquals(refValue, value, "Philox4x32 initialization for i=" + i);
        }
    }

    @Test
    void testJump() {
        RandomAssert.assertJumpEquals(EXPECTED_SEQUENCE_DEFAULT, EXPECTED_SEQUENCE_AFTER_JUMP, new Philox4x32(67280421310721L, 0, 0));
    }

    @Test
    void testLongJump() {
        RandomAssert.assertLongJumpEquals(EXPECTED_SEQUENCE_DEFAULT, EXPECTED_SEQUENCE_AFTER_LONG_JUMP, new Philox4x32(67280421310721L, 0, 0));
    }

    @Test
    void testOffset() {
        Philox4x32 rng = new Philox4x32(67280421310721L, 0, 1);
        assertEquals(1, rng.getOffset());
        assertEquals(EXPECTED_SEQUENCE_DEFAULT[4], rng.nextInt());
        rng = new Philox4x32(67280421310721L, 0, 1L << 32);
        assertEquals(1L << 32, rng.getOffset());
        rng.setOffset(0);
        assertEquals(EXPECTED_SEQUENCE_DEFAULT[0], rng.nextInt());
    }

    @Test
    void testDouble() {
        Philox4x32 rng = new Philox4x32();
        double valueOpen = rng.nextDoubleOpen();
        rng.setOffset(0);
        double value = rng.nextDouble();
        assertEquals(value, valueOpen, Math.pow(2, -20)); //will differ after 20bits
    }

    @Test
    void testReset() {
        Philox4x32 rng = new Philox4x32(67280421310721L, 0, 1);
        assertEquals(EXPECTED_SEQUENCE_DEFAULT[4], rng.nextInt());
        rng.resetState(1234L, 0);
        assertEquals(EXPECTED_SEQUENCE_1234[0], rng.nextInt());
    }

    @Test
    void testInternalCounter() {
        //test of incrementCounter
        Philox4x32 rng = new Philox4x32(67280421310721L, 0, (1L << 32) - 1);
        for (int i = 0; i < 4; i++) {
            rng.next();
        }
        long value = rng.next();
        Philox4x32 rng2 = new Philox4x32(67280421310721L, 0, 1L << 32);
        long value2 = rng2.next();
        assertEquals(value, value2);

        rng = new Philox4x32(67280421310721L, 0, 0xffffffffffffffffL);
        for (int i = 0; i < 4; i++) {
            rng.next();
        }
        value = rng.next();
        rng2 = new Philox4x32(67280421310721L, 1, 0);
        value2 = rng2.next();
        assertEquals(value, value2);

        rng = new Philox4x32(67280421310721L, (1L << 32) - 1, 0xffffffffffffffffL);
        for (int i = 0; i < 4; i++) {
            rng.next();
        }
        value = rng.next();
        rng2 = new Philox4x32(67280421310721L, 1L << 32, 0);
        value2 = rng2.next();
        assertEquals(value, value2);
    }

    @Test
    void testJumpCounter() {
        Philox4x32[] rngs = new Philox4x32[]{
            new Philox4x32(67280421310721L, 0, (1L << 32) - 1),
            new Philox4x32(67280421310721L, 0, 0xffffffffffffffffL),
            new Philox4x32(67280421310721L, (1L << 32) - 1, 0xffffffffffffffffL)
        };
        for (Philox4x32 rng : rngs) {
            UniformRandomProvider rngOrig = rng.jump(10);
            long value = rng.nextInt();
            for (int i = 0; i < 10; i++) {
                rngOrig.nextInt();
            }
            long value2 = rngOrig.nextInt();
            assertEquals(value2, value);
        }
    }

    @Test
    void testLongJumpCounter() {
        Philox4x32 rng = new Philox4x32(1234L, 0xffffffffL, 0xffffffffffffffffL);
        UniformRandomProvider rngOrig = rng.longJump();
        long value = rng.nextInt();
        Philox4x32 rng2 = new Philox4x32(1234L, 1L << 32, 0xffffffffffffffffL);
        long value2 = rng2.nextInt();
        assertEquals(value2, value);
    }

    @Test
    void testArbitraryJumps() {
        XoShiRo128PlusPlus rngForSkip = new XoShiRo128PlusPlus(10, 20, 30, 40);
        Philox4x32 rng = new Philox4x32();
        for (int i = 0; i < 5120; i++) {
            int n = rngForSkip.nextInt() >>> 23;
            UniformRandomProvider rngOrig = rng.jump(n);
            int jumpValue = rng.next();
            for (int k = 0; k < n; k++) {
                rngOrig.nextInt();
            }
            int origValue = rngOrig.nextInt();
            assertEquals(origValue, jumpValue);
        }


    }
}
