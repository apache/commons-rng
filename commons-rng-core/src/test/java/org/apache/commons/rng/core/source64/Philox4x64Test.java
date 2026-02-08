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
package org.apache.commons.rng.core.source64;

import org.apache.commons.rng.UniformRandomProvider;
import org.apache.commons.rng.core.RandomAssert;
import org.apache.commons.rng.core.source32.XoShiRo128PlusPlus;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class Philox4x64Test {
    /*
     * Data from python randomgen.philox.Philox(key=1234,number=4,width=32) random_raw()
     * https://bashtage.github.io/randomgen/bit_generators/philox.html
     */

    private static final long[] EXPECTED_SEQUENCE_1234 = {
        6174562084317992592L, -7568142518571726206L, -5685918792241859306L,
        6151287208724416091L, -7525285015497232737L, -2526119061336846091L,
        -2093373494943999176L, 2505686065164099867L, 1493954073060533072L,
        2386252059344830309L, -3981277096068706128L, 4825385527958964709L,
        5896359280427319232L, 2130638389021018825L, 1001529696243618836L,
        6229771985419955916L, -8030183820248387325L, 5924921954534026109L,
        -2430661683740471500L, -7119094164204651921L, 2451935767711287279L,
        8424479353221384040L, -5011970289299902244L, 8792348508803652203L,
        9109768561113011588L, 24126314432238277L, -8946976403367747978L,
        6224712922535513938L, 8733921062828259483L, 3855129282970288492L,
        -15371244630355388L, -3103082637265535013L, -5696416329331263984L,
        -5000982493478729316L, -3077201427991874994L, 4502749081228919907L,
        1930363720599024367L, -7884649763770700010L, 9162677665382083018L,
        -1491083349895074892L
    };

    private static final long[] EXPECTED_SEQUENCE_DEFAULT = {
        7651105821017786633L, -986727441099762072L, -1758232618730818806L,
        -6892647654339096064L, 2003912625120555464L, 847995992558080923L,
        2561190448322591348L, 5089323078274549892L, -6215224099279536444L,
        2839273132443259286L, -1538091565590055595L, 2262400997606952131L,
        4794890345824897152L, 2654554423835782039L, 5232844452212050618L,
        4968309811735346778L, -6677562093502275256L, -2345486924693103657L,
        2546479265789531422L, 1397198500311783458L, -3029924206687987745L,
        3915450377326980183L, -1798629713529533718L, 7813856890368443409L,
        -7530219763187390588L, 7752320264114599504L, 4497386005519180400L,
        8983526426341050924L, 3157770966203722859L, 6531619948763639990L,
        -2561361262383382379L, -7341089376366770572L, 5588349311041971766L,
        -5547961913507498237L, 557535079196835645L, -7564858493373145745L,
        -5687482083658299050L, -6040393957990987713L, 3376696212464637986L,
        -4460669316800568753L
    };

    private static final long[] EXPECTED_SEQUENCE_AFTER_JUMP = {
        -1727205271972662868L, -929408180418766908L, -1945866921247798399L,
        4169268208105524116L, 632888508629215617L, -1524268609550446551L,
        1017151209430921623L, 6132992108188153451L, 20451580683440948L,
        -5983342812596318147L, 7111485060182475632L, -3139710623798850919L,
        -1285398253303499143L, 6122663610027136797L, 2115295045070288083L,
        3475295225226077398L, -4567382713692939160L, 2219019105759378889L,
        9185093757989334009L, -4483498154421417534L, 4899113076935204533L,
        2822902514642117601L, 8699417340289208720L, -3702000480108440166L,
        182332162670312430L, -4588636866812122027L, -6097870520215051203L,
        -5437421417978893129L, -2642557189401455147L, -2357350941097728376L,
        -3256721505120722383L, 7808773965898529545L, -8282907275396061324L,
        -2119140891863246985L, 3101477059676926823L, 4342196138195251014L,
        -6435107576506612831L, 4741991854358995576L, 2272452471517297382L,
        -3745481320084764683L
    };

    private static final long[] EXPECTED_SEQUENCE_AFTER_LONG_JUMP = {
        -8246258822028745854L, -8108712508154901372L, 2654408923912105318L,
        -6418747939867464899L, 8695124057120477387L, -4062778777418832523L,
        -2866609061902870832L, -1985485291750970064L, -3716513824860297891L,
        2708966168515282018L, -8441862438570589384L, -3332504021977608920L,
        8275431876927502767L, -37683753608778224L, 4850475723424833501L,
        -2864632267522668999L, -6547048909303846355L, -6804759155034193445L,
        -1607076952104749058L, 7993605125443204784L, 7601442483044023354L,
        -7379694727972198096L, -1902536664833944445L, -908773878773086264L,
        -7367142976738044337L, 2845297286559921499L, 5398165976383543580L,
        2574122219286874876L, 3780790808954139828L, -7038343169285503987L,
        1381442423564430946L, -4910467881295472851L, 839863310680617535L,
        3700507604505113976L, 2586645934793105407L, 1058068213122536369L,
        -1876209807038423750L, 8994121856634859944L, 4145729862086221315L,
        -7214331765643557828L
    };

    @Test
    void testReferenceCode() {
        RandomAssert.assertEquals(EXPECTED_SEQUENCE_1234, new Philox4x64(new long[]{1234L, 0}, new long[]{0, 0}, new long[]{0, 0}));
    }

    @Test
    void testReferenceCodeDefaultSeed() {
        RandomAssert.assertEquals(EXPECTED_SEQUENCE_DEFAULT, new Philox4x64());
    }


    @Test
    void testJump() {
        RandomAssert.assertJumpEquals(EXPECTED_SEQUENCE_DEFAULT, EXPECTED_SEQUENCE_AFTER_JUMP, new Philox4x64(67280421310721L, 0x9E3779B97F4A7C15L, 0, 0, 0, 0));
    }

    @Test
    void testLongJump() {
        RandomAssert.assertLongJumpEquals(EXPECTED_SEQUENCE_DEFAULT, EXPECTED_SEQUENCE_AFTER_LONG_JUMP, new Philox4x64());
    }

    @Test
    void testOffset() {
        Philox4x64 rng = new Philox4x64(67280421310721L, 0x9E3779B97F4A7C15L, 1, 0, 0, 0);
        assertEquals(1, rng.getOffset()[0]);
        assertEquals(EXPECTED_SEQUENCE_DEFAULT[4], rng.next());
        rng = new Philox4x64(67280421310721L, 0x9E3779B97F4A7C15L, 0, 1, 0, 0);
        assertEquals(1, rng.getOffset()[1]);
    }

    @Test
    void testReset() {
        Philox4x64 rng = new Philox4x64(67280421310721L, 0x9E3779B97F4A7C15L, 1, 0, 0, 0);
        assertEquals(EXPECTED_SEQUENCE_DEFAULT[4], rng.next());
        rng.resetState(new long[]{1234L, 0}, new long[]{0, 0});
        assertEquals(EXPECTED_SEQUENCE_1234[0], rng.next());
    }

    @Test
    void testInternalCounter() {
        //test of incrementCounter
        Philox4x64 rng = new Philox4x64(new long[]{67280421310721L, 1234L}, new long[]{0, 0}, new long[]{0xffffffffffffffffL, 0});
        for (int i = 0; i < 4; i++) {
            rng.next();
        }
        long value = rng.next();
        Philox4x64 rng2 = new Philox4x64(new long[]{67280421310721L, 1234L}, new long[]{0, 0}, new long[]{0, 1});
        long value2 = rng2.next();
        assertEquals(value, value2);

        rng = new Philox4x64(new long[]{67280421310721L, 1234L}, new long[]{0, 0}, new long[]{0xffffffffffffffffL, 0xffffffffffffffffL});
        for (int i = 0; i < 4; i++) {
            rng.next();
        }
        value = rng.next();
        rng2 = new Philox4x64(new long[]{67280421310721L, 1234L}, new long[]{1, 0}, new long[]{0, 0});
        value2 = rng2.next();
        assertEquals(value, value2);

        rng = new Philox4x64(new long[]{67280421310721L, 1234L}, new long[]{0xffffffffffffffffL, 0}, new long[]{0xffffffffffffffffL, 0xffffffffffffffffL});
        for (int i = 0; i < 4; i++) {
            rng.next();
        }
        value = rng.next();
        rng2 = new Philox4x64(new long[]{67280421310721L, 1234L}, new long[]{0, 1}, new long[]{0, 0});
        value2 = rng2.next();
        assertEquals(value, value2);

    }

    @Test
    void testSetOffset() {
        Philox4x64 rng = new Philox4x64(new long[]{1234L, 0}, new long[]{0, 0}, new long[]{1, 0});
        assertEquals(1, rng.getOffset()[0]);
        assertEquals(0, rng.getOffset()[1]);
        assertEquals(EXPECTED_SEQUENCE_1234[4], rng.next());
        rng = new Philox4x64(new long[]{1234L, 0}, new long[]{0, 0}, new long[]{0, 1});
        assertEquals(0, rng.getOffset()[0]);
        assertEquals(1, rng.getOffset()[1]);
        rng.setOffset(0);
        assertEquals(EXPECTED_SEQUENCE_1234[0], rng.next());
        rng.setOffset(1, 0);
        assertEquals(EXPECTED_SEQUENCE_1234[4], rng.next());

    }

    @Test
    void testLongJumpCounter() {
        Philox4x64 rng = new Philox4x64(new long[]{1234L, 0}, new long[]{0xffffffffffffffffL, 0}, new long[]{0xffffffffffffffffL, 0});
        UniformRandomProvider rngOrig = rng.longJump();
        long value = rng.nextInt();
        Philox4x64 rng2 = new Philox4x64(new long[]{1234L, 0}, new long[]{0, 1}, new long[]{0xffffffffffffffffL, 0});
        long value2 = rng2.nextInt();
        assertEquals(value2, value);
        rng = new Philox4x64(new long[]{1234L, 0}, new long[]{0xffffffffffffffffL, 0}, new long[]{0xffffffffffffffffL, 0xffffffffffffffffL});
        rngOrig = rng.longJump();
        value = rng.nextInt();
        rng2 = new Philox4x64(new long[]{1234L, 0}, new long[]{0, 1}, new long[]{0xffffffffffffffffL, 0xffffffffffffffffL});
        value2 = rng2.nextInt();
        assertEquals(value2, value);

        rng = new Philox4x64(new long[]{1234L, 0}, new long[]{0, 0}, new long[]{0xffffffffffffffffL, 0xffffffffffffffffL});
        rngOrig = rng.jump();
        value = rng.nextInt();
        rng2 = new Philox4x64(new long[]{1234L, 0}, new long[]{1, 0}, new long[]{0xffffffffffffffffL, 0});
        value2 = rng2.nextInt();
        assertEquals(value2, value);
    }

    @Test
    void testJumpCounter() {
        Philox4x64[] rngs = new Philox4x64[]{
            new Philox4x64(new long[]{67280421310721L, 1234L}, new long[]{0, 0}, new long[]{0xffffffffffffffffL, 0}),
            new Philox4x64(new long[]{67280421310721L, 1234L}, new long[]{0, 0}, new long[]{0xffffffffffffffffL, 0xffffffffffffffffL}),
            new Philox4x64(new long[]{67280421310721L, 1234L}, new long[]{0xffffffffffffffffL, 0}, new long[]{0xffffffffffffffffL, 0xffffffffffffffffL}),
            new Philox4x64(new long[]{67280421310721L, 1234L}, new long[]{0xffffffffffffffffL, 0}, new long[]{0, 0}),
            new Philox4x64(new long[]{67280421310721L, 1234L}, new long[]{0xffffffffffffffffL, 0}, new long[]{0, 0xffffffffffffffffL}),
        };
        for (Philox4x64 rng : rngs) {
            UniformRandomProvider rngOrig = rng.jump(10);
            long value = rng.nextLong();
            for (int i = 0; i < 10; i++) {
                rngOrig.nextLong();
            }
            long value2 = rngOrig.nextLong();
            assertEquals(value2, value);
        }
    }

    @Test
    void testArbitraryJumps() {
        XoShiRo128PlusPlus rngForSkip = new XoShiRo128PlusPlus(10, 20, 30, 40);
        Philox4x64 rng = new Philox4x64();
        for (int i = 0; i < 512; i++) {
            int n = rngForSkip.nextInt() >>> 23;
            UniformRandomProvider rngOrig = rng.jump(n);
            long jumpValue = rng.nextLong();
            for (int k = 0; k < n; k++) {
                rngOrig.nextLong();
            }
            long origValue = rngOrig.nextLong();
            assertEquals(origValue, jumpValue);
        }
    }

    @Test
    void testConstructors() {
        Philox4x64[] rngs = new Philox4x64[]{
            new Philox4x64(),
            new Philox4x64(new long[]{67280421310721L, 0x9E3779B97F4A7C15L}, new long[]{0, 0}, new long[]{0, 0}),
            new Philox4x64(67280421310721L, 0x9E3779B97F4A7C15L),
            new Philox4x64(67280421310721L, 0x9E3779B97F4A7C15L, 0, 0),
            new Philox4x64(67280421310721L, 0x9E3779B97F4A7C15L, 0, 0, 0, 0),
            new Philox4x64(new long[]{67280421310721L, 0x9E3779B97F4A7C15L, 0, 0, 0, 0})
        };
        long refValue = rngs[0].next();
        for (int i = 1; i < rngs.length; i++) {
            long value = rngs[i].next();
            assertEquals(refValue, value, "Philox4x64 initialization for i=" + i);
        }

        rngs = new Philox4x64[]{
            new Philox4x64(new long[]{1234L, 0}, new long[]{1, 0}, new long[]{1, 0}),
            new Philox4x64(1234, 0, 1, 0, 1),
            new Philox4x64(1234, 0, 1, 0, 1, 0),
        };
        refValue = rngs[0].next();
        for (int i = 1; i < rngs.length; i++) {
            long value = rngs[i].next();
            assertEquals(refValue, value, "Philox4x32 initialization for i=" + i);
        }
        rngs = new Philox4x64[]{
            new Philox4x64(1234L),
            new Philox4x64(1234L, 0, 0),
            new Philox4x64(1234, 0, 0, 0),
            new Philox4x64(1234, 0, 0, 0, 0),
            new Philox4x64(1234, 0, 0, 0, 0, 0),
            new Philox4x64(1234, 0, 0, 0, 0, 0, 0),
        };
        refValue = rngs[0].next();
        for (int i = 1; i < rngs.length; i++) {
            long value = rngs[i].next();
            assertEquals(refValue, value, "Philox4x32 initialization for i=" + i);
        }
    }
}
