//
// Created by Joiy on 2022/12/28.
//

#ifndef MYASSEMBLER_CC_ASSEMBLER_H
#define MYASSEMBLER_CC_ASSEMBLER_H


enum State {
    NEWLINE, INSTRUCTION, COMMENT, LABEL_DEFINE
};

std::pair<std::string, std::string> PRE_DEFINED_KEYS[] = {
    {"SP",     "0"},
    {"LCL",    "1"},
    {"ARG",    "2"},
    {"THIS",   "3"},
    {"THAT",   "4"},
    {"R0",     "0"},
    {"R1",     "1"},
    {"R2",     "2"},
    {"R3",     "3"},
    {"R4",     "4"},
    {"R5",     "5"},
    {"R6",     "6"},
    {"R7",     "7"},
    {"R8",     "8"},
    {"R9",     "9"},
    {"R10",    "10"},
    {"R11",    "11"},
    {"R12",    "12"},
    {"R13",    "13"},
    {"R14",    "14"},
    {"R15",    "15"},
    {"SCREEN", "16384"},
    {"KBD",    "24576"}
};

std::pair<std::string, uint16_t> DEST_MAP[] = {
    {{},    0x0},
    {"M",   0x8},
    {"D",   0x10},
    {"MD",  0x18},
    {"A",   0x20},
    {"AM",  0x28},
    {"AD",  0x30},
    {"AMD", 0x38}
};
std::pair<std::string, uint16_t> JUMP_MAP[] = {
    {{},    0x0},
    {"JGT", 0x1},
    {"JEQ", 0x2},
    {"JGE", 0x3},
    {"JLT", 0x4},
    {"JNE", 0x5},
    {"JLE", 0x6},
    {"JMP", 0x7}
};
std::pair<std::string, uint16_t> COMP_MAP[] = {
    {{"0"}, 0xa80},
    {"1",   0xfc0},
    {"-1",  0xe80},
    {"D",   0x300},

    {"A",   0xc00},
    {"M",   0x1c00},

    {"!D",  0x340},

    {"!A",  0xc40},
    {"!M",  0x1c40},

    {"-D",  0x3c0},

    {"-A",  0xcc0},
    {"-M",  0x1cc0},

    {"D+1", 0x7c0},

    {"A+1", 0xdc0},
    {"M+1", 0x1dc0},

    {"D-1", 0x380},

    {"A-1", 0xc80},
    {"M-1", 0x1c80},

    {"D+A", 0x80},
    {"D+M", 0x1080},

    {"D-A", 0x4c0},
    {"D-M", 0x14c0},

    {"A-D", 0x1c0},
    {"M-D", 0x11c0},

    {"D&A", 0x0},
    {"D&M", 0x1000},

    {"D|A", 0x0540},
    {"D|M", 0x1540},
};
#endif //MYASSEMBLER_CC_ASSEMBLER_H
