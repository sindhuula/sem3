/**
* This program is a model of the chart used by the Earley Parser
*/
package nlp_parse.support;

import java.util.ArrayList;
import java.util.HashMap;

import nlp_parse.data_structures.LinkedList;

public class Chart {
    ArrayList<LinkedList<EarleyDottedRule>> columns = null;
    ArrayList<HashMap<String,ArrayList<EarleyDottedRule>>> indexedColumns = new ArrayList<HashMap<String,ArrayList<EarleyDottedRule>>>();
    public LinkedList<EarleyDottedRule> getColumnValues(int i) {
        return columns.get(i);
    }
    public int getSizeColumns() {
        return columns.size();
    }

    //Add the possible rules for a column
    public void addRuleToColumn(final EarleyDottedRule rule, int column) {
        getColumnValues(column).addNode(rule);
        HashMap<String,ArrayList<EarleyDottedRule>> indexedColumn = indexedColumns.get(column);

        if (!rule.checkComplete()) {
            ArrayList<EarleyDottedRule> indexedRules;
            if (indexedColumn.containsKey(rule.symbolAfterDot())) {
                indexedRules = indexedColumn.get(rule.symbolAfterDot());
            } else {
                indexedRules = new ArrayList<EarleyDottedRule>();
                indexedColumn.put(rule.symbolAfterDot(), indexedRules );
            }
            indexedRules.add(rule);
        }
    }

    //Initialize chart based on sentence length
    public void initializeChart(Grammar grammar, Integer sent_length) {
        columns = new ArrayList<LinkedList<EarleyDottedRule>>();
        for(int i=0; i<sent_length+1; i++) {
            LinkedList<EarleyDottedRule> column = new LinkedList<EarleyDottedRule>();
            if(i==0) {
            }
            columns.add(column);
            HashMap<String,ArrayList<EarleyDottedRule>> indexedColumn = new HashMap<String,ArrayList<EarleyDottedRule>>();
            indexedColumns.add(indexedColumn);
        }
        for(Rule r : grammar.getStartRule()) {
            EarleyDottedRule start = new EarleyDottedRule(r,0,0, r.ruleWeight);
            addRuleToColumn(start, 0);
        }
    }

    //Get all the rules in column with index state.start whose symbolAfterDot is the lhs of the state
    public ArrayList<EarleyDottedRule> getAttachableRules(EarleyDottedRule state) {
        HashMap<String,ArrayList<EarleyDottedRule>> indexedColumn = indexedColumns.get(state.start);

        return indexedColumn.get(state.rule.getLhs());
    }
}
