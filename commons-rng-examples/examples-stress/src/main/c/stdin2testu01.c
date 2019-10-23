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
 * Utility for simple interfacing with the "TestU01" library:
 *  http://simul.iro.umontreal.ca/testu01/tu01.html
 *
 * It reads from its standard input an infinite sequence of 32-bits
 * integers and runs one of the test suites "SmallCrush", "Crush" or
 * "BigCrush".
 * "TestU01" writes its report to standard output.
 */

#include <stdint.h>
#include <unistd.h>
#include <string.h>

/*
 * Use this flag to switch the includes for TestU01.
 * - An install from the TestU01 source puts headers directly
 *   into the <install directory> (e.g. /usr/local/include).
 * - The linux package install uses a testu01 sub-directory.
 */
#define TEST_U01_SRC 0

#if TEST_U01_SRC
  #include <unif01.h>
  #include <bbattery.h>
  #include <util.h>
#else
  #include <testu01/unif01.h>
  #include <testu01/bbattery.h>
  #include <testu01/util.h>
#endif

#define TU_S "SmallCrush"
#define TU_C "Crush"
#define TU_B "BigCrush"
#define T_RAW_32 "raw32"
#define T_RAW_64 "raw64"
#define BUFFER_LENGTH_32 2048
/* The 64-bit buffer must be the same size. */
#define BUFFER_LENGTH_64 (BUFFER_LENGTH_32 / 2)

typedef struct {
  uint32_t buffer[BUFFER_LENGTH_32];
  uint32_t index;
} StdinReader_state;

typedef struct {
  uint64_t buffer[BUFFER_LENGTH_64];
  uint32_t index;
} Stdin64Reader_state;

/* Lookup table for binary representation of bytes. */
const char *bit_rep[16] = {
    [ 0] = "0000", [ 1] = "0001", [ 2] = "0010", [ 3] = "0011",
    [ 4] = "0100", [ 5] = "0101", [ 6] = "0110", [ 7] = "0111",
    [ 8] = "1000", [ 9] = "1001", [10] = "1010", [11] = "1011",
    [12] = "1100", [13] = "1101", [14] = "1110", [15] = "1111",
};

/*
 * Print a binary string representation of the 8-bits of the byte to stdout.
 *
 * 01101101
 */
void printByte(uint8_t byte)
{
  printf("%s%s", bit_rep[byte >> 4], bit_rep[byte & 0x0F]);
}

/*
 * Print a string representation of the 4 bytes of the 32-bit unsigned integer
 * to stdout on a single line using: a binary string representation of
 * the bytes; the unsigned integer; and the signed integer.
 *
 * 11001101 00100011 01101111 01110000   3441651568  -853315728
 */
void printInt(uint32_t value)
{
  /* Write out as 4 bytes with spaces between them, high byte first. */
  printByte((uint8_t)((value >> 24) & 0xff));
  putchar(' ');
  printByte((uint8_t)((value >> 16) & 0xff));
  putchar(' ');
  printByte((uint8_t)((value >>  8) & 0xff));
  putchar(' ');
  printByte((uint8_t)( value        & 0xff));
  /* Write the unsigned and signed int value */
  printf("  %10u %11d\n", value, (int32_t) value);
}

/*
 * Print a string representation of the 8 bytes of the 64-bit unsigned integer
 * to stdout on a single line using: a binary string representation of
 * the bytes; the unsigned integer; and the signed integer.
 *
 * 10011010 01010011 01011010 11100100 01000111 00010000 01000011 11000101  11120331841399178181 -7326412232310373435
 */
void printLong(uint64_t value)
{
  /* Write out as 8 bytes with spaces between them, high byte first. */
  printByte((uint8_t)((value >> 56) & 0xff));
  putchar(' ');
  printByte((uint8_t)((value >> 48) & 0xff));
  putchar(' ');
  printByte((uint8_t)((value >> 40) & 0xff));
  putchar(' ');
  printByte((uint8_t)((value >> 32) & 0xff));
  putchar(' ');
  printByte((uint8_t)((value >> 24) & 0xff));
  putchar(' ');
  printByte((uint8_t)((value >> 16) & 0xff));
  putchar(' ');
  printByte((uint8_t)((value >>  8) & 0xff));
  putchar(' ');
  printByte((uint8_t)( value        & 0xff));
  /* Write the unsigned and signed int value */
  printf("  %20lu %20ld\n", value, (int64_t) value);
}

/*
 * Method to read 32-bit input from stdin.
 * Matches the signature for generators in TestU01.
 */
unsigned long nextInt(void *par,
                      void *sta) {
  static size_t last_read = 0;

  StdinReader_state *state = (StdinReader_state *) sta;
  if (state->index >= last_read) {
    /* Refill. */
    last_read = fread(state->buffer, sizeof(uint32_t), BUFFER_LENGTH_32, stdin);
    if (last_read != BUFFER_LENGTH_32) {
      // Allow reading less than the buffer length, but not zero
      if (last_read == 0) {
        // Error handling
        if (feof(stdin)) {
          // End of stream, just exit. This is used for testing.
          exit(0);
        } else if (ferror(stdin)) {
          // perror will contain a description of the error code
          perror("[ERROR] Failed to read stdin");
          exit(1);
        } else {
          printf("[ERROR] No data from stdin\n");
          exit(1);
        }
      }
    }
    state->index = 0;
  }

  uint32_t random = state->buffer[state->index];
  ++state->index; /* Next request. */

  return random;
}

/*
 * Dedicated method to read 64-bit input from stdin.
 * Not used for TestU01. Used to test reading 64-bit binary data.
 */
uint64_t nextLong(void *sta) {
  static size_t last_read = 0;

  /* This works because the 64-bit state is the same size. */
  Stdin64Reader_state *state = (Stdin64Reader_state *) sta;
  if (state->index >= last_read) {
    /* Refill. */
    last_read = fread(state->buffer, sizeof(uint64_t), BUFFER_LENGTH_64, stdin);
    if (last_read != BUFFER_LENGTH_64) {
      // Allow reading less than the buffer length, but not zero
      if (last_read == 0) {
        // Error handling
        if (feof(stdin)) {
          // End of stream, just exit. This is used for testing.
          exit(0);
        } else if (ferror(stdin)) {
          // perror will contain a description of the error code
          perror("[ERROR] Failed to read stdin");
          exit(1);
        } else {
          printf("[ERROR] No data from stdin\n");
          exit(1);
        }
      }
    }
    state->index = 0;
  }

  uint64_t random = state->buffer[state->index];
  ++state->index; /* Next request. */

  return random;
}

double nextDouble(void *par,
                  void *sta) {
  return nextInt(par, sta) / 4294967296.0;
}

static void dummy(void *sta) {
  printf("N/A");

  return;
}

unif01_Gen *createStdinReader(void) {
   unif01_Gen *gen;
   StdinReader_state *state;
   size_t len;
   char name[60];

   state = util_Malloc(sizeof(StdinReader_state));

   gen = util_Malloc(sizeof(unif01_Gen));
   gen->state = state;
   gen->param = NULL;
   gen->Write = dummy;
   gen->GetU01 = nextDouble;
   gen->GetBits = nextInt;

   strcpy(name, "stdin");
   len = strlen(name);
   gen->name = util_Calloc(len + 1, sizeof (char));
   strncpy(gen->name, name, len);

   // Read binary input.
   freopen(NULL, "rb", stdin);
   state->index = BUFFER_LENGTH_32;

   return gen;
}

void deleteStdinReader(unif01_Gen *gen) {
   gen->state = util_Free(gen->state);
   gen->name = util_Free(gen->name);
   util_Free(gen);
}

unsigned long getCount(int argc, char **argv) {
    if (argc < 3) {
        /* 2^64 - 1 == Unlimited. */
        return -1;
    }
    return strtoul(argv[2], 0, 0);
}

int main(int argc,
         char **argv) {
  if (argc < 2) {
    printf("[ERROR] Specify test suite: '%s', '%s' or '%s'\n", TU_S, TU_C, TU_B);
    exit(1);
  }

  unif01_Gen *gen = createStdinReader();
  char *spec = argv[1];

  if (strcmp(spec, TU_S) == 0) {
    bbattery_SmallCrush(gen);
  } else if (strcmp(spec, TU_C) == 0) {
    bbattery_Crush(gen);
  } else if (strcmp(spec, TU_B) == 0) {
    bbattery_BigCrush(gen);
  } else if (strcmp(spec, T_RAW_32) == 0) {
    unsigned long count = getCount(argc, argv);
    /* Print to stdout until stdin closes or count is reached. */
    while (count--) {
      printInt(nextInt(0, gen->state));
    }
  } else if (strcmp(spec, T_RAW_64) == 0) {
    unsigned long count = getCount(argc, argv);
    /* Print to stdout until stdin closes or count is reached. Use dedicated 64-bit reader. */
    while (count--) {
      printLong(nextLong(gen->state));
    }
  } else {
    printf("[ERROR] Unknown specification: '%s'\n", spec);
    exit(1);
  }

  deleteStdinReader(gen);
  return 0;
}
