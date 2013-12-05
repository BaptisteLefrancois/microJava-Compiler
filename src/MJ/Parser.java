
//Baptiste
//Matthew 
//Thomas

/*  MicroJava Parser (HM 06-12-28)
    ================
*/
package MJ;

import java.awt.Point;
import java.util.*;
import MJ.SymTab.*;
//import MJ.CodeGen.*;

public class Parser {
	private static final int  // token codes
		none      = 0,
		ident     = 1,
		number    = 2,
		charCon   = 3,
		plus      = 4,
		minus     = 5,
		times     = 6,
		slash     = 7,
		rem       = 8,
		eql       = 9,
		neq       = 10,
		lss       = 11,
		leq       = 12,
		gtr       = 13,
		geq       = 14,
		assign    = 15,
		semicolon = 16,
		comma     = 17,
		period    = 18,
		lpar      = 19,
		rpar      = 20,
		lbrack    = 21,
		rbrack    = 22,
		lbrace    = 23,
		rbrace    = 24,
		class_    = 25,
		else_     = 26,
		final_    = 27,
		if_       = 28,
		new_      = 29,
		print_    = 30,
		program_  = 31,
		read_     = 32,
		return_   = 33,
		void_     = 34,
		while_    = 35,
		eof       = 36;
	private static final String[] name = { // token names for error messages
		"none", "identifier", "number", "char constant", "+", "-", "*", "/", "%",
		"==", "!=", "<", "<=", ">", ">=", "=", ";", ",", ".", "(", ")",
		"[", "]", "{", "}", "class", "else", "final", "if", "new", "print",
		"program", "read", "return", "void", "while", "eof"
		};

	private static Token t;			// current token (recently recognized)
	private static Token la;		// lookahead token
	private static int sym;			// always contains la.kind
	public  static int errors;  // error counter
	private static int errDist;	// no. of correctly recognized tokens since last error

	private static BitSet exprStart, relopStart, statStart, statSeqFollow, declStart, declFollow, statSync;
        
        private static Obj curMethod;

	//------------------- auxiliary methods ----------------------
	private static void scan() {
		t = la;
		la = Scanner.next();
		sym = la.kind;
		errDist++;
	}

	private static void check(int expected) {
		if (sym == expected) scan();
		else error(name[expected] + " expected");
	}

	public static void error(String msg) { // syntactic error at token la
		if (errDist >= 3) {
			System.out.println("-- line " + la.line + " col " + la.col + ": " + msg);
			errors++;
		}
		errDist = 0;
	}

	//-------------- parsing methods (in alphabetical order) -----------------

	//ActPars = "(" [ Expr {"," Expr} ] ")".
    private static void ActPars()
    {
        check(lpar);
        if(exprStart.get(sym))
        {
            Expr();
            while(sym == comma)
            {
                scan();
                Expr();
            }
        }
        check(rpar);
    }
    
    //Addop = "+" | "-"
    private static void Addop()
    {
        if(sym == plus || sym == minus)scan();
        else error("invalid Addop");
    }

	//Block = "{" {Statement} "}".
    private static void Block()
    {
        check(lbrace);
        while(!statSeqFollow.get(sym))Statement();
        check(rbrace);
    }

	//ClassDecl = "class" ident "{" {VarDecl} "}".
    private static void ClassDecl()
    {
        Struct type;
        Obj obj;
        
        check(class_);
        check(ident);
        
        obj = Tab.insert(Obj.Type, t.string, new Struct(Struct.Class));
        
        check(lbrace);
        Tab.openScope();
        for(;;) {					//we use this loop to check every var from the class, until a rbrace or eof
			if (sym == ident) {		//this way the parser don't go out of the class when he finds an error
				VarDecl();
			}
			else if (sym == rbrace || sym == eof) break;
			else {
				error("...");
				do scan(); while (sym != rbrace && sym != ident && sym != eof);
			}
		}
        obj.type.fields = Tab.curScope.locals;
        obj.type.nFields = Tab.curScope.nVars;
        check(rbrace);
        Tab.closeScope();
        
    }

	//Condition = Expr Relop Expr.
    private static void Condition()
    {
        Expr();
        Relop();
        Expr();
    }

	//ConstDecl = "final" Type ident "=" (number | charConst) ";".
    private static void ConstDecl()
    {
        Struct type;
        Obj obj;
        
        check(final_);
        type = Type();
        check(ident);
        obj = Tab.insert(Obj.Con, t.string, type);
        check(assign);
        if(sym == number) 
        {
            scan();
            if(type.kind != Struct.Int)error(obj.name + " is not a valid constant type");
            obj.val = t.val;
        }
        else if(sym == charCon)
        {
            scan();
            if(type.kind != Struct.Char)error(obj.name + " is not a valid constant type");
            obj.val = t.val;
        }
        else error("invalid ConstDecl");
        check(semicolon);
    }
    
    //Designator = ident {"." ident | "[" Exp "]"}.
    private static void Designator()
    {
        check(ident);
        while(sym == period || sym == lbrack)
        {
            if(sym == period)
            {
                scan();
                check(ident);
            }
            else
            {
                scan();
                Expr();
                check(rbrack);
            }
        }
    }

	//Expr = ["-"] Term {Addop Term}.
    private static void Expr()
    {
        if(sym == minus)scan();
        Term();
        while(sym == plus || sym == minus)
        {
            scan();
            Term();
        }
    }
    
    /*Factor = Designator  [ActPArs]
               | Number
               | charConst
               |
     */
    private static void Factor()
    {
        if(sym == ident)
        {
            Designator();
            if(sym == lpar)
            	ActPars();                 
        } 
        else if(sym == number || sym == charCon)scan();
        else if(sym == new_)
        {
            scan();
            check(ident);
            if(sym == lbrack)
            {
                scan();
                Expr();
                check(rbrack);
            }
        }
        else if(sym == lpar)
        {
            scan();
            Expr();
            check(rpar);
        }
        else error("invalid Factor");
    }

  //FormPars = Type ident {"," Type ident}.
  	private static int FormPars()
  	{
  		int paramCount = 0;
  		Struct type = Type(); 	//get the type from the variable
  		check(ident);
  		paramCount++;
  		Tab.insert(Obj.Var, t.string, type);
  		while(sym == comma) { 
  			scan(); 
  			Struct type2 = Type(); 
  			check(ident); 
  			paramCount++; 
  			Tab.insert(Obj.Var, t.string, type2); 
  		}
  		
  		return paramCount;
  	}

	//MethodDecl = (Type | "void") ident "(" [FormPars] ")" {VarDecl} Block.
    private static void MethodDecl()
    {
        Struct type = Tab.noType;
        if(sym == ident)
            type =Type();
        else if(sym == void_)scan();
        else error("invalid MethodDecl");
        check(ident);
        curMethod = Tab.insert(Obj.Meth, t.string, type);
        check(lpar);
        Tab.openScope();
        if(sym == ident) curMethod.nPars = FormPars();
        	check(rpar);
        while(sym == ident)VarDecl();
        Block();
        curMethod.locals = Tab.curScope.locals;
        Tab.closeScope();
    }
    
    //Mulop = "*" | "/" | "%".
    private static void Mulop()
    {
        if(sym == times || sym == slash || sym == rem)scan();
        else error("invalid Mulop");
    }
        
    // Program = "program" ident {ConstDecl | ClassDecl | VarDecl} '{' {MethodDecl} '}'.
	private static void Program() 
    {
        check(program_);
        check(ident);
        Tab.openScope();
        for(;;)             
        {
            if(sym == final_)ConstDecl();
            else if(sym == ident)VarDecl();
            else if(sym == class_)ClassDecl();
            else break;
        }
        check(lbrace);
        while(sym == ident || sym == void_)MethodDecl();
        check(rbrace);
        //Tab.dumpScope(Tab.curScope.locals);
        Tab.closeScope();
            
	}

	//Relop = "==" | "!=" | ">" | ">=" | "<" | "<=".
    private static void Relop()
    {
        if(relopStart.get(sym))scan();
        else error("invalid Relop");
    }
    

	//Statement = Designator ("=" Expr | ActPars) ";"
	//	| "if" "(" Condition ")" Statement ["else" Statement]
	//	| "while" "(" Condition ")" Statement
	//	| "return" [Expr] ";"
	//	| "read" "(" Designator ")" ";"
	//	| "print" "(" Expr ["," number] ")" ";"
	//	| Block
	//	| ";".
    private static void Statement()
    {
        if (!statStart.get(sym)) 
        {
            error("invalid Statement");
            do scan(); while (!statSync.get(sym));
            if (sym == semicolon) scan();
            errDist = 0;
        }
        
        if(sym == ident)
        {
            Designator();
            if(sym == assign)
            {
                scan();
                Expr();
            }
            else if(sym == lpar)ActPars();
            else error("invalid Statement");
            check(semicolon);
        }
        else if (sym == if_)
        {
            scan();
            check(lpar);
            Condition();
            check(rpar);
            Statement();
            if(sym == else_)
            {
                scan();
                Statement();
            }
        }
        else if (sym == while_)
        {
            scan();
            check(lpar);
            Condition();
            check(rpar);
            Statement();
        }
        else if (sym == return_)
        {
            scan();
            if(exprStart.get(sym))Expr();
            check(semicolon);
        }
        else if (sym == read_)
        {
            scan();
            check(lpar);
            Designator();
            check(rpar);
            check(semicolon);
        }
        else if(sym == print_)
        {
            scan();
            check(lpar);
            Expr();
            if(sym == comma)
            {
                scan();
                check(number);
            }
            check(rpar);
            check(semicolon);
        }
        else if(sym == lbrace)Block();
        else if(sym == semicolon)scan();
        else error("invalid Statement");
        
    }

	//Term = Factor {Mulop Factor}.
    private static void Term()
    {
        Factor();
        while(sym == times || sym == slash || sym == rem)
        {
            scan(); //instead of Mulop, already checked.
            Factor();
        }
    }

    //Type = ident ["[" "]"].
  	private static Struct Type()
  	{
  		Obj o = null;
  		if(sym == ident)
  		{
  			scan();
  			o = Tab.find(t.string);
  			if(o.kind != Obj.Type)
  				error("Type expected");
  		}
  		if(sym == lbrack) 
  		{ 
  			scan(); 
  			check(rbrack); 
  			o.type.elemType = new Struct(o.type.kind);  	//if there is brack, we change the struct type as array of the old type
  			o.type.kind = Struct.Arr; 
  		}
  		return o.type;
  	}

	//VarDecl = Type ident {"," ident } ";".
    private static void VarDecl()
    {
        Struct type;
        
        type = Type();
        check(ident);            
        Tab.insert(Obj.Var, t.string, type);
        
        while(sym == comma)
        {
            scan();
            check(ident);
            Tab.insert(Obj.Var, t.string, type);
        }
        check(semicolon);
    }
        
	public static void parse() {
		// initialize symbol sets
		BitSet s;
		s = new BitSet(64); exprStart = s;
		s.set(ident); s.set(number); s.set(charCon); s.set(new_); s.set(lpar); s.set(minus);
                
        s = new BitSet(64); relopStart = s;
        s.set(eql); s.set(neq); s.set(gtr); s.set(geq); s.set(lss); s.set(leq);
        
        s = new BitSet(64); statSync = s;
        s.set(if_); s.set(while_); s.set(read_);s.set(return_); s.set(print_); s.set(lbrace);
        s.set(semicolon); s.set(eof);

		s = new BitSet(64); statStart = s;
		s.set(ident); s.set(if_); s.set(while_); s.set(read_);
		s.set(return_); s.set(print_); s.set(lbrace); s.set(semicolon);

		s = new BitSet(64); statSeqFollow = s;
		s.set(rbrace); s.set(eof);

		s = new BitSet(64); declStart = s;
		s.set(final_); s.set(ident); s.set(class_);

		s = new BitSet(64); declFollow = s;
		s.set(lbrace); s.set(void_); s.set(eof);

		// start parsing
		errors = 0; errDist = 3;
		scan();
		Program();
		if (sym != eof) error("end of file found before end of program");
	}

}







