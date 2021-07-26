package parsing;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Scanner;

import util.OpinRankFiles;

public class ReviewCleaner extends DataCleaner {

	public static void main(String args[]) throws IOException {
		//let's try just cleaning one to start with
		ReviewCleaner cleaner = new ReviewCleaner();
		
		cleaner.cleanAllCars();
		//cleaner.cleanFile(OpinRankFiles.carFile + "2007//2007_acura_mdx",
		//		OpinRankFiles.carFile + "2007//acura_mdx.txt", true);
	}
	
	public void cleanAllCars() {
		File folder = new File(OpinRankFiles.carFile);
		List<File> processing = new ArrayList<>();
		processing.addAll(Arrays.asList(folder.listFiles()));

		List<File> newFiles = new ArrayList<>();
		while (!processing.isEmpty()) {
			for (File file : processing) {
			    if (file.isFile()) {
			    	//also we want to make sure not to clean generated files, so break on them
			    	if(file.getName().contains(".txt"))
			    		continue;
			    	
			    	String path = file.getParent();
			    	String fileName = file.getName();
			    	String out = path + File.separatorChar + fileName.substring(fileName.indexOf('_')+1) + ".txt";
			    	try {
						cleanFile(file.getPath(), out, true);
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
	
	public void cleanFile(String inFile, String outFile, boolean sentenceDivision) throws IOException {
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
}
