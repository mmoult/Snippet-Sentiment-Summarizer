package classifying;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Scanner;
import java.util.Set;

import classifying.PartOfSpeechTagger.PartOfSpeech;
import parsing.DataCleaner;
import sentiment.SentimentAnalyzer;
import util.FileResources;

public class TermTyper {
	private SentimentAnalyzer analyzer;
	private PartOfSpeechTagger tagger;
	
	public static enum TermType {
		SENTIMENT, PRODUCT, FACET, NONE
	};
	
	public static void main(String args[]) throws IOException {
		TermTyper typer = new TermTyper();
		//typer.typeFile("C:\\Users\\moult\\Development\\dataset\\queries.txt",
		//		"C:\\Users\\moult\\Development\\dataset\\typed-queries.txt");
		
		//We are going to get all of the facets for a particular file
		Depluralizer stemmer = new Depluralizer();
		Scanner scan = new Scanner(new File(FileResources.metaFiles[0]));
		CountedWords facets = new CountedWords();
		int lineNo = 0;
		while(scan.hasNextLine()) {
			String line = scan.nextLine();
			lineNo++;
			if(lineNo % 1000 == 0)
				System.out.println("Processing... (" + lineNo + ")");
if(lineNo > 5000)
	break;
			
			String matchOn = "\"words\": ";
			if(!line.contains(matchOn))
				continue;

// Stemming in any form will reduce redundancies, but in the process it will create word stems
// that are not fit for display.
final boolean STEM = false;

			String doc = line.substring(line.indexOf(matchOn) + matchOn.length());
			List<Pair<String, TermType>> words = typer.typeWords(doc);
			for(Pair<String, TermType> term: words) {
				if(term.second == TermType.FACET) {
					//right now, we could still get duplicate words. We should stem them, but
					//still keep track of the original.
					String normTerm = term.first.toLowerCase();
					
					if(STEM) {
						//stem the last word (if any)
						int lastBegin = normTerm.lastIndexOf(' ');
						if(lastBegin == -1) { //no spaces
							normTerm = stemmer.stem(normTerm);
						}else {
							//only stem the last word (since that is where plurals tend to reside)
							String prefix = normTerm.substring(0, lastBegin);
							normTerm = prefix + stemmer.stem(normTerm.substring(lastBegin+1));
						}
					}
					
					facets.addWord(normTerm);
				}
			}
		}
		
		System.out.println("Results: (" + facets.getDistinctSize() + ")");
		//print the words out from most common to least common
		Set<String> words = facets.getDistinctWords();
		List<String> commonFacets = new ArrayList<>();
		commonFacets.addAll(words);
		//sort by their occurrences
		Collections.sort(commonFacets, (String o1, String o2) -> {
			return Integer.compare(-facets.getOccurrences(o1), -facets.getOccurrences(o2));
		});
		
		for(int i=0; i<commonFacets.size(); i++) {
			String word = commonFacets.get(i);
			//skip all that only have one occurrence
			int occ = facets.getOccurrences(word);
			if(occ == 1) {
				System.out.println("... " + (commonFacets.size() - i) + " ommitted single-occurrence facets");
				break;
			}
			System.out.println("  " + word + ": " + occ);
		}
		//System.out.println(facets);
	}
	
	public TermTyper() {
		analyzer = new SentimentAnalyzer(SentimentAnalyzer.path);
		tagger = new StanfordTagger();
	}
	
	public void typeFile(String fileIn, String fileOut) throws IOException {
		Scanner scan = new Scanner(new File(fileIn));
		FileWriter out = new FileWriter(new File(fileOut));
		while(scan.hasNextLine()) {
			String line = scan.nextLine();
			List<Pair<String, TermType>> list = typeWords(line);
			
			for(Pair<String, TermType> pair: list) {
				out.write(pair.first);
				out.write('_');
				out.write(pair.second.toString());
				out.write(' ');
			}
			out.write('\n');
		}
		out.close();
		scan.close();
		System.out.println("Finished!");
	}
	
	protected List<Pair<String, TermType>> typedWords;
	protected StringBuilder facet;
	
	public List<Pair<String, TermType>> typeWords(String sentence) {
		DataCleaner cleaner = new DataCleaner();
		
		//the tagger can get confused when it encounters numbers by themselves. Therefore,
		//we remove them for this
		String[] words = sentence.replace('|', '.').split(" ");
		StringBuilder newSentence = new StringBuilder();
		boolean first = true;
		for(String word: words) {
			try {
				Double.parseDouble(word);
				//if that worked, then we don't include this number "word"
			}catch(NumberFormatException e) {
				//if it failed, then it must have some text component to include
				if(first)
					first = false;
				else
					newSentence.append(' ');
				newSentence.append(word);
			}
		}
		sentence = newSentence.toString();
		words = sentence.split(" ");
		
		List<Pair<String, PartOfSpeech>> posList = tagger.identify(sentence);
		int i = 0;
		
		typedWords = new ArrayList<>();
		
		StringBuilder object = new StringBuilder();
		boolean definitiveArticle = false;
		boolean objectPhrase = false;
		facet = new StringBuilder();
		
		short afterQuantifier = 0; //quantifiers such as much or many come before facets
		for(int k=0; k<words.length; k++) {
			String word = words[k];
			
			//find the matching part of speech from the tagger
			for(; i<posList.size(); i++) {
				//if the tagged word in its entirety starts at index 0 of this word, then they are
				//the same word (the tagger word can be truncated)
				if(word.indexOf(posList.get(i).first) == 0)
					break;
			}
			if(i >= posList.size())
				break; //there was some error
			PartOfSpeech pos = posList.get(i).second;
			if(afterQuantifier > 0)
				afterQuantifier--;
			
			//System.out.println(posList.get(i).first + " " + pos);
			if(pos == PartOfSpeech.ARTICLE) {
				definitiveArticle = word.toLowerCase().equals("the");
				objectPhrase = true;
				addTerm(word, TermType.NONE);
				continue;
			}
			//if this is a verb or a preposition
			//verb: what does the_power steering switch_do
			//preposition: where is a_good place_in Salt Lake City.
			//then the after article object phrase has ended
			if(objectPhrase &&
					(pos == PartOfSpeech.VERB || pos == PartOfSpeech.PREP)) {
				objectPhrase = false;
				//flush if any
				if(object.length() > 0) {
					typedWords.add(new Pair<>(object.toString(), TermType.PRODUCT));
					object = new StringBuilder();
				}
			}
			
			//check for a quantifier word (much, many) that comes before facets
			if(word.toLowerCase().equals("much") || word.toLowerCase().equals("many"))
				afterQuantifier = 2; //2 is at quantifier, 1 is after, 0 is none
			
			//first check that it is not a stop word
			if(cleaner.isStopWord(word.toLowerCase())) {
				addTerm(word, TermType.NONE);
				continue;
			}
			
			//try to find products and facets
			if(objectPhrase) {
				//test if it is an adjective
				if(pos != PartOfSpeech.ADJ_COMPARE && pos != PartOfSpeech.ADJ_SUPER &&
						(definitiveArticle || pos != PartOfSpeech.ADJ)) {
					//If it is not a definitive article and just a regular adjective, then it
					//could be sentiment or facet
					
					//we add this as part of the object phrase
					if(object.length() > 0)
						object.append(' ');
					object.append(word);
					continue;
				}
			}
				
			//if it is not part of an object phrase, it could still be a noun
			if(pos == PartOfSpeech.NOUN || pos == PartOfSpeech.NOUN_PL ||
					pos == PartOfSpeech.NOUN_PROPER || pos == PartOfSpeech.NOUN_PROPER_PL) {
				
				//If the noun comes immediately after a quantifier, we assume that it is a facet.
				//
				
				//If it comes after a quantifier or is a noun after a facet phrase, we guess it
				//is a facet. Or if it is a plural non-proper noun, unless it is the last word
				//in the query (Users commonly make the last word plural since they want several
				//results.)
				//Otherwise, we guess that it is a product
				if(afterQuantifier == 1 || facet.length()>0 || (pos == PartOfSpeech.NOUN_PL &&
						k!=words.length-1)) {
					addTerm(word, TermType.FACET);
					continue;
				}else {
					addTerm(word, TermType.PRODUCT);
					continue;
				}
			}
			
			//if it is an adjective, then either sentiment or facet
			if(pos == PartOfSpeech.ADJ_COMPARE || pos == PartOfSpeech.ADJ_SUPER ||
					pos == PartOfSpeech.ADJ) {
				if(analyzer.querySentiment(word.toLowerCase()) != null)
					addTerm(word, TermType.SENTIMENT);
				else
					addTerm(word, TermType.FACET);
				continue;
			}
			
			//now we just try for sentiment, regardless of part of speech
			if(analyzer.querySentiment(word.toLowerCase()) != null) {
				addTerm(word, TermType.SENTIMENT);
			}else {
				//if we could not place it otherwise, we conclude none
				addTerm(word, TermType.NONE);
			}
		}
		//flush out from any product phrase
		if(object.length() > 0)
			typedWords.add(new Pair<>(object.toString(), TermType.PRODUCT));
		//flush out any facet phrase
		if(facet.toString().length() > 0) { //if a facet is not being pushed, break the facet phrase
			typedWords.add(new Pair<>(facet.toString(), TermType.FACET));
			facet.setLength(0); //clear out the old phrase
		}
		
		return typedWords;
	}
	
	private void addTerm(String word, TermType type) {
		if(type == TermType.FACET) {
			//append it to facet phrase
			if(facet.toString().length() > 0)
				facet.append(' ');
			facet.append(word);
		}else if(facet.toString().length() > 0) { //if a facet is not being pushed, break the facet phrase
			typedWords.add(new Pair<>(facet.toString(), TermType.FACET));
			facet.setLength(0); //clear out the old phrase
		}
		typedWords.add(new Pair<>(word, type));
	}

}
