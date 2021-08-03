package classifying;

import java.io.File;
import java.io.FileNotFoundException;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;

import parsing.DataCleaner;
import util.AmazonFileResources;

public class Classifier {
	private static String[] metaFiles = AmazonFileResources.metaFiles;
	private List<CountedWords> classes = new ArrayList<>(metaFiles.length);
	private List<List<CountedWords>> entries = new ArrayList<>(metaFiles.length);
	private double[] pC = new double[metaFiles.length];
	
	private final int TRAINING_NUMBER = 5000;
	private final int TEST_NUMBER = 1000;
	private final boolean PRINT_EACH_TEST = false;
	
	
	public static void main(String args[]) throws FileNotFoundException {
		Classifier classifier = new Classifier(true);
		//OPTIONAL user query sort
		Scanner scan = new Scanner(System.in);
		while(scan.hasNextLine()) {
			String line = scan.nextLine();
			if(line.isEmpty())
				break; //quit signal
			
			DataCleaner cleaner = new DataCleaner();
			List<String> tokens = cleaner.filterStopWords(DataCleaner.removePunctuation(line, false));
			CountedWords entry = new CountedWords();
			for(String token: tokens) {
				entry.addWord(token);
			}
			int classChosen = classifier.classify(entry);
			System.out.println("Sorted into " + metaFiles[classChosen]);
		}
		scan.close();
	}
	
	public Classifier(boolean performTests) throws FileNotFoundException {
		for(int i=0; i<metaFiles.length; i++) {
			classes.add(new CountedWords());
			entries.add(new ArrayList<>());
		}
		
		//for classification of metadata, the asin field does not matter. We just need the words
		//We need to have some number of training data entries, from which we can sort the rest of
		//the entries and test accuracy
		
		Scanner[] scanners = new Scanner[metaFiles.length];
		for(int i=0; i<metaFiles.length; i++) {
			scanners[i] = new Scanner(new File(metaFiles[i]));
			for(int j=0; j<TRAINING_NUMBER; j++) {
				if(scanners[i].hasNextLine()) {
					String line = scanners[i].nextLine();
					int wordsStart = line.indexOf("\"words\": ");
					if(wordsStart == -1) {//occurs for empty lines or intro lines
						j--; //don't count this line as an entry added
						continue;
					}
					
					String words = line.substring(wordsStart + 9);
					//words = words.substring(0, words.length()-1); //remove the final quote
					String[] allWords = words.split(" ");
					CountedWords entry = new CountedWords();
					for(String word: allWords) {
						//add this word to the correct class and entry
						entry.addWord(word);
						classes.get(i).addWord(word);
					}
					entries.get(i).add(entry);
				}else {
					break;
				}
			}
			
		}
		
		
		//After the sets are all built, we want to maximize:
		//	P(c | d) = [P(d | c) P(c)] / [SUM_[c in C] P(d | c) P(c)]
		//Also, P(d | c), which is proportional to  PRODUCT_[w in V] P(w|c)^(tf_w,d)
		//which then requires P(w|c) = [tfw,c + 1] / [|c| + |V|]
				
		int trainingSum = 0;
		for(int j=0; j<metaFiles.length; j++) {
			trainingSum += entries.get(j).size();
		}
		// P(c) = N_c/N
		for(int j=0; j<metaFiles.length; j++) {
			pC[j] = entries.get(j).size() / (double)trainingSum;
		}
		
		if(performTests) {
			//run 100 tests to see how the classifier is working
			int numCorrect = 0;
			for(int i=0; i<TEST_NUMBER; i++) {
				int rand = (int)(Math.random()*metaFiles.length);
				
				if(scanners[rand].hasNextLine()) {
					String line = scanners[rand].nextLine();
					int wordsStart = line.indexOf("\"words\": \"");
					if(wordsStart == -1) {//occurs for empty lines or intro lines
						i--; //don't count this line as a test
						continue;
					}
					
					String words = line.substring(wordsStart + 10);
					words = words.substring(0, words.length()-1); //remove the final quote
					String[] allWords = words.split(" ");
					CountedWords entry = new CountedWords();
					for(String word: allWords) {
						//we want to ignore any sentence breaks we come across
						if(word.equals("|"))
							continue;
						
						//add this word to the correct entry
						entry.addWord(word);
					}
					
					//now we want to run the test and see which class it fits best in
					int classChosen = classify(entry);
					if(PRINT_EACH_TEST)
						System.out.println(classChosen + " " + rand);
					else if(classChosen != rand)
						System.out.println(classChosen + " " + rand);
					//the class chosen is at index classChosen
					//if classChosen matches rand, then the classification was correct
					if(classChosen == rand)
						numCorrect++;
				}else {
					i--;
					continue;
				}
			}
			
			System.out.println("Tests finished! " + numCorrect + "/" + TEST_NUMBER);
		}
		
		for(int i=0; i<metaFiles.length; i++) {
			scanners[i].close();
		}
	}
	
	public int classify(List<String> toClassify) {
		CountedWords doc = new CountedWords();
		for(String term: toClassify) {
			doc.addWord(term);
		}
		
		int res = classify(doc);
		return res; //metaFiles[res];
	}
	private int classify(CountedWords document) {
		double pCofDMax = 0;
		int classChosen = -1;
		for(int j=0; j<metaFiles.length; j++) {
			// P(c | d) = [P(d | c) P(c)] / [SUM_[c in C] P(d | c) P(c)]
			//  As I understand it, since all arguments are divided by the same denominator
			//  used to make it a probability (out of 1), then we can simply ignore the
			//  denominator completely here and simply select the highest numerator
			
			// P(d | c) = PRODUCT_[w in V] P(w|c)^(tf_w,d)
			double pDofC = 1;
			// P(w|c) = [tfw,c + 1] / [|c| + |V|]
			CountedWords clazz = classes.get(j);
			for(String word: document.getDistinct()) {
				//Laplacian smoothed here
				double base = (clazz.getOccurrences(word) + 1d) /
						//(clazz.getSize() + clazz.getDistinctSize()); //TRUE
						(clazz.getDistinctSize()); //MORE ACCURATE AND FASTER
				//double pWofC = Math.pow(base, entry.getOccurences(word)); //TRUE
				//normalization on exponent?
				double pWofC = Math.pow(base, document.getOccurrences(word)/(double)document.getSize());
				pDofC *= pWofC;
			}
			double pCofD = pDofC * pC[j];
			if(classChosen == -1 || pCofD > pCofDMax) {
				classChosen = j;
				pCofDMax = pCofD;
			}
		}
		return classChosen;
	}

}
