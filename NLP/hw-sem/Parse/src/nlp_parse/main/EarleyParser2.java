/**
 * This is the optimized version of the Earley Parser
 */
package nlp_parse.main;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;

import nlp_parse.support.*;
import nlp_parse.data_structures.*;

public class EarleyParser2 extends Parser {
	private Chart chart;
	private Grammar grammar;

	public EarleyParser2(Grammar grammar) {
		this.grammar = grammar;
		chart = new Chart();
	}

	@Override
    //Recognize the rule
	public boolean recognize(String[] sent) {

		chart.initializeChart(grammar, sent.length);
		fillChart(sent);
		for (EarleyDottedRule dr : chart.getColumnValues(chart.getSizeColumns()-1)) {
			if(dr.rule.getLhs().equals(Grammar.ROOT) && dr.checkComplete()) {
				return true;
			}
		}
		return false;
	}

	@Override
	//Print the tree
    public Tree parse(String[] sent) {
		if(recognize(sent) == true) {
			EarleyDottedRule lowestDr = new EarleyDottedRule(null, 0, 0, Double.MAX_VALUE);
			for (EarleyDottedRule dr : chart.getColumnValues(chart.getSizeColumns()-1)) {
				if(dr.checkComplete() && dr.rule.getLhs().equals(Grammar.ROOT)) {
					if (dr.treeWeight < lowestDr.treeWeight) {
						lowestDr = dr;
					}
				}
			}
			return new Tree(lowestDr);
		}

		return null;
	}

    //Fill the chart
	private void fillChart(String[] sent) {
		for(int i=0; i<chart.getSizeColumns(); i++) {
			LinkedList<EarleyDottedRule> column = chart.getColumnValues(i);
			NodeList<EarleyDottedRule> entry = column.getFirstNode();
			HashMap<EarleyDottedRule,EarleyDottedRule> columnAttachments = new HashMap<EarleyDottedRule,EarleyDottedRule>();
			HashMap<String,HashSet<String>> leftAncestorPairTable = null;
			if( i < sent.length ) {
				leftAncestorPairTable = createAncestorPairTable(grammar,sent[i]);
			}
            while( entry != null) {
				EarleyDottedRule state = entry.getNodeValue();
				if (state.checkComplete()) {
					attach(state, i, columnAttachments);
				}
				else if (grammar.checkIfNonterminal(state.symbolAfterDot()) && i < chart.getSizeColumns() - 1 ) {
					predict(state, i, leftAncestorPairTable);
				}
				else {
					scanRule(state, sent, i);
				}

				entry = entry.getNextNode();
			}

		}
	}

    //Create anscestor word pair table for word B
	private HashMap<String,HashSet<String>> createAncestorPairTable(Grammar grammar, String B) {
		HashMap<String,HashSet<String>> ancestors = new HashMap<String,HashSet<String>>();
		HashSet<String> processedSymbols = new HashSet<String>();
		processB(grammar,ancestors,processedSymbols,B);
		return ancestors;
	}

	//Populate the left ancestor pair table
	private void processB(Grammar grammar, HashMap<String,HashSet<String>> ancestors, HashSet<String> processedSymbols, String B) {
		processedSymbols.add(B);
		HashSet<String> parents = grammar.leftParentTable.get(B);
		if(parents != null) {
			for(String A : parents) {
				if(ancestors.containsKey(A)) {
					ancestors.get(A).add(B);
				} else {
					HashSet<String> ancestorsA = new HashSet<String>();
					ancestorsA.add(B);
					ancestors.put(A, ancestorsA);
				}
				if(!processedSymbols.contains(A)) {
					processB(grammar,ancestors,processedSymbols,A);
				}
			}
		}
	}

	//Make predictions
	private void predict(EarleyDottedRule state, int column, HashMap<String,HashSet<String>> leftAncestorPairTable) {
		String predictedSymbol = state.symbolAfterDot();
		if(leftAncestorPairTable != null) {
			HashSet<String> ancestorsLeft = leftAncestorPairTable.get(predictedSymbol);
			if(ancestorsLeft != null) {
				for(String B : ancestorsLeft) {
					Pairs<String,String> key = new Pairs<String,String>(predictedSymbol,B);
					for(Rule r : grammar.prefixTable.get(key)) {
						chart.addRuleToColumn(new EarleyDottedRule(r,0,column, r.ruleWeight),column);
					}
				}
				leftAncestorPairTable.put(predictedSymbol, null);
			}
		} else {
			for(Rule r : grammar.getRuleByLhs(predictedSymbol)) {
				chart.addRuleToColumn(new EarleyDottedRule(r,0,column, r.ruleWeight),column);
			}
		}
	}

    //Scan the rule
	private void scanRule(EarleyDottedRule state, String[] sent, int column) {
		if(column < sent.length && sent[column].equals(state.symbolAfterDot())) {
			EarleyDottedRule scannedRule = new EarleyDottedRule(state.rule,state.dot+1,state.start, state.treeWeight);
            scannedRule.completedRule = null; //Dot is at the end of rule. No more scanning
            scannedRule.attacheeRule  = state; //There are still elements left to scan
            chart.addRuleToColumn(scannedRule,column+1);
		}
	}

	//Attach completed constituents
	private void attach(EarleyDottedRule state, int column, HashMap<EarleyDottedRule,EarleyDottedRule> columnAttachments) {

		ArrayList<EarleyDottedRule> attachableRules = chart.getAttachableRules(state);

		if (attachableRules != null) {
			for(EarleyDottedRule r : attachableRules) {
				EarleyDottedRule newRule = new EarleyDottedRule(r.rule,r.dot+1,r.start, state.treeWeight + r.treeWeight);
                newRule.completedRule = state;//Dot is at the end
                newRule.attacheeRule  = r;//Dot is in between
				EarleyDottedRule existingRule = columnAttachments.get(newRule);
				if (existingRule != null) {
					if (existingRule.treeWeight > newRule.treeWeight) {
					    //Update weight
						existingRule.treeWeight = newRule.treeWeight;
						existingRule.completedRule = newRule.completedRule;
						existingRule.attacheeRule = newRule.attacheeRule;
					}
					continue;
				}

				columnAttachments.put(newRule, newRule);
				chart.addRuleToColumn(newRule, column);
			}
		}
	}
}
