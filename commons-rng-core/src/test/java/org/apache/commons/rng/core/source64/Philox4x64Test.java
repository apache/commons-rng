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

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.Arrays;
import java.util.SplittableRandom;
import java.util.stream.Stream;
import java.util.stream.Stream.Builder;
import org.apache.commons.rng.ArbitrarilyJumpableUniformRandomProvider;
import org.apache.commons.rng.UniformRandomProvider;
import org.apache.commons.rng.core.RandomAssert;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.ValueSource;

public class Philox4x64Test {
    // Data from python 3.12.12 using randomgen v2.3.0, e.g.
    // from randomgen import Philox
    // import numpy as np
    // Philox(number=4,width=64).random_raw(10)
    // Note that use of 'advance' for jumping moves the underlying RNG state which is the counter.
    // See: https://bashtage.github.io/randomgen/bit_generators/philox.html

    private static final long[][] SEEDS = {
        {1234},
        {0, 0, 1234},
        {67280421310721L, 0x9E3779B97F4A7C15L},
        {123, 456, 789, 10, 11, 12},
    };

    private static final long[][] EXPECTED_SEQUENCE = {
        // Philox(number=4, width=64, key=1234).random_raw(40).astype('int64')
        {6174562084317992592L, -7568142518571726206L, -5685918792241859306L,
         6151287208724416091L, -7525285015497232737L, -2526119061336846091L,
         -2093373494943999176L,  2505686065164099867L,  1493954073060533072L,
         2386252059344830309L, -3981277096068706128L,  4825385527958964709L,
         5896359280427319232L,  2130638389021018825L,  1001529696243618836L,
         6229771985419955916L, -8030183820248387325L,  5924921954534026109L,
         -2430661683740471500L, -7119094164204651921L,  2451935767711287279L,
         8424479353221384040L, -5011970289299902244L,  8792348508803652203L,
         9109768561113011588L,    24126314432238277L, -8946976403367747978L,
         6224712922535513938L,  8733921062828259483L,  3855129282970288492L,
         -15371244630355388L, -3103082637265535013L, -5696416329331263984L,
         -5000982493478729316L, -3077201427991874994L,  4502749081228919907L,
         1930363720599024367L, -7884649763770700010L,  9162677665382083018L,
         -1491083349895074892L},
        // Philox(number=4, width=64, key=0, counter=1234).random_raw(40).astype('int64')
        {4602593894542135114L, -7779924014051009558L,    25407474640490857L,
         -219118047623655109L, -6737959372319246238L,  6002341716364983403L,
         1043522413660961709L,  8989614606876290416L, -3265317364297248690L,
         -2651599613192895308L,   -33234785373800097L,  2180478516430940079L,
         4447221087757588434L,  2625784107438363831L,  3660926185088674743L,
         -4598730064658163989L, -7174827943241197376L, -3540487041265702560L,
         929624192182325146L, -7945300272147771819L, -9075433845744037455L,
         1653871600920301334L,  5016334957584870631L,  7955099121277176291L,
         -5858016637190515519L,  3182274064694275540L, -8654813329766318637L,
         2889975639218908751L, -6637931775896435853L,  6336542263838017602L,
         -1761462408058575606L, -3249772053450270102L,  3524966407403594877L,
         -4401058000570047535L,  -356195494872117591L,  7456568338621007200L,
         -716430788996422291L,  4421169763123182859L, -5355483254806311750L,
         -4392676542049710666L},
        // Philox(number=4, width=64, key=np.array([67280421310721, -7046029254386353131]).astype('uint64')).random_raw(40).astype('int64')
        {7651105821017786633L,  -986727441099762072L, -1758232618730818806L,
         -6892647654339096064L,  2003912625120555464L,   847995992558080923L,
         2561190448322591348L,  5089323078274549892L, -6215224099279536444L,
         2839273132443259286L, -1538091565590055595L,  2262400997606952131L,
         4794890345824897152L,  2654554423835782039L,  5232844452212050618L,
         4968309811735346778L, -6677562093502275256L, -2345486924693103657L,
         2546479265789531422L,  1397198500311783458L, -3029924206687987745L,
         3915450377326980183L, -1798629713529533718L,  7813856890368443409L,
         -7530219763187390588L,  7752320264114599504L,  4497386005519180400L,
         8983526426341050924L,  3157770966203722859L,  6531619948763639990L,
         -2561361262383382379L, -7341089376366770572L,  5588349311041971766L,
         -5547961913507498237L,   557535079196835645L, -7564858493373145745L,
         -5687482083658299050L, -6040393957990987713L,  3376696212464637986L,
         -4460669316800568753L},
        // Philox(number=4, width=64, key=np.array([123, 456]).astype('uint64'), counter=np.array([789, 10, 11, 12]).astype('uint64')).random_raw(40).astype('int64')
        {-2676203041825239039L, -2740683717708353481L, -7644481655179606917L,
         -2013881432229927560L,  9138893946526912114L,  1895483512753989539L,
         1120228708301007249L, -8645295091941041469L,  1312679789862818620L,
         1319096805073214953L, -5683321287439058896L,  6779310761360215443L,
         222706252396899469L,  -748745743036821263L, -5986005458400436807L,
         7774766133030748118L,  1358590715822147155L,  1770310740130956307L,
         2974801238206570717L,  7793577192931967650L,  5879673181320289139L,
         424191421592832169L,   -44950074335928319L, -5328467318680198093L,
         -2309212140127930007L, -1351409165995434564L, -5220285486477251080L,
         -8009017136074094091L,  8458238586024979051L,  2006971827104453375L,
         4972853697981997314L,   378065969023616518L, -7986031009609218509L,
         -6267286231853473662L,  6223331114424954036L,  7219801061004957681L,
         -2754254636675099747L,  1985462617359309242L, -2111202985232615810L,
         7039073644158018717L},
    };

    private static final long[][] EXPECTED_SEQUENCE_AFTER_JUMP = {
        // Philox(number=4, width=64, key=1234).advance(2**128).random_raw(40).astype('int64')
        {-6605138030737368705L,  -104786067033919314L,  7262974134564151645L,
         7389185314653228008L, -1845243711001917053L, -8129367108856859081L,
         5863099844641795352L, -1588562292987043296L, -5276900919329730796L,
         -4839825532692855212L, -3005384905083100382L,  4940049058055828836L,
         3125893001993866808L,  -194882599129534243L,  -829937644935958488L,
         -5116596352362109591L,  4084649079511430158L, -5005839683746769722L,
         6425231510455054227L,  8487896632853834854L, -4839491972718487707L,
         -7074461298074149905L,  9188462327589217895L, -6982319248678123776L,
         -6775976665973778805L,  -556558278788825107L,  7702435004312873190L,
         -1975866573012244840L, -8485406220385537373L, -8056922448147710353L,
         -500242631712533401L,  2988305376269021637L, -8877908281402284755L,
         -6789239851216290959L,  7618533941039439319L,  3018227186469623707L,
         -1308945031324862857L,  8415302125905967071L,  5514678642633369121L,
         9125947772243208515L},
        // Philox(number=4, width=64, key=0, counter=1234).advance(2**128).random_raw(40).astype('int64')
        {-5996392050049026711L, -1318936820765691795L, -9094284727756391589L,
         -1901997585771151057L, -3351998583650956486L,  5927359909200354132L,
         3923787640345836576L,  8030233969872264897L,  7721628235005184176L,
         5204873021801122550L,  8841042332981725793L,  8751261754975264474L,
         -1396551571285459908L,  6619357736738156759L,  8074859281497737300L,
         3775721535544822342L,   649066789960207960L, -3997796172252220720L,
         -8180428337384109818L, -2029011529683615605L,  5978629419694088403L,
         516137925447301856L,  4511351912403215384L, -2864745703391442603L,
         5588173985125839852L,  5129198807675375218L, -5495770699920876602L,
         1032483860456749428L, -4160602732039251670L,  9166056875173431227L,
         -5323396327562813801L, -4412148518359004370L,  6213879606243182450L,
         -6121830851256230642L, -8292725627689455207L, -4355645941967971721L,
         -4487446471831222419L,  3387370570801761730L,  7614965405387469427L,
         1190225730608448085L},
        // Philox(number=4, width=64, key=np.array([67280421310721, -7046029254386353131]).astype('uint64')).advance(2**128).random_raw(40).astype('int64')
        {-8246258822028745854L, -8108712508154901372L,  2654408923912105318L,
         -6418747939867464899L,  8695124057120477387L, -4062778777418832523L,
         -2866609061902870832L, -1985485291750970064L, -3716513824860297891L,
         2708966168515282018L, -8441862438570589384L, -3332504021977608920L,
         8275431876927502767L,   -37683753608778224L,  4850475723424833501L,
         -2864632267522668999L, -6547048909303846355L, -6804759155034193445L,
         -1607076952104749058L,  7993605125443204784L,  7601442483044023354L,
         -7379694727972198096L, -1902536664833944445L,  -908773878773086264L,
         -7367142976738044337L,  2845297286559921499L,  5398165976383543580L,
         2574122219286874876L,  3780790808954139828L, -7038343169285503987L,
         1381442423564430946L, -4910467881295472851L,   839863310680617535L,
         3700507604505113976L,  2586645934793105407L,  1058068213122536369L,
         -1876209807038423750L,  8994121856634859944L,  4145729862086221315L,
         -7214331765643557828L},
        // Philox(number=4, width=64, key=np.array([123, 456]).astype('uint64'), counter=np.array([789, 10, 11, 12]).astype('uint64')).advance(2**128).random_raw(40).astype('int64')
        {3349751575362712976L,  4608440157693993962L,  3883718938903630941L,
         8379756212699093866L,   390751818379862828L,  6214324102944420326L,
         -9093057865784782673L, -9079469339572200499L, -3207495654737585921L,
         -6548986330896199510L, -4998597093655133924L, -4824629255068294996L,
         7486466667494742622L, -6776537202200027309L, -6922298379051891853L,
         1537299585771456374L, -5366046569819517446L,  2766318896344099915L,
         -1201388944601380813L,  5364647126195770327L, -2140941972163607827L,
         4949287138790584778L,  4389747182286788270L,  2184175561877610136L,
         -7955385516003041888L,  8330840451290928924L, -3831569839270627649L,
         345238441937314251L,  4161446148399321608L,  4389512229762606514L,
         8955178869851923592L,  9088326409147430925L, -3143982187496500426L,
         2188487582733058705L,  3558033954995484416L, -2422633464131504776L,
         1254411349428332786L, -1872694959740265133L,  5970793232526744797L,
         6069543789003258620L},
    };

    private static final long[][] EXPECTED_SEQUENCE_AFTER_LONG_JUMP = {
     // Philox(number=4, width=64, key=1234).advance(2**192).random_raw(40).astype('int64')
        {4729667657206115862L,  5069392759088092340L,  5168121409554825282L,
         6201670828523444440L, -6090350473139111216L, -6164019615213184878L,
         -6844589991275452426L,  -208001825200947314L,  6200739843477938507L,
         -8299990747833237637L, -2164470069038941615L, -9209620719840271404L,
         9081231528507684539L,  8908471573711510650L, -8058670042152319425L,
         3248489557631613260L, -8417950531948119599L,   956310744760793755L,
         -5575807368130857426L,  5078688292607078835L,    48551215327160077L,
         -6261461200338795633L,  7641027733164929012L, -1914781566205681639L,
         -3259499852285659773L, -3243035490307766069L, -3930046787986016200L,
         8913600274294591416L,  3345205382516796765L,  1109341621883788743L,
         -4272760025058845714L, -2070445270914473680L,  3097370023424432401L,
         -1734064794176902027L, -1335443040824312040L,  4640148833701081118L,
         5532350755191031071L, -7421602509069023925L,  -788484102164256003L,
         -833810026861722387L},
        // Philox(number=4, width=64, key=0, counter=1234).advance(2**192).random_raw(40).astype('int64')
        {4804461682444033063L,  7818950057644610446L, -7866065027772896341L,
         3031375537531453110L, -3059458255956092290L, -7642938890090194442L,
         -5762168589392890821L, -5017689672807317317L,  5473175449239068764L,
         -3711301587478299670L,  8497887689556816981L,  8189696803685397753L,
         -8812087071370563647L,  8622426530496559856L,  -637586155481121234L,
         3889415814579951453L, -5871534805606488876L,  7611999930936906682L,
         -1576989345667753915L,  6867945765436218525L,  5202158881319978590L,
         2045239002982159681L,  4663254536949944197L,  8876493868030189101L,
         4935099116978923557L,   446776610031439568L,   334227789590223137L,
         -5381919626658211052L,   510764511670686813L,  8233086459162481229L,
         976926462235729074L,  1536544679897819789L,  8996157192669372948L,
         -3104488001329546301L, -2048373571520530732L,  2867036562008815199L,
         -1038239521452990857L,  1392156399255463136L, -2462986381111916505L,
         3281922332739427659L},
        // Philox(number=4, width=64, key=np.array([67280421310721, -7046029254386353131]).astype('uint64')).advance(2**192).random_raw(40).astype('int64')
        {234199833207670492L,  4847236961490835302L,  4652995647109309910L,
         -3737386356448340712L, -5273383760715124519L, -3647957810120825499L,
         5146817817305263920L,  5710973906845063179L, -1479449555641285865L,
         4084674574582715314L, -5547600708256898652L, -4421640461296589483L,
         -2968992335347510287L, -4790862279320238050L, -2473190691392812606L,
         965983568262991078L,   601327440871821012L,  8223565539892887311L,
         7546441310634873026L,  2825517271552261878L,  1821450327999942380L,
         1829354945050293158L, -4141883204296663957L,  2272925410140103105L,
         6950466720264053689L,   942049061182241074L,  -423320710605977014L,
         -7153892430601162036L, -3577327671114607603L,  2251213489013696162L,
         -869366985991136417L,  6210870867759981069L,  8104504070499194349L,
         -5828300645374305433L, -8988635423527025878L,  2037830179166981888L,
         600555068878135939L, -1046966376945680441L,  9153700819137910983L,
         6246833740445288808L},
        // Philox(number=4, width=64, key=np.array([123, 456]).astype('uint64'), counter=np.array([789, 10, 11, 12]).astype('uint64')).advance(2**192).random_raw(40).astype('int64')
        {8157299100059745334L,  4713970036167630570L,  1925775445559372963L,
         7180361223621361785L,  8843524849090010032L,  -960383571525922334L,
         -2545670064987878013L, -2968163338076222462L, -5106533345134101839L,
         8984560659256704654L,  6576648049919544319L,  8145632934135680489L,
         -3770413115017865215L, -2537462036822202750L,  4963131869106796019L,
         -4485390882674206349L,  6844429631864831341L,   995181801821742817L,
         3995309260950667201L,   161582214927090305L,  1769434531125855763L,
         -5468502469648815645L,  1006207750638600255L, -1638647394071042494L,
         -5828990503353515893L, -4765576091717507787L,  -615245979548517961L,
         8593941375848057311L,  7419589535498155068L,  8238031295047420234L,
         -902749205580949727L, -6780311655663106756L, -1758982342294001289L,
         1305848364946456461L,  7693624295610312003L,   776998124829998570L,
         -2170156404632340022L, -5784906183293255807L, -4690407458513825180L,
         -4835972139341539591L},
    };

    @ParameterizedTest
    @MethodSource
    void testReferenceCode(long[] seed, long[] expected) {
        RandomAssert.assertEquals(expected, new Philox4x64(seed));
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
        final long[][] seeds = {
            {67280421310721L},
            {67280421310721L, 0, 1},
        };
        final int n = 10;
        for (long[] seed : seeds) {
            final int[] expected = new Philox4x64(seed).ints(n).toArray();
            for (int i = seed.length + 1; i <= 6; i++) {
                final long[] padded = Arrays.copyOf(seed, i);
                RandomAssert.assertEquals(expected, new Philox4x64(padded));
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
    private static Philox4x64 skip(Philox4x64 rng, int n) {
        for (int i = n; --i >= 0;) {
            rng.next();
        }
        return rng;
    }

    @ParameterizedTest
    @MethodSource
    void testJump(long[] seed, long[] expected, long[] expectedAfterJump) {
        for (int i = 0; i <= 4; i++) {
            RandomAssert.assertJumpEquals(
                Arrays.copyOfRange(expected, i, expected.length),
                Arrays.copyOfRange(expectedAfterJump, i, expectedAfterJump.length),
                skip(new Philox4x64(seed), i));
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
    void testLongJump(long[] seed, long[] expected, long[] expectedAfterJump) {
        for (int i = 0; i <= 4; i++) {
            RandomAssert.assertLongJumpEquals(
                Arrays.copyOfRange(expected, i, expected.length),
                Arrays.copyOfRange(expectedAfterJump, i, expectedAfterJump.length),
                skip(new Philox4x64(seed), i));
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
    void testInternalCounter(long[] seed1, long[] seed2) {
        RandomAssert.assertNextLongEquals(10,
            skip(new Philox4x64(seed1), 4),
            new Philox4x64(seed2));
    }

    static Stream<Arguments> testInternalCounter() {
        // Test of counter increment. Note that the value of -1 is all bits set and incrementing
        // will carry a 1-bit to the next counter up.
        final long key0 = 67280421310721L;
        final long key1 = 1234L;

        return Stream.of(
            Arguments.of(new long[] {key0, key1,  0,  0,  0,  0},
                         new long[] {key0, key1,  1,  0,  0,  0}),
            Arguments.of(new long[] {key0, key1, -1,  0,  0,  0},
                         new long[] {key0, key1,  0,  1,  0,  0}),
            Arguments.of(new long[] {key0, key1, -1, -1,  0,  0},
                         new long[] {key0, key1,  0,  0,  1,  0}),
            Arguments.of(new long[] {key0, key1, -1, -1, -1,  0},
                         new long[] {key0, key1,  0,  0,  0,  1}),
            Arguments.of(new long[] {key0, key1, -1, -1, -1, -1},
                         new long[] {key0, key1,  0,  0,  0,  0})
        );
    }

    @Test
    void testJumpCounter() {
        Philox4x64 rng1 = new Philox4x64(new long[] {1234L, 0, -1, 0, -1, 0});
        rng1.jump();
        Philox4x64 rng2 = new Philox4x64(new long[] {1234L, 0, -1, 0, 0, 1});
        RandomAssert.assertNextLongEquals(10, rng1, rng2);

        rng1 = new Philox4x64(new long[] {1234L, 0, -1, -1, -1, 0});
        rng1.jump();
        rng2 = new Philox4x64(new long[] {1234L, 0, -1, -1, 0, 1});
        RandomAssert.assertNextLongEquals(10, rng1, rng2);
    }

    @Test
    void testLongJumpCounter() {
        final Philox4x64 rng1 = new Philox4x64(new long[] {1234L, 0, -1, -1, -1, 0});
        rng1.longJump();
        final Philox4x64 rng2 = new Philox4x64(new long[] {1234L, 0, -1, -1, -1, 1});
        RandomAssert.assertNextLongEquals(10, rng1, rng2);
    }

    @ParameterizedTest
    @ValueSource(doubles = {0x1.0p258, 0x1.0p456, Double.MAX_VALUE})
    void testJumpThrowsWithInvalidDistance(double distance) {
        final Philox4x64 rng = new Philox4x64(new long[] {1234});
        Assertions.assertThrows(IllegalArgumentException.class, () -> rng.jump(distance));
    }

    @ParameterizedTest
    @ValueSource(ints = {258, 456, Integer.MAX_VALUE})
    void testJumpPowerOfTwoThrowsWithInvalidDistance(int logDistance) {
        final Philox4x64 rng = new Philox4x64(new long[] {1234});
        Assertions.assertThrows(IllegalArgumentException.class, () -> rng.jumpPowerOfTwo(logDistance));
    }

    @ParameterizedTest
    @MethodSource(value = "testJump")
    void testArbitraryJumpMatchesJump(long[] seed, long[] ignored, long[] ignored2) {
        for (int i = 0; i <= 4; i++) {
            final Philox4x64 rng1 = skip(new Philox4x64(seed), i);
            final Philox4x64 rng2 = skip(new Philox4x64(seed), i);
            rng1.jump();
            rng2.jump(0x1.0p130);
            RandomAssert.assertNextLongEquals(10, rng1, rng2);
        }
    }

    @ParameterizedTest
    @MethodSource(value = "testJump")
    void testArbitraryJumpPowerOfTwoMatchesJump(long[] seed, long[] ignored, long[] ignored2) {
        for (int i = 0; i <= 4; i++) {
            final Philox4x64 rng1 = skip(new Philox4x64(seed), i);
            final Philox4x64 rng2 = skip(new Philox4x64(seed), i);
            rng1.jump();
            rng2.jumpPowerOfTwo(130);
            RandomAssert.assertNextLongEquals(10, rng1, rng2);
        }
    }

    @ParameterizedTest
    @MethodSource(value = "testJump")
    void testArbitraryJumpMatchesLongJump(long[] seed, long[] ignored, long[] ignored2) {
        for (int i = 0; i <= 4; i++) {
            final Philox4x64 rng1 = skip(new Philox4x64(seed), i);
            final Philox4x64 rng2 = skip(new Philox4x64(seed), i);
            rng1.longJump();
            rng2.jump(0x1.0p194);
            RandomAssert.assertNextLongEquals(10, rng1, rng2);
        }
    }

    @ParameterizedTest
    @MethodSource(value = "testJump")
    void testArbitraryJumpPowerOfTwoMatchesLongJump(long[] seed, long[] ignored, long[] ignored2) {
        for (int i = 0; i <= 4; i++) {
            final Philox4x64 rng1 = skip(new Philox4x64(seed), i);
            final Philox4x64 rng2 = skip(new Philox4x64(seed), i);
            rng1.longJump();
            rng2.jumpPowerOfTwo(194);
            RandomAssert.assertNextLongEquals(10, rng1, rng2);
        }
    }

    @ParameterizedTest
    @MethodSource
    void testArbitraryJumpCounter(long[] seed1, double distance, long[] seed2) {
        // Test the buffer in a used and partially used state
        for (int i = 0; i < 2; i++) {
            final Philox4x64 rng1 = skip(new Philox4x64(seed1), i);
            final Philox4x64 rng2 = skip(new Philox4x64(seed2), i);
            rng1.jump(distance);
            RandomAssert.assertNextLongEquals(10, rng1, rng2,
                () -> String.format("seed=%s, distance=%s", Arrays.toString(seed1), distance));
        }
    }

    static Stream<Arguments> testArbitraryJumpCounter() {
        // Test of counter increment. Note that the value of -1 is all bits set and incrementing
        // will carry a 1-bit to the next counter up.
        final long key0 = 67280421310721L;
        final long key1 = 1234L;

        final Stream.Builder<Arguments> builder = Stream.builder();

        // Any power of two jump can be expressed as a double
        testArbitraryJumpPowerOfTwoCounter().map(Arguments::get).forEach(objects -> {
            final int logDistance = (int) objects[1];
            final double distance = Math.scalb(1.0, logDistance);
            builder.add(Arguments.of(objects[0], distance, objects[2]));
        });

        // Largest allowed jump
        builder.add(Arguments.of(new long[] {key0, key1, 0, 0, 0, 0}, Math.nextDown(0x1.0p258),
                                 new long[] {key0, key1, 0, 0, 0, -1L << 11}));

        // Arbitrary jumps
        builder.add(Arguments.of(new long[] {key0, key1, 0, 0, 0, 0}, 0x1.0p3 + 0x1.0p40,
                                 new long[] {key0, key1, 2 + (1L << 38), 0, 0, 0}));
        builder.add(Arguments.of(new long[] {key0, key1, 0, 0, 0, 0}, 0x1.0p40 + 0x1.0p73,
                                 new long[] {key0, key1, 1L << 38, 1 << 7, 0, 0}));
        builder.add(Arguments.of(new long[] {key0, key1, 0, 0, -1, 0}, 0x1.0p195 + 0x1.0p169,
                                 new long[] {key0, key1, 0, 0, -1 + (1L << 39), 3}));
        builder.add(Arguments.of(new long[] {key0, key1, 0, -1, 0, 0}, 0x1.0p73 + 0x1.0p23,
                                 new long[] {key0, key1, 1 << 21, -1 + (1 << 7), 1, 0}));
        builder.add(Arguments.of(new long[] {key0, key1, 0, 0, 0, 0}, 1234 * 0x1.0p34 + 5678 * 0x1.0p66,
                                 new long[] {key0, key1, 1234L << 32, 5678, 0, 0}));

        final SplittableRandom rng = new SplittableRandom();
        for (int i = 0; i < 10; i++) {
            final long[] counter1 = rng.longs(4).toArray();
            // Jump in range [0, 2^256): 256 - 63 = 193
            final double counterDistance = Math.abs(Math.scalb((double) rng.nextLong(), rng.nextInt(193)));
            final BigInteger jump = new BigDecimal(counterDistance).toBigInteger();
            final long[] counter2 = add(counter1, jump);
            builder.add(Arguments.of(new long[] {key0, key1, counter1[0], counter1[1], counter1[2], counter1[3]},
                                     jump.doubleValue() * 4,
                                     new long[] {key0, key1, counter2[0], counter2[1], counter2[2], counter2[3]}));
        }
        return builder.build();
    }

    @ParameterizedTest
    @MethodSource
    void testArbitraryJumpPowerOfTwoCounter(long[] seed1, int logDistance, long[] seed2) {
        // Test the buffer in a used and partially used state
        for (int i = 0; i < 2; i++) {
            final Philox4x64 rng1 = skip(new Philox4x64(seed1), i);
            final Philox4x64 rng2 = skip(new Philox4x64(seed2), i);
            rng1.jumpPowerOfTwo(logDistance);
            RandomAssert.assertNextLongEquals(10, rng1, rng2,
                () -> String.format("seed=%s, logDistance=%d", Arrays.toString(seed1), logDistance));
        }
    }

    static Stream<Arguments> testArbitraryJumpPowerOfTwoCounter() {
        // Test of counter increment. Note that the value of -1 is all bits set and incrementing
        // will carry a 1-bit to the next counter up.
        final long key0 = 67280421310721L;
        final long key1 = 1234L;

        final Stream.Builder<Arguments> builder = Stream.builder();
        // Jumps for each part of the counter
        builder.add(Arguments.of(new long[] {key0, key1, 0, 0, 0, 0}, 2,
                                 new long[] {key0, key1, 1, 0, 0, 0}));
        builder.add(Arguments.of(new long[] {key0, key1, 0, 0, 0, 0}, 32,
                                 new long[] {key0, key1, 1 << 30, 0, 0, 0}));
        builder.add(Arguments.of(new long[] {key0, key1, 0, 0, 0, 0}, 99,
                                 new long[] {key0, key1, 0, 1L << 33, 0, 0}));
        builder.add(Arguments.of(new long[] {key0, key1, 0, 0, 0, 0}, 157,
                                 new long[] {key0, key1, 0, 0, 1L << 27, 0}));
        builder.add(Arguments.of(new long[] {key0, key1, 0, 0, 0, 0}, 244,
                                 new long[] {key0, key1, 0, 0, 0, 1L << 50}));
        // Largest jump does not wrap the overflow bit. It should be lost
        // to the unrepresented 257-th bit of the counter.
        builder.add(Arguments.of(new long[] {key0, key1, -1, -1, -1, -1}, 257,
                                 new long[] {key0, key1, -1, -1, -1, -1 + (1L << 63)}));
        // Roll-over by incrementing the counter by 1.
        // Note that the Philox counter is incremented by 1 upon first call to regenerate
        // the output buffer. This test aims to use the jump to rollover the bits
        // so the counter must be initialised 1 before the rollover threshold,
        // i.e. -2 + 1 (increment) + 1 (jump) == 0 + rollover.
        // The test uses a variable skip forward of the generator to cover cases of counter
        // increment before or after jump increment.
        builder.add(Arguments.of(new long[] {key0, key1, -2,  0,  0,  0}, 2,
                                 new long[] {key0, key1, -1,  0,  0,  0}));
        builder.add(Arguments.of(new long[] {key0, key1, -2, -1,  0,  0}, 2,
                                 new long[] {key0, key1, -1, -1,  0,  0}));
        builder.add(Arguments.of(new long[] {key0, key1, -2, -1, -1,  0}, 2,
                                 new long[] {key0, key1, -1, -1, -1,  0}));
        builder.add(Arguments.of(new long[] {key0, key1, -2, -1, -1, -1}, 2,
                                 new long[] {key0, key1, -1, -1, -1, -1}));
        // Since carry addition uses 32-bit integers also test rollover within a long.
        // Low should rollover to high by jump addition.
        final long l = 0xffff_ffffL;
        final long h = l + 1;
        builder.add(Arguments.of(new long[] {key0, key1, l - 1, 0, 0, 0}, 2,
                                 new long[] {key0, key1, h - 1, 0, 0, 0}));
        builder.add(Arguments.of(new long[] {key0, key1, 0, l, 0, 0}, 66,
                                 new long[] {key0, key1, 0, h, 0, 0}));
        builder.add(Arguments.of(new long[] {key0, key1, 0, 0, l, 0}, 130,
                                 new long[] {key0, key1, 0, 0, h, 0}));
        builder.add(Arguments.of(new long[] {key0, key1, 0, 0, 0, l}, 194,
                                 new long[] {key0, key1, 0, 0, 0, h}));
        // Arbitrary jumps
        final SplittableRandom rng = new SplittableRandom();
        for (int i = 0; i < 10; i++) {
            final long[] counter1 = rng.longs(4).toArray();
            // Jump in range [0, 2^256)
            final int logDistance = rng.nextInt(256);
            final BigInteger jump = BigInteger.ONE.shiftLeft(logDistance);
            final long[] counter2 = add(counter1, jump);
            builder.add(Arguments.of(new long[] {key0, key1, counter1[0], counter1[1], counter1[2], counter1[3]},
                                     logDistance + 2,
                                     new long[] {key0, key1, counter2[0], counter2[1], counter2[2], counter2[3]}));
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
    void testArbitraryJumpCounterWithSkip(long[] seed1, long distance, long[] seed2) {
        Assertions.assertNotEquals((double) distance, distance + 1.0,
            "Small distance required to allow jumping within the buffer position");
        // Skip within the generator output buffer
        for (int i = 0; i <= 4; i++) {
            // Jump within the generator output buffer
            for (int j = 0; j <= 4; j++) {
                final Philox4x64 rng1 = skip(new Philox4x64(seed1), i);
                final Philox4x64 rng2 = skip(new Philox4x64(seed2), i + j);
                rng1.jump(distance + j);
                RandomAssert.assertNextLongEquals(10, rng1, rng2,
                    () -> String.format("seed=%s, distance=%s", Arrays.toString(seed1), distance));
            }
        }
    }

    static Stream<Arguments> testArbitraryJumpCounterWithSkip() {
        final long key0 = 67280421310721L;
        final long key1 = 1234L;

        final Stream.Builder<Arguments> builder = Stream.builder();
        final SplittableRandom rng = new SplittableRandom();
        for (int i = 0; i < 5; i++) {
            final long[] counter1 = rng.longs(4).toArray();
            // Jump in range [0, 2^51)
            final long counterDistance = rng.nextLong(1L << 51);
            final BigInteger jump = BigInteger.valueOf(counterDistance);
            final long[] counter2 = add(counter1, jump);
            builder.add(Arguments.of(new long[] {key0, key1, counter1[0], counter1[1], counter1[2], counter1[3]},
                                     counterDistance * 4,
                                     new long[] {key0, key1, counter2[0], counter2[1], counter2[2], counter2[3]}));
        }
        return builder.build();
    }

    private static long[] add(long[] counter, BigInteger jump) {
        final BigInteger sum = LongJumpDistancesTest.toBigInteger(counter).add(jump);
        final long[] value = LongJumpDistancesTest.toLongArray(sum);
        // Return result with the same counter size
        return Arrays.copyOf(value, counter.length);
    }

    @Test
    void userGuideExample1() {
        ArbitrarilyJumpableUniformRandomProvider jumpable = new Philox4x64(SEEDS[3]);

        double distance = 42;
        for (int i = 0; i < 5; i++) {
            // Copy the state and then jump ahead
            UniformRandomProvider copy = jumpable.jump(distance);

            // Catch up the jump using the native 64-bit output
            for (int j = 0; j < distance; j++) {
                copy.nextLong();
            }

            // The copy matches the jumped generator
            Assertions.assertEquals(copy.nextLong(), jumpable.nextLong());
        }
    }

    @Test
    void userGuideExample2() {
        ArbitrarilyJumpableUniformRandomProvider jumpable = new Philox4x64(SEEDS[3]);

        int logDistance = 123;
        ArbitrarilyJumpableUniformRandomProvider copy = jumpable.jumpPowerOfTwo(logDistance);

        // Catch up the jump using: 4 * 2^119 + 2^121 + 2^122
        copy.jumpPowerOfTwo(logDistance - 4);
        copy.jumpPowerOfTwo(logDistance - 4);
        copy.jumpPowerOfTwo(logDistance - 2);
        copy.jumpPowerOfTwo(logDistance - 4);
        copy.jumpPowerOfTwo(logDistance - 1);
        copy.jumpPowerOfTwo(logDistance - 4);

        // The copy matches the jumped generator
        Assertions.assertEquals(copy.nextLong(), jumpable.nextLong());
    }
}
