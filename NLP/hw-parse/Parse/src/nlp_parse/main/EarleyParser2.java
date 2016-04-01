package nlp_parse.main;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;

import nlp_parse.support.*;
import nlp_parse.utilities.*;

public class EarleyParser2 extends Parser {
	private Chart chart;
	private Grammar grammar;
	
	public EarleyParser2(Grammar grammar) {
		this.grammar = grammar;
		chart = new Chart();
	}

	@Override
	public boolean recognize(String[] sent) {

		chart.initializeChart(grammar, sent.length);
		fillChart(sent);
		
		// if the special rule exists in the last chart column with a dot at the end, this sentence is grammatical
		for (EarleyDottedRule dr : chart.getColumnValues(chart.getSizeColumns()-1)) {
			if(dr.rule.getLhs().equals(Grammar.ROOT) && dr.checkComplete()) {
				return true;
			}
		}
		return false;
	}
	@Override
	public Tree parse(String[] sent) {
		if(recognize(sent) == true) {
			// recover the lowest weight parse from backpointers
			// Fill lowestDr with a dummy EarleyDottedRule
			EarleyDottedRule lowestDr = new EarleyDottedRule(null, 0, 0, Double.MAX_VALUE);
			for (EarleyDottedRule dr : chart.getColumnValues(chart.getSizeColumns()-1)) {
				if(dr.checkComplete() && dr.rule.getLhs().equals(Grammar.ROOT)) {
					if (dr.treeWeight < lowestDr.treeWeight) {
						lowestDr = dr;
					}
				}
			}
			// recognize() == true ensures that lowestDr will not be the dummy dotted rule.
			return new Tree(lowestDr);
		}

		return null;
	}


	private void fillChart(String[] sent) {

		// For each chart column (sent.length + 1)
		for(int i=0; i<chart.getSizeColumns(); i++) {
			LinkedList<EarleyDottedRule> column = chart.getColumnValues(i);
			Node<EarleyDottedRule> entry = column.getFirstNode();
			HashMap<EarleyDottedRule,EarleyDottedRule> columnAttachments = new HashMap<EarleyDottedRule,EarleyDottedRule>();

			HashMap<String,HashSet<String>> leftAncestorPairTable = null;
						
			if( i < sent.length ) {
				leftAncestorPairTable = createAncestorPairTable(grammar,sent[i]);
			}

			// while there are states in the linked list
			while( entry != null) {
				EarleyDottedRule state = entry.getNodeValue();
				// ATTACH if we encounter a complete state
				if (state.checkComplete()) {
					// e.g. S -> NP VP .
					attach(state, i, columnAttachments);
				} 
				// PREDICT if we hit a nonterminal that isn't in the very last column
				else if (grammar.checkIfNonterminal(state.symbolAfterDot()) && i < chart.getSizeColumns() - 1 ) {
					// e.g. S -> . NP VP
					predict(state, i, leftAncestorPairTable);
				} 
				// otherwise SCAN
				else {
					// e.g. NP -> . Det N     (pre-terminal after dot)
					//  or
					// e.g. NP -> NP . and NP (terminal after dot) 
					scanRule(state, sent, i);
				}
				
				entry = entry.getNextNode();
			}
			
		}
	}


	// create ancestor pair table, starting from word B in the sentence
	private HashMap<String,HashSet<String>> createAncestorPairTable(Grammar grammar, String B) {
		HashMap<String,HashSet<String>> ancestors = new HashMap<String,HashSet<String>>();
		HashSet<String> processed_symbols = new HashSet<String>();
		
		// DFS
		processB(grammar,ancestors,processed_symbols,B);
		
		return ancestors;
	}

	// recursively populate left ancestor pair table 
	private void processB(Grammar grammar, HashMap<String,HashSet<String>> ancestors, HashSet<String> processed_symbols, String B) {
		processed_symbols.add(B); // don't process any symbol more than once
		HashSet<String> parents = grammar.leftParentTable.get(B);

		if(parents != null) {
			// for each parent A of B
			for(String A : parents) {
				
				// either create the hash of ancestors for this symbol or add to it
				if(ancestors.containsKey(A)) {
					ancestors.get(A).add(B);
				} else {
					HashSet<String> ancestors_of_A = new HashSet<String>();
					ancestors_of_A.add(B);
					ancestors.put(A, ancestors_of_A);
				}
				
				if(!processed_symbols.contains(A)) {
					processB(grammar,ancestors,processed_symbols,A);
				}
			}
		}
	}

	// don't need to store back-pointers for predictions
	private void predict(EarleyDottedRule state, int column, HashMap<String,HashSet<String>> leftAncestorPairTable) {
		String predictedSymbol = state.symbolAfterDot();
		
		// constrain predictions using the left ancestor pair table
		if(leftAncestorPairTable != null) {
			HashSet<String> left_ancestors = leftAncestorPairTable.get(predictedSymbol);
			if(left_ancestors != null) {
				for(String B : left_ancestors) {
					Pairs<String,String> key = new Pairs<String,String>(predictedSymbol,B);
					for(Rule r : grammar.prefixTable.get(key)) {
						chart.addRuleToColumn(new EarleyDottedRule(r,0,column, r.ruleWeight),column);
					}
				}
				leftAncestorPairTable.put(predictedSymbol, null);
			}
		} else { // first column of the chart (no string in sentence)
			for(Rule r : grammar.getRuleByLhs(predictedSymbol)) {
				chart.addRuleToColumn(new EarleyDottedRule(r,0,column, r.ruleWeight),column);
			}
		}
	}
	
	private void scanRule(EarleyDottedRule state, String[] sent, int column) {
		// if the symbol after the dot expands to the current word in the sentence
		// Only scan if there is text remaining in the sentence
		if(column < sent.length && sent[column].equals(state.symbolAfterDot())) {
			// Only change the position of the dot
			EarleyDottedRule scannedRule = new EarleyDottedRule(state.rule,state.dot+1,state.start, state.treeWeight);
			// For the scannedRule (NP -> NP and . NP, i)
			scannedRule.completedRule = null;
			scannedRule.attacheeRule  = state; // NP -> NP . and NP
			chart.addRuleToColumn(scannedRule,column+1);
		}
	}
	
	// attach completed constituent to customers
	private void attach(EarleyDottedRule state, int column, HashMap<EarleyDottedRule,EarleyDottedRule> columnAttachments) {

		ArrayList<EarleyDottedRule> attachableRules = chart.getAttachableRules(state);
		
		if (attachableRules != null) {
			for(EarleyDottedRule r : attachableRules) {
				EarleyDottedRule new_rule = new EarleyDottedRule(r.rule,r.dot+1,r.start, state.treeWeight + r.treeWeight);
				new_rule.completedRule = state;    // e.g. VP -> V .
				new_rule.attacheeRule  = r;	      // e.g. S  -> NP . VP
				
				// This will only match rules with the same symbols, dot position, and start position.
				EarleyDottedRule existingRule = columnAttachments.get(new_rule);
				if (existingRule != null) {
					if (existingRule.treeWeight > new_rule.treeWeight) {
						// The new_rule has lower weight, so update the higher weight 
						// rule of the same type
						// Note that we do not reprocess these rules when they are completed consistuents
						// and should be attached.
						existingRule.treeWeight = new_rule.treeWeight;
						existingRule.completedRule = new_rule.completedRule;
						existingRule.attacheeRule = new_rule.attacheeRule;
					} else {
						// The new_rule has higher weight, so do nothing
					}
					continue;
				}
				
				columnAttachments.put(new_rule, new_rule);
				chart.addRuleToColumn(new_rule, column);
			}
		}
	}

}