grammar PL;

@header {
import backend.*;

}

@members {
}

/*
program returns [Expr expr]
    : { $expr = new NoneExpr(); }
    | WORD { $expr = new StringLiteral($WORD.text); } 
    | WORD '=' program                             
    | e1=program CONCAT e2=program { $expr = new Arithmetics($CONCAT.text, $e1.expr, $e2.expr); } 
    | CONCAT { $expr = new StringLiteral($CONCAT.text); } 
    | STRING { $expr = new StringLiteral($STRING.text); } 
    | INTEGER { $expr = new IntLiteral($INTEGER.text); } 
    ;
*/


program returns [Expr expr]
    : { List <Expr> statements = new ArrayList<Expr>(); }
    (statement 
        { statements.add($statement.expr);} 
    )+ EOF
    { $expr = new Block (statements);}
    ;


statement returns [Expr expr]
    : expression ';'? { $expr = $expression.expr; }
    ;

expression returns [Expr expr]
    : '(' expression ')' { $expr = $expression.expr; }
    | '-' expression     { $expr = new NegationExpr($expression.expr); } // Unary negation
    | e1=expression '++' e2=expression { $expr = new AddString($e1.expr,$e2.expr); }
    | e1=expression '+' e2=expression { $expr = new Arithmetics(Operator.Add, $e1.expr,$e2.expr); }
    | e1=expression '*' e2=expression { $expr = new Arithmetics(Operator.Mul, $e1.expr,$e2.expr); }
    | e1=expression '-' e2=expression { $expr = new Arithmetics(Operator.Sub, $e1.expr,$e2.expr); }
    | e1=expression '>' e2=expression { $expr = new Compare(Comparator.GT, $e1.expr,$e2.expr); }
    | e1=expression '<' e2=expression { $expr = new Compare(Comparator.LT, $e1.expr,$e2.expr); }
    | e1=expression '==' e2=expression { $expr = new Compare(Comparator.EQ, $e1.expr,$e2.expr); }
    | e1=expression '>=' e2=expression { $expr = new Compare(Comparator.GE, $e1.expr,$e2.expr); }
    | condition=expression '?' trueExpr=expression ':' falseExpr=expression { $expr = new TernaryExpr($condition.expr, $trueExpr.expr, $falseExpr.expr); }
    | left=expression '&&' right=expression { $expr = new LogicalAndExpr($left.expr, $right.expr); }
    | left=expression '||' right=expression { $expr = new LogicalOrExpr($left.expr, $right.expr); }
    | '!' subExpr=expression             { $expr = new LogicalNotExpr($subExpr.expr); }
    | assignment { $expr = $assignment.expr; }
    | funCall { $expr = $funCall.expr; }
    | value { $expr = $value.expr; }
    | readFile { $expr = $readFile.expr; }
    | splitString { $expr = $splitString.expr; } 
    ;

readFile returns [Expr expr]
    : 'readFile' '(' STRING ')' { 
        $expr = new ReadFileExpr(new StringLiteral($STRING.text.substring(1, $STRING.text.length() - 1))); 
      }
    ;

value returns [Expr expr]
    : INTEGER { $expr = new IntLiteral($INTEGER.text); }
    | STRING { $expr = new StringLiteral($STRING.text); }
    | INTERPOLATED_STRING { $expr = new InterpolatedStringExpr($INTERPOLATED_STRING.text); }
    | BOOL { $expr = new BoolLiteral($BOOL.text); }
    | WORD { $expr = new Deref($WORD.text); }
    ;


assignment returns [Expr expr]
    : 'let'? WORD '=' expression { $expr = new Assign($WORD.text, $expression.expr); } 
    | 'function' WORD '(' argDecl ')'  body  { $expr = new Declare($WORD.text, $argDecl.strList, $body.expr); }
    ;
  
funCall returns [Expr expr]
   // : 'print' '(' expression ')' { System.out.println($expression.expr); }
    : 'print' '(' expression ')' { $expr = new PrintExpr($expression.expr); }
    | 'for' '(' i=value 'in' a=value '..' z=value ')' body { $expr = new ForLoop($i.text, $a.expr, $z.expr, $body.expr); }
    | 'if' '(' expression ')' b1=body 'else' b2=body { $expr = new Ifelse($expression.expr, $b1.expr, $b2.expr); }
    | WORD '(' argList ')' { $expr = new Invoke($WORD.text, $argList.exprList); }
    ;

argDecl returns [List<String> strList]
    : { $strList = new ArrayList<String>(); }
    expression {$strList.add($expression.text);} (',' expression {$strList.add($expression.text);})*
    ;

argList returns [List<Expr> exprList] 
    : { $exprList = new ArrayList<Expr>(); }
    expression {$exprList.add($expression.expr);} (',' expression {$exprList.add($expression.expr);})*
    ;
    
body returns [Expr expr]
    : { List <Expr> statements = new ArrayList<Expr>(); }
    '{' (statement 
        { statements.add($statement.expr);} 
    )+ '}'
    { $expr = new Block (statements);}
    ;

list returns [List<String> valueList]
    @init { $valueList = new ArrayList<String>(); }
    : '[' val=STRING { $valueList.add($val.text.substring(1, $val.text.length()-1)); }
      (',' val=STRING { $valueList.add($val.text.substring(1, $val.text.length()-1)); })* ']'
    ;

splitString returns [Expr expr]
    : 'split' '(' stringToSplit=expression ',' delimiter=expression ')' { 
        $expr = new SplitStringExpr($stringToSplit.expr, $delimiter.expr); 
      }
    ;



BOOL : 'true' | 'false';
INTEGER: ('0'..'9')+;
STRING : '"' (~["\r\n])* '"' {setText(getText().substring(1,getText().length()-1));};
CONCAT: '++'|[+*/-]; 
WORD: ('a'..'z'|'A'..'Z'|'0'..'9'|'_')+;
COMMA: ',';
SINGLE_LINE_COMMENT: '//' ~[\r\n]* -> skip;
MULTI_LINE_COMMENT: '/*' .*? '*/' -> skip;
WS : [ \t\r\n] -> skip;
INTERPOLATED_STRING : '`' (ESC_SEQ | ~('\\'|'`'))* '`';

fragment ESC_SEQ : '\\' ('b' | 't' | 'n' | 'f' | 'r' | '"' | '\'' | '`' | '\\');