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

/*
 * Utility for testing the endianness of the platform:
 *  https://en.wikipedia.org/wiki/Endianness
 *
 * Note:
 * If the c compiler is little endian for uint32_t the bytes
 * read from stdin using the fread() function must be reversed.
 */

#include <stdio.h>
#include <stdint.h>

int main(int argc,
         char **argv) {
  /*
   * Determine endianess.
   */
  uint32_t val = 0x01;
  /*
   * Use a raw view of the bytes with a char* to determine if
   * the first byte is set (little endian) or unset (big endian).
   */
  char *buff = (char *)&val;

  if (buff[0] != 0) {
    printf("Little-endian\n");
  } else {
    printf("Big-endian\n");
  }

  /* Demonstrate this. */
  if (argc > 1) {
    val = 0;

    printf("\n");
    printf("Test byte order using 4 bytes for an unsigned integer\n");
    printf("byte   network order   logical order   value\n");
    for (int i = 0; i < 4; i++) {
      buff[i] = -1;
      printf("[%d]    ", i);
      /* Print the network byte order (big endian). */
      for (int j = 0; j < 4; j++) {
        printf((i == j) ? "ff" : "00");
      }
      printf("        ");
      /* Print the logical byte order starting at the most significant byte. */
      for (int j = 4; j--;) {
        /* Shifts 24, 16, 8, 0. */
        printf(((val >> j * 8) & 0xff) ? "ff" : "00");
      }
      /* Print the value for reference. */
      printf("       %11u\n", val);
      buff[i] = 0;
    }
  }

  return 0;
}
