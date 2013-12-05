
//Baptiste 
//Matthew 
//Thomas 

/* MicroJava Scanner (HM 06-12-28)
   =================
*/
package MJ;
import java.io.*;
import java.nio.BufferOverflowException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Hashtable;

public class Scanner {
	private static final char eofCh = '\u0080';
	private static final char eol = '\n';
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
        
        private static Hashtable<String, Integer> keys; // list of keywords
        private static StringBuilder builder;
        
	private static char ch;			// lookahead character
	public  static int col;			// current column
	public  static int line;		// current line
	private static int pos;			// current position from start of source file
	private static Reader in;  	// source file reader
	private static char[] lex;	// current lexeme (token string)

	//----- ch = next input character
	private static void nextCh() {
		try {
			ch = (char)in.read(); col++; pos++;
			if (ch == eol) {line++; col = 0;}
			else if (ch == '\uffff') {ch = eofCh;}
		} catch (IOException e) {
			ch = eofCh;
		}
	}

	//--------- Initialize scanner
	public static void init(Reader r) {
		in = new BufferedReader(r);
		lex = new char[64];
		line = 1; col = 0;
		nextCh();
                
		//We complete the Hashtable with the keywords/values
		keys = new Hashtable<String, Integer>();
		keys.put("class", class_);
		keys.put("else", else_);
		keys.put("final", final_);
		keys.put("if", if_);
		keys.put("new", new_);
		keys.put("print", print_);
		keys.put("program", program_);
		keys.put("read", read_);
		keys.put("return", return_);
		keys.put("void", void_);
		keys.put("while", while_);
                
        builder = new StringBuilder();
	}

	//---------- Return next input token
public static Token next() {

	while (ch <= ' ') 
    {
        nextCh();
    }  // skip blanks, tabs, eols

	Token t = new Token(); t.line = line; t.col = col;

	switch (ch) 
	{
		case 'a': case 'b': case 'c': case 'd': case 'e': case 'f': case 'g': case 'h': case 'i': case 'j': case 'k': case 'l': case 'm': case 'n': case 'o': case 'p': case 'q': case 'r': case 's': case 't': case 'u': case 'v': case 'w': case 'x': case 'y': case 'z': case 'A': case 'B': case 'C': case 'D': case 'E': case 'F': case 'G': case 'H': case 'I': case 'J': case 'K': case 'L': case 'M': case 'N': case 'O': case 'P': case 'Q': case 'R': case 'S': case 'T': case 'U': case 'V': case 'W': case 'X': case 'Y': case 'Z':
			readName(t); break;
		case '0': case '1': case '2': case '3': case '4': case '5': case '6': case '7': case '8': case '9':
			readNumber(t); break;
        case '\'': readCharCon(t); break;
		case '+': nextCh(); t.kind = plus; break;
		case '-': nextCh(); t.kind = minus; break;
		case '*': nextCh(); t.kind = times; break;
		case '%': nextCh(); t.kind = rem; break;
		case ';': nextCh(); t.kind = semicolon; break;
		case '.': nextCh(); t.kind = period; break;
		case ',': nextCh(); t.kind = comma; break;
		case '(': nextCh(); t.kind = lpar; break;
		case ')': nextCh(); t.kind = rpar; break;
		case '[': nextCh(); t.kind = lbrack; break;
		case ']': nextCh(); t.kind = rbrack; break;
		case '{': nextCh(); t.kind = lbrace; break;
		case '}': nextCh(); t.kind = rbrace; break;
		case eofCh: t.kind = eof; break;  // no nextCh() any more
		case '=': nextCh();
			if (ch == '=') { nextCh(); t.kind = eql; } else t.kind = assign;
		break;
		case '!': nextCh();
			if (ch == '=') { nextCh(); t.kind = neq; } else t.kind = none;
		break;
		case '<': nextCh();
			if (ch == '=') { nextCh(); t.kind = leq; } else t.kind = lss;
		break;
		case '>': nextCh();
			if (ch == '=') { nextCh(); t.kind = geq; } else t.kind = gtr;
		break;
		case '/': nextCh();
		if (ch == '/') {
			do nextCh(); while (ch != '\n' && ch != eofCh);
			t = next();  // call scanner recursively
		} else t.kind = slash;
		break;

		default: nextCh(); t.kind = none; break;
	}
	return t;
}

private static void readName(Token t)
{
    builder = new StringBuilder();
    while (Character.isLetterOrDigit(ch) || ch == '_') //while the next char is a digit or a letter, this is the same token
    {
        builder.append(ch);  
        nextCh();
    }
    
    t.string = builder.toString();
    
    if(!keys.containsKey(t.string)) //we check if the identifier is a keyword or not
        t.kind = ident;
    else
        t.kind = keys.get(t.string);
    
}

private static void readNumber(Token t)
{
    builder = new StringBuilder();
    while (Character.isDigit(ch)) //while the next char is a digit, this is the same token number
    {
        builder.append(ch);  
        nextCh();
    }
    
    t.kind = number;
    try
    {
    	t.val = Integer.parseInt(builder.toString());
	} 
    catch(Exception e)
    {
		System.out.println("-- Buffer overflow, this number is not a valid Integer. ");
	}
    
}


	static void readCharCon(Token token)
	{
		token.kind = charCon;
		StringBuilder bstr = new StringBuilder();
		bstr.append(ch);
		nextCh();
		char c = 0;
		if(ch == '\\') //if the char is an escaped character
		{
			bstr.append(ch);
			nextCh();
			switch(ch)
			{
				case 't':
					c = '\t';
					break;
				case 'r':
					c = '\r';
					break;
				case 'n':
					c = '\n';
					break;
				default:
					c = ch;
			}
			bstr.append(ch);
		}
		else if(Character.isLetter(ch))
		{
			c = ch;
			bstr.append(ch);
		}

		nextCh();
		bstr.append(ch);
		token.string = bstr.toString(); //string contains something like 'e' or '\n'


		if((!token.string.substring(0, 1).equals("\'") || !token.string.substring(token.string.length() - 1).equals("\'")) || 	//if there is not ' at begining and end
				(token.string.length() == 4 && !token.string.substring(1, 2).equals("\\")))								//if there is 4 char in string without escaped character
		{
			System.out.println("Error line " + token.line + ", col " + token.col + ": " + "Invalid character constant");
		}
		else
			token.val = (int) c;	//finally assign the token value with the char

		nextCh();
	}
	
}




