/**
 * This program contains what all actions are performed on a grammar
 */
package nlp_parse.support;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;

import nlp_parse.data_structures.*;
public class Grammar {
	public static final String ROOT = "ROOT";

	public HashMap<String,ArrayList<Rule>> lhsToRules = new HashMap<String,ArrayList<Rule>>();
	public HashMap<Pairs<String,String>,ArrayList<Rule>> prefixTable = new HashMap<Pairs<String,String>,ArrayList<Rule>>();
	public HashMap<String,HashSet<String>> leftParentTable = new HashMap<String,HashSet<String>>();
	Set<String> preterminals = new HashSet<String>();
	Set<String> terminals = new HashSet<String>();
	
    //Read the grammar file
	public Grammar(String fileName) {
		readGrammar(fileName);
	}
    public void readGrammar(String fileName) {
		System.out.println("Loading grammar from : " + fileName);
		File f = new File(fileName);
		try {
			FileInputStream fis = new FileInputStream(f);
			BufferedReader bis = new BufferedReader(new InputStreamReader(fis));
			String line = null;
			Set<String> symbols = new HashSet<String>();
			while ((line = bis.readLine()) != null) {
				String[] tokens = line.split("\\s+");
				if(tokens.length > 0) {
					// keep track of what symbols we've found
					for(int i=1;i<tokens.length;i++) {
						symbols.add(tokens[i]);
					}
					String key = makeKey(tokens);
					ArrayList<Rule> rules = lhsToRules.containsKey(key) ? (ArrayList<Rule>) lhsToRules.get(key) : new ArrayList<Rule>();
					Rule newRule = new Rule(tokens);
					rules.add(newRule);
					lhsToRules.put(key,rules);
    				String A = newRule.symbols[0]; String B = newRule.symbols[1];
					Pairs<String,String> ptKey = new Pairs<String,String>(A,B);
					if( ! prefixTable.containsKey(ptKey) ) {
						if( leftParentTable.containsKey(B) ) {
							leftParentTable.get(B).add(A);
						} else {
							HashSet<String> newSet = new HashSet<String>();
							newSet.add(A);
							leftParentTable.put(B, newSet);
						}
					}
					if(prefixTable.containsKey(ptKey)) {
						prefixTable.get(ptKey).add(newRule);
					} else {
						ArrayList<Rule> newRuleList = new ArrayList<Rule>();
						newRuleList.add(newRule);
						prefixTable.put(ptKey, newRuleList);
					}
				}
				else {
					System.out.println("Empty rule found in grammar file");
				}
			}

            //Make a set of terminals
			for(String symbol : symbols) {
				if(!lhsToRules.keySet().contains(symbol)) {
					terminals.add(symbol);
				}
			}
			
			// Based on terminals find the preterminals
			for(String lhs : lhsToRules.keySet()) {
				boolean preterminal = true;
				for(Rule r : lhsToRules.get(lhs)) {
					Set<String> ruleRhs = new HashSet<String>();
					for(String token : r.getRhs()) {
						ruleRhs.add(token);
					}
					if(!terminals.containsAll(ruleRhs)) {
						preterminal = false;
						break;
                    }
				}
				if(preterminal == true) {
					preterminals.add(lhs);
				}
			}
			
		} catch (IOException e) {
			throw new RuntimeException("Problem reading grammar file:" + fileName, e);
		}
	}
	// Return the set of all non-terminals in the grammar
	public Set<String> getNonterminals() {
		return lhsToRules.keySet();
	}

	// Given a string of tokens pick one to use as a hash key
	private String makeKey(String[] tokens) {
		return tokens[1];
	}

	// Checks if given symbol is a nonterminal
	public boolean checkIfNonterminal(String symbol) {
		return !terminals.contains(symbol);
	}

	// Return rule by the LHS
	public ArrayList<Rule> getRuleByLhs(String symbol) {
		return lhsToRules.get(symbol);
	}

	// return rules that signal the root of the parse tree
	public ArrayList<Rule> getStartRule() {
		ArrayList<Rule> startRule = lhsToRules.get(ROOT);
		return startRule;
	}
}
