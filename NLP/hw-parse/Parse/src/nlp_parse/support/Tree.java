package nlp_parse.support;

public class Tree {
	EarleyDottedRule root = null;
	public Tree(EarleyDottedRule dr) {
		root = dr;
	}
	// extract the tree using back pointers
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