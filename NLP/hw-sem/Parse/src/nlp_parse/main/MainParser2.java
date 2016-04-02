/**
 * This is the driver program for the EarleyParser2
 * It contains the main() and is the point of access between user and program
 */
package nlp_parse.main;

import nlp_parse.support.Grammar;
import nlp_parse.support.Parser;
import nlp_parse.data_structures.Tree;
import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;

public class MainParser2 {

	public static void main(String[] args) throws IOException {
		if (args.length < 2) {
			System.err.println("Usage:\n\t .\\parse2 foo.gr foo.sen ");
			System.exit(1);
		}

		Grammar grammar = new Grammar(args[0]);
		ArrayList<String> sentences = readSentences(args[1]);
		parseSentences(grammar,sentences);
	}

	public static void recognizeSentences(Grammar grammar, ArrayList<String> sentences) {
		//Check if sentence is grammatical
		for(String sent : sentences) {
			Parser parser = new EarleyParser2(grammar);
			boolean grammatical = parser.recognize(sent.split("\\s+"));
			System.out.println("Grammatical = " + grammatical + ":\n\t" + sent);
		}
	}

	// Return parse tree or NONE
	public static void parseSentences(Grammar grammar, ArrayList<String> sentences) {
		for(String sent : sentences) {
			Parser parser = new EarleyParser2(grammar);
			Tree tree = parser.parse(sent.split("\\s+"));
			if(tree != null) {
				System.out.println(tree.toString());
			} else {
				System.out.println("NONE");
			}
		}
	}

	// Read sentences from .sen file
	public static ArrayList<String> readSentences(String sent_file) {
		ArrayList<String> sentences = new ArrayList<String>();
		try {
			BufferedReader in = new BufferedReader(new FileReader(sent_file));
			String line;
			while ((line = in.readLine()) != null) {
				if (line.matches("\\s*")) {//ignore empty lines
				} else {
					sentences.add(line);
				}
			}
			in.close();
		}
		catch (IOException e) {
			e.printStackTrace();
			System.exit(1);
		}
		return sentences;
	}
}
