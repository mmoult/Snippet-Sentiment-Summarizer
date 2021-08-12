package sentiment;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Scanner;
import java.util.Set;
import java.util.Stack;

import classifying.Classifier;
import coreference.CoreferenceResolver;
import parsing.DataCleaner;
import sentiment.Document.Sentence;
import util.AmazonFileResources;
import util.OpinRankFiles;
import util.PythonBridge;

public class SentimentRanker {
	protected SentimentAnalyzer analyzer;
	
	public static void main(String args[]) throws IOException {
		final String QUERY = "honda 2007 fast car luxury";
		
		SentimentRanker ranker = new SentimentRanker();
		List<String> goodWords = cleanQuery(QUERY);
		
		// Here we would classify to find whether car or hotel is being described by the query
		char sep = File.separatorChar;
		String searchPath = OpinRankFiles.root + sep;
		// For the sake of time, we will blacklist some folders.
		List<String> blackList = Arrays.asList("2007", "2009", "beijing", "dubai", "london", "montreal", "new-delhi", "shanghai");
		System.out.println("Determining type of query...");
		Classifier classifier = new Classifier(false, searchPath, blackList);
		String path = classifier.classify(goodWords);
		
		//Here we would try to choose which car is to be processed. We will find all all
		// files in the directory that was classified (car or hotel). Then we will match
		// the query to the file names. Since file names will likely be insufficient to
		// limit to one product, we will perform tf.idf relevance on those that remain
		List<File> relevants = new ArrayList<>();
		Stack<File> toSearch = new Stack<>();
		toSearch.add(new File(path));
		
		//We will keep track of all words matched in the file directory. We don't want
		// them to be used again in the tf.idf measure, and the words in the file
		// directory tend to be product terms that shouldn't be used, regardless.
		Set<String> usedWords = new HashSet<>();
		
		while(!toSearch.isEmpty()) {
			File analyze = toSearch.pop();
//If I want to blacklist files in the product search...
//if(blackList.contains(analyze.getName()))
//	continue; //skip names matching the blacklist
			
			if(analyze.isFile() && analyze.getName().endsWith(".txt"))
				relevants.add(analyze);
			else if(analyze.isDirectory()) {
				File[] subs = analyze.listFiles();
				List<File> mostRelevant = new ArrayList<>();
				int relevantMax = 0; //if none are found to be "relevant", add all
				//However, if one is found to be relevant, ignore all that aren't
				for(File sub: subs) {
					if(sub.isFile() && !sub.getName().endsWith(".txt"))
						continue;
					
					int relevant = 0;
					String[] words = sub.getName().split("[-_.\\s]");
					for(String word: words) {
						if(containsIgnoreCase(goodWords, word)) {
							relevant++;
							usedWords.add(word);
						}
					}
					if(relevant >= relevantMax) {
						if(relevant > relevantMax)
							mostRelevant.clear();
						mostRelevant.add(sub);
						relevantMax = relevant;
					}
				}
				
				for(File sub: mostRelevant)
					toSearch.push(sub);
			}
		}
		
		//Now we have a list of potential matches called "relevants".
		//But we only want to have one product to search reviews for.
		System.out.println("Selecting best product...");
		if(relevants.size() < 1) {
			System.err.println("No document matched the query!");
			return;
		}else {
			//We want to remove all terms from the query that were used in the file directory.
			//However, just to be safe, we only want to remove words from the query when
			// processing files that actually used said terms.
			List<String> termsRemaining = new ArrayList<>();
			termsRemaining.addAll(goodWords);
			for(String word: usedWords)
				termsRemaining.remove(word);
			goodWords = termsRemaining; //we don't need the original good words anymore
			
			if(relevants.size() == 1)
				path = relevants.get(0).getPath();
			else {
				//We must do some pruning with the content of the files
				if(goodWords.isEmpty()) {
					path = relevants.get(0).getPath(); //just choose the first since we have nothing
				}else {
long startMs = System.currentTimeMillis();
					/* Rank by tf.idf
					List<Document> files = ranker.rankFileRelevance(termsRemaining, 1, -1, relevants);
					path = files.get(0).getId();
					*/
					
					//We are going to try using Word Vectors with Python's spaCy to solve this.
					StringBuilder fileOptions = new StringBuilder(relevants.get(0).getAbsolutePath());
					for(int i=1; i<relevants.size(); i++) {
						fileOptions.append(';');
						fileOptions.append(relevants.get(i).getAbsolutePath());
					}
					StringBuilder goodQuery = new StringBuilder();
					for(int i=0; i<goodWords.size(); i++) {
						if(i > 0)
							goodQuery.append(' ');
						goodQuery.append(goodWords.get(i));
					}
					//now load the Python script
					path = PythonBridge.launch("classifying\\FileRanker.py", goodQuery.toString(), fileOptions.toString());
					path = path.replace("\n", "");
					//
	long endMs = System.currentTimeMillis();
	System.out.println("Elapsed time: " + (endMs - startMs));
				}
			}
		}

		//Now that we have the query-relevant product, we find the most relevant reviews
		System.out.println("Searching in " + path + " for \"" + QUERY + "\".");
		//We use the modified good words with product terms removed
		List<Document> rankedReviews = ranker.rankDocRelevance(goodWords, 100, -1, path);
/*evaluate here how good the document retrieval was
System.out.println("PRODUCTS RETRIEVED (" + result.size()+")=");
//FileWriter temp = new FileWriter("out.txt");
int iii=0;
for(Document doc: result) {
	//temp.write(doc.toString() + '\n');
	System.out.println("[" + (++iii) + "] " + doc.getText());
}
System.exit(0);
//temp.close();
*/

		//System.out.println("Retrieving reviews...");
		/*Now we want to get the product reviews associated with the ranked documents
		List<String> reviews = new ArrayList<>();
		PorterStemmer stemmer = new PorterStemmer();
		AsinBridge bridge = new AsinBridge(file, stemmer);
		for(Document doc: result) //add all that we get from the bridge
			reviews.addAll(bridge.getReviewsByAsin(doc.getAsin()));
		
		//we also want to rank the reviews by document score
		 */
		System.out.println("Ranking reviews");
		//List<Document> rankedReviews = ranker.rankDocRelevance(goodWords, 100, -1, reviews);
		
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
			try {
				doc.setText(CoreferenceResolver.resolve(doc.getText()));
			}catch(Exception e) {
				//if we had some issue, we can notify the user, but we don't *have* to resolve
				// the references
				System.err.println("Failed to resolve coreferences of document with text: \"" + doc.getText() + "\".");
			}
			
			done++;
			System.out.print("Resolving coreferences: "+ ((done*100)/rankedReviews.size())+"%  \r");
		}

int iii=0;
for(Document doc: rankedReviews) {
	//temp.write(doc.toString() + '\n');
	System.out.println("[" + (++iii) + "] " + doc.getText());
}
System.exit(0); //END

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
System.exit(0);
		System.out.println("Building summary...");
		StringBuilder summary = new StringBuilder();
		int summarySize = 0;
		for(int i=0; i<rankedSentences.size(); i++) {
			if(summarySize * 10 >= sumWords)
				break; //the summary is long enough
			
			summarySize += rankedSentences.get(i).getWords().length;
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
	
	private static boolean containsIgnoreCase(List<String> ls, String test) {
		for(String it: ls) {
			if(it.equalsIgnoreCase(test))
				return true;
		}
		return false;
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
		return cleaner.filterStopWords(cleaned, false);
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
	 * @param file the file index in {@link AmazonFileResources}.
	 * @return a list of at most <code>topHowMany</code> documents that were found to be relevant
	 * to the query. Sorted by descending relevance.
	 * @throws FileNotFoundException If the file to read from cannot be found
	 */
	public List<Document> rankDocRelevance(List<String> query, int topHowMany, int toSearch, /*int file*/ String file) 
			throws FileNotFoundException {
		
		PorterStemmer stemmer = new PorterStemmer();
		List<Document> documents = new ArrayList<>(toSearch!=-1 ? toSearch: topHowMany);
		//Scanner scan = new Scanner(new File(AmazonFileResources.metaFiles[file]));
		Scanner scan = new Scanner(new File(file));
		for(int j=0; j<toSearch || toSearch==-1; j++) {
			if(scan.hasNextLine()) {
				String line = scan.nextLine();
				int wordsStart = line.indexOf("\"words\": ");
				if(wordsStart == -1) {//occurs for empty lines or intro lines
					j--; //don't count this line as an entry added
					continue;
				}
				String asin = DataCleaner.getStringField(line, "asin", false);
				
				String words = line.substring(wordsStart + 9);
				if(words.isBlank())
					continue; //don't add empty documents
				
				documents.add(new Document(words, stemmer, asin));
			}else //in case more lines are to be searched than exist
				break;
		}
		scan.close();
		
		return rankDocRelevance(query, topHowMany, documents, stemmer);
	}
	public List<Document> rankFileRelevance(List<String> query, int topHowMany, int toSearch, List<File> files) 
			throws FileNotFoundException {
		PorterStemmer stemmer = new PorterStemmer();
		List<Document> documents = new ArrayList<>(toSearch!=-1 ? toSearch: topHowMany);
		
		for(File file: files) {
			Scanner scan = new Scanner(file);
			
			StringBuilder allWords = new StringBuilder();
			for(int j=0; j<toSearch || toSearch==-1; j++) {
				if(scan.hasNextLine()) {
					String line = scan.nextLine();
					int wordsStart = line.indexOf("\"words\": ");
					if(wordsStart == -1) {//occurs for empty lines or intro lines
						j--; //don't count this line as an entry added
						continue;
					}
					
					String words = line.substring(wordsStart + 9);
					words = words.substring(0, words.length());
					allWords.append(words);
				}else //in case more lines are to be searched than exist
					break;
			}
			scan.close();
			documents.add(new Document(allWords.toString(), stemmer, file.getPath()));
		}
		
		return rankDocRelevance(query, topHowMany, documents, stemmer);
	}
	
	public List<Document> rankDocRelevance(List<String> query, int topHowMany, List<Document> documents, PorterStemmer stemmer) {		
		String[] queryStems = new String[query.size()];
		for(int i=0; i<query.size(); i++) {
			String stemmed = stemmer.stem(query.get(i));
			if(stemmed.isEmpty())
				queryStems[i] = query.get(i);
			else
				queryStems[i] = stemmed;
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
		
		//as a preliminary measure for tf.idf, find the number of documents that contain each query word
		int[] docsContain = new int[queryStems.length];
		for(int i=0; i<queryStems.length; i++) {
			for(Document doc: documents) {
				if(doc.getCountedWords().getOccurrences(queryStems[i]) > 0)
					docsContain[i]++;
			}
		}
		
		//sort by the score (as defined by a anonymous inner class comparator)
		int numDocs = documents.size();
		documents.sort(new Comparator<Document>() {
			@Override
			public int compare(Document o1, Document o2) {
				//Reverse the order since we want to sort descending
				return Double.compare(o2.findScore(docsContain, queryStems, numDocs),
						o1.findScore(docsContain, queryStems, numDocs));
			}
		});
		
		//great, now we found the most likely documents.
		List<Document> relevant = null;
		for(int i=0; i<documents.size(); i++) {
			if(i >= topHowMany || 
					documents.get(i).findScore(docsContain, queryStems, numDocs) <= 0) {
				relevant = documents.subList(0, i);
				break;
			}
		}
		if(relevant == null)
			relevant = new ArrayList<>();
		return relevant;
	}

}
