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
=========================================

The stress test application will output raw binary data for generated numbers. An integer is 4
bytes and a long is 8 bytes and thus the byte order or
[Endianness](https://en.wikipedia.org/wiki/Endianness) of the data
must be correct for the test application. The stress test application can support either big-endian
or little-endian format. The application will auto-detect the platform and will default to output
binary data using the native byte order.

You can determine the native platform format using the stress test application `endian` command:

        mvn package -P examples-stress
        java -jar target/examples-stress.jar endian

This will output:

        [INFO] LITTLE_ENDIAN

The **Dieharder** application and `stdin2testu01` bridge application for the **TestU01**
test suites read binary data using a c-library function that requires the data to be in the native
byte order. These test suites will be supported automatically.

The following sections show examples of verifying the endianness for a test application. The first
uses **Dieharder** which can read test data in both text and binary format. The second uses the
`stdin2testu01` bridge application to verify data passed to an application using a C API is
correct. The third verifies that any data tests using the `output` command are applicable to
the `stress` command.

Testing using file output
-------------------------

This example demonstrates writing text and binary data from the Java application to file. This can
then be passed to a test application that reads both formats. The example uses the **Dieharder**
application to demonstrate the result.

Build the `examples-stress` jar file:

        mvn package -P examples-stress

This will create a single jar file with all the dependencies in the `target` directory. The program
contains a simple command to output data from a random generator. To output data in both text
and binary format use the following commands:

        java -jar target/examples-stress.jar output KISS -s 1 -n 2048000 \
                -f DIEHARDER -o test.dh
        java -jar target/examples-stress.jar output KISS -s 1 -n 1000 \
                -f BINARY -o test.big -b big_endian --buffer-size 8192
        java -jar target/examples-stress.jar output KISS -s 1 -n 1000 \
                -f BINARY -o test.little -b little_endian --buffer-size 8192

This should produce the following output files:

| File | Description |
| --------- | ----------- |
| test.dh | Text file in the Dieharder format (unsigned integer data) |
| test.big | Binary file using the big-endian format |
| test.little | Binary file using the little-endian format |

Note that the `-n` parameter is the count of numbers in text output but the count of buffers in binary output. The example above has 2048 4-byte integers per 8192 buffer. The `-n` parameter has been adjusted so the output numbers are the same.

The data can then be used to run a test within **Dieharder**:

        dieharder -g 202 -d 0 -f test.dh
        dieharder -g 201 -d 0 -f test.little
        dieharder -g 201 -d 0 -f test.big

Only one of the raw binary files should match the result from the text file. This is the correct
endianness.

Note that **Dieharder** rewinds the file if necessary to get enough numbers for the test.
If the file is created to be large enough to use without rewind then the binary output can be
written direct to dieharder. For the birthdays test (`-d 0` option) 20,000,000 should be enough.
In the following example the stress test application directly writes to `stdout` which is then
piped to the `dieharder` application which reads using `stdin` (`-g 200` option):

        java -jar target/examples-stress.jar output KISS -s 1 -n 20000000 \
                -f DIEHARDER -o test.dh
        dieharder -g 202 -d 0 -f test.dh
        java -jar target/examples-stress.jar output KISS -s 1 -n 10000 \
                -f BINARY -b little_endian | dieharder -g 200 -d 0

If the results are not the same then the second command can be repeated with `-b big_endian`.

Remember to clean up the output files:

        rm -f test.dh test.big test.little

Testing using the Java to c bridge
----------------------------------

This example demonstrates passing data from the Java application to a `c` program. The `c` program
that receives the data can then call a library of test suites. This is the mechanism used by the
`stdin2testu01` bridge application for the **TestU01** test suite but is applicable to any library
with a `c` language API.

Build the `examples-stress` jar file:

        mvn package -P examples-stress

This will create a single jar file with all the dependencies in the `target` directory. The program
contains a simple test for the bridge between the Java application and a sub-process. This will
send a set of test integer data in binary format to the standard input (stdin) of a provided
application.

The `stdin2testu01` application is able to read the input binary data and convert it to human
readable format when run with the argument `raw32`. Details of how to build the `stdin2testu01`
application are provided in the [stress test](./stress_test.md) page.

The platform endianess is auto-detected by the Java application. To run the bridge test
use the following command:

        java -jar target/examples-stress.jar bridge ./stdin2testu01 raw32

This should produce the following output files:

| File | Description |
| --------- | ----------- |
| bridge.data | Text file containing the data sent by the Java application |
| bridge.out | Text file containing the stdout written by the sub-process |
| bridge.err | Text file containing the stderr written by the sub-process |

The `bridge.data` file contains 32 integers with a single bit set, least significant first.
Then 32 random integers. The data is written using the logical byte representation of the data
(this is done using bit shifts to isolate each byte in turn most significant first), the unsigned
integer value and the signed integer value. The contents will be similar to:

        00000000 00000000 00000000 00000001           1           1
        00000000 00000000 00000000 00000010           2           2
        00000000 00000000 00000000 00000100           4           4
        ...
        00100000 00000000 00000000 00000000   536870912   536870912
        01000000 00000000 00000000 00000000  1073741824  1073741824
        10000000 00000000 00000000 00000000  2147483648 -2147483648
        01011100 10001111 11110001 11000001  1552937409  1552937409
        10110000 01110101 10010011 00011100  2960495388 -1334471908

The `stdin2testu01` has been written to output in the same format when using the `raw32` mode.
If the data has been correctly read the `bridge.data` and `bridge.out` should match.
If the endianess is incorrect then the data sent by the Java application will not match the
data read by the sub-process. For example to swap the endianness use the `-b` option:

        java -jar target/examples-stress.jar bridge -b BIG_ENDIAN ./stdin2testu01 raw32

In this case the little-endian plaform has been sent big-endian data and the contents of the
`bridge.out` file begin with the reverse byte order:

        00000001 00000000 00000000 00000000    16777216    16777216
        00000010 00000000 00000000 00000000    33554432    33554432
        00000100 00000000 00000000 00000000    67108864    67108864
        ...

The least significant byte in the Java application is now the most significant and the integer
values do not match; the sub-process has incorrectly received the data.

Remember to clean up the output files:

        rm -f bridge.data bridge.out bridge.err

Testing the stress command
--------------------------

The `stress` command will open the provided application and send binary data. The data transfer
modes should match the equivalent modes in the `output` command. However the `output` command
can be used to output more formats and used to test data transfer to a test application that
accepts different input (such as the **Dieharder** text and binary format).

To verify that the `stress` command is sending the same data as the `output` command requires
piping data from each to a program. This example uses `stdin2testu01` with the raw mode.

Create a list for the `stress` command:

        java -jar target/examples-stress.jar list | grep SPLIT_MIX_64 > rng.list

Run the `stress` command:

        java -jar target/examples-stress.jar stress -l rng.list \
                -x 012345 \
                --prefix stress_ \
                -o overwrite \
                ./stdin2testu01 raw32 10

The `overwrite` mode will replace existing files and is used here to allow repeat testing.

Run the equivalent `output` command:

        java -jar target/examples-stress.jar output SPLIT_MIX_64 \
                -x 012345 \
                -f binary \
                -b little_endian \
                | ./stdin2testu01 raw32 10 > output.data

Note the use of the same hex seed with the `-x` option.

Compare the data in `stress_10_1` and `output.data`.
The number data in the file should be identical if the endianness was correctly configured.

        diff stress_10_1 output.data

The only difference should be the the header and footer added by the `stress` command
to the results file. If the endianness is incorrect for the `output` command then the number
data will not match. Note that the `output` command by default uses big endian for consistency
across all platforms; the `stress` command uses the native byte order of the platform.

An equivalent 64-bit output would use the `--raw64` option for each command:

        java -jar target/examples-stress.jar stress -l rng.list \
                -x 012345 \
                --prefix stress_ \
                -o overwrite \
                --raw64 \
                ./stdin2testu01 raw64 10
        java -jar target/examples-stress.jar output SPLIT_MIX_64 \
                -x 012345 \
                -f binary \
                -b little_endian \
                --raw64 \
                | ./stdin2testu01 raw64 10 > output.data

Remember to clean up the output files:

        rm -f rng.list stress_10_1 output.data
