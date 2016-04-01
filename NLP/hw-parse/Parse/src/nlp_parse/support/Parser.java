package nlp_parse.support;

import nlp_parse.support.Tree;

// parser interface assumed in MainParser
public abstract class Parser {
	public abstract Tree parse(String[] sent);
	public abstract boolean recognize(String[] sent);
}
