package nlp_parse.main;

import nlp_parse.support.Grammar;
import nlp_parse.support.Parser;
import nlp_parse.support.Tree;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;

// driver
public class MainParser2 {

	public static void main(String[] args) throws IOException {
		if (args.length < 2) {
			System.err.println("Usage:\n\tjava nlp_parse\\main\\MainParser2.java foo.gr foo.sen ");
			System.exit(1);
		}


		// initialize grammar
		Grammar grammar = new Grammar(args[0]);
		// read sentences to parse
		ArrayList<String> sentences = readSentences(args[1]);
		// parse sentences
		parseSentences(grammar,sentences);
	}

	// make grammaticality judgments on list of sentences
	public static void recognizeSentences(Grammar grammar, ArrayList<String> sentences) {

		for(String sent : sentences) {
			Parser parser = new EarleyParser2(grammar);
			boolean grammatical = parser.recognize(sent.split("\\s+"));
			System.out.println("Grammatical = " + grammatical + ":\n\t" + sent);
		}
	}

	// return parse trees or NONE for each sentence
	public static void parseSentences(Grammar grammar, ArrayList<String> sentences) {

		for(String sent : sentences) {

			Parser parser = new EarleyParser2(grammar);
			Tree tree = parser.parse(sent.split("\\s+"));

			if(tree != null) {
				System.out.println(tree.toString());
			} else {
				// According to spec of hw3
				System.out.println("NONE");
			}
		}
	}

	// read sentences in the supplies .sen file
	public static ArrayList<String> readSentences(String sent_file) {
		ArrayList<String> sentences = new ArrayList<String>();
		try {
			BufferedReader in = new BufferedReader(new FileReader(sent_file));
			String line;
			while ((line = in.readLine()) != null) {
				if (line.matches("\\s*")) {
					// Ignore empty lines since arith.par does so.
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