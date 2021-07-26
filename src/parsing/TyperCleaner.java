package parsing;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Scanner;

import util.AmazonFileResources;

public class TyperCleaner extends DataCleaner {
	protected static final String allQueryTypes = "orcas-doctrain-queries.tsv";
	protected static final String testQueries = "queries.train.tsv";
	protected static final String prodQueries = "product-queries.tsv";
	
	public static void main(String args[]) throws IOException {
		new SvmCleaner().clean(AmazonFileResources.fileRoot + prodQueries,
				AmazonFileResources.fileRoot + "queries.txt", 1000);
	}
	
	public void clean(String inFile, String outFile, int lineCap) throws IOException {
		Scanner scan =  new Scanner(new File(inFile));
		FileWriter out = new FileWriter(new File(outFile));
		
		String line;
		int lineNo = 0;
		while(scan.hasNextLine()) {
			lineNo++;
			if(lineNo > lineCap)
				break;
			line = scan.nextLine();
			
			//in the query dataset, each line has a number, a tab, then all the words until the new line
			
			String sentence = line.substring(line.indexOf('\t')+1); //ignore before the words
			sentence = sentence.replace('-', ' ');
			String[] words = sentence.split(" ");
			boolean first = true;
			for(String word: words) {
				if(word.isEmpty())
					continue;
				
				StringBuilder newWord = new StringBuilder();
				for(int k=0; k<word.length(); k++) {
					char c = word.charAt(k);
					//we are choosing to get rid of -. That may be ok, maybe not, depending on context
					if(Character.isAlphabetic(c) || c=='\'')
						newWord.append(c);
				}
				
				word = newWord.toString();
				if(word.equals("&"))
					word = "and";
				if(word.length() < 2 && !word.toLowerCase().equals("a"))
					continue;
				
				if(!first)
					out.write(' ');
				else
					first = false;
				out.write(word);
			}
			out.write("\n");
		}
		scan.close();
		out.close();
		
		System.out.println("Finished!");
	}

}
