grammar ScimFilter;

filter:   filter AND filter             #andExpression
        | filter OR filter              #orExpression
        | NOT '(' filter ')'            #notExpression
        | '(' filter ')'                #parenthesisExpression
        | attributeExpression           #attrExpression
        ;

attributeExpression:    attributePath compareOperator compareValue
                      | attributePath PR ;

attributePath:   resourceUri=NAME_URI? attribute=ATTRIBUTE_NAME ('.' subattribute=ATTRIBUTE_NAME)? ;

compareOperator:  isEqual           = EQ
                | isNotEqual        = NE
                | contains          = CO
                | startsWith        = SW
                | endsWith          = EW
                | isGreaterThan     = GT
                | isGreaterOrEqual  = GE
                | isLowerThan       = LT
                | isLowerOrEqual    = LE ;

compareValue:     isFalse = 'false'
                | isNull  = 'null'
                | isTrue  = 'true'
                | number  = DECIMAL
                | string  = TEXT ;

PR: P R;
EQ: E Q;
NE: N E;
CO: C O;
SW: S W;
EW: E W;
GT: G T;
GE: G E;
LT: L T;
LE: L E;

OR:  O R;
AND: A N D;
NOT: N O T;

DECIMAL: '-'? INTEGER '.' DIGIT+ | '-'? INTEGER;
ATTRIBUTE_NAME: ALPHA (NAMECHAR)*;
NAMECHAR: '_' | DIGIT | ALPHA;
NAME_URI: ALPHA (NAMECHAR | ':' | '.')* NAMECHAR+ ':';
TEXT: '"' STRING '"';
EXCLUDE: [ \b\t\r\n]+ -> skip ;


fragment ALPHA: ([a-zA-Z]);
fragment INTEGER: '0' | [1-9] DIGIT*;
fragment DIGIT: [0-9] ;
fragment STRING: ~[\])]+?;

fragment A : [aA];
fragment B : [bB];
fragment C : [cC];
fragment D : [dD];
fragment E : [eE];
fragment F : [fF];
fragment G : [gG];
fragment H : [hH];
fragment I : [iI];
fragment J : [jJ];
fragment K : [kK];
fragment L : [lL];
fragment M : [mM];
fragment N : [nN];
fragment O : [oO];
fragment P : [pP];
fragment Q : [qQ];
fragment R : [rR];
fragment S : [sS];
fragment T : [tT];
fragment U : [uU];
fragment V : [vV];
fragment W : [wW];
fragment X : [xX];
fragment Y : [yY];
fragment Z : [zZ];

