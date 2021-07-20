package classifying;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.HashSet;
import java.util.List;
import java.util.Scanner;
import java.util.Set;

import classifying.PartOfSpeechTagger.PartOfSpeech;
import parsing.DataCleaner;
import sentiment.PorterStemmer;
import sentiment.Sentiment;
import sentiment.SentimentAnalyzer;

@Deprecated
public class SvmDataBuilder {
	public static String inPath = "C:\\Users\\moult\\Development\\dataset\\svm-data-raw.txt";
	public static String trainPath = "C:\\Users\\moult\\Development\\dataset\\svm-training.txt";
	public static String outPath = "C:\\Users\\moult\\Development\\dataset\\svm-testing.txt";
	
	private SentimentAnalyzer analyzer;
	private PorterStemmer stem;
	
	
	public static void main(String args[]) throws IOException {
		SvmDataBuilder builder = new SvmDataBuilder(new PorterStemmer());
		//we are building the training set, which means the output format needs to be slightly
		//different. It will also output the word, which we can use to manually go in and assign
		//a class. This is because I couldn't find a dataset to use for this
		
		//builder.build8VectorTrain(inPath);
		builder.build8VectorOut(inPath);
	}
	
	public SvmDataBuilder(PorterStemmer stem) {
		analyzer = new SentimentAnalyzer(SentimentAnalyzer.path);
		if(stem != null) {
			this.stem = stem;			
			analyzer.convertToStemmed();
		}
	}
	
	protected void build8VectorOut(String inPath) throws IOException {
		build8Vector(inPath, outPath, false);
	}
	protected void build8VectorTrain(String inPath) throws IOException {
		build8Vector(inPath, trainPath, true);
	}
	
	@SuppressWarnings("null")
	public void build8Vector(String inPath, String outPath, boolean train) throws IOException {
		FileWriter fw = new FileWriter(new File(outPath));
		Scanner scan = new Scanner(new File(inPath));
		
		@SuppressWarnings("unused")
		PartOfSpeechTagger tagger = new StanfordTagger();
		DataCleaner clean = new DataCleaner();
		Set<String> processed = new HashSet<>();
		while(scan.hasNextLine()) {
			String line = scan.nextLine();
			if(line.isEmpty() || line.isBlank())
				continue;
			
			//the only things that we want in this is the words and apostrophes where necessary
			//with the proper capitalization. We want all symbols and punctuation stripped at this point
			
			//give the "sentence" to the tagger, and after we can stem it
			//TODO if you actually want to use this, then uncomment the next line and delete "null;"
			List<PartOfSpeech> posList = null; //tagger.identify(line);
			String[] words = line.split(" ");
			
			boolean nextSentiment = false;
			boolean lastPrep = false;
			boolean lastGenitive = false;
			for(int i=0; i<words.length; i++) {
				if(processed.contains(words[i]))
					continue; //skip already processed words
				processed.add(words[i]);
				
				//for each word, we make a bit/flag vector here of 9 attributes:
				//0: is-singular
				//1: is-capitalized
				//2: is-adjective
				//3: is-sentiment
				//4: is-after-preposition
				//5: is-after-apostrophe
				//6: is-before-sentiment
				//7: is-stopword
				//8: is-5W1H
				boolean[] vect = new boolean[9];
				
				vect[0] = posList.get(i) == PartOfSpeech.NOUN ||
						posList.get(i) == PartOfSpeech.NOUN_PROPER;
				vect[1] = Character.isUpperCase(words[i].charAt(0));
				vect[2] = posList.get(i) == PartOfSpeech.ADJ ||
						posList.get(i) == PartOfSpeech.ADJ_COMPARE ||
						posList.get(i) == PartOfSpeech.ADJ_SUPER;
				
				//find sentiment
				//if this is the first, then we have to find sentiment ourselves.
				//otherwise, we already found it last iteration
				vect[3] = i==0? findSentiment(words[i]) : nextSentiment;
				vect[4] = lastPrep;
				vect[5] = lastGenitive;
				
				//find for the next iteration
				if(i < words.length-1) {
					nextSentiment = findSentiment(words[i+1]);
					lastPrep = posList.get(i) == PartOfSpeech.PREP;
					
					int index = words[i].indexOf('\'');
					//fix genitive to use the tagger--
					
					lastGenitive = index!=-1 && (index==words[i].length()-1 || 
							(index==words[i].length()-2 && words[i].charAt(words[i].length()-1)=='s'));
				}else
					nextSentiment = false;
				
				vect[6] = nextSentiment;
				vect[7] = clean.isStopWord(words[i]);
				vect[8] = posList.get(i) == PartOfSpeech.WH_INTEROG;
				
				//now we output the results
				if(train)
					fw.write(words[i]);
				else
					fw.write('0');
				for(int j=0; j<vect.length; j++) {
					fw.write(" " + String.valueOf(j+1) + ":" + (vect[j]? -1: 1));
				}
				fw.write(" #" + words[i]);
				fw.write('\n'); //end the line
			}
			
		}
		scan.close();
		fw.close();
		System.out.println("Finished!");
	}
	
	private boolean findSentiment(String word) {
		word = word.replace("'", "").toLowerCase(); //get rid of any apostrophes
		if(stem != null)
			word = stem.stem(word);
		List<Sentiment> sentiments = analyzer.querySentiment(word);
		return sentiments!=null && !sentiments.isEmpty();
	}
	

}
