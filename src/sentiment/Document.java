package sentiment;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.regex.Pattern;

import classifying.CountedWords;
import parsing.DataCleaner;

public class Document {
	private Sentence[] sentences;
	private List<String> sigWords;
	private CountedWords forAllDoc;
	private String id;
	private PorterStemmer stemmer;
	private double score = Double.NaN;
	
	public Document(String document, PorterStemmer stemmer, String id) {
		this.id = id;
		this.stemmer = stemmer;
		
		setText(document);
	}
	
	//This is where we actually calculate score
	/**
	 * 
	 * @param docsContain An array with an index for each query term. The indices should be the number
	 * of documents that contain the query word at that index
	 * @param queryStems the query terms (stemmed)
	 * @param numDocs the number of documents this document is being compared against. Used in conjunction with docsContain
	 * @return a score of this document's relevance
	 */
	public double findScore(int[] docsContain, String[] queryStems, int numDocs) {
		if(!Double.isNaN(score))
			return score;
		
		int maxFreq = 0;
		for(String word: getCountedWords().getDistinct()) {
			int occurences = getCountedWords().getOccurrences(word);
			if(occurences > maxFreq)
				maxFreq = occurences;
		}
		
		double sum = 0;
		for(int i=0; i<queryStems.length; i++) {
			if(docsContain[i] == 0) //this gives us divide by zero error
				//also doesn't make sense to factor occurrences into the score if no document has the word
				continue;
			
			//tf is a relative measure of how often the word occurs compared to how many total words there are
			double tf = getCountedWords().getOccurrences(queryStems[i]) / (double)maxFreq;
			if(tf == 0) //since we multiply tf by idf, we don't have to even calculate idf
				continue;
			//idf is a weighted measure of this document's exclusive use of the term 
			// Typically, we use log[numDocs / docsContain[i]], but if all documents have the term, then we
			// just get 0. This is problematic since it completely disregards tf
			double idf = Math.log(2 * numDocs / (double)docsContain[i])/ Math.log(2);
			sum += tf*idf;
		}
		
		if(Double.isNaN(sum))
			score = 0;
		score = sum;
		
		return score;
	}
	
	public List<Sentence> identifySignificantSentences(SentimentAnalyzer analyzer, boolean posSentiment) {
		identifySignificantWords();
		
		// --Regular significance factor--
		double maxSig = Double.NEGATIVE_INFINITY;
		double minSig = Double.POSITIVE_INFINITY;
		//now determine significance for each of my sentences
		for(Sentence sentence: sentences) {
			int numSigWords = 0;
			for(String sigWord: sigWords) {
				numSigWords += sentence.contains(sigWord);
			}
			double significance = (Math.pow(numSigWords, 2) / (double)sentence.size());
			sentence.setSignificance(significance);
			if(significance < minSig)
				minSig = significance;
			if(significance > maxSig)
				maxSig = significance;
		}
		//normalize significance values
		for(Sentence sentence: sentences)
			sentence.setSignificance((sentence.getSignificance() - minSig) / (maxSig - minSig));
		
		// --Sentiment weight value--
		double docMin = Double.POSITIVE_INFINITY;
		double docMax = Double.NEGATIVE_INFINITY;
		for(Sentence sentence: sentences) {
			double sumSentiment = 0;
			for(String word: sentence.getWords()) {
				List<Sentiment> sentiments = analyzer.querySentiment(word.toLowerCase());
				//we can get several sentiment results back for one word. Right now we won't try to
				//find which one is really true, we will just find the average of all returned
				if(sentiments != null) {
					double value = 0;
					int numSents = 0;
					for(Sentiment sent: sentiments) {
						//a sentiment will only be null if the file is corrupted 
						//(which is true since we trimmed the non-sentiment words).
						if(sent != null) {
							numSents++;
							value += sent.getScore().getPositive() - sent.getScore().getNegative();
						}
					}		
					sumSentiment += (posSentiment? 1:-1) * value / numSents;
				}
			}
			//We don't weight for sentence length since longer sentences are generally
			//more descriptive, which we would like in this instance.
			sentence.sentimentWeight = sumSentiment;
			if(sentence.sentimentWeight > docMax)
				docMax = sentence.sentimentWeight;
			if(sentence.sentimentWeight < docMin)
				docMin = sentence.sentimentWeight;
		}
		//normalize the values
		for(Sentence sentence: sentences) 
			sentence.sentimentWeight = (sentence.sentimentWeight - docMin) / (docMax - docMin);
		
		// --Document similarity by tf.idf--
		//In preparation, we need to find how many sentences contain each word
		ArrayList<String> docStemsList = new ArrayList<>();
		docStemsList.addAll(getCountedWords().getDistinct());
		String[] docStems = new String[docStemsList.size()];
		docStems = docStemsList.toArray(docStems);
		
		int[] sentsContain = new int[docStems.length];
		CountedWords[] counted = new CountedWords[sentences.length];
		for(int j=0; j<sentences.length; j++) { //populate counted words for each sentence
			counted[j] = new CountedWords();
			for(String word: sentences[j].getWords()) {
				counted[j].addWord(word);
			}
		}
		for(int i=0; i<docStems.length; i++) {
			String docStem = docStems[i];
			sentsContain[i] = 0;
			for(int j=0; j<sentences.length; j++) {
				if(counted[j].getOccurrences(docStem) > 0)
					sentsContain[i]++;
			}
		}
		
		double minScore = Double.POSITIVE_INFINITY;
		double maxScore = Double.NEGATIVE_INFINITY;
		for(int i=0; i<sentences.length; i++) {
			Sentence sentence = sentences[i];
			
			sentence.findScore(sentsContain, docStems, sentences.length, counted[i]);
			if(sentence.similarityVal > maxScore)
				maxScore = sentence.similarityVal;
			if(sentence.similarityVal < minScore)
				minScore = sentence.similarityVal;
		}
		//go back through and normalize all
		for(Sentence sentence: sentences)
			sentence.similarityVal = (sentence.similarityVal - minScore) / (maxScore - minScore);
		
		// Lastly, compute the Stanford certainty factor for each sentence
		for(Sentence sentence: sentences) {
			double min = Math.min(Math.min(sentence.significance, sentence.sentimentWeight),
					sentence.similarityVal);
			sentence.certaintyFactor =
					(sentence.significance + sentence.sentimentWeight + sentence.similarityVal) / (1 - min);
		}
		
		//now sort all the sentences and return the result
		Sentence[] ranked = sentences.clone();
		List<Sentence> rankedList = Arrays.asList(ranked);
		rankedList.sort(Sentence.sigComparator); //Sentence.certaintyComparator
		return rankedList;
	}
	
	private void identifySignificantWords() {
		//now we can go through each term in the doc and identify if it is significant
		for(String word: forAllDoc.getDistinct()) {
			boolean significant = false;
			if(sentences.length < 25) {
				significant = forAllDoc.getOccurrences(word) >= 7 - 0.1*(25 - sentences.length);
			}else if(sentences.length <= 40) {
				significant = forAllDoc.getOccurrences(word) >= 7;
			}else {
				significant = forAllDoc.getOccurrences(word) >= 7 + 0.1*(40 - sentences.length);
			}
			if(significant) {
				sigWords.add(word);
			}
		}
	}
	
	public int numOfWords() {
		int sum = 0;
		for(Sentence sentence: sentences) {
			sum += sentence.size();
		}
		return sum;
	}
	
	public int size() {
		return sentences.length;
	}
	
	public Sentence[] getSentences() {
		return sentences;
	}
	
	public List<String> getSignificantWords() {
		return sigWords;
	}
	public CountedWords getCountedWords() {
		return forAllDoc;
	}
	
	public String getId() {
		return id;
	}
	
	public String getText() {
		StringBuilder toReturn = new StringBuilder();
		boolean first = true;
		for(Sentence sentence: sentences) {
			if(!first)
				toReturn.append(" | ");
			else
				first = false;
			toReturn.append(sentence.originalText);
		}
		return toReturn.toString();
	}
	
	public void setText(String newText) {
		String[] sents = newText.split(Pattern.quote("|"));
		//If the last "sentence" is blank, then it just means that the author
		//used good grammar and ended with a period
		int size = sents.length;
		if(sents[size-1].trim().isEmpty())
			size--;
		
		sentences = new Sentence[size];
		for(int i=0; i<size; i++) {
			sentences[i] = new Sentence(this, sents[i], stemmer);
		}
		
		sigWords = new ArrayList<>();
		forAllDoc = new CountedWords();
		for(Sentence sentence: sentences) {
			for(String word: sentence.getWords()) {
				forAllDoc.addWord(word);
			}
		}
	}
	
	@Override
	public String toString() {
		StringBuilder out = new StringBuilder();
		out.append("Document (" + id + "): {\n");
		for(Sentence sentence: sentences) {
			out.append("  ");
			out.append(sentence);
			out.append('\n');
		}
		out.append("}");
		return out.toString();
	}

	@Override
	public int hashCode() {
		return id.hashCode() + sentences.length;
	}
	
	public static class Sentence {
		private String originalText;
		private String[] words;
		private Document source;
		
		private double significance;
		private double sentimentWeight;
		private double similarityVal;
		private double certaintyFactor;
		
		public static Comparator<Sentence> certaintyComparator = new Comparator<Sentence>() {
			@Override
			public int compare(Sentence o1, Sentence o2) {
				//compare by ascending significance
				//NaN should always be at the end of the list though
				if(Double.isNaN(o1.certaintyFactor)) {
					if(Double.isNaN(o2.certaintyFactor))
						return 0;
					else
						return 1;
				}
				if(Double.isNaN(o2.certaintyFactor))
					return -1;
				
				return Double.compare(o2.certaintyFactor, o1.certaintyFactor);
			}
		};
		public static Comparator<Sentence> sigComparator = new Comparator<Sentence>() {
			@Override
			public int compare(Sentence o1, Sentence o2) {
				//compare by ascending significance
				//NaN should always be at the end of the list though
				if(Double.isNaN(o1.significance)) {
					if(Double.isNaN(o2.significance))
						return 0;
					else
						return 1;
				}
				if(Double.isNaN(o2.significance))
					return -1;
				
				return Double.compare(o2.significance, o1.significance);
			}
		};
		
		
		public Sentence(Document source, String sentence, PorterStemmer stemmer) {
			this.source = source;
			originalText = sentence.trim();
			//split for each word
			String condensed = DataCleaner.condenseSpaces(sentence.toLowerCase().trim());
			String[] raw = condensed.split(" ");
			
			List<String> toWords = new ArrayList<>();
			for(int i=0; i < raw.length; i++) {
				if(raw[i].isEmpty())
					continue;
				raw[i] = (stemmer!=null)? stemmer.stem(raw[i]) : raw[i];
				if(!raw[i].isEmpty())
					toWords.add(raw[i]);
			}
			this.words = new String[toWords.size()];
			words = toWords.toArray(words);
			
			significance = 0;
		}
		
		public void setSignificance(double significance) {
			this.significance = significance;
		}
		public double getSignificance() {
			return significance;
		}
		
		
		private void findScore(int[] sentsContain, String[] docStems, int numSentences, CountedWords countedWords) {
			if(countedWords == null) {
				countedWords = new CountedWords();
				//populate counted words
				for(String word: words)
					countedWords.addWord(word);				
			} //otherwise, we assume that it was populated for us
			
			int maxFreq = 0;
			for(String word: countedWords.getDistinct()) {
				int occurences = countedWords.getOccurrences(word);
				if(occurences > maxFreq)
					maxFreq = occurences;
			}
			
			double sum = 0;
			for(int i=0; i<docStems.length; i++) {
				if(sentsContain[i] == 0) //this gives us divide by zero error
					continue; //should not occur, since we only review words in the document
				
				double tf = countedWords.getOccurrences(docStems[i]) / (double)maxFreq;
				double idf = Math.log(numSentences / (double)sentsContain[i])/ Math.log(2);
				sum += tf*idf;
			}
			
			if(Double.isNaN(sum))
				this.similarityVal = 0;
			this.similarityVal = sum;
		}
		
		public int contains(String word) {
			int matches = 0;
			for(String match: words) {
				if(match.equals(word))
					matches++;
			}
			return matches;
		}
		
		public int size() {
			return words.length;
		}
		
		public String getText() {
			return originalText;
		}
		
		public String[] getWords() {
			return words;
		}
		
		public Document getDocument() {
			return source;
		}
		
		@Override
		public String toString() {
			return "Sentence: " + originalText;
		}
	}
	
}
