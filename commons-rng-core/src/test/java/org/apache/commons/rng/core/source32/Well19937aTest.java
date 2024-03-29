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

import org.apache.commons.rng.core.RandomAssert;
import org.junit.jupiter.api.Test;

class Well19937aTest {
    /** The size of the array seed. */
    private static final int SEED_SIZE = 624;

    @Test
    void testReferenceCode() {
        final int[] base = {
            0x2c2878c6, 0x47af36c4, 0xf422e677, 0xf08fd8d3, 0xee9a47c7, 0xba983942, 0xa2a9f9a5, 0x1d443748,
            0x8fc260b2, 0x5275c681, 0x4a2f5a28, 0x2911683d, 0xa204c27e, 0xb20a6a26, 0x54ba33be, 0x67d63eb0,
            0xdc8174cf, 0x3e73a4bc, 0x6fce0775, 0x9e6141fc, 0x5232218a, 0x0fa9e601, 0x0b6fdb4a, 0xf10a0a8c,
            0x97829dba, 0xc60b0778, 0x0566db41, 0x620807aa, 0x599b89c9, 0x1a34942b, 0x6baae3da, 0x4ba0b73d
        };
        final int[] seed = new int[624];
        for (int i = 0; i < seed.length; ++i) {
            seed[i] = base[i % base.length] + i;
        }

        final Well19937a rng = new Well19937a(seed);

        final int[] expectedSequence = {
            0xdb784719, 0xead77ddc, 0x926f567b, 0x95cf8aff, 0x1097e8d5, 0x486a7ca8, 0x97893622, 0xd2a69352,
            0x99e777cb, 0xf19f7dea, 0xf7558586, 0x1d8b0188, 0xe1570edd, 0xb43dd614, 0x9e3f95f3, 0x329ee317,
            0xdf3bcb8e, 0xb80fe779, 0xdf761444, 0x3064ffba, 0x9cb6fb9f, 0xa515e0ae, 0x546a34d2, 0xf6de0926,
            0x447a5908, 0xec8f7324, 0x66d11776, 0x6abf70cc, 0x9dbb253e, 0xe5d49a5d, 0x06b459d9, 0xa78b2bc0,
            0x8fde0f04, 0xb4468449, 0xb783bfff, 0x5e1f43b0, 0xb705349b, 0x7fd12154, 0xe73109bf, 0xa4d9eb27,
            0x61185ec8, 0x09cd3c3d, 0x59d1e914, 0x375261fc, 0x5597a0ab, 0x336178f2, 0x21621b89, 0x8f82d5ad,
            0xe15779e6, 0x676b70ea, 0x1c5975af, 0x68a76af3, 0x119c2f29, 0xf6bccbc7, 0x003c9d3f, 0x518d1774,
            0xe446bd32, 0x9f47c1e5, 0xf154d298, 0x6b34eef0, 0x8c879e92, 0x9aa381f4, 0xee83cb52, 0xca8e7a11,
            0x837d689c, 0xf469f58c, 0xe36073ce, 0x9f8760e7, 0x8d4259c7, 0xf70bdd26, 0x64128cad, 0x7f01ebec,
            0xe30f254a, 0xa6a3a46b, 0x55420864, 0x8f063a2a, 0x0c8c333a, 0x6a2a0e07, 0x8c50ee8f, 0xb6341941,
            0x1cf696fd, 0xf9d8f054, 0x25c229cf, 0xda45fedd, 0x297073d5, 0x9cf3ee98, 0x108be4ef, 0xc6e0de5e,
            0x0f513d0d, 0x6e48e142, 0xec7c7454, 0xf2f180ce, 0x5249af0d, 0xb72e7d84, 0x4f9fa9c8, 0xdf837741,
            0xddb5bea9, 0xb2c6591d, 0xf7a6e146, 0x3f60b635, 0xb31170e7, 0xd0e36df6, 0xf5d460cf, 0xb849ee17,
            0x56e84704, 0xb7b6da6b, 0x5c2ca062, 0x7dec45fd, 0xa8cbf340, 0x93f5f2f0, 0xcb5668f2, 0x436af93e,
            0x03b2b035, 0x7f4e061a, 0xb8ee4b2d, 0x05dce0b2, 0xb01151e3, 0xe3abc189, 0x9eb9242d, 0x6805975c,
            0x0a0cc3b1, 0xc97667e1, 0x105b73eb, 0xcb778e36, 0x0a028fac, 0x8a3ca344, 0xca1ea85a, 0xae4870cc,
            0xdcdf3019, 0x6a50166a, 0xde04cbde, 0x0442eed1, 0x0b974635, 0xb5db8403, 0xaca4b8ee, 0x0d36bd49,
            0xfcada53e, 0xd9eae74e, 0xd2113c5c, 0x8e0e821e, 0xee6719fd, 0x11f8095e, 0x7ab455c0, 0x289eb707,
            0xa3bd740a, 0x9c9244fc, 0x4d712982, 0x6250e4db, 0x112a7489, 0x8435dccc, 0x4722e39a, 0xcd19c89e,
            0x07285159, 0x3eb1a526, 0xb332d4d9, 0xf67ee352, 0x106f97d4, 0xe361d77d, 0xdbf14c7f, 0x808f5c74,
            0x47ae7223, 0x55115cef, 0x8e5f7deb, 0xb6a3cb73, 0x64c0f4d8, 0x2bb31eb9, 0x2f59bd48, 0xbd92cde7,
            0xa08a3813, 0x3a37fdad, 0x5a779812, 0x7cae086d, 0x3eb5733a, 0x7265ee53, 0xf49d5e02, 0x6b9c2c96,
            0x2beccd04, 0xc10a8e49, 0xd4a3c684, 0xd8b17443, 0x2f8d8563, 0xbe289fe4, 0x3e20e0c1, 0xfe89eeeb,
            0x7268c974, 0x71788032, 0xcf78efd0, 0x5fb093b1, 0xf05cd668, 0xeec6fa7d, 0x2b76c86c, 0xbc7dcfa2,
            0x2c6eb71a, 0x171f1d1b, 0x4137c299, 0x7d2fb76b, 0x09598cd6, 0xf0a8baf8, 0xd51bf570, 0xa6d49d03,
            0x71c8eed0, 0x78385472, 0x36c301e2, 0xf7e55bfd, 0xaf20be41, 0x9a0160b3, 0x57c5edb8, 0xe845794b,
            0x257a9877, 0xada2d4ac, 0x937a47cd, 0xdc9e9501, 0xa92d9211, 0x9272b8c9, 0xace56f0c, 0xdece200a,
            0x31aa1949, 0x0296fd96, 0x3d58e964, 0x83a37e27, 0xe49d265c, 0xe03a9498, 0x984f12e1, 0xae0f646f,
            0x78b1a66a, 0x52d4cf98, 0x001eccfe, 0xad0c3928, 0xd08efc01, 0x35ee693d, 0x31dd3f70, 0x6945fd63,
            0xae13d8da, 0xca175f8c, 0x790322bc, 0xf0295ed9, 0x7a97a49a, 0xcfb0c103, 0x34e17ff9, 0xfb5cb0e0,
            0x592fea10, 0x9047b70d, 0xe4cf08b6, 0x809df842, 0x33be7743, 0x01bae4af, 0x88cc70bc, 0x7749c15f,
            0xdd7ad451, 0xe8b375e8, 0x5ee42c3d, 0xedbcc901, 0x0594c916, 0xf49f6706, 0x72a41dc9, 0x100968ed,
            0xe864c0b3, 0xc523762f, 0x1ca1930f, 0x4c19935d, 0xa9d6528b, 0xc6eca043, 0x860864f3, 0x99c12358,
            0x425ff7dc, 0x95c71b16, 0xd66c13ae, 0x31c9f8c0, 0x3c754f3f, 0x81a5e603, 0x1b6be909, 0xa8b15681,
            0xb38a2bee, 0x819710eb, 0x0522f161, 0xd46c821d, 0x319ee76d, 0x90240f80, 0x203f2170, 0x20d97dff,
            0xe51f8524, 0x269d9039, 0x81e9982e, 0x3a5e21e5, 0x414ce824, 0x72ea50a0, 0x0a1a5467, 0x772f27ae,
            0x7862dd0e, 0x62b4e42b, 0xbddbe646, 0x165daa22, 0x5679abe4, 0x36d5e8c0, 0x32a653f2, 0x56d8ee2f,
            0x6991db71, 0x9e4168d9, 0xcd300f96, 0x6953405e, 0x272d881a, 0xdd1908e9, 0x2c279907, 0x633724e4,
            0xd8f950be, 0x421e3556, 0x0ae237b7, 0x41f84571, 0x94510980, 0x018befd4, 0x6836131b, 0x0baf98ed,
            0xf6364e27, 0x648df3d2, 0x2e9cd1dc, 0x700a865b, 0xa4e93825, 0x18978f09, 0x46f2ed7d, 0x765d3cd7,
            0xb42065ce, 0xb62be5af, 0x1bc3d46a, 0xa45900cf, 0x43783dcf, 0x35fab81b, 0x244f66c0, 0x43daf4cf,
            0x5ac6bfeb, 0x23266615, 0xb1dd55f4, 0x351adce5, 0xcb3cd8ca, 0xe88b1f32, 0x49d09dde, 0x6a22b882,
            0x28667605, 0xbd272138, 0x5c932ac8, 0x421c8770, 0xa990eb3f, 0x5ae981a9, 0xaf3de7af, 0xdc82e233,
            0xb407e229, 0xcc2fd17f, 0x6a9f2a67, 0x786ac0e4, 0x43006859, 0xe81bae94, 0x1282fc1c, 0x50bab3f5,
            0xb1e322cb, 0x70fe9375, 0x2559c0ce, 0x6361d2dd, 0x7b1f1f3a, 0x8f680209, 0xe5c3b9cb, 0x6912f2b7,
            0x9b09d836, 0xfca04eb4, 0xc883fcb4, 0xe83fd1ac, 0x2e64c6b2, 0x820bc8ef, 0x028dea34, 0x6def4df4,
            0x82ee5362, 0x93fe9ec3, 0x55e6cb69, 0x359e7d83, 0x4b84cbfc, 0x6dc116ef, 0xd89de764, 0xfd6bfc42,
            0x430682e0, 0x70596738, 0x3473b11b, 0x69ce7c05, 0xaeee53af, 0x612f4dc9, 0x159c58c7, 0x425e47d5,
            0x114debaf, 0xb7e863cd, 0x17bb39f7, 0xc6ea02f2, 0x46ad4a3e, 0x5a946c1f, 0xc9997505, 0xbd69f6f3,
            0xedc978ba, 0x8d148bad, 0x96b806c1, 0x657df4d4, 0x65256e68, 0x8f0b7d75, 0x37975127, 0xde3b2960,
            0x3b1bc65c, 0x035e9396, 0x3a74d42f, 0x2ace5a86, 0xd9415a4e, 0xcb23dd3d, 0x6eb0500b, 0xde1563af,
            0x937c45d6, 0xcd3fdcd9, 0xc6db645c, 0xdaefb19f, 0xfc164320, 0xe197d531, 0x2d60058f, 0x9b247afd,
            0x670f8a5e, 0x60c6a410, 0xfccface2, 0x0fedb167, 0x274a9d85, 0x44b797c3, 0xcedfc9f2, 0x1541d920,
            0xe20e1eaf, 0xdfe7da90, 0x03f0d730, 0x9ea5f77e, 0xa546c41c, 0x1007d64e, 0x28cca1f3, 0xccc575bb,
            0x5e941a39, 0xa5a92c63, 0x130e2af2, 0xedb32b02, 0x041e061c, 0x3ea0d181, 0xb01307c2, 0x20e9c670,
            0xf39c6a73, 0xefb19b7f, 0xdfd6a7e8, 0x2a56f9d0, 0x74a69c9f, 0xbe01e235, 0xde87e938, 0x96334d95,
            0x75f1fbc8, 0x735dddbb, 0x83c7fa20, 0xe6fb3b4a, 0xd4084f42, 0x24993b87, 0x63f4436c, 0x2de3a24b,
            0xbedd6589, 0xbd6a3479, 0xf024b455, 0xeb211ea9, 0x0c61d6ae, 0x751df13f, 0x02e8b5a4, 0x6780e3bf,
            0x20dc0311, 0x91daecd3, 0x6ea99ce1, 0x77a7b8a0, 0xd75ba385, 0xb8965883, 0x586dcd83, 0xe0c5fdec,
            0x93d9568a, 0xa5560c54, 0xaf9a07ea, 0x4d5ec6c1, 0x6c1c3b26, 0x386d3f06, 0x04b870f0, 0x6946d9b3,
            0x1c2d6795, 0x692e2edc, 0x920bb6c1, 0xf836e95b, 0xe1ada474, 0x96f1d15b, 0x3c3509bf, 0x0a23a0da,
            0x65a6218c, 0xbc11d5f6, 0x0af3a2d9, 0x631ac789, 0x6dd54965, 0x66333c4d, 0x02c1176c, 0x24c26935,
            0xf0f2572d, 0x76649116, 0xf9da5b72, 0x73d3467d, 0x0699dc1f, 0x20fa1608, 0x5eb54559, 0x8caffe45,
            0x80293bec, 0x83a6d938, 0x73b55dbc, 0xd1645736, 0x487e430e, 0x2417dd11, 0xd5a0f1ea, 0x7a1bb6a1,
            0x8991e390, 0xc3fa2169, 0x4a1775ed, 0xdb634ba5, 0x8125eb95, 0x22aa3425, 0x45f1a4b0, 0x8e5811a7,
            0x0f9392c1, 0x945b05a9, 0x2de29302, 0x6ec9fc9c, 0xde8430e9, 0x1ec7e796, 0xb733fde3, 0xcecbdbd5,
            0x410ce730, 0xc4c5f8d9, 0xa80ea934, 0xc02cd260, 0x902700ce, 0x359d3218, 0x28bd2ed8, 0x3461899f,
            0x03604cf7, 0xb468c281, 0x52347ece, 0xad560d83, 0xbaf6c0ec, 0x68db1e18, 0x5f39e55d, 0x541c7689,
            0xb53d61c9, 0x40217ed3, 0xc93c8e63, 0x80e1f590, 0x38cfc698, 0x71aa3cbd, 0x44cbb4be, 0xea38ac68,
            0xbbe70106, 0x2a3b6351, 0x80c07834, 0xa4bbb77f, 0x82cc8157, 0x4b980ed3, 0x7b7849e3, 0x0b83028b,
            0x51e7fd66, 0x3afaa743, 0xc85a3556, 0x80c0f053, 0xb258ebf9, 0x834e2b8f, 0x6c0126ea, 0x7c9feded,
            0x778730b0, 0x24257459, 0xfc6a50b6, 0x00aa3b88, 0x20285d7d, 0xf8b306d6, 0xcac24569, 0xc5b2f5c0,
            0x59ff45f8, 0xe51dd529, 0x198b12bc, 0x9f438135, 0x892be6ab, 0x896d50d9, 0x41cf67e3, 0xd09b25e7,
            0xca61eb6f, 0x82396527, 0xf205c6d3, 0x5e972495, 0xe1f8f369, 0x1f8be206, 0x136419b4, 0xf9abb95d,
            0x03e575db, 0xa80a9826, 0xf25f787b, 0x2aace961, 0x4186c92b, 0x94e5bc3c, 0x520c5fec, 0xed95e9ac,
            0xeb70a05d, 0xe86ed6bf, 0xa2da85f2, 0x6f72ecb5, 0x4be7fcb6, 0x7bb6f454, 0xd0f01d1f, 0x5d2eca8c,
            0x441f4bfb, 0x63ec2d16, 0x98ef254f, 0xac967d1b, 0x93dc8d63, 0x94220eec, 0x5316a296, 0xde02bbbc,
            0x3f82adad, 0xd1690fb7, 0x48a36325, 0xbe8e9ee1, 0x4dd2545e, 0x35b8c32f, 0xd5fe2f52, 0x157e0dd4,
            0x54280c10, 0x6e274a85, 0xdb5f75e5, 0xf73ea45b, 0xc986f013, 0x2b20266d, 0xf73c7310, 0xf41f553d,
            0x2bcf2cbb, 0x847491ca, 0x7ee9e8c3, 0xad717248, 0x37875770, 0x3f81e4c7, 0xa4007285, 0xe053528c,
            0x8173c5ef, 0xa44ce68f, 0xbb3dafd7, 0x5cfaac40, 0xd8f75ead, 0x3471ad70, 0x58cabbf5, 0x77244104,
            0x8aed4b70, 0x09c16ee6, 0xccd6670c, 0xecba76b7, 0x2fc17948, 0x2a12a1b1, 0x19ffa4b3, 0x265f35a8,
            0x7c6e2a0b, 0x080666d1, 0xd34c9272, 0x9d36f01c, 0x93084e78, 0xfe5e8f89, 0xbbbe3988, 0xb8ebaf94,
            0x0ce22924, 0x24711e81, 0xff2e26a1, 0xea9fb94e, 0xb7c38e93, 0x236130e0, 0x2732a2d0, 0xa9d06280,
            0xf10e8b9d, 0x6b245333, 0x8180172d, 0xed1e6657, 0xee56c62f, 0xcc4356f8, 0xfad519c3, 0x786fba71,
            0x47bada3f, 0xae3b65b0, 0x27e83da5, 0x86f0e214, 0x4eea8e42, 0x00d1177c, 0x2ed0d44c, 0xa8a1568e,
            0xd696dd52, 0x80fa7b06, 0x97f69054, 0xbeae04eb, 0x37d34a69, 0xbd4c5eda, 0x32fcbbe2, 0x65a1722c,
            0xca050560, 0x09ac7535, 0x4fab312f, 0xeb799417, 0xfa7ae7b4, 0xbf97c080, 0x77589171, 0x838b2dd3,
            0x9b9d2ab9, 0xf84ec802, 0xc768bd34, 0xf85f218b, 0xf315f7d2, 0x65562bdf, 0xd59c5607, 0x5969ebaf,
            0x484141c2, 0x87c17705, 0xb4af0795, 0x1e836c00, 0xecb02cc4, 0x8785355f, 0x6b779c5c, 0x374b2150,
            0x140b07b3, 0x4a29dac4, 0x9cbc29e4, 0x3b177d3e, 0xf1068044, 0x4b8bb017, 0x5c43ddd1, 0x2c1f9f5c,
            0x69e23041, 0x6490f1b3, 0x0d9a82b9, 0x102d05e9, 0xd74bab65, 0xe5e34598, 0x46f49fbc, 0xd3a34d64,
            0x7002a39f, 0x44425eb2, 0x98d34a56, 0xbd4a38a3, 0x0569e7e6, 0x5d73ab24, 0x61aee8de, 0x9ded6b1c,
            0x5d65245b, 0x40e27b71, 0x3d6d07ad, 0x906ac0d2, 0x2aef38b7, 0xcb3f2716, 0xecda9eeb, 0x2c1a17fb,
            0x15f8185f, 0xe58ea8c0, 0x49799853, 0xaf2dd85c, 0xc194d30f, 0xa255e3b8, 0x23b8970c, 0x3dcee9c7,
            0x5a3b57eb, 0xfce9a2d5, 0xfe6963f3, 0xb4b9ff22, 0xc962f14a, 0x5e1ee21d, 0xee985229, 0x4cd4ca6a,
            0x9fe80524, 0x5687c62c, 0x1abd3939, 0x1c65ca60, 0xb6b5a5a3, 0x2edf1739, 0x376d93d5, 0xf70da57a,
            0x1ab65128, 0xb12efd40, 0xccf21705, 0x3f074dc4, 0xa46625ae, 0x226c13a2, 0x22dcf58a, 0x1068abbb,
            0xdbfbb149, 0xf1ee441e, 0x90c07676, 0x8478f7db, 0x2683348c, 0x3284c732, 0xed85af3f, 0xbe2fd09e,
            0x1770b093, 0xd47b85ae, 0x1a4672a2, 0x629e337b, 0xb2bb1ebf, 0x58a74d60, 0xc5b85024, 0xd1c1cebd,
            0xb2977c3e, 0x1ef08aae, 0x6528a400, 0x0d951107, 0xe9e11c4d, 0x4beb27a8, 0x9a127fd8, 0x808b7c38,
            0x43ec3c96, 0x5b4a0a1b, 0xa6a8e545, 0x468ae3a2, 0x6fc6ebe5, 0x9daaf583, 0x7f476988, 0xc6a20cd9,
            0x194831e8, 0x1e0e9842, 0x0c0d6457, 0x6a4d89b4, 0x78234c60, 0xd7ab3f6b, 0x7b3ae1c2, 0x3681dd1f,
            0xdb057707, 0x7014b597, 0x53d79456, 0x744b7fd1, 0x25c2625d, 0x0d671ead, 0xfc80ee25, 0x3b4d9f37,
            0xe37307bb, 0x54b9979b, 0x05f871a8, 0x0ee2f239, 0xb7575c5a, 0x677de416, 0x15b2687e, 0x97b09591,
            0xe36fba0b, 0x628573d2, 0x82444fd5, 0x616276de, 0xa3c08817, 0x37d3cd25, 0xbb49ac01, 0x357675fb,
            0xac178c7e, 0xe0c7eb4a, 0x1549d9c6, 0x33fe23b8, 0x2c6635ec, 0x2af6fcec, 0xf1a5066c, 0xf956613c,
            0x4d0f1ddb, 0x952e171e, 0x740c4035, 0x46701731, 0xbd33defc, 0x46a49a7f, 0xad540543, 0x759cc8a7,
            0xf8cb674c, 0xd24ef3b9, 0xe58a6c8f, 0x2ee2df43, 0x198b612d, 0xf9f75216, 0xe69c5e54, 0x0bce7de5,
            0x45eeb6a2, 0x97ffd946, 0x9f5f6f0a, 0xa43d1da9, 0xbe5be234, 0x02963a1f, 0xb5174b46, 0x47a3db13,
            0x7a27c2db, 0x3275428f, 0x818c2917, 0xb84c98e5, 0xe3f5df74, 0x1331233e, 0x6efc27f4, 0xc748a58a,
            0xcab30192, 0xe65881f9, 0x9ce48085, 0x27228938, 0x5e329711, 0xe64b0896, 0x62194943, 0x8b8f8b3d,
            0xe9aacd0c, 0x1c451e28, 0x3fdeaf19, 0x1e75d605, 0xd2aa2f6a, 0xde2819db, 0xb9e06596, 0xf0006753,
            0x8100f1fc, 0xfbf2d9fd, 0x68ab2664, 0x5539f06e, 0xbc968add, 0xf88decf1, 0xa3f1c39c, 0x90ece141,
            0x3d16fc07, 0xad3b16be, 0xa23acbb7, 0x35346fdc, 0x6d26f685, 0x461ee7fe, 0x4513a27a, 0x8862dbe6,
            0xb6cc390f, 0x5a5e0680, 0xa6a4c40d, 0x1c0bf75a, 0x61e825cd, 0x54f1ec66, 0x9356230d, 0x962f925c,
            0xc3d612fc, 0x09d1d6ea, 0x4355812a, 0x6dd47e93, 0xe34007ec, 0xe1c076d4, 0x62b4be15, 0x761cd7e1,
            0xcb7c5d38, 0xe17bb275, 0x146953a9, 0x16b88982, 0x3a97835f, 0x2c2af581, 0x901d2c22, 0xf4204431,
            0xe5030cf0, 0x7302f0e0, 0x9bf74f3d, 0xcc7aae0f, 0xa000c921, 0x8c958a8a, 0xd352b799, 0xcc638378,
            0xde330469, 0xb246f09f, 0xc7fe4722, 0xdade299d, 0x19871c09, 0x3e8a17a6, 0x32ccc5bd, 0x052a2872,
            0xaf4dddf7, 0xc1b01f01, 0x3586f013, 0x9e7b2fd4, 0xbf7012d1, 0x0e9b1e83, 0xea8e5f5e, 0xc2b27679,
            0x3eadc34e, 0x69c1822b, 0x6da2dae7, 0xa0fcaca4, 0x2be94571, 0x6fac35d6, 0x17e99d78, 0xdd35e8d0,
            0xfabb7fda, 0x775f9247, 0x0fc40c55, 0xb23a25b5, 0xd2bf779a, 0xf925df44, 0x90023531, 0x322a0198,
            0x71e577cc, 0x98667c2d, 0x29cf5e30, 0x6ac16080, 0x91ac955d, 0x63ad293f, 0x45de2e89, 0x240d4300,
            0xa02fa8a1, 0x1233956e, 0xc77c51e3, 0x8a83b45c, 0x91cb00d6, 0xdd28a703, 0x025971ff, 0x9b521646,
            0x3852ae06, 0x69ec9023, 0x5b6f1131, 0x78903be0, 0xfe76031f, 0x002ef63f, 0xed2afa48, 0x6ed0e808,
            0xacaed16e, 0x9cad2f1f, 0x3331fc0f, 0xdb97a5c4, 0x5caf0272, 0x22927601, 0x9ce0d020, 0xe7a4ccf6,
            0x4e7af894, 0x2c001a6f, 0x38327139, 0xd2ca6839, 0x2e832201, 0x27c4216d, 0x839de77f, 0x2f127f0c,
        };

        RandomAssert.assertEquals(expectedSequence, rng);
    }

    @Test
    void testConstructorWithZeroSeedIsNonFunctional() {
        RandomAssert.assertNextIntZeroOutput(new Well19937a(new int[SEED_SIZE]), 2 * SEED_SIZE);
    }

    @Test
    void testConstructorWithSingleBitSeedIsFunctional() {
        RandomAssert.assertIntArrayConstructorWithSingleBitInPoolIsFunctional(Well19937a.class, 19937);
    }
}
