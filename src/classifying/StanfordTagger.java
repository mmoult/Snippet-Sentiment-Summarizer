package classifying;

import static classifying.PartOfSpeechTagger.PartOfSpeech.*;

import java.io.IOException;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.List;

import edu.stanford.nlp.ling.HasWord;
import edu.stanford.nlp.ling.Sentence;
import edu.stanford.nlp.ling.TaggedWord;
import edu.stanford.nlp.tagger.maxent.MaxentTagger;
import util.AmazonFileResources;


public class StanfordTagger implements PartOfSpeechTagger {
	private MaxentTagger tagger;
	
	public static void main(String args[]) {
		StanfordTagger tagger = new StanfordTagger();
		String words = "Hello we like to run in Denmark";
		System.out.println(tagger.identify(words));
	}
	
	public StanfordTagger() {
		try {
			tagger = new MaxentTagger(AmazonFileResources.fileRoot + "english-left3words-distsim.tagger");
		} catch (ClassNotFoundException | IOException e) {
			e.printStackTrace();
		}
	}

	@Override
	public List<Pair<String, PartOfSpeech>> identify(String sentenc) {
		List<List<HasWord>> sentences = MaxentTagger.tokenizeText(new StringReader(sentenc));
		List<Pair<String, PartOfSpeech>> pos = new ArrayList<>();
		
		for (List<HasWord> sentence : sentences) {
			List<TaggedWord> tSentence = tagger.tagSentence(sentence);
			String res = Sentence.listToString(tSentence, false);
			String[] parts = res.split(" ");
			
			for(String part: parts) {
				String word = part.substring(0, part.indexOf('/'));
				switch(part.substring(part.indexOf('/')+1)) {
				case "ADD":
				case "FW":
				case "GW":
				case "LS":
				case "SYM":
				case "TO":
				case "UH":
					pos.add(new Pair<>(word, EXTRA));
					break;
				case "AFX":
					pos.add(new Pair<>(word, PREFIX));
					break;
				case "CC":
					pos.add(new Pair<>(word, CONJ));
					break;
				case "CD":
					pos.add(new Pair<>(word, CARDINAL));
					break;
				case "DT":
				case "PDT":
					pos.add(new Pair<>(word, ARTICLE));
					break;
				case "EX": //there can be practically any part of speech
					pos.add(new Pair<>(word, ADV));
					break;
				case "HYPH":
				case "NFP":
				case "-RRB-":
				case "-LRB-":
				case ".":
				case ",":
				case ":":
				case "$":
				case "''":
				case "``":
					//don't add anything- these are punctuation
					break;
				case "IN":
					pos.add(new Pair<>(word, PREP));
					break;
				case "JJ":
					pos.add(new Pair<>(word, ADJ));
					break;
				case "JJR":
					pos.add(new Pair<>(word, ADJ_COMPARE));
					break;
				case "JJS":
					pos.add(new Pair<>(word, ADJ_SUPER));
					break;
				case "MD":
					pos.add(new Pair<>(word, VERB));
					break;
				case "NN":
					pos.add(new Pair<>(word, NOUN));
					break;
				case "NNS":
					pos.add(new Pair<>(word, NOUN_PL));
					break;
				case "NNP":
					pos.add(new Pair<>(word, NOUN_PROPER));
					break;
				case "NNPS":
					pos.add(new Pair<>(word, NOUN_PROPER_PL));
					break;
				case "POS":
					pos.add(new Pair<>(word, GENITIVE_MARK));
					break;
				case "PRP":
					pos.add(new Pair<>(word, PRON));
					break;
				case "PRP$":
					pos.add(new Pair<>(word, PRON_POS));
					break;
				case "RB":
					pos.add(new Pair<>(word, ADV));
					break;
				case "RBR":
					pos.add(new Pair<>(word, ADV_COMPARE));
					break;
				case "RBS":
					pos.add(new Pair<>(word, ADV_SUPER));
					break;
				case "RP":
					pos.add(new Pair<>(word, PREP));
					break;
				case "VB":
				case "VBD":
				case "VBG":
				case "VGN":
				case "VBP":
				case "VBZ":
				case "VBN":
					pos.add(new Pair<>(word, VERB));
					break;
				case "WDT":
				case "WP":
				case "WP$":
				case "WRB":
					pos.add(new Pair<>(word, WH_INTEROG));
					break;
				default: 
					System.err.println("Could not identify: " + part.substring(part.indexOf('/')+1) +
							" for " + word);
					pos.add(new Pair<>(word, EXTRA));
				}
			}
		}
		return pos;
	}

}
