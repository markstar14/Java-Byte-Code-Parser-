package parser;
/* 		OO PARSER AND BYTE-CODE GENERATOR FOR TINY PL

Grammar for TinyPL (using EBNF notation) is as follows:

 program ->  decls stmts end
 decls   ->  int idlist ;
 idlist  ->  id { , id } 
 stmts   ->  stmt [ stmts ]
 cmpdstmt->  '{' stmts '}'
 stmt    ->  assign | cond | loop
 assign  ->  id = expr ;
 cond    ->  if '(' rexp ')' cmpdstmt [ else cmpdstmt ]
 loop    ->  while '(' rexp ')' cmpdstmt  
 rexp    ->  expr (< | > | =) expr
 expr    ->  term   [ (+ | -) expr ]
 term    ->  factor [ (* | /) term ]
 factor  ->  int_lit | id | '(' expr ')'
 
Lexical:   id is a single character; 
	      int_lit is an unsigned integer;
		 equality operator is =, not ==

Sample Program: Factorial
 
int n, i, f;
n = 4;
i = 1;
f = 1;
while (i < n) {
  i = i + 1;
  f= f * i;
}
end

   Sample Program:  GCD
   
int x, y;
x = 121;
y = 132;
while (x != y) {
  if (x > y) 
       { x = x - y; }
  else { y = y - x; }
}
end

 */

public class Parser {
	public static void main(String[] args)  {
		System.out.println("Enter program and terminate with 'end'!\n");
		Lexer.lex(); //the first token should be the int declared at the beginning
		Program p = new Program();
		Code.output();
	}
}

class Program{	//program -> decls stmts end
	Decls d;
	Stmts s;
	public Program(){
		d = new Decls();
		// token is the semi colon at the end of the declarations	 
		Lexer.lex();//token is the first token of the stmts
		s = new Stmts();
		Code.gen("return");// add return to the bytecode to represent end
	}
}

class Decls{	//decls -> int idlist ;
	Idlist i;
	
	public Decls(){	
		i = new Idlist();
	}
}

class Idlist{	//idlist -> id { , id }
	
	public Idlist(){
		//add a bunch of bytecodes for all of the characters
		Lexer.lex();	//the token is now the first id
		//adds the id bytcode with the int idCounter or not it might just add it to the array and then when it adds the number later, it then adds this
		char c = Lexer.ident; //= Lexer.ident;	 //c is the id
		if(!(Code.charDeclared(c))){
			Code.addChar(c);
		}
		Lexer.lex();// token is either the comma or the semi-colon
		while(Lexer.nextToken == Token.COMMA){
			Lexer.lex();	//token is the next ID, after the comma
			c = Lexer.ident;	 //v is the id
			if(!(Code.charDeclared(c))){
				Code.addChar(c);
			}
			Lexer.lex(); // token is the comma or semi-colon
		}
	}
}

class Stmts{	//stmts -> stmt [ stmts ]
	Stmt s;
	Stmts st;
	int temp;
	public Stmts(){
		//going into it, the token is the first token of the stmts
		s = new Stmt();
		temp = Lexer.nextToken;
		if(temp != Token.KEY_END && temp != Token.RIGHT_BRACE){	
			st = new Stmts();
		}
	}
}

class Cmpdstmt{	//cmpdstmt -> '{' stmts '}'
	Stmts s;
	
	public Cmpdstmt(){
		Lexer.lex(); // token is the first token of the stmts
		s = new Stmts(); //should leave the token as the right bracket, }
	}
}

class Stmt{		// stmt -> assign | loop | cond
	Assign a;
	Loop l;
	Cond c;
	
	public Stmt(){
		if(Lexer.nextToken == Token.KEY_WHILE){
			l = new Loop();
		}
		else if (Lexer.nextToken == Token.KEY_IF){
			c = new Cond();
		}
		else if (Lexer.nextToken == Token.ID){ //if it isn't a loop or if, then it must be an assign
			a = new Assign();
		}
	}
}

class Assign{	//assign -> id = expr ;
	Expr e;
	char v;
	public Assign(){//token should be the ID at the beginning of the assign
		v = Lexer.ident;	 //v is the id
		Lexer.lex(); //token should now be the = sign
		Lexer.lex();// token is the first token of the expr
		e = new Expr();
		Lexer.lex();// token is the token after the semi-colon, so an end or the stmt of the next stmts
		Code.gen("istore_" + Code.findChar(v));
	}
}

class Loop{		// loop -> while '(' rexp ')' cmpdstmt
	Rexp r;
	Cmpdstmt c;
	
	public Loop(){//token should be while
		Lexer.lex();//token should be (
		Lexer.lex();//token should be the first token of the rexp
		int index1 = Code.codeptr;
		r = new Rexp();
		int index2 = Code.codeptr;
		Code.gen(r.oper);
		Code.codeptr = Code.codeptr + 2;
		Lexer.lex();//token should be the first token of the cmpdstmt 
		c = new Cmpdstmt();
		Code.gen("goto " + index1);
		Code.codeptr = Code.codeptr + 2;
		Code.code[index2] = r.oper+ Code.codeptr;
		Lexer.lex();//token is the token after the }, so the end, or the start of the next stmts
	}
}

class Cond{		// cond -> if '(' rexp ')' cmpdstmt [ else cmpdstmt ]
	Rexp r;
	Cmpdstmt c1;
	Cmpdstmt c2;
	
	public Cond(){
		Lexer.lex();//token is (
		Lexer.lex();//token is the first token of the rexp
		r = new Rexp();
		int index1 = Code.codeptr;
		Code.gen(r.oper);
		Code.codeptr = Code.codeptr + 2;
		Lexer.lex();//token should be the first token of the cmpdstmt
		c1 = new Cmpdstmt();
		Lexer.lex();//token is now either the else or the start of the next stmts or the end
		if(Lexer.nextToken == Token.KEY_ELSE){
			int index2 = Code.codeptr;
			Code.gen("goto ");
			Code.codeptr = Code.codeptr + 2;
			Code.code[index1] = r.oper + Code.codeptr;
			Lexer.lex();//token is the first token of the cmpsdtmt
			c2 = new Cmpdstmt();
			Lexer.lex();//token is the token after the }, so an end or the start of another stmt
			Code.code[index2] = "goto " + Code.codeptr;
		}
		else{
			Code.code[index1] = r.oper + Code.codeptr;
		}	
	}
}

class Rexp{		// expr (< | > | = | !=) expr
	Expr e1;
	Expr e2;
	String oper;
	public Rexp(){
		e1 = new Expr();//this leaves the token as the last token of the expr
		if(Lexer.nextToken == Token.ASSIGN_OP){
			oper = "if_icmpne ";
		}
		else if(Lexer.nextToken == Token.LESSER_OP){ // <
			oper = "if_icmpge ";
		}
		else if(Lexer.nextToken == Token.GREATER_OP){ //>
			oper = "if_icmple ";
		}
		else if(Lexer.nextToken == Token.NOT_EQ){
			oper = "if_icmpeq ";
		}
		Lexer.lex();//token first token of the second expr
		e2 = new Expr(); //should leave the token as the ) after the rexp
	}
}

class Expr   { // expr -> term [ (+ | -) expr ]
	Term t;
	Expr e;
	char op;

	public Expr() {
		t = new Term();
		if (Lexer.nextToken == Token.ADD_OP || Lexer.nextToken == Token.SUB_OP) {
			op = Lexer.nextChar;
			Lexer.lex();//token is the + or - symbol
			e = new Expr();
			Code.gen(Code.opcode(op));
		}
	}
}

class Term    { // term -> factor [ (* | /) term ]
	Factor f;
	Term t;
	char op;

	public Term() {
		f = new Factor();
		if (Lexer.nextToken == Token.MULT_OP || Lexer.nextToken == Token.DIV_OP) {
			op = Lexer.nextChar;
			Lexer.lex();// token is the * or / symbol
			t = new Term();
			Code.gen(Code.opcode(op));
		}
	}
}

class Factor { //Factor -> int_lit | id | '(' expr ')'
	Expr e;
	int i;
	char c;
	public Factor() {
		switch (Lexer.nextToken) {
		case Token.INT_LIT: // number
			i = Lexer.intValue;
			Lexer.lex();// token is now the token after the INT_LIT
			if(i < 6){
				Code.gen("iconst_" + i);
			}
			else if(i < 128){
				Code.gen("bipush " + i);
				Code.codeptr++;
			}
			else {
				Code.gen("sipush " + i);
				Code.codeptr = Code.codeptr + 2;
			}
			break;
		case Token.LEFT_PAREN: // '('
			Lexer.lex();//token is the left parenthesis
			e = new Expr();
			Lexer.lex(); // skip over ')'
			break;
		case Token.ID:	//letter or char
			c = Lexer.ident; //add this to the bytecode somehow
			Code.gen("iload_" + Code.findChar(c));
			Lexer.lex();//token is the token after the ID, like a semi colon or an operator
			break;
		default:
			break;
		}
	}
}

class Code {
	static String[] code = new String[100];
	static int codeptr = 0;
	static char[] characters = new char[100];
	static int charptr = 0;
	
	public static void gen(String s) {
		code[codeptr] = s;
		codeptr++;
	}
	
	public static void addChar(char c){
		if(!(Code.charDeclared(c))){
			characters[charptr] = c;
			charptr++;
		}
	}
	
	public static int findChar(char c){
		int v = 0;
		for(int i = 0; i < charptr; i++){
			if (characters[i] == c){
				v = i;
			}
		}
		return v;
	}
	
	public static boolean charDeclared(char c){
		for (int i = 0; i < codeptr; i++){
			if(characters[i] == c){
				return true;
			}
		}
		return false;
		
	}
	
	public static String opcode(char op) {
		switch(op) {
		case '+' : return "iadd";
		case '-':  return "isub";
		case '*':  return "imul";
		case '/':  return "idiv";
		default: return "";
		}
	}
	
	public static void output() {
		for (int i=0; i<codeptr; i++){
			if(code[i] != null){
				System.out.println(i + ": " + code[i]);
			}	
		}
	}
}


