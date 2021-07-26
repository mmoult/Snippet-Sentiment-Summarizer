package parsing;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Scanner;

import util.AmazonFileResources;

public class AmazonReviewCleaner extends DataCleaner {
	private int PROGRESS_BIG = 1000;
	private int PROGRESS_SMALL = 9;
	
	private static String[] inFiles;
	static {
		inFiles = new String[AmazonFileResources.types.length];
		for(int i=0; i<AmazonFileResources.types.length; i++) {
			inFiles[i] = AmazonFileResources.fileRoot + "review_" + AmazonFileResources.types[i] + ".json";
		}
	}
	
	public static void main(String args[]) throws IOException {
		new AmazonReviewCleaner().cleanAll(true);
	}
	
	public void cleanAll(boolean sentenceDivision) throws IOException {
		PROGRESS_BIG = 10000;
		PROGRESS_SMALL = 25;
		
		for(int i=0; i<inFiles.length; i++) {
			System.out.println("Processing file " + (i+1) + "/"+inFiles.length + " (" +
					AmazonFileResources.reviewFiles[i] +")");
			try {
				cleanFile(inFiles[i], AmazonFileResources.reviewFiles[i], sentenceDivision);
			}catch(IOException e) {
				System.err.println(e.getMessage());
			}
		}
	}
	
	public void cleanFile(String inFile, String outFile, boolean sentenceDivision) throws IOException {
		//now we need to go about parsing the json file and removing stopwords
		//we will output to a file "reviews.txt" what we get for each line
		Scanner scan =  new Scanner(new File(inFile));
		FileWriter out = new FileWriter(new File(outFile));
		out.write("REVIEWS\n");
		
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
			
			//we got to pull out the asin if there is one listed
			int asinIndex = line.indexOf("\"asin\":");
			if(asinIndex != -1) {
				out.write(line.substring(asinIndex, line.indexOf(',', asinIndex)+1) + " ");
			}
			StringBuilder review = new StringBuilder();
			int reviewIndex = line.indexOf("\"reviewText\":");
			if(reviewIndex != -1) {
				//find the first " after the end of "reviewText":
				int startIndex = line.indexOf("\"", reviewIndex + 13);
				review.append(getStringField(line, startIndex, sentenceDivision));
				review.append(" ");
			}
			int subjectIndex = line.indexOf("\"summary\":");
			if(subjectIndex != -1) {
				//find the first " after the end of "reviewText":
				int startIndex = line.indexOf("\"", subjectIndex + 10);
				review.append(getStringField(line, startIndex, sentenceDivision));
				review.append(" ");
			}
			
			out.write("\"words\": ");
			/* Amazon star rating system:
			 *	1 = I hated <the product>.
			 *	2 = I didn't like <the product>.
			 *	3 = <the product> was OK.
			 *	4 = I liked <the product>.
			 *	5 = I loved <the product>.
			 */
			String reviewText = review.toString();
			String productHolder = "the product";
			reviewText = reviewText.replaceAll("(?i)one star", "I hated " + productHolder);
			reviewText = reviewText.replaceAll("(?i)two stars", "I didn't like " + productHolder);
			reviewText = reviewText.replaceAll("(?i)three stars", productHolder + " was ok");
			reviewText = reviewText.replaceAll("(?i)four stars", "I liked " + productHolder);
			reviewText = reviewText.replaceAll("(?i)five stars", "I loved " + productHolder);
			writeWords(reviewText, out);
		}
		scan.close();
		out.close();
		
		System.out.println(" Finished!");
	}
	
}
