/**
 * This will form the parse tree
 */
package nlp_parse.data_structures;
import nlp_parse.support.EarleyDottedRule;

public class Tree {
	EarleyDottedRule root = null;
	public Tree(EarleyDottedRule dr) {
		root = dr;
	}
    //Convert the dotted rule to string format
	public static String dottedRuleToString(EarleyDottedRule dr) {
		StringBuilder sb = new StringBuilder();
		if(dr != null) {
			if(dr.checkComplete()) {
				sb.append("(");
				sb.append(dr.rule.getLhs());
				sb.append(" ");
			}
		
			sb.append(dottedRuleToString(dr.attacheeRule));
			
			if(dr.completedRule == null && dr.attacheeRule != null) {
				sb.append(dr.attacheeRule.symbolAfterDot());
				sb.append(" ");
			} 
			
			sb.append(dottedRuleToString(dr.completedRule));
			
			if(dr.checkComplete()) {
				sb.append(")");
			}
		}
		
		return sb.toString();
	}
	
	@Override
	public String toString() {	
		return dottedRuleToString(root) + "\n" + root.treeWeight;	
	}
}