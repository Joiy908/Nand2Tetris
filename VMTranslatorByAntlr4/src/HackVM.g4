grammar HackVM ;

file    : command+ EOF;

command : arith_cmd
        | mem_cmd
        | branch_cmd
        | func_cmd
        ;

arith_cmd     : BIN_CMD            # binaryCmd
              | CMP_CMD            # cmpCmd
              | UNA_CMD            # unaryCmd
              ;

BIN_CMD : ADD | SUB | AND | OR ;
UNA_CMD : NEG | NOT ;
CMP_CMD : EQ | LT | GT ;


mem_cmd : PUSH CONSTANT INT        # pushConst
        | PUSH COMM_SEG INT        # pushComm
        | POP COMM_SEG INT         # popComm
        | PUSH BASE_SEG INT        # pushBase
        | POP BASE_SEG  INT        # popBase
        | PUSH STATIC INT          # pushStatic
        | POP STATIC INT           # popStatic
        ;

COMM_SEG   : LOCAL | ARGUMENT | THIS | THAT ;
BASE_SEG   : TEMP | POINTER ;

branch_cmd : BRANCH ID;

func_cmd : FUNCTION ID INT         # funcDef
         | CALL    ID INT          # call
         | RETURN                  # return
         ;

BRANCH : LABEL | GOTO | IF_GOTO ;
fragment LABEL : 'label' ;
fragment GOTO     : 'goto'  ;
fragment IF_GOTO  : 'if-goto' ;

FUNCTION : 'function' ;
CALL     : 'call' ;
RETURN   : 'return' ;

fragment ADD : 'add' ;
fragment SUB : 'sub' ;
fragment AND : 'and' ;
fragment OR  : 'or' ;
fragment NEG : 'neg' ;
fragment NOT : 'not' ;
fragment EQ  : 'eq' ;
fragment LT  : 'lt' ;
fragment GT  : 'gt' ;
PUSH : 'push' ;
POP  : 'pop' ;
fragment ARGUMENT : 'argument' ;
fragment LOCAL : 'local' ;
STATIC : 'static' ;
CONSTANT : 'constant' ;
fragment THIS : 'this' ;
fragment THAT : 'that' ;
fragment POINTER : 'pointer' ;
fragment TEMP : 'temp' ;


ID : CHAR (DIGIT | CHAR)* ;
INT: DIGIT+;

fragment CHAR: [a-zA-Z.:_] ;
fragment DIGIT: [0-9] ;
WS: [ \n\r\t]+ -> skip;
SL_COMMENT: '//' .*? '\n' -> skip;
