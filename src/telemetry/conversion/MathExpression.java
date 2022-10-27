package telemetry.conversion;

import common.Spacecraft;
import telemetry.FramePart;

public class MathExpression {
	int pos = -1, ch;
	String str;
	FramePart framePart;
	Spacecraft spacecraft;
	double rawValue = 0;
	
	public MathExpression(String str, double rawValue) {
		this.str = str;
		this.rawValue = rawValue;
	}
	
	 void nextChar() {
        ch = (++pos < str.length()) ? str.charAt(pos) : -1;
    }

    boolean eat(int charToEat) {
        while (ch == ' ' || ch == '\t' || ch == '"') nextChar();
        if (ch == charToEat) {
            nextChar();
            return true;
        }
        return false;
    }

    public double parse(FramePart framePart, Spacecraft spacecraft) {
    	this.framePart = framePart;
    	this.spacecraft = spacecraft;
        nextChar();
        double x = parseExpression();
        if (pos < str.length()) throw new RuntimeException("Unexpected character: " + (char)ch + " at position " + pos);
        return x;
    }

    // Grammar:
    // expression = term | expression `+` term | expression `-` term
    // term = factor | term `*` factor | term `/` factor
    // factor = `+` factor | `-` factor | `(` expression `)`
    //        | number | functionName factor | factor `^` factor

    double parseExpression() {
        double x = parseTerm();
        for (;;) {
            if      (eat('+')) x += parseTerm(); // addition
            else if (eat('-')) x -= parseTerm(); // subtraction
            else return x;
        }
    }

    double parseTerm() {
        double x = parseFactor();
        for (;;) {
        	if      (eat('^')) x = Math.pow(x, parseFactor()); // exponentiation
            if      (eat('*')) x *= parseFactor(); // multiplication
            else if (eat('/')) x /= parseFactor(); // division
            else return x;
        }
    }
    
    boolean validChar(int ch) {
    	if (ch >= 'a' && ch <= 'z' || ch >= 'A' && ch <= 'Z') return true;
    	if (ch == '_') return true;
    	return false;
    }

    double parseFactor() {
        if (eat('+')) return parseFactor(); // unary plus
        if (eat('-')) return -parseFactor(); // unary minus

        double x;
        int startPos = this.pos;
        if (eat('(')) { // parentheses
            x = parseExpression();
            eat(')');
        } else if ((ch >= '0' && ch <= '9') || ch == '.') { // numbers
            while ((ch >= '0' && ch <= '9') || ch == '.') nextChar();
            x = Double.parseDouble(str.substring(startPos, this.pos));
        } else if (validChar(ch)) { // functions
            while (validChar(ch) || ch >= '0' && ch <= '9') 
            	nextChar();
            String func = str.substring(startPos, this.pos);
            
            // First check if this is the reserved term x or X.  If so use the raw value
            if (func.equalsIgnoreCase("x")) {
            	x = rawValue;
            	return x;
            }
            
            // Next check if this is a field name and use that value if it is
            if (framePart.hasFieldName(func)) {
            	x = framePart.getDoubleValue(func, spacecraft);
            	return x;
            }
                        
            // Otherwise it needs to be the form functionName factor
            x = parseFactor();
            
            if (func.equalsIgnoreCase("sqrt")) x = Math.sqrt(x);
            else if (func.equalsIgnoreCase("sin")) x = Math.sin(x);
            else if (func.equalsIgnoreCase("cos")) x = Math.cos(x);
            else if (func.equalsIgnoreCase("tan")) x = Math.tan(x);
            else if (func.equalsIgnoreCase("acos")) x = Math.acos(x);
            else if (func.equalsIgnoreCase("asin")) x = Math.asin(x);
            else if (func.equalsIgnoreCase("atan")) x = Math.atan(x);
            else if (func.equalsIgnoreCase("abs")) x = Math.abs(x);
            
            else throw new RuntimeException("Unknown function or variable: " + func);
        } else {
            throw new RuntimeException("Unexpected term ending: " + (char)ch + " at position " + pos);
        }

        

        return x;
    }
}
