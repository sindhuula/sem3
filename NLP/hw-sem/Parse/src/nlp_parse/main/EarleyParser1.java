/**
 * This is the basic version of the Earley Parser
 */
package nlp_parse.main;

import nlp_parse.support.*;
import nlp_parse.data_structures.*;

import java.util.ArrayList;
import java.util.HashSet;

public class EarleyParser1 extends Parser{
    ArrayList<LinkedList<EarleyDottedRule>> chart = null;
    private Grammar grammar;

    public EarleyParser1(Grammar grammar) {
        this.grammar = grammar;
    }

    @Override
    public Tree parse(String[] sent) {
        //Check if the sentence is grammatical
        if(recognize(sent) == true) {
            EarleyDottedRule lowestDr = new EarleyDottedRule(null, 0, 0, Double.MAX_VALUE);
            for (EarleyDottedRule dr : chart.get(chart.size()-1)) {
                if(dr.rule.getLhs().equals(Grammar.ROOT) && dr.checkComplete() &&
                        dr.treeWeight < lowestDr.treeWeight) {
                    lowestDr = dr;
                }
            }
            return new Tree(lowestDr);
        }

        return null;
    }

    @Override
    //Recognize the rule
    public boolean recognize(String[] sent) {

        chartInitialization(sent.length);
        fillChart(sent);
        for (EarleyDottedRule dr : chart.get(chart.size()-1)) {
            if(dr.rule.getLhs().equals(Grammar.ROOT) && dr.checkComplete()) {
                return true;
            }
        }
        return false;
    }

    //Add values to the chart
    private void fillChart(String[] sent) {
        for(int i=0; i<chart.size(); i++) {
            LinkedList<EarleyDottedRule> column = chart.get(i);
            NodeList<EarleyDottedRule> entry = column.getFirstNode();
            HashSet<String> columnPredictions = new HashSet<String>();
            while( entry != null) {
                EarleyDottedRule state = entry.getNodeValue();
                if (state.checkComplete()) {
                    attach(state, i);
                } else if (grammar.checkIfNonterminal(state.symbolAfterDot())) {
                    predict(state, i, columnPredictions);
                } else {
                    ScanRule(state, sent, i);
                }

                entry = entry.getNextNode();
            }

        }
    }

    //Make predictions
    private void predict(EarleyDottedRule state, int column, HashSet<String> columnPredictions) {
        String symbolAfterDot = state.symbolAfterDot();
        if (columnPredictions.contains(symbolAfterDot)) {
            return;
        }
        columnPredictions.add(symbolAfterDot);
        for(Rule r : grammar.getRuleByLhs(symbolAfterDot)) {
            addRule(new EarleyDottedRule(r,0,column, r.ruleWeight),column);
        }
    }

    //Scan the rule
    private void ScanRule(EarleyDottedRule state, String[] sent, int column) {
        if(column < sent.length && sent[column].equals(state.symbolAfterDot())) {
            EarleyDottedRule scannedRule = new EarleyDottedRule(state.rule,state.dot+1,state.start, state.treeWeight);
            scannedRule.completedRule = null; //Dot is at the end of rule. No more scanning
            scannedRule.attacheeRule  = state; //There are still elements left to scan
            addRule(scannedRule,column+1);
        }
    }

    //Attach the completed constituents
    private void attach(EarleyDottedRule state, int column) {
        for(EarleyDottedRule r : chart.get(state.start)) {
            if(!r.checkComplete() && r.symbolAfterDot().equals(state.rule.getLhs())) {
                EarleyDottedRule newRule = new EarleyDottedRule(r.rule,r.dot+1,r.start, state.treeWeight + r.treeWeight);
                newRule.completedRule = state;//Dot is at the end
                newRule.attacheeRule  = r;//Dot is in between
                addRule(newRule, column);
            }
        }
    }

    //Add the rule
    private void addRule(EarleyDottedRule rule, Integer column) {
        chart.get(column).addNode(rule);
    }

    //Initialize the chart
    private void chartInitialization(Integer sentenceLength) {
        chart = new ArrayList<LinkedList<EarleyDottedRule>>();
        for(int i=0; i<sentenceLength+1; i++) {
            LinkedList<EarleyDottedRule> column = new LinkedList<EarleyDottedRule>();
            if(i==0) {
                for(Rule r : grammar.getStartRule()) {
                    EarleyDottedRule start = new EarleyDottedRule(r,0,0, r.ruleWeight);
                    column.addNode(start);
                }
            }
            chart.add(column);
        }
    }
}

