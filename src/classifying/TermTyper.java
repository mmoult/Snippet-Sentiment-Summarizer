package classifying;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Scanner;
import java.util.Set;

import classifying.PartOfSpeechTagger.PartOfSpeech;
import parsing.DataCleaner;
import sentiment.SentimentAnalyzer;
import util.OpinRankFiles;

/**
 * A class to type words in a document. Each word is sorted into one of four types:
 * sentiment, product, facet, or none. This is the TermType enumeration. Use the
 * provided methods of {@link #typeReviewWords(String)} or {@link #extractReviewFacets(String)}.
 */
public class TermTyper {
	private SentimentAnalyzer analyzer;
	private PartOfSpeechTagger tagger;
	
	public static enum TermType {
		SENTIMENT, PRODUCT, FACET, NONE
	};
	
	public static void main(String args[]) throws IOException {
		TermTyper typer = new TermTyper();
		
		//We are going to get all of the facets for a particular file
		Depluralizer stemmer = new Depluralizer();
// Stemming in any form will reduce redundancies, but in the process it will create word stems
// that are not fit for display.
final boolean STEM = true;
		Scanner scan = new Scanner(new File(OpinRankFiles.carFile+"2007" +File.separator+ "chevrolet_impala.txt"));
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


			String doc = line.substring(line.indexOf(matchOn) + matchOn.length());
			List<Pair<String, TermType>> words = typer.typeReviewWords(doc);
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
							normTerm = prefix + " " + stemmer.stem(normTerm.substring(lastBegin+1));
						}
					}
					
					facets.addWord(normTerm);
				}
			}
		}
		
		System.out.println("Results: (" + facets.getDistinctSize() + ")");
		//print the words out from most common to least common
		List<String> banned = Arrays.asList("car", "people", "vehicle");
		if(STEM) {
			for(int j=0; j<banned.size(); j++)
				banned.set(j, stemmer.stem(banned.get(j)));
		}
		printResults(facets, false, banned);
	}
	
	private static void printResults(CountedWords facets, boolean printAll, List<String> banned) {
		Set<String> words = facets.getDistinct();
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
			if(!printAll && occ == 1) {
				System.out.println("... " + (commonFacets.size() - i) + " omitted single-occurrence facets");
				break;
			}
			
			//also we want to filter out a couple of subject-specific banned words
			if(banned.contains(word.toLowerCase()))
				continue;
			System.out.println("  " + word + ": " + occ);
		}
		//System.out.println(facets);
	}
	
	@SuppressWarnings("unused") //this is still in experimental development
	private static void printGroupedResults(CountedWords facets, Depluralizer stemmer, boolean STEM, boolean printAll, List<String> banned) {
		FacetGroups groups = new TermTyper.FacetGroups();
		
		for(String term: facets.getDistinct()) {
			if(banned.contains(term))
				continue;
			groups.addFacet(term);
		}
		List<FacetGroups.Group> sorted = new ArrayList<>(groups.getGroupNames().size());
		Collections.sort(sorted, (FacetGroups.Group o1, FacetGroups.Group o2) -> {
			return Integer.compare(-o1.occurrences, -o2.occurrences); //sort descending
		});
		
		for(FacetGroups.Group group: sorted) {
			//TODO here
			System.out.println(/*group.name +*/ "(" + group.occurrences + ") {");
			for(String facet: group.facets)
				System.out.println("    " + facet);
			System.out.println("}");
		}
	}
	
	public TermTyper() {
		analyzer = new SentimentAnalyzer(SentimentAnalyzer.path);
		tagger = new StanfordTagger();
	}
	
	/**
	 * Extracts the facet words from the doc using {@link #typeReviewWords(String)}.
	 * @param doc the text in the document to be analyzed.
	 * @return the facet words and number of occurrences for each.
	 */
	public CountedWords extractReviewFacets(String doc) {
		List<Pair<String, TermType>> words = typeReviewWords(doc);
		CountedWords facets = new CountedWords();
		Depluralizer stemmer = new Depluralizer();
		for(Pair<String, TermType> term: words) {
			if(term.second == TermType.FACET) {
				//right now, we could still get duplicate words. We should stem them, but
				//still keep track of the original.
				String normTerm = term.first.toLowerCase();
				
				if(true) { //always stem
					//stem the last word (if any)
					int lastBegin = normTerm.lastIndexOf(' ');
					if(lastBegin == -1) { //no spaces
						normTerm = stemmer.stem(normTerm);
					}else {
						//only stem the last word (since that is where plurals tend to reside)
						String prefix = normTerm.substring(0, lastBegin);
						normTerm = prefix + " " + stemmer.stem(normTerm.substring(lastBegin+1));
					}
				}
				
				facets.addWord(normTerm);
			}
		}
		return facets;
	}
	
	public List<Pair<String, TermType>> typeReviewWords(String sentence) {
		//This method is highly tuned for product reviews. Since this should be the description
		// text, we should only rarely encounter product words. Almost all nouns will be sorted
		// as facets instead.
		
		DataCleaner cleaner = new DataCleaner();
		
		//the tagger can get confused when it encounters numbers by themselves. Therefore,
		//we remove them for this
		String[] words = sentence.replace(" | ", ". ").split(" ");
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
		
		List<Pair<String, PartOfSpeech>> posList = tagger.identify(sentence);
		int i = 0;
		
		typedWords = new ArrayList<>();
		
		StringBuilder object = new StringBuilder();
		boolean definitiveArticle = false;
		boolean objectPhrase = false;
		TermType phraseType = TermType.FACET;
		facet = new StringBuilder();
		
		short afterQuantifier = 0; //quantifiers such as much or many come before facets
		for(int k=0; k<words.length; k++) {
			String word = words[k];
			
			//find the matching part of speech from the tagger
			int ii=i;
			for(; ii<posList.size(); ii++) {
				//if the tagged word in its entirety starts at index 0 of this word, then they are
				//the same word (the tagger word can be truncated)
				if(word.indexOf(posList.get(ii).first) == 0)
					break;
			}
			if(ii >= posList.size())
				continue; //there was some error. Happens on numbers, so skip
			i = ii;
			
			if(word.charAt(word.length()-1) == '.')
				word = word.substring(0, word.length()-1);
			PartOfSpeech pos = posList.get(i).second;
			if(afterQuantifier > 0)
				afterQuantifier--;
			
			//System.out.println(posList.get(i).first + " " + pos);
			if(pos == PartOfSpeech.ARTICLE || pos == PartOfSpeech.PRON_POS) {
				if(objectPhrase) {
					//if we are already in a phrase, break and push
					objectPhrase = false;
					//flush if any
					if(object.length() > 0) {
						typedWords.add(new Pair<>(object.toString(), phraseType));
						object = new StringBuilder();
					}
				}
				definitiveArticle = word.toLowerCase().equals("the");
				objectPhrase = true;
				//if the phrase begins with 'this', then it is talking about the product
				phraseType = TermType.FACET;
				if(pos == PartOfSpeech.PRON_POS)
					phraseType = TermType.NONE; //probably some other personal thing
				if(word.toLowerCase().equals("this"))
					phraseType = TermType.PRODUCT;
				addTerm(word, TermType.NONE);
				continue;
			}
			//if this is a verb or a preposition
			// verb: what does the {power steering switch} do
			// preposition: where is a {good place} in Salt Lake City.
			// conjunction: one ride in the {fast mercedes} and we were hooked 
			//then the after article object phrase has ended
			if(objectPhrase &&
					(pos == PartOfSpeech.VERB || pos == PartOfSpeech.PREP || pos == PartOfSpeech.CONJ)) {
				objectPhrase = false;
				//flush if any
				if(object.length() > 0) {
					typedWords.add(new Pair<>(object.toString(), phraseType));
					object = new StringBuilder();
				}
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
			
			//first check that it is not a stop word
			if(cleaner.isStopWord(word.toLowerCase())) {
				addTerm(word, TermType.NONE);
				continue;
			}
				
			//if it is not part of an object phrase, it could still be a noun
			//All nouns are considered facets EXCEPT for capitalized non-acronyms.
			if(pos == PartOfSpeech.NOUN || pos == PartOfSpeech.NOUN_PL ||
					pos == PartOfSpeech.NOUN_PROPER || pos == PartOfSpeech.NOUN_PROPER_PL) {
				
				if(pos == PartOfSpeech.NOUN_PROPER || pos == PartOfSpeech.NOUN_PROPER_PL
						&& !isAcronym(word)) {
					addTerm(word, TermType.PRODUCT);
				}else {
					addTerm(word, TermType.FACET);
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
			typedWords.add(new Pair<>(object.toString(), phraseType));
		//flush out any facet phrase
		if(facet.toString().length() > 0) { //if a facet is not being pushed, break the facet phrase
			typedWords.add(new Pair<>(facet.toString(), TermType.FACET));
			facet.setLength(0); //clear out the old phrase
		}
		
		return typedWords;
	}
	
	protected boolean isAcronym(String word) {
		//the way that we will guess if it is an acronym is if more than 50% of the letters are caps
		//Remember that come acronyms have lower-case components.
		int cap = 0;
		for(char c: word.toCharArray()) {
			if((Character.isLetter(c) && Character.isUpperCase(c)) || !Character.isLetter(c))
				cap++;
		}
		return (cap / (double)word.length()) > 0.5;
	}
	
	protected List<Pair<String, TermType>> typedWords;
	protected StringBuilder facet;
	/**This is a generic method for typing any kind of sentence. This was used for the
	 * Amazon dataset. Do NOT use for the OpinRank dataset. Use {@link #typeReviewWords(String)}
	 * or {@link #extractReviewFacets(String)} instead.
	 */
	public List<Pair<String, TermType>> typeWords(String sentence) {
		DataCleaner cleaner = new DataCleaner();
		
		//the tagger can get confused when it encounters numbers by themselves. Therefore,
		//we remove them for this
		String[] words = sentence.replace(" | ", " ").split(" ");
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
	
	protected static class FacetGroups {
		protected HashMap<String, Group> map;
		
		public void addFacet(String facet) {
			//We try to match this facet to all current groups
			//We break apart all the component words in the facet
			String[] words = facet.split(" ");
			//now we want to depluralize each term
			Depluralizer stemmer = new Depluralizer();
			for(int i=0; i<words.length; i++) {
				words[i] = stemmer.stem(words[i]);
			}
			
			for(String group: map.keySet()) {
				//now we try to match for each facet in the group
				//TODO I need to fix this so I match for single words (or maybe more)
				//and create groups with group names that represent what is in common
				Group groupPair = map.get(group);
				for(String groupFacet: groupPair.facets) {
					for(String word: words) {
						if(groupFacet.contains(word)) {
							// We found a match!
							groupPair.facets.add(facet);
							groupPair.occurrences++;
							return;
						}
					}
				}
			}
			
			//If we made it here, the facet fits no groups.
			//We create a new group for it, and the group takes its name. Therefore,
			// the group name is not necessarily representative of the group identity
			map.put(facet, new Group(facet));
		}
		
		public Set<String> getGroupNames() {
			return map.keySet();
		}
		
		public Group getGroup(String groupName) {
			return map.get(groupName);
		}
		
		public class Group {
			int occurrences = 0;
			Set<String> facets = new HashSet<>();
			
			public Group() {}
			public Group(String facet) {
				facets.add(facet);
			}
		}
	}

}
