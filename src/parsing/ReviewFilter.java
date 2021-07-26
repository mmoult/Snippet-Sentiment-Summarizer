package parsing;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.HashSet;
import java.util.Scanner;
import java.util.Set;

import util.AmazonFileResources;

public class ReviewFilter {
	
	public static void main(String args[]) throws IOException {
		new ReviewFilter().filterReviews(6, "filtered.json");
	}
	
	public void filterReviews(int file, String outFile) throws IOException {
		//to filter reviews, we will delete all reviews that do not
		//have a matching product (as according to asin) in the corresponding
		//metadata file
		
		//first we have to load the metadata file
		Set<String> asins = new HashSet<>();
		System.out.println("Reading asins from " + AmazonFileResources.metaFiles[file]);
		Scanner scan = new Scanner(new File(AmazonFileResources.metaFiles[file]));
		String asinToken = "\"asin\": \"";
		while(scan.hasNextLine()) {
			String line = scan.nextLine();
			if(line.contains(asinToken)) {
				asins.add(line.substring(asinToken.length(),
						line.indexOf('"', asinToken.length()+2)));
			}else
				continue;
		}
		scan.close();
		
		//now we go through the file to filter
		String toFilter = AmazonFileResources.fileRoot + "review_" + AmazonFileResources.types[file] + ".json";
		scan = new Scanner(new File(toFilter));
		System.out.println("Filtering " + toFilter);
		FileWriter write = new FileWriter(new File(AmazonFileResources.fileRoot+outFile));
		System.out.println("Printing to " + AmazonFileResources.fileRoot+outFile);
		
		int updateDelta = 100000;
		int lineNo = 0;
		System.out.print("Working");
		while(scan.hasNextLine()) {
			String line = scan.nextLine();
			lineNo++;
			if(lineNo % updateDelta == 0) {
				System.out.print('.');
				write.flush();
			}
			
			if(line.contains(asinToken)) {
				//get the asin out of it
				int asinAt = line.indexOf(asinToken) + asinToken.length();
				String asin = line.substring(asinAt, line.indexOf('"', asinAt+1));
				if(asins.contains(asin)) {
					write.write(line);
					write.write('\n');
				}
				//if there is no metadata (so the asin is not found), then just skip the line
			}
		}
		scan.close();
		write.close();
		System.out.println("\nFinished!");
	}

}
