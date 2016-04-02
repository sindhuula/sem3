/**
 * Parser interface used by the MainParser programs
 */
package nlp_parse.support;
import nlp_parse.data_structures.Tree;
public abstract class Parser {
	public abstract Tree parse(String[] sent);
	public abstract boolean recognize(String[] sent);
}
