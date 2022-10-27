package telemetry.conversion;

import java.text.DecimalFormat;

import common.Spacecraft;

/**
 * FOX 1 Telemetry Decoder
 * @author chris.e.thompson g0kla/ac2cz
 *
 * Copyright (C) 2022 amsat.org
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 *
 * This class implements a check as the final step in a conversion.  If the
 * result is a specific value then an error message is printed.  Otherwise the
 * result is printed with a defined number of decimal places.
 * 
 * invalid_check(value,"message", decimal_places)
 * 
 * 
 */
public class ConversionInvalidCheck extends Conversion {
	public static final String KEYWORD = "invalid_check";
	int pos = -1, ch;
	String str;
	double triggerValue;
	String message;
	int decimalPlaces;
	
	public ConversionInvalidCheck(String name, Spacecraft fox) {
		super(name, fox);
		this.str = name;
		parse();
	}

	// Grammar:
	// expression = function
	// function = word  `(` number `,` quotedstring `,` integer`)`
	// quotedString = word | '"' string '"'
	// factor = string | number


	void nextChar() {
		ch = (++pos < str.length()) ? str.charAt(pos) : -1;
	}

	boolean eatWhiteSpace() {
		while (ch == ' ' || ch == '\t' || ch == '"') nextChar();
		return true;
	}
	
	boolean eat(int charToEat) {
		eatWhiteSpace();
		if (ch == charToEat) {
			nextChar();
			return true;
		}
		return false;
	}

	boolean parse() {
		boolean result = false;
		nextChar();
		eatWhiteSpace();
		result = parseFunction(KEYWORD);
		eatWhiteSpace();
		if (!result) throw new RuntimeException("Corrupt invalid check expression: " + str + " at position " + pos);
		if (pos < str.length()) throw new RuntimeException("Unexpected character: " + (char)ch + " at position " + pos);
		return result;
	}

	boolean parseFunction(String name) {
		String func = parseWord();
		if (func == null || !func.equalsIgnoreCase(name)) throw new RuntimeException("Invalid function name: " + func + " at position " + (pos-1));;
		if (eat('(')) { // parentheses
			triggerValue = parseNumber();

			message = parseQuotedString();
			if (message == null) return false;

			decimalPlaces = parseInt();
			if (!eat(')')) throw new RuntimeException("Missing: ) at position " + (pos-1));
			return true;
		} else throw new RuntimeException("Missing: ( at position " + (pos-1));
	}
	
	String parseWord() {
		while (ch == ' ' || ch == '\t' || ch == '"') nextChar();
		int startPos = this.pos;
		while (validChar(ch) || ch >= '0' && ch <= '9') 
			nextChar();
		String func = str.substring(startPos, this.pos);
		return func;
	}

	String parseQuotedString() {
		String s = null;
		if (eat('\'')) { // quote
            s = parseString();
            eat('\'');
		} else if (eat('\"')) { // quote
            s = parseString();
            eat('\"');
        } else {
        	s = parseWord(); // we can not have spaces if there are no quotes
        }
		return s;
	}
	
	String parseString() {
		int startPos = this.pos;
		while (validChar(ch) || ch == ' ' 
				|| ch == '-' || ch == '^' || ch == '&' || ch == '$' || ch == '/' || ch == '?' || ch == '<' || ch == '>' || ch == ':' 
				|| ch == '_' || ch == '~' || ch == '@' || ch == '#' || ch == '=' || ch == '%' || ch == '+' || ch == '*' || ch == '!' 
				|| ch == '.' || ch == '['|| ch == ']'
				|| ch >= '0' && ch <= '9') 
			nextChar();
		String func = str.substring(startPos, this.pos);
		return func;

	}

	double parseNumber() {
		while (ch == ' ' || ch == '\t' || ch == '"') nextChar();
		int startPos = this.pos;
		if ((ch >= '0' && ch <= '9') || ch == '.') { // numbers
			while ((ch >= '0' && ch <= '9') || ch == '.') nextChar();
			Double x = Double.parseDouble(str.substring(startPos, this.pos));
			return x;
		}
		return 0.0;
	}

	int parseInt() {
		while (ch == ' ' || ch == '\t' || ch == '"') nextChar();
		int startPos = this.pos;
		if ((ch >= '0' && ch <= '9')) { // int digits
			while ((ch >= '0' && ch <= '9')) nextChar();
			Integer x = Integer.parseInt(str.substring(startPos, this.pos));
			return x;
		}
		return 0;

	}

	boolean validChar(int ch) {
		if (ch >= 'a' && ch <= 'z' || ch >= 'A' && ch <= 'Z') return true;
		if (ch == '_') return true;
		return false;
	}
	
	public String toString() {
		String s = "Invalid Check:\n";
		s = s + triggerValue + "\n";
		s = s + message + "\n";
		s = s + decimalPlaces + "\n";
		return s;
	}

	@Override
	public double calculate(double x) {
		return x;
	}

	@Override
	public String calculateString(double x) {
		String s = "";
		if ( almostEqual(x, triggerValue, 0.01)) {
			return message;
		}
		if (decimalPlaces == 0) {
			s = ""+ Math.round(x);
			return s;
		}
		String numberSigns = "";
		for (int i = 0; i < decimalPlaces; i++) {
		    numberSigns += "0";
		}

		DecimalFormat fmt = new DecimalFormat ("0." + numberSigns);
		s = fmt.format(x);
		return s;
	}
	
	public static boolean almostEqual(double a, double b, double eps){
	    return Math.abs(a-b)<eps;
	}
	
	public static void main(String[] args) {
		ConversionInvalidCheck invalid_check = new ConversionInvalidCheck(" invalid_check (123.5 'invalid if < 1.5' 1)", null);
		System.out.println(invalid_check);
		
		System.out.println(invalid_check.calculateString(120.12345));
		System.out.println(invalid_check.calculateString(123.5));
	}

}    


