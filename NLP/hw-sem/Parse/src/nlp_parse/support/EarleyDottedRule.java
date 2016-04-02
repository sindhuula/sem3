/**
 * This program manages the position etc for the dotted rules used with the Earley Parser
 */
package nlp_parse.support;
public class EarleyDottedRule {
	public int start;
	public int dot;
	public Rule rule = null;
	public double treeWeight;
    public EarleyDottedRule completedRule = null;
	public EarleyDottedRule attacheeRule = null;
	//EarleyDottedRule beingattachedRule = null; sounds too silly maybe call it attachee

	public EarleyDottedRule(Rule rule, Integer dot, Integer start, double treeWeight) {
		this.dot = dot;
		this.rule = rule;
		this.start = start;
		this.treeWeight = treeWeight;
	}
	
	//Returns what is the next symbol after the dot
    public String symbolAfterDot() {
		return rule.symbols[dot+1];
	}

    //Returns whether the reading of the rule has completed
	public boolean checkComplete() {
		//If the dot is at a position greater than number of symbols then true else false.
		return dot >= rule.symbols.length - 1;
	}
    //Define a new definition of hashCode that only considers start, dot and rule symbols.
    @Override
    public int hashCode() {
        int result = 17;
        result = 37*result + start;
        result = 37*result + dot;
        result = 37*result + (rule == null ? 0 : rule.symbolsHashCode);
        return result;
    }

    //Define a new definition of equals that only considers start, dot and rule symbols.
    @Override
    public boolean equals(Object o) {
        if (o instanceof EarleyDottedRule) {
            EarleyDottedRule other = (EarleyDottedRule)o;
            if (start == other.start &&
                    dot == other.dot &&
                    rule == other.rule) {
                //(rule == other.rule || Arrays.deepEquals(rule.symbols, other.rule.symbols))) {
                return true;
            }
        }
        return false;
    }

	@Override 
	public String toString() { 
	    StringBuilder sb = new StringBuilder();
	    sb.append(start);
	    sb.append(" ");
	    sb.append(rule.symbols[0]);
	    sb.append(" --> ");
	    int i;
	    for(i=1; i<rule.symbols.length; i++) {
	        if (dot+1 == i) {
	            sb.append(". ");
	        }
	        sb.append(rule.symbols[i]);
	        sb.append(" ");
	    }
	    if (dot+1 == i) {
	        sb.append(". ");
	    }
		return sb.toString();
	}

}