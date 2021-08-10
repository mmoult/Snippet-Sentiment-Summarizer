package chunking;

import java.io.File;
import java.io.FileNotFoundException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Scanner;

public abstract class FacetGrouper {
	private static final boolean PRINT_RESULTS = true;
	
	public abstract List<TermOcc> categorize(List<TermOcc> terms);
	
	public static void main(String args[]) throws FileNotFoundException {
		//This is where we will test the different options
		
		//We need to load all the words and occurrences that we are going to use
		List<TermOcc> words = new ArrayList<>();
		Scanner scan = new Scanner(new File("car_facet.csv"));
		scan.nextLine(); //ignore the intro line
		while(scan.hasNextLine()) {
			String line = scan.nextLine();
			
			//id#, term, occ, examples
			if(line.indexOf(',') == -1)
				continue;
			
			int firstComma = line.indexOf(',');
			int secComma = line.indexOf(',', firstComma+1);
			String text = line.substring(firstComma+1, secComma);
			int occ = Integer.parseInt(line.substring(secComma+1, line.indexOf(',', secComma+1)));
			TermOcc term = new TermOcc(text, occ);
			
			words.add(term);
		}
		scan.close();
		
		timeWordGrouper(words);
		timeSmartForce(words);
		timeBruteForce(words);
	}
	
	private static void timeBruteForce(List<TermOcc> terms) {
		long nano = System.nanoTime();
		List<TermOcc> results = new BruteForceGrouper().categorize(terms);
		long now = System.nanoTime();
		printResults(results, now-nano, "Brute force");
	}
	private static void timeSmartForce(List<TermOcc> terms) {
		long nano = System.nanoTime();
		List<TermOcc> results = new SmartForceGrouper().categorize(terms);
		long now = System.nanoTime();
		printResults(results, now-nano, "Smart force");
	}
	private static void timeWordGrouper(List<TermOcc> terms) {
		long nano = System.nanoTime();
		List<TermOcc> results = new WordGrouper().categorize(terms);
		long now = System.nanoTime();
		printResults(results, now-nano, "Word groups");
	}
	
	private static void printResults(List<TermOcc> results, long time, String label) {
		System.out.println("===" + label +" in " + time + " ns:");
		if(PRINT_RESULTS) {
			Collections.sort(results);
			System.out.print(results.size() + " ");
			System.out.println(results);			
		}
	}

}
