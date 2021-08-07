package parsing;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Scanner;

import util.OpinRankFiles;

/**
 * A cleaner for dataset files. In recent updates, this class has been
 * customized specifically for the format of files in the OpinRank
 * dataset.
 */
public class ReviewCleaner extends DataCleaner {

	public static void main(String args[]) throws IOException {
		//let's try just cleaning one to start with
		ReviewCleaner cleaner = new ReviewCleaner();
		
		//cleaner.cleanCarDirectory(OpinRankFiles.hotelFile);
		cleaner.cleanDirectory(OpinRankFiles.hotelFile, false);
	}
	
	public void cleanDirectory(String directory, boolean car) {
		File folder = new File(directory);
		List<File> processing = new ArrayList<>();
		processing.addAll(Arrays.asList(folder.listFiles()));

		List<File> newFiles = new ArrayList<>();
		while (!processing.isEmpty()) {
			for (File file : processing) {
			    if (file.isFile() && !file.getName().endsWith(".pdf")) {
			    	//also we want to make sure not to clean generated files, so break on them
			    	if(file.getName().contains(".txt"))
			    		continue;
			    	
			    	String path = file.getParent();
			    	String fileName = file.getName();
			    	String out = path + File.separatorChar + fileName.substring(fileName.indexOf('_')+1) + ".txt";
			    	try {
			    		if(car)
			    			cleanCarFile(file.getPath(), out, true);
			    		else
			    			cleanHotelFile(file.getPath(), out, true);
					} catch (IOException e) {
						e.printStackTrace();
					}
			    }else {
			    	//otherwise, it is a directory, so we do recursive in there
			    	newFiles.addAll(Arrays.asList(file.listFiles()));
			    }
			}
			//we wait until the end of the iteration to add to avoid concurrent
			processing.clear(); //clear the finished files
			processing.addAll(newFiles);
			newFiles.clear();
		}
	}
	
	public void cleanCarFile(String inFile, String outFile, boolean sentenceDivision) throws IOException {
		//now we need to go about parsing the json file and removing stopwords
		//we will output to a file "reviews.txt" what we get for each line
		Scanner scan =  new Scanner(new File(inFile));
		FileWriter out = new FileWriter(new File(outFile));
		System.out.print("Cleaning \"" + inFile + "\"...");
		out.write("REVIEWS\n");
		
		String line;
		String text;
		while(scan.hasNextLine()) {
			line = scan.nextLine();
			
			String textTag = "<TEXT>";
			if(line.indexOf(textTag) == 0) {
				out.write(" \"words\": ");
				int end = line.indexOf("</TEXT>");
				while(end == -1) {
					//keep fetching more lines until this text is done
					line += '\n' + scan.nextLine();
					end = line.indexOf("</TEXT>");
				}
				text = line.substring(textTag.length(), end);
				//now we can handle that text
				text = super.removePunctuation(text, true);
				out.write(text + '\n');
			}
		}
		scan.close();
		out.close();
		
		System.out.println(" Finished!");
	}
	
	public void cleanHotelFile(String inFile, String outFile, boolean sentenceDivision) throws IOException {
		// The hotel files are in a completely different format than the car reviews.
		// From what I can tell, it is DATE tab SUBJECT tab BODY
		Scanner scan =  new Scanner(new File(inFile));
		FileWriter out = new FileWriter(new File(outFile));
		System.out.print("Cleaning \"" + inFile + "\"...");
		out.write("REVIEWS\n");
		
		String line;
		String text;
		while(scan.hasNextLine()) {
			line = scan.nextLine();
			
			text = line.substring(line.indexOf('\t')+1).replace("\t", " ");
			text = super.removePunctuation(text, true);
			out.write(" \"words\": ");
			out.write(text + '\n');
		}
		scan.close();
		out.close();
		
		System.out.println(" Finished!");
	}
}
