<!---
 Licensed to the Apache Software Foundation (ASF) under one or more
 contributor license agreements.  See the NOTICE file distributed with
 this work for additional information regarding copyright ownership.
 The ASF licenses this file to You under the Apache License, Version 2.0
 (the "License"); you may not use this file except in compliance with
 the License.  You may obtain a copy of the License at

      http://www.apache.org/licenses/LICENSE-2.0

 Unless required by applicable law or agreed to in writing, software
 distributed under the License is distributed on an "AS IS" BASIS,
 WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 See the License for the specific language governing permissions and
 limitations under the License.
-->

Apache Commons RNG Stress Test Endianness
===================

The **Dieharder** and **TestU01** test suites read raw binary data. This is done using the native
platform byte order or [Endianness](https://en.wikipedia.org/wiki/Endianness). The stress test
application can support either big-endian or little-endian format. You can determine the required
format using the provided utility program:

        > gcc src/main/c/testendianness.c -o testendianness && ./testendianness
        Little-endian

An example of what happens when you set each byte in a 4-byte integer is provided if an argument
is given to the utility program:

        > ./testendianness demo
        Little-endian

        Test byte order using 4 bytes for an unsigned integer
        byte   network order   logical order   value
        [0]    ff000000        000000ff               255
        [1]    00ff0000        0000ff00             65280
        [2]    0000ff00        00ff0000          16711680
        [3]    000000ff        ff000000        4278190080

Each byte is set in turn to `255` or `0xff` in hex. The network order is the standard byte order
for data transmission and is big-endian. The logical order is determined by testing each
of the bytes in the `unsigned int` using bit shifts to isolate each byte, most significant first, and
testing if it is set. The value of the `unsigned int` is output for reference.

In this little-endian example when the last byte is set the value is highest, i.e. the most
significant byte is the last.

