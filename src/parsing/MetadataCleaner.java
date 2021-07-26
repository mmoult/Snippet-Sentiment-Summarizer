package parsing;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Scanner;

import util.AmazonFileResources;

public class MetadataCleaner extends DataCleaner {
	private int PROGRESS_BIG = 1000;
	private int PROGRESS_SMALL = 9;
	
	private static String prefix = AmazonFileResources.fileRoot;
	private static String[] inFiles;
	static {
		inFiles = new String[AmazonFileResources.types.length];
		for(int i=0; i<AmazonFileResources.types.length; i++) {
			inFiles[i] = prefix + "meta_" + AmazonFileResources.types[i] + ".json";
		}
	}
	private static String[] outFiles = AmazonFileResources.metaFiles;
	
	public static void main(String args[]) {
		MetadataCleaner cleaner = new MetadataCleaner();
		//cleaner.clean(3);
		cleaner.cleanAll();
	}
	
	public void clean(int index) {
		try {
			cleanFile(inFiles[index], outFiles[index], null, true);
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	public void cleanAll() {
		PROGRESS_BIG = 10000;
		PROGRESS_SMALL = 25;
		for(int i=0; i<inFiles.length; i++) {
			System.out.println("Processing file " + (i+1) + "/"+inFiles.length + " (" + outFiles[i]+")");
			clean(i);
		}
	}
	
	public void cleanFile(String inFile, String outFile, String filterCategory, boolean sentenceDivision)
			throws IOException {
		Scanner scan =  new Scanner(new File(inFile));
		FileWriter out = new FileWriter(new File(outFile));
		out.write("METADATA\n");
		
		String line;
		int lineNo = 0;
		while(scan.hasNextLine()) {
			line = scan.nextLine();
			if(lineNo % PROGRESS_BIG == 0) {
				if(lineNo % (PROGRESS_SMALL*PROGRESS_BIG) == 0) {
					if(lineNo > 0)
						System.out.println();
					System.out.print("Cleaning");
				}else
					System.out.print(".");
			}
			lineNo++;
			
			//verify that this line should not be filtered out
			if(filterCategory != null) {
				int catIndex = line.indexOf("\"category\":") + 12;
				String category = combineSet(line, catIndex, sentenceDivision);
				if(!category.contains(filterCategory))
					continue;
			}
			
			//we got to pull out the asin if there is one listed
			int asinIndex = line.indexOf("\"asin\":");
			if(asinIndex != -1) {
				out.write(line.substring(asinIndex, line.indexOf(',', asinIndex)+1) + " ");
			}
			StringBuilder wordList = new StringBuilder();
			int index = line.indexOf("\"title\":");
			if(index != -1) {
				int startIndex = index + 8;
				wordList.append(getStringField(line, startIndex, sentenceDivision));
				if(sentenceDivision)
					wordList.append(" " + sentenceSplitter);
				wordList.append(' ');
			}
			index = line.indexOf("\"description\":");
			//a description is a set of strings
			if(index != -1) {
				int startIndex = line.indexOf("[", index);
				wordList.append(combineSet(line, startIndex, sentenceDivision));
				wordList.append(' ');
			}
			index = line.indexOf("\"brand\":");
			if(index != -1) {
				int startIndex = index + 8;
				wordList.append(getStringField(line, startIndex, sentenceDivision));
				wordList.append(' ');
			}
			index = line.indexOf("\"feature\":");
			if(index != -1) {
				int startIndex = line.indexOf("[", index);
				wordList.append(combineSet(line, startIndex, sentenceDivision));
				wordList.append(' ');
			}
			
			out.write("\"words\": ");
			//we may want to make wordList lowercase or filter stop words, which 
			//the superclass method does not do
			writeWords(wordList.toString(), out);
		}
		scan.close();
		out.close();
		
		System.out.println(" Finished!");
	}

}
