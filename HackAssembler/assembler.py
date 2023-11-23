from sys import argv
from os import path
from typing import Generator
import io, re


PRE_DEFINED_LABELS = {
    'SP':     '0',
    'LCL':    '1',
    'ARG':    '2',
    'THIS':   '3',
    'THAT':   '4',
    'R0':     '0',
    'R1':     '1',
    'R2':     '2',
    'R3':     '3',
    'R4':     '4',
    'R5':     '5',
    'R6':     '6',
    'R7':     '7',
    'R8':     '8',
    'R9':     '9',
    'R10':    '10',
    'R11':    '11',
    'R12':    '12',
    'R13':    '13',
    'R14':    '14',
    'R15':    '15',
    'SCREEN': '16384',
    'KBD':    '24576'
}

DEST_MAP = {
    '':    0x0,
    'M':   0x8,
    'D':   0x10,
    'MD':  0x18,
    'A':   0x20,
    'AM':  0x28,
    'AD':  0x30,
    'AMD': 0x38
}

JUMP_MAP = {
    '':    0x0,
    'JGT': 0x1,
    'JEQ': 0x2,
    'JGE': 0x3,
    'JLT': 0x4,
    'JNE': 0x5,
    'JLE': 0x6,
    'JMP': 0x7
}

COMP_MAP = {
    '0':   0xa80,
    '1':   0xfc0,
    '-1':  0xe80,
    'D':   0x300,
    'A':   0xc00,
    'M':   0x1c00,
    '!D':  0x340,
    '!A':  0xc40,
    '!M':  0x1c40,
    '-D':  0x3c0,
    '-A':  0xcc0,
    '-M':  0x1cc0,
    'D+1': 0x7c0,
    'A+1': 0xdc0,
    'M+1': 0x1dc0,
    'D-1': 0x380,
    'A-1': 0xc80,
    'M-1': 0x1c80,
    'D+A': 0x80,
    'D+M': 0x1080,
    'D-A': 0x4c0,
    'D-M': 0x14c0,
    'A-D': 0x1c0,
    'M-D': 0x11c0,
    'D&A': 0x0,
    'D&M': 0x1000,
    'D|A': 0x0540,
    'D|M': 0x1540,
}

labels = dict(PRE_DEFINED_LABELS)

MAX_15UINT = (1 << 15) - 1

def rm_comment(file_path) -> Generator[str, None, None]:
    '''
    Pre: assume file_path is valid file name
    Post: yield a line without comment and whitespace prefix/suffix
    '''
    pat = re.compile(r'^\s*(.*?)\s*(?://.*)?$')
    with open(file_path, 'r', encoding='utf-8') as f:
        for line in f:
            code = pat.search(line).group(1)
            if code:
                yield code


def rm_labels(codes: Generator[str, None, None]) -> Generator[str, None, None]:
    '''
    Pre: Generator that yields a line without comment and whitespace prefix/suffix
    Post: remove all label-definition and change @label to @num
          yield a line without labels and whitespace suffix
    '''
    # remove label-definition and add them to labels dict
    pat = re.compile(r'^\s*[(]([a-zA-Z0-9_$.]+)\s*[)]$')

    line_num = 0 # next line number
    out = io.StringIO()
    for line in codes:
        if line[0] == '(':
            m = pat.search(line)
            if m and m.group(1):
                labels[m.group(1)] = line_num
                continue
        # else
        out.write(line + '\n')
        line_num += 1
        
    out.seek(0)
    
    # now, if @label not in labels, it is a variable label
    next_free_var_addr = 16;
    for line in out:
        line = line[:-1] # remove \n
        if line[0] == '@':
            addr = line[1:] # remove @
            if addr.isdigit():
                yield line
            elif addr in labels:
                yield '@' + str(labels[addr])
            else: # addr is a variable
                labels[addr] = next_free_var_addr
                yield '@' + str(next_free_var_addr)
                next_free_var_addr += 1
        else:
            yield line
        


def to_binary(asms: Generator[str, None, None], file_name: str):
    '''
    Pre: Generator that yields a line of code without comment and labels
         file_name is a valid file_path
    Post: transform asms to machine code(text) and write to file_path
    '''
    pat = re.compile('^(%s)?=?(%s);?(%s)?$' % (
            '|'.join([k for k in DEST_MAP.keys() if k != '']),
            '|'.join([k.replace('+', '\+').replace('|', '\|') for k in COMP_MAP.keys()]),
            '|'.join([k for k in JUMP_MAP.keys() if k != ''])))

    with open(file_name, 'w') as f:
        for line in asms:
            line = line.replace(' ', '')
            if not line: continue
            if line[0] == '@': # A-instruction
                n = int(line[1:])
                if n not in range(0, MAX_15UINT + 1):
                    raise ValueError('%s not in [0, MAX_15UINT]' % line)
                f.write('{:016b}\n'.format(n))
            else: # C-instruction
                m = pat.search(line)
                if m:
                    m.group(1)
                    dest = '' if m.group(1) is None else m.group(1)
                    comp = m.group(2)
                    jump = '' if m.group(3) is None else m.group(3)
                    code = 0xe000 | DEST_MAP[dest] | COMP_MAP[comp] | JUMP_MAP[jump] 
                    f.write('{:016b}\n'.format(int(code)))
                else:
                    raise ValueError('invalid syntax: ' + line)


if __name__ == '__main__':
    if len(argv) != 2:
        print('usage: %s xxx.asm' % argv[0])
        exit(1)

    if not path.isfile(argv[1]):
        print('%s is not a valid file path.' % argv[1])

    
    # rmComment(file_name) | rm_labels | to_binary(out_name)
    out_name = argv[1].replace('.asm', '.hack')
    asms = rm_labels(rm_comment(argv[1]))
    to_binary(asms, out_name)
    print('write to %s successfully.' % out_name)

