package sentiment;

import java.io.File;
import java.io.FileNotFoundException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Scanner;
import java.util.TreeMap;

import util.AmazonFileResources;

public class SentimentAnalyzer {
	public static String path = AmazonFileResources.fileRoot + "trimmed-senti-list.txt";
	protected Map<String, List<Sentiment>> sentimentBank;
	
	
	public static void main(String args[]) {
		PorterStemmer stemmer = new PorterStemmer();
		
		SentimentAnalyzer analyzer = new SentimentAnalyzer(SentimentAnalyzer.path);
		System.out.println(analyzer.size());
		analyzer.convertToStemmed();
		System.out.println(analyzer.size());
		
		String toTest = "associate";
		String stemmed = stemmer.stem(toTest);
		System.out.println(stemmed + "(" + toTest + "): + . -");
		List<Sentiment> result = analyzer.querySentiment(stemmed);
		for(Sentiment sent: result) {
			if(sent == null) {
				System.out.println("--");
				continue;
			}
			System.out.print(Arrays.toString(sent.getSynonyms()) + ": ");
			SentiScore score = sent.getScore();
			System.out.println(score.getPositive()+" "+score.getObjective()+" "+ score.getNegative());
		}
	}
	
	public SentimentAnalyzer(String filePath) {
		sentimentBank = new HashMap<>();
		
		Scanner scan = null;
		try {
			scan = new Scanner(new File(path));
			while(scan.hasNextLine()) {
				String line = scan.nextLine();
				
				if(line.length() < 5 || line.charAt(0) == '#')
					continue; //comment that we ignore
				
				//the file is well-defined as:
				//POS \t ID \t score+ \t score- \t terms \t definition
				String[] sections = line.split("\t");
				char pos = sections[0].charAt(0);
				int id = Integer.parseInt(sections[1]);
				SentiScore score = new SentiScore(Double.parseDouble(sections[2]), 
												  Double.parseDouble(sections[3]));
				String[] terms = sections[4].split(" ");
				String[] words = new String[terms.length];
				for(int i=0; i<terms.length; i++)
					words[i] = terms[i].substring(0, terms[i].indexOf('#'));
				
				Sentiment sentiment = new Sentiment(pos, words, id, score, sections[5]);
				for(int i=0; i<terms.length; i++) {
					insertAtRank(words[i], sentiment, 
							Integer.parseInt(terms[i].substring(terms[i].indexOf('#')+1)) - 1);
				}
			}
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} finally {
			if(scan != null)
				scan.close();
		}
	}
	
	protected void insertAtRank(String word, Sentiment sentiment, int rank) {
		if(!sentimentBank.containsKey(word)) {
			ArrayList<Sentiment> vals = new ArrayList<>(rank+1);
			if(vals.size() < rank) {
				Sentiment[] nulls = new Sentiment[rank - vals.size()];
				Arrays.fill(nulls, null);
				vals.addAll(Arrays.asList(nulls));
			}
			vals.add(sentiment);
			sentimentBank.put(word, vals);
		}else {
			//try to add at the given rank
			List<Sentiment> vals = sentimentBank.get(word);
			if(vals.size() <= rank) {
				Sentiment[] nulls = new Sentiment[rank+1 - vals.size()];
				Arrays.fill(nulls, null);
				vals.addAll(Arrays.asList(nulls));
			}
			vals.set(rank, sentiment);
		}
	}
	
	public List<Sentiment> querySentiment(String word) {
		return sentimentBank.get(word);
	}
	
	public void convertToStemmed() {
		PorterStemmer stemmer = new PorterStemmer();
		
		Map<String, List<Sentiment>> newBank = new TreeMap<>();
		for(String key: sentimentBank.keySet()) {
			String stemmed = stemmer.stem(key);
			if(newBank.containsKey(stemmed)) {
				newBank.get(stemmed).addAll(sentimentBank.get(key));
			}else {
				newBank.put(stemmer.stem(key), sentimentBank.get(key));
			}
		}
		sentimentBank = newBank;
	}
	
	public int size() {
		return sentimentBank.size();
	}
	
}
