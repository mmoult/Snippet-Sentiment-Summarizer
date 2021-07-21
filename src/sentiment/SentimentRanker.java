package sentiment;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.Scanner;
import java.util.regex.Pattern;

import classifying.Classifier;
import classifying.CountedWords;
import coreference.CoreferenceResolver;
import parsing.DataCleaner;
import util.AsinBridge;
import util.FileResources;

public class SentimentRanker {
	protected SentimentAnalyzer analyzer;
	
	public static void main(String args[]) throws IOException {
		final String QUERY = "folding clothing dryer";
		
		SentimentRanker ranker = new SentimentRanker();
		List<String> goodWords = cleanQuery(QUERY);
		Classifier classifer = new Classifier(false);
		int file = classifer.classify(goodWords);
		System.out.println("Searching in " + FileResources.metaFiles[file] + " for \"" + QUERY + "\".");
		List<Document> result = ranker.rankDocRelevance(goodWords, 100, 784, file);
		//evaluate here how good the document retrieval was
System.out.println("PRODUCTS RETRIEVED (" + result.size()+")=");
//FileWriter temp = new FileWriter("out.txt");
/*for(Document doc: result) {
	//temp.write(doc.toString() + '\n');
	System.out.println(doc);
}*/
//temp.close();

		System.out.println("Retrieving reviews...");
		//Now we want to get the product reviews associated with the ranked documents
		List<String> reviews = new ArrayList<>();
		PorterStemmer stemmer = new PorterStemmer();
		AsinBridge bridge = new AsinBridge(file, stemmer);
		for(Document doc: result) //add all that we get from the bridge
			reviews.addAll(bridge.getReviewsByAsin(doc.getAsin()));
		
		//we also want to rank the reviews by document score
		System.out.println("Ranking reviews");
		List<Document> rankedReviews = ranker.rankDocRelevance(goodWords, 100, reviews);
		
		System.out.println("REVIEWS RETRIEVED (" + rankedReviews.size() + ")=");

/*for(Document doc: rankedReviews) {
	System.out.println(doc);
}*/
		
		//Rank each of the sentences so we can get a review
		System.out.print("Resolving coreferences: 0%  \r");
		int done = 0;
		for(Document doc: rankedReviews) {
			//perform coreference resolution before sentence ranking
			//TODO we could potentially use multi-threading to speed up this part since it is so slow
			doc.setText(CoreferenceResolver.resolve(doc.getText()));
			done++;
			System.out.print("Resolving coreferences: "+ ((done*100)/rankedReviews.size())+"%  \r");
		}
		System.out.println();
		List<Sentence> rankedSentences = new ArrayList<>();
		int sumWords = 0;
		System.out.println("Sorting sentences by relevance");
		boolean posQuery = isQueryPosSentiment(goodWords, ranker.analyzer);
		for(Document doc: rankedReviews) {
			sumWords += doc.getCountedWords().getSize();
			rankedSentences.addAll(doc.identifySignificantSentences(ranker.analyzer, posQuery));
			//System.out.println(doc);
		}
		rankedSentences.sort(Sentence.certaintyComparator);
		//we now have a collection of the most significant sentences in the most relevant documents
/*final int HOW_MANY_SENTENCES = 10;
for(int i=0; i<HOW_MANY_SENTENCES; i++) {
	Sentence s = rankedSentences.get(i);
	System.out.println("["+s.getSignificance() +"] " + s);
}*/
		//We want to build a summary that is about 10% of all the words gotten
		System.out.println("Building summary...");
		StringBuilder summary = new StringBuilder();
		int summarySize = 0;
		for(int i=0; i<rankedSentences.size(); i++) {
			if(summarySize * 10 >= sumWords)
				break; //the summary is long enough
			
			summarySize += rankedSentences.get(i).words.length;
			if(i > 0)
				summary.append(". ");
//append some info about the doc the sentence came from
summary.append("[" + rankedReviews.indexOf(rankedSentences.get(i).getDocument()) + "] ");
			summary.append(rankedSentences.get(i).getText());
		}
		summary.append(".");
		//print the summary
		System.out.println("Summary of reviews for \"" + QUERY + "\":");
System.out.println("Summary=");
		System.out.println(summary.toString());
		
//now we want to print doc information
System.out.println();
System.out.println("Documents=");
for(int i=0; i<rankedReviews.size(); i++) {
	System.out.println("[" + i + "] " + rankedReviews.get(i).getText());
}
	}
	
	private static boolean isQueryPosSentiment(List<String> query, SentimentAnalyzer analyzer) {
		//We need to also stem the query words
		PorterStemmer stemmer = new PorterStemmer();
		for(String term: query) {
			term = stemmer.stem(term);
		}
		
		//now we can go through and analyze sentiment
		double sentiment = 0;
		for(String root: query) {
			List<Sentiment> sentiments = analyzer.querySentiment(root);
			if(sentiments == null)
				continue;
			//otherwise, we want to get the average sentiment for this word
			double sum = 0;
			int numSents = 0;
			for(Sentiment sent: sentiments) {
				if(sent == null)
					continue;
				numSents++;
				
				sum += sent.getScore().getPositive() - sent.getScore().getNegative();
			}
			if(numSents > 0)
				sentiment += sum / numSents;
		}
		
		return sentiment >= 0; //if it by some chance has 0 sentiment, we assume positive
	}

	public SentimentRanker() {
		analyzer = new SentimentAnalyzer(SentimentAnalyzer.path);
		//analyzer.convertToStemmed();
	}
	
	
	private static List<String> cleanQuery(String query) {
		DataCleaner cleaner = new DataCleaner();
		String cleaned = DataCleaner.removePunctuation(query, false);
		return cleaner.filterStopWords(cleaned);
	}
	
	public List<Document> rankDocRelevance(String query, int topHowMany, int toSearch, int file)
			throws FileNotFoundException {
		List<String> goodWords = cleanQuery(query);
		return rankDocRelevance(goodWords, topHowMany, toSearch, file);
	}
	/**
	 * Ranks the documents found at the given file location and returns the relevant documents
	 * sorted in descending relevance order. 
	 * @param query a cleaned list of string terms in the query
	 * @param topHowMany The maximum number of documents that should be returned from this ranking.
	 * If there are not found <code>topHowMany</code> documents that are relevant, the list will be
	 * shorter. In other words, irrelevant documents will not be returned just to meet the specified
	 * number here.
	 * @param toSearch the number of lines to search in the file
	 * @param file the file index in {@link FileResources}.
	 * @return a list of at most <code>topHowMany</code> documents that were found to be relevant
	 * to the query. Sorted by descending relevance.
	 * @throws FileNotFoundException If the metafile read from cannot be found
	 */
	public List<Document> rankDocRelevance(List<String> query, int topHowMany, int toSearch, int file) 
			throws FileNotFoundException {
		List<String> lines = new ArrayList<>(toSearch);
		Scanner scan = new Scanner(new File(FileResources.metaFiles[file]));
		for(int j=0; j<toSearch; j++) {
			if(scan.hasNextLine()) {
				String line = scan.nextLine();
				int wordsStart = line.indexOf("\"words\": ");
				if(wordsStart == -1) {//occurs for empty lines or intro lines
					j--; //don't count this line as an entry added
					continue;
				}
				lines.add(line);
			}else //in case more lines are to be searched than exist
				break;
		}
		scan.close();
		
		return rankDocRelevance(query, topHowMany, lines);
	}
	public List<Document> rankDocRelevance(List<String> query, int topHowMany, List<String> docs) {
		PorterStemmer stemmer = new PorterStemmer();
		
		String[] queryStems = new String[query.size()];
		for(int i=0; i<query.size(); i++) {
			queryStems[i] = stemmer.stem(query.get(i));
		}
		
		//load the metadata file so that we can properly find the correct products
		Document[] documents = new Document[docs.size()];
		
		for(int j=0; j<docs.size(); j++) {
			String line = docs.get(j);
			int wordsStart = line.indexOf("\"words\": ");
			if(wordsStart == -1) {//occurs for empty lines or intro lines
				j--; //don't count this line as an entry added
				continue;
			}
			String asin = DataCleaner.getStringField(line, "asin", false);
			
			String words = line.substring(wordsStart + 9);
			words = words.substring(0, words.length());
			
			documents[j] = new Document(words, stemmer, asin);
		}
		
		//now we need to choose the top ones
		//use tf.idf measure, which is tf * idf
		//(http://tfidf.com/) =
		//TF(t) = (Number of times term t appears in a document) / (Total number of terms in the document)
		//IDF(t) = log_e(Total number of documents / Number of documents with term t in it)
		
		//Dr. Ng uses a slightly different definition, but the idea is the same:
		//TF(t,d) = freq(t,d) / [max for all l in d (freq(l,d)]
		//IDF(t) = log_2(Total number of documents / number of documents with term t in it)
		//And we can compute score as:
		//SCORE(q,d) = Sum for t in q (TF(t,d) * IDF(t))
		
		//as a preliminary measure, find the number of documents that contain each query word
		int[] docsContain = new int[queryStems.length];
		for(int i=0; i<queryStems.length; i++) {
			for(Document doc: documents) {
				if(doc.getCountedWords().getOccurrences(queryStems[i]) > 0)
					docsContain[i]++;
			}
		}
		
		//sort by the score (as defined by a anonymous inner class comparator)
		List<Document> sorted = Arrays.asList(documents);
		sorted.sort(new Comparator<Document>() {
			@Override
			public int compare(Document o1, Document o2) {
				//Reverse the order since we want to sort descending
				return Double.compare(o2.findScore(docsContain, queryStems, documents),
						o1.findScore(docsContain, queryStems, documents));
			}
		});
		
		//great, now we found the most likely products. We want to use this information
		//to get the reviews for the products that we found
		List<Document> relevant = null;
		for(int i=0; i<sorted.size(); i++) {
			if(i >= topHowMany || 
					sorted.get(i).findScore(docsContain, queryStems, documents) <= 0) {
				relevant = sorted.subList(0, i);
				break;
			}
		}
		if(relevant == null)
			relevant = new ArrayList<>();
		return relevant;
	}
	
	public static class Document {
		private Sentence[] sentences;
		private List<String> sigWords;
		private CountedWords forAllDoc;
		private String asin;
		private PorterStemmer stemmer;
		
		public Document(String document, PorterStemmer stemmer, String asin) {
			this.asin = asin;
			this.stemmer = stemmer;
			
			setText(document);
		}
		
		//This is where we actually calculate score
		private double findScore(int[] docsContain, String[] queryStems, Document[] documents) {
			int maxFreq = 0;
			for(String word: getCountedWords().getDistinctWords()) {
				int occurences = getCountedWords().getOccurrences(word);
				if(occurences > maxFreq)
					maxFreq = occurences;
			}
			
			double sum = 0;
			for(int i=0; i<queryStems.length; i++) {
				if(docsContain[i] == 0) //this gives us divide by zero error
					//also doesn't make sense to factor occurrences into the score if no document has the word
					continue;
				
				double tf = getCountedWords().getOccurrences(queryStems[i]) / (double)maxFreq;
				double idf = Math.log(documents.length / (double)docsContain[i])/ Math.log(2);
				sum += tf*idf;
			}
			
			if(Double.isNaN(sum))
				return 0;
			return sum;
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
				for(String word: sentence.words) {
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
			docStemsList.addAll(getCountedWords().getDistinctWords());
			String[] docStems = new String[docStemsList.size()];
			docStems = docStemsList.toArray(docStems);
			
			int[] sentsContain = new int[docStems.length];
			CountedWords[] counted = new CountedWords[sentences.length];
			for(int j=0; j<sentences.length; j++) { //populate counted words for each sentence
				counted[j] = new CountedWords();
				for(String word: sentences[j].words) {
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
			
			// Lastly, compute the certainty factor for each sentence
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
			for(String word: forAllDoc.getDistinctWords()) {
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
		
		public String getAsin() {
			return asin;
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
				for(String word: sentence.words) {
					forAllDoc.addWord(word);
				}
			}
		}
		
		@Override
		public String toString() {
			StringBuilder out = new StringBuilder();
			out.append("Document (" + asin + "): {\n");
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
			return asin.hashCode() + sentences.length;
		}
		
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
			for(String word: countedWords.getDistinctWords()) {
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
		
		public Document getDocument() {
			return source;
		}
		
		@Override
		public String toString() {
			return "Sentence: " + originalText;
		}
	}

}
