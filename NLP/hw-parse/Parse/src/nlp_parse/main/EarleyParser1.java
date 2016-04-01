package nlp_parse.main;

import nlp_parse.support.*;
import nlp_parse.utilities.*;

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

        // if this sentence is grammatical
        if(recognize(sent) == true) {
            // recover the lowest weight parse from backpointers
            // Fill lowestDr with a dummy EarleyDottedRule
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
    public boolean recognize(String[] sent) {

        initialize_chart(sent.length);
        fill_chart(sent);

        // if the special rule exists in the last chart column with a dot at the end, this sentence is grammatical
        for (EarleyDottedRule dr : chart.get(chart.size()-1)) {
            if(dr.rule.getLhs().equals(Grammar.ROOT) && dr.checkComplete()) {
                return true;
            }
        }
        return false;
    }

    private void fill_chart(String[] sent) {

        // For each chart column (sent.length + 1)
        for(int i=0; i<chart.size(); i++) {
            LinkedList<EarleyDottedRule> column = chart.get(i);
            Node<EarleyDottedRule> entry = column.getFirstNode();
            HashSet<String> columnPredictions = new HashSet<String>();

            while( entry != null) {
                EarleyDottedRule state = entry.getNodeValue();
                if (state.checkComplete()) {
                    // e.g. S -> NP VP .
                    attach(state, i);
                } else if (grammar.checkIfNonterminal(state.symbolAfterDot())) {
                    // e.g. S -> . NP VP
                    predict(state, i, columnPredictions);
                } else {
                    // e.g. NP -> . Det N     (pre-terminal after dot)
                    //  or
                    // e.g. NP -> NP . and NP (terminal after dot) 
                    scan(state, sent, i);
                }

                entry = entry.getNextNode();
            }

        }
    }

    // don't need to store backpointers for predictions
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

    private void scan(EarleyDottedRule state, String[] sent, int column) {
        // if the symbol after the dot expands to the current word in the sentence
        // Only scan if there is text remaining in the sentence
        if(column < sent.length && sent[column].equals(state.symbolAfterDot())) {
            // Only change the position of the dot
            EarleyDottedRule scannedRule = new EarleyDottedRule(state.rule,state.dot+1,state.start, state.treeWeight);
            scannedRule.completedRule = null;
            scannedRule.attacheeRule  = state; // NP -> NP . and NP
            addRule(scannedRule,column+1);
        }
    }

    private void attach(EarleyDottedRule state, int column) {
        for(EarleyDottedRule r : chart.get(state.start)) {
            if(!r.checkComplete() && r.symbolAfterDot().equals(state.rule.getLhs())) { // problem
                EarleyDottedRule new_rule = new EarleyDottedRule(r.rule,r.dot+1,r.start, state.treeWeight + r.treeWeight);
                new_rule.completedRule = state;    // e.g. VP -> V .
                new_rule.attacheeRule  = r;	      // e.g. S  -> NP . VP
                addRule(new_rule, column);
            }
        }
    }

    private void addRule(EarleyDottedRule rule, Integer column) {
        chart.get(column).addNode(rule);
    }

    private void initialize_chart(Integer sent_length) {
        chart = new ArrayList<LinkedList<EarleyDottedRule>>();
        for(int i=0; i<sent_length+1; i++) {
            LinkedList<EarleyDottedRule> column = new LinkedList<EarleyDottedRule>();
            if(i==0) {
                // Enqueue special start rule
                for(Rule r : grammar.getStartRule()) {
                    EarleyDottedRule start = new EarleyDottedRule(r,0,0, r.ruleWeight);
                    column.addNode(start);
                }
            }
            chart.add(column);
        }
    }
}

