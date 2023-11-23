import regex as re
import io
import sys

sys.path.append('/mnt/d/src/etc/nand2tetris/myGitRep/HackAssembler')

from assembler import *

def test_rm_comment_re():
    s1 = io.StringIO('''
    // This file is part of www.nand2tetris.org
    // and the book "The Elements of Computing Systems"
    // by Nisan and Schocken, MIT Press.
    // File name: projects/06/add/Add.asm

    // Computes R0 = 2 + 3  (R0 refers to RAM[0])
       
    @2
    D=A
    @3 // inline comment
    D=D+A
    @0
    M=D
    (sfds) // some comment
    ''')

    pat = re.compile(r'^\s*(.*?)\s*(?://.*)?$')
    for line in s1:
        s =  pat.search(line).group(1)
        if s:
            print(s)
    
def test_to_binary():
    s = io.StringIO('''@0
    AM=M-1
    D=M
    A=A-1
    D=M-D
    M=0
    @35
    M=D+1
    D;JLE
    ''')
    pat = re.compile('^(%s)?=?(%s);?(%s)?$' % (
            '|'.join([k for k in DEST_MAP.keys() if k != '']),
            '|'.join([k.replace('+', '\+').replace('|', '\|') for k in COMP_MAP.keys()]),
            '|'.join([k for k in JUMP_MAP.keys() if k != ''])))
    print(pat)
    for line in s:
        print(line, end='')
        line = line.replace(' ', '')
        if not line: continue
        if line[0] == '@': # A-instruction
            n = int(line[1:])
            if n not in range(0, MAX_15UINT + 1):
                raise ValueError('%s not in [0, MAX_15UINT]' % line)
            print(format(n, '016b'))
        else: # C-instruction
            m = pat.search(line)
            if m:
                m.group(1)
                dest = '' if m.group(1) is None else m.group(1)
                comp = m.group(2)
                jump = '' if m.group(3) is None else m.group(3)
                code = 0xe000 | DEST_MAP[dest] | COMP_MAP[comp] | JUMP_MAP[jump] 
                print(m.group(1), m.group(2), m.group(3))
                print('{:016b}'.format(int(code)))
            else:
                raise ValueError('invalid syntax: ' + line)
    
test_to_binary()

