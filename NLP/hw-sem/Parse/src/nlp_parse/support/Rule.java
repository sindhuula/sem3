/**
 * This program contains functions that can be performed on a rule
 */
package nlp_parse.support;

import java.util.Arrays;

public class Rule {
	public Double ruleWeight;
	public String[] symbols;
	public int symbolsHashCode;
	
	public Rule(String[] tokens) {
		ruleWeight = new Double(tokens[0]);
		symbols = new String[tokens.length-1];  
		for(int i = 1; i < tokens.length; i++) {
			symbols[i-1] = tokens[i]; 
		}
		symbolsHashCode = Arrays.deepHashCode(symbols);
	}
	
	//Return the RHS of the rule
    public String[] getRhs() {
		String[] rhs = new String[symbols.length-1];
		for(int i=1; i<symbols.length; i++) {
			rhs[i-1] = symbols[i];
		}
		return rhs;
	}

    //Return the LHS of the rule
	public String getLhs() {
		return symbols[0];
	}

    //Convert the rule to string format
	public String toString() {
		StringBuilder sb = new StringBuilder();
		sb.append(symbols[0]+"->");
		int i;
	    for(i=1; i<symbols.length; i++) {
	        sb.append(symbols[i]);
	        sb.append(" ");
	    }
	    return sb.toString();
	}
}