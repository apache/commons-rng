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

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.Arrays;
import java.util.SplittableRandom;
import java.util.stream.Stream;
import java.util.stream.Stream.Builder;
import org.apache.commons.rng.core.RandomAssert;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.ValueSource;

public class Philox4x32Test {
    // Data from python 3.12.12 using randomgen v2.3.0, e.g.
    // from randomgen import Philox
    // import numpy as np
    // Philox(number=4,width=32).random_raw(10)
    // Note that use of 'advance' for jumping moves the underlying RNG state which is the counter.
    // See: https://bashtage.github.io/randomgen/bit_generators/philox.html

    private static final int[][] SEEDS = {
        {1234},
        {0, 0, 1234},
        {(int) 67280421310721L, (int) (67280421310721L >>> 32)},
        {123, 456, 789, 10, 11, 12},
    };

    private static final int[][] EXPECTED_SEQUENCE = {
        // Philox(number=4, width=32, key=1234).random_raw(40).astype('int32')
        {-1628512715, 482218876, -98078573, 343858512, 1070188760,
         -66651592, -870905049, -1994573039, -1238984130, 599211371,
         1926069095, -394512546, 346514135, -352142790, 196394741,
         -107436867, -903274039, 860026475, -1309487194, -1778049224,
         -49503714, -1441076994, -866074276, -1339523817, -1290919251,
         1857369626, -1839251177, -2041498882, -1956330288, 905306810,
         -2114083635, 200746399, 20291031, 214040874, -1628891823,
         -1958807646, 9198301, -1607720479, -1349496224, 1418271217},
        // Philox(number=4, width=32, key=0, counter=1234).random_raw(40).astype('int32')
        {1588178667, -15343145, -1762892058, 147865742, 1527647189,
         -1146912951, 789604719, -1073485892, 745262311, 63585623,
         -1445638242, 652080414, -757630012, 1163173032, -1203855035,
         1460101531, 2107195747, 682429934, -1965793769, -1029488275,
         -1818981590, 1468888062, -2135981593, -1921442767, 1686674465,
         967757176, 409355582, -16041811, 1838102384, 501839319,
         331061676, 867578402, 1339237696, 1936782006, -462263710,
         -106326927, -2043771088, 1984717264, -1223654577, 76757069},
        // Philox(number=4, width=32, key=67280421310721).random_raw(40).astype('int32')
        {623720234, -686991347, 358698524, 234508473, 1303720625,
         1235930736, -75297729, 110380616, 829652807, -1101240720,
         -1443748750, -1366075136, -1702811520, 232450464, 350957237,
         1425642103, 256542391, 1837662153, -448554748, 637025846,
         -902021712, 1085962074, -1391041963, 201580325, 1416828610,
         599210676, -628463662, -576572235, 457140358, -1026551805,
         -917125498, 529387774, 1254882949, 1278069784, 724938314,
         -4044975, -1211844829, -198846304, 286548119, 2085574084},
        // Philox(number=4, width=32, key=(456 << 32) + 123, counter=np.array([(10 << 32) + 789, (12 << 32) + 11]).astype('uint64')).random_raw(40).astype('int32')
        {-535170951, 275221793, 1433076906, -205211198, -1380267964,
         139940474, -1810475080, -308969699, -647683762, -699647401,
         482740232, 1777104922, -1173526227, -1207223504, 497855376,
         1896493753, 845895107, 1967908077, 656573128, -1121130179,
         -1902565320, -1205382398, -2009155663, 1168299691, -1541028015,
         -1304344170, -960078080, 107782008, 1148958317, 796082912,
         874119447, 1769678880, 52620189, 1756670656, -700354866,
         953356383, -1452577468, -729552223, 535835908, 1924953862},
    };

    private static final int[][] EXPECTED_SEQUENCE_AFTER_JUMP = {
        // Philox(number=4, width=32, key=1234).advance(2**64).random_raw(40).astype('int32')
        {118660703, 1698527859, 495137217, -2030106961, 738435355,
         1259184545, 1723463268, -1662013072, 693635153, -476069467,
         -822428670, 387892547, 131340545, -1533289666, -43205117,
         429431617, 1449020292, 1693470789, 1141686666, -1736413862,
         -801039137, 316527407, -1680325655, -931170191, 2077601348,
         -1507675802, 72525406, 1240002720, 1771112641, 1455653820,
         -909323108, 1671557492, 286297822, -1686103305, -877915529,
         -1184900472, 1506222466, -1951595556, -855367498, 1047388114},
        // Philox(number=4, width=32, key=0, counter=1234).advance(2**64).random_raw(40).astype('int32')
        {-202845880, -2120641957, -1014238235, -238991547, -480275352,
         210505187, 15325843, 283385090, 248911406, 176875505,
         -1511130127, 1565090857, 1145917045, -672548942, -1502592224,
         -106688787, 1468487700, -1771715173, 200975693, 2096506440,
         284093911, -1939531792, -515368252, 906130442, 907908254,
         -1403308500, 489380872, 255776250, -2095013438, 2061616737,
         2035255236, 158457620, 1662832094, -1248384350, -454215131,
         848809645, 433384569, 418361004, -157814135, -1229591251},
        // Philox(number=4, width=32, key=67280421310721).advance(2**64).random_raw(40).astype('int32')
        {-1941342745, 535234737, -1560986946, 1333403881, -467630828,
         -1212243215, 1924495835, 1889500660, 118588722, -444471278,
         -984974572, 2134204567, 620921081, -929199568, -44345645,
         -346841340, -557091335, 1023562906, -1544843001, 2014718360,
         -186712859, -874952234, -1016908504, 953606755, -1406346322,
         -1297454974, 1426742334, 1461035068, 206733349, 1606578263,
         -1354963004, -604654637, 782017623, 1501746828, 853947605,
         -1380277812, 1855551741, -1023933348, -635058958, 1752530776},
        // Philox(number=4, width=32, key=(456 << 32) + 123, counter=np.array([(10 << 32) + 789, (12 << 32) + 11]).astype('uint64')).advance(2**64).random_raw(40).astype('int32')
        {-316352649, 868560207, 431734875, -1087054321, -1344689335,
         2053255317, 317869638, 1990916806, -864678526, -654290538,
         -798562114, 1528742874, 789468240, 756464845, 1677786346,
         848692121, -1815362412, 182965956, 321615886, -1759726032,
         1921289363, -1966064280, -1524974766, -360366215, -1667726491,
         -535699271, -2013106405, 448724077, -188930586, 1038028606,
         -1764258924, 503038703, 310666992, 1212567665, -1268006326,
         864557636, -527414345, -1058566187, 156134420, -1779203194},
    };

    private static final int[][] EXPECTED_SEQUENCE_AFTER_LONG_JUMP = {
        // Philox(number=4, width=32, key=1234).advance(2**96).random_raw(40).astype('int32')
        {247815469, -898687275, 1927646008, -362363215, 1715186570,
         1181602348, 1288059292, 1337451713, -1446555076, 558469158,
         -2072418582, -711858372, 819909767, -2014710640, 707313251,
         772897177, -1262673472, -1981664054, 202641230, 1001438663,
         -1178694679, -220584936, -1858839822, -127670915, -1527920649,
         1701538950, -713233322, -1287840322, 647894020, 354455577,
         953048473, -1569501537, -1609890138, 1366014642, -1744199145,
         46811262, -2144924080, -1911337000, 1384171275, 1424730314},
        // Philox(number=4, width=32, key=0, counter=1234).advance(2**96).random_raw(40).astype('int32')
        {-1621648791, 504123868, 861944862, 1672079932, -200211361,
         -1651181668, -822389236, 1055058382, 2100396499, -767444323,
         1009742051, 1946749259, -707543707, -1030924289, 1189105052,
         1675443950, -437748219, -1158769627, 2083376849, -1122629944,
         596119414, 2022226652, 1878507662, 817772381, 1775413259,
         784623945, -1600776731, -112169058, -182371908, -2089748839,
         1528353932, 1126796033, 2103671040, 1032802689, 514361239,
         -34902671, 1060958996, 730982344, -1710894636, -1052673728},
        // Philox(number=4, width=32, key=67280421310721).advance(2**96).random_raw(40).astype('int32')
        {-643973534, -1464631510, -1204127809, 380399830, 1336312468,
         862647039, -970571153, -1473390944, 811398823, -598244991,
         -1474151641, -1228756553, -166611808, -231601273, -2055417682,
         -1102476522, 1497124960, 438167652, -657449781, -404513325,
         -621271837, -10198296, -267651022, -296539606, -1564719261,
         -652626768, -973911394, 1388361366, 1675611708, -1270745165,
         -620748722, -1569788343, 831908952, 1873081673, -1058521087,
         -26171115, -1211556401, -65210719, -1194284085, 1579466740},
        // Philox(number=4, width=32, key=(456 << 32) + 123, counter=np.array([(10 << 32) + 789, (12 << 32) + 11]).astype('uint64')).advance(2**96).random_raw(40).astype('int32')
        {-1408819093, -2081321244, 843933313, 530668732, -707468793,
         1287510074, 1564579137, 929320152, -1398781210, 491867722,
         967481241, -129228520, -1310895500, -359063823, 1757588212,
         711532522, -1133369673, -839796146, -290511088, 2105118668,
         -586929449, 430833197, 1123084195, -1176908581, -1447619646,
         1740365919, 2091939365, -1509931931, -308810741, 417269944,
         1277127433, 2142243566, 2009240192, -1617374692, 2104849635,
         -1129243600, -694576868, 1860548372, -1972407911, -153909985},
    };

    @ParameterizedTest
    @MethodSource
    void testReferenceCode(int[] seed, int[] expected) {
        RandomAssert.assertEquals(expected, new Philox4x32(seed));
    }

    static Stream<Arguments> testReferenceCode() {
        final Builder<Arguments> builder = Stream.builder();
        for (int i = 0; i < SEEDS.length; i++) {
            builder.add(Arguments.of(SEEDS[i], EXPECTED_SEQUENCE[i]));
        }
        return builder.build();
    }

    @Test
    void testConstructors() {
        // Test zero padding
        final int[][] seeds = {
            {1234},
            {1234, 0, 1},
        };
        final int n = 10;
        for (int[] seed : seeds) {
            final int[] expected = new Philox4x32(seed).ints(n).toArray();
            for (int i = seed.length + 1; i <= 6; i++) {
                final int[] padded = Arrays.copyOf(seed, i);
                RandomAssert.assertEquals(expected, new Philox4x32(padded));
            }
        }
    }

    /**
     * Skip the generator forward using calls to next. This is used to test advancement of
     * the sequence and then advancement of the internal counter. This is done as the
     * generator outputs 4 values per increment of the counter. The output position must
     * stay the same after jumping.
     *
     * @param rng Generator.
     * @param n Count to skip ahead.
     * @return the generator
     */
    private static Philox4x32 skip(Philox4x32 rng, int n) {
        for (int i = n; --i >= 0;) {
            rng.next();
        }
        return rng;
    }

    @ParameterizedTest
    @MethodSource
    void testJump(int[] seed, int[] expected, int[] expectedAfterJump) {
        for (int i = 0; i <= 4; i++) {
            RandomAssert.assertJumpEquals(
                Arrays.copyOfRange(expected, i, expected.length),
                Arrays.copyOfRange(expectedAfterJump, i, expectedAfterJump.length),
                skip(new Philox4x32(seed), i));
        }
    }

    static Stream<Arguments> testJump() {
        final Builder<Arguments> builder = Stream.builder();
        for (int i = 0; i < SEEDS.length; i++) {
            builder.add(Arguments.of(SEEDS[i], EXPECTED_SEQUENCE[i], EXPECTED_SEQUENCE_AFTER_JUMP[i]));
        }
        return builder.build();
    }

    @ParameterizedTest
    @MethodSource
    void testLongJump(int[] seed, int[] expected, int[] expectedAfterJump) {
        for (int i = 0; i <= 4; i++) {
            RandomAssert.assertLongJumpEquals(
                Arrays.copyOfRange(expected, i, expected.length),
                Arrays.copyOfRange(expectedAfterJump, i, expectedAfterJump.length),
                skip(new Philox4x32(seed), i));
        }
    }

    static Stream<Arguments> testLongJump() {
        final Builder<Arguments> builder = Stream.builder();
        for (int i = 0; i < SEEDS.length; i++) {
            builder.add(Arguments.of(SEEDS[i], EXPECTED_SEQUENCE[i], EXPECTED_SEQUENCE_AFTER_LONG_JUMP[i]));
        }
        return builder.build();
    }

    @ParameterizedTest
    @MethodSource
    void testInternalCounter(int[] seed1, int[] seed2) {
        RandomAssert.assertNextIntEquals(10,
            skip(new Philox4x32(seed1), 4),
            new Philox4x32(seed2));
    }

    static Stream<Arguments> testInternalCounter() {
        // Test of counter increment. Note that the value of -1 is all bits set and incrementing
        // will carry a 1-bit to the next counter up.
        final int key0 = (int) 67280421310721L;
        final int key1 = (int) (67280421310721L >>> 32);

        return Stream.of(
            Arguments.of(new int[] {key0, key1,  0,  0,  0,  0},
                         new int[] {key0, key1,  1,  0,  0,  0}),
            Arguments.of(new int[] {key0, key1, -1,  0,  0,  0},
                         new int[] {key0, key1,  0,  1,  0,  0}),
            Arguments.of(new int[] {key0, key1, -1, -1,  0,  0},
                         new int[] {key0, key1,  0,  0,  1,  0}),
            Arguments.of(new int[] {key0, key1, -1, -1, -1,  0},
                         new int[] {key0, key1,  0,  0,  0,  1}),
            Arguments.of(new int[] {key0, key1, -1, -1, -1, -1},
                         new int[] {key0, key1,  0,  0,  0,  0})
        );
    }

    @Test
    void testJumpCounter() {
        Philox4x32 rng1 = new Philox4x32(new int[] {1234, 0, -1, 0, -1, 0});
        rng1.jump();
        Philox4x32 rng2 = new Philox4x32(new int[] {1234, 0, -1, 0, 0, 1});
        RandomAssert.assertNextIntEquals(10, rng1, rng2);

        rng1 = new Philox4x32(new int[] {1234, 0, -1, -1, -1, 0});
        rng1.jump();
        rng2 = new Philox4x32(new int[] {1234, 0, -1, -1, 0, 1});
        RandomAssert.assertNextIntEquals(10, rng1, rng2);
    }

    @Test
    void testLongJumpCounter() {
        final Philox4x32 rng1 = new Philox4x32(new int[] {1234, 0, -1, -1, -1, 0});
        rng1.longJump();
        final Philox4x32 rng2 = new Philox4x32(new int[] {1234, 0, -1, -1, -1, 1});
        RandomAssert.assertNextIntEquals(10, rng1, rng2);
    }

    @ParameterizedTest
    @ValueSource(doubles = {0x1.0p130, 0x1.0p456, Double.MAX_VALUE})
    void testJumpThrowsWithInvalidDistance(double distance) {
        final Philox4x32 rng = new Philox4x32(new int[] {1234});
        Assertions.assertThrows(IllegalArgumentException.class, () -> rng.jump(distance));
    }

    @ParameterizedTest
    @ValueSource(ints = {130, 456, Integer.MAX_VALUE})
    void testJumpPowerOfTwoThrowsWithInvalidDistance(int logDistance) {
        final Philox4x32 rng = new Philox4x32(new int[] {1234});
        Assertions.assertThrows(IllegalArgumentException.class, () -> rng.jumpPowerOfTwo(logDistance));
    }

    @ParameterizedTest
    @MethodSource(value = "testJump")
    void testArbitraryJumpMatchesJump(int[] seed, int[] ignored, int[] ignored2) {
        for (int i = 0; i <= 4; i++) {
            final Philox4x32 rng1 = skip(new Philox4x32(seed), i);
            final Philox4x32 rng2 = skip(new Philox4x32(seed), i);
            rng1.jump();
            rng2.jump(0x1.0p66);
            RandomAssert.assertNextIntEquals(10, rng1, rng2);
        }
    }

    @ParameterizedTest
    @MethodSource(value = "testJump")
    void testArbitraryJumpPowerOfTwoMatchesJump(int[] seed, int[] ignored, int[] ignored2) {
        for (int i = 0; i <= 4; i++) {
            final Philox4x32 rng1 = skip(new Philox4x32(seed), i);
            final Philox4x32 rng2 = skip(new Philox4x32(seed), i);
            rng1.jump();
            rng2.jumpPowerOfTwo(66);
            RandomAssert.assertNextIntEquals(10, rng1, rng2);
        }
    }

    @ParameterizedTest
    @MethodSource(value = "testJump")
    void testArbitraryJumpMatchesLongJump(int[] seed, int[] ignored, int[] ignored2) {
        for (int i = 0; i <= 4; i++) {
            final Philox4x32 rng1 = skip(new Philox4x32(seed), i);
            final Philox4x32 rng2 = skip(new Philox4x32(seed), i);
            rng1.longJump();
            rng2.jump(0x1.0p98);
            RandomAssert.assertNextIntEquals(10, rng1, rng2);
        }
    }

    @ParameterizedTest
    @MethodSource(value = "testJump")
    void testArbitraryJumpPowerOfTwoMatchesLongJump(int[] seed, int[] ignored, int[] ignored2) {
        for (int i = 0; i <= 4; i++) {
            final Philox4x32 rng1 = skip(new Philox4x32(seed), i);
            final Philox4x32 rng2 = skip(new Philox4x32(seed), i);
            rng1.longJump();
            rng2.jumpPowerOfTwo(98);
            RandomAssert.assertNextIntEquals(10, rng1, rng2);
        }
    }

    @ParameterizedTest
    @MethodSource
    void testArbitraryJumpCounter(int[] seed1, double distance, int[] seed2) {
        // Test the buffer in a used and partially used state
        for (int i = 0; i < 2; i++) {
            final Philox4x32 rng1 = skip(new Philox4x32(seed1), i);
            final Philox4x32 rng2 = skip(new Philox4x32(seed2), i);
            rng1.jump(distance);
            RandomAssert.assertNextLongEquals(10, rng1, rng2,
                () -> String.format("seed=%s, distance=%s", Arrays.toString(seed1), distance));
        }
    }

    static Stream<Arguments> testArbitraryJumpCounter() {
        // Test of counter increment. Note that the value of -1 is all bits set and incrementing
        // will carry a 1-bit to the next counter up.
        final int key0 = (int) 67280421310721L;
        final int key1 = (int) (67280421310721L >>> 32);

        final Stream.Builder<Arguments> builder = Stream.builder();

        // Any power of two jump can be expressed as a double
        testArbitraryJumpPowerOfTwoCounter().map(Arguments::get).forEach(objects -> {
            final int logDistance = (int) objects[1];
            final double distance = Math.scalb(1.0, logDistance);
            builder.add(Arguments.of(objects[0], distance, objects[2]));
        });

        // Largest allowed jump
        builder.add(Arguments.of(new int[] {key0, key1, 0, 0, 0, 0}, Math.nextDown(0x1.0p130),
                                 new int[] {key0, key1, 0, 0, -1 << 11, -1}));

        // Arbitrary jumps
        builder.add(Arguments.of(new int[] {key0, key1, 0, 0, 0, 0}, 0x1.0p99 + 0x1.0p73,
                                 new int[] {key0, key1, 0, 0, 1 << 7, 2}));
        builder.add(Arguments.of(new int[] {key0, key1, 0, 0, -1, 0}, 0x1.0p99 + 0x1.0p73,
                                 new int[] {key0, key1, 0, 0, -1 + (1 << 7), 3}));
        builder.add(Arguments.of(new int[] {key0, key1, 0, 0, -1, 0}, 0x1.0p73 + 0x1.0p23,
                                 new int[] {key0, key1, 1 << 21, 0, -1 + (1 << 7), 1}));
        builder.add(Arguments.of(new int[] {key0, key1, 0, 0, 0, 0}, 1234 * 4 + 5678 * 0x1.0p34,
                                 new int[] {key0, key1, 1234, 5678, 0, 0}));

        final SplittableRandom rng = new SplittableRandom();
        for (int i = 0; i < 10; i++) {
            final int[] counter1 = rng.ints(4).toArray();
            // Jump in range [0, 2^128): 128 - 63 = 65
            final double counterDistance = Math.abs(Math.scalb((double) rng.nextLong(), rng.nextInt(65)));
            final BigInteger jump = new BigDecimal(counterDistance).toBigInteger();
            final int[] counter2 = add(counter1, jump);
            builder.add(Arguments.of(new int[] {key0, key1, counter1[0], counter1[1], counter1[2], counter1[3]},
                                     jump.doubleValue() * 4,
                                     new int[] {key0, key1, counter2[0], counter2[1], counter2[2], counter2[3]}));
        }
        return builder.build();
    }

    @ParameterizedTest
    @MethodSource
    void testArbitraryJumpPowerOfTwoCounter(int[] seed1, int logDistance, int[] seed2) {
        // Test the buffer in a used and partially used state
        for (int i = 0; i < 2; i++) {
            final Philox4x32 rng1 = skip(new Philox4x32(seed1), i);
            final Philox4x32 rng2 = skip(new Philox4x32(seed2), i);
            rng1.jumpPowerOfTwo(logDistance);
            RandomAssert.assertNextLongEquals(10, rng1, rng2,
                () -> String.format("seed=%s, logDistance=%d", Arrays.toString(seed1), logDistance));
        }
    }

    static Stream<Arguments> testArbitraryJumpPowerOfTwoCounter() {
        // Test of counter increment. Note that the value of -1 is all bits set and incrementing
        // will carry a 1-bit to the next counter up.
        final int key0 = (int) 67280421310721L;
        final int key1 = (int) (67280421310721L >>> 32);

        final Stream.Builder<Arguments> builder = Stream.builder();
        // Jumps for each part of the counter
        builder.add(Arguments.of(new int[] {key0, key1, 0, 0, 0, 0}, 2,
                                 new int[] {key0, key1, 1, 0, 0, 0}));
        builder.add(Arguments.of(new int[] {key0, key1, 0, 0, 0, 0}, 32,
                                 new int[] {key0, key1, 1 << 30, 0, 0, 0}));
        builder.add(Arguments.of(new int[] {key0, key1, 0, 0, 0, 0}, 53,
                                 new int[] {key0, key1, 0, 1 << 19, 0, 0}));
        builder.add(Arguments.of(new int[] {key0, key1, 0, 0, 0, 0}, 75,
                                 new int[] {key0, key1, 0, 0, 1 << 9, 0}));
        builder.add(Arguments.of(new int[] {key0, key1, 0, 0, 0, 0}, 99,
                                 new int[] {key0, key1, 0, 0, 0, 2}));
        // Largest jump does not wrap the overflow bit. It should be lost
        // to the unrepresented 129-th bit of the counter.
        builder.add(Arguments.of(new int[] {key0, key1, -1, -1, -1, -1}, 129,
                                 new int[] {key0, key1, -1, -1, -1, -1 + (1 << 31)}));
        // Roll-over by incrementing the counter by 1.
        // Note that the Philox counter is incremented by 1 upon first call to regenerate
        // the output buffer. The test aims to use the jump to rollover the bits
        // so the counter must be initialised 1 before the rollover threshold,
        // i.e. -2 + 1 (increment) + 1 (jump) == 0 + rollover.
        // The test uses a variable skip forward of the generator to cover cases of counter
        // increment before or after jump increment.
        builder.add(Arguments.of(new int[] {key0, key1, -2,  0,  0,  0}, 2,
                                 new int[] {key0, key1, -1,  0,  0,  0}));
        builder.add(Arguments.of(new int[] {key0, key1, -2, -1,  0,  0}, 2,
                                 new int[] {key0, key1, -1, -1,  0,  0}));
        builder.add(Arguments.of(new int[] {key0, key1, -2, -1, -1,  0}, 2,
                                 new int[] {key0, key1, -1, -1, -1,  0}));
        builder.add(Arguments.of(new int[] {key0, key1, -2, -1, -1, -1}, 2,
                                 new int[] {key0, key1, -1, -1, -1, -1}));
        // Arbitrary jumps
        final SplittableRandom rng = new SplittableRandom();
        for (int i = 0; i < 10; i++) {
            final int[] counter1 = rng.ints(4).toArray();
            // Jump in range [0, 2^128)
            final int logDistance = rng.nextInt(128);
            final BigInteger jump = BigInteger.ONE.shiftLeft(logDistance);
            final int[] counter2 = add(counter1, jump);
            builder.add(Arguments.of(new int[] {key0, key1, counter1[0], counter1[1], counter1[2], counter1[3]},
                                     logDistance + 2,
                                     new int[] {key0, key1, counter2[0], counter2[1], counter2[2], counter2[3]}));
        }
        return builder.build();
    }

    /**
     * Test arbitrary jumps with the internal state of the generator anywhere in the
     * output buffer. The jump targets both the counter increment (distance [4, 2^51))
     * and the output buffer position (distance [0, 4)).
     */
    @ParameterizedTest
    @MethodSource
    void testArbitraryJumpCounterWithSkip(int[] seed1, long distance, int[] seed2) {
        Assertions.assertNotEquals((double) distance, distance + 1.0,
            "Small distance required to allow jumping within the buffer position");
        // Skip within the generator output buffer
        for (int i = 0; i <= 4; i++) {
            // Jump within the generator output buffer
            for (int j = 0; j <= 4; j++) {
                final Philox4x32 rng1 = skip(new Philox4x32(seed1), i);
                final Philox4x32 rng2 = skip(new Philox4x32(seed2), i + j);
                rng1.jump(distance + j);
                RandomAssert.assertNextIntEquals(10, rng1, rng2,
                    () -> String.format("seed=%s, distance=%s", Arrays.toString(seed1), distance));
            }
        }
    }

    static Stream<Arguments> testArbitraryJumpCounterWithSkip() {
        final int key0 = (int) 67280421310721L;
        final int key1 = (int) (67280421310721L >>> 32);

        final Stream.Builder<Arguments> builder = Stream.builder();
        final SplittableRandom rng = new SplittableRandom();
        for (int i = 0; i < 5; i++) {
            final int[] counter1 = rng.ints(4).toArray();
            // Jump in range [0, 2^51)
            final long counterDistance = rng.nextLong(1L << 51);
            final BigInteger jump = BigInteger.valueOf(counterDistance);
            final int[] counter2 = add(counter1, jump);
            builder.add(Arguments.of(new int[] {key0, key1, counter1[0], counter1[1], counter1[2], counter1[3]},
                                     counterDistance * 4,
                                     new int[] {key0, key1, counter2[0], counter2[1], counter2[2], counter2[3]}));
        }
        return builder.build();
    }

    private static int[] add(int[] counter, BigInteger jump) {
        final BigInteger sum = IntJumpDistancesTest.toBigInteger(counter).add(jump);
        final int[] value = IntJumpDistancesTest.toIntArray(sum);
        // Return result with the same counter size
        return Arrays.copyOf(value, counter.length);
    }
}
