package util;

import java.io.File;
import java.io.FileNotFoundException;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Scanner;

import parsing.DataCleaner;
import sentiment.PorterStemmer;

public class AsinBridge {
	private Map<String, Collection<String>> documents;
	
	public AsinBridge(int file, PorterStemmer stem) throws FileNotFoundException {
		//load the documents and save them according to asin in a set
		Scanner scan = new Scanner(new File(AmazonFileResources.reviewFiles[file]));
		documents = new HashMap<>();
		while(scan.hasNextLine()) {
			String line = scan.nextLine();
			String asin = DataCleaner.getStringField(line, "asin", false);
			if(asin == null)
				continue;
			
			String findToken = "\"words\":";
			String words = line.substring(line.indexOf(findToken) + findToken.length());
			
			put("\"asin\": \"" + asin + "\", \"words\": " + words, asin);
		}
		scan.close();
	}
	
	private void put(String doc, String asin) {
		if(!documents.containsKey(asin)) {
			Collection<String> set = new HashSet<>();
			set.add(doc);
			documents.put(asin, set);
		}else {
			documents.get(asin).add(doc);
		}
	}
	
	public Collection<String> getReviewsByAsin(String asin) {
		if(!documents.containsKey(asin))
			return new HashSet<>();
		else
			return documents.get(asin);
	}

}
