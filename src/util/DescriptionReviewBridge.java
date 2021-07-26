package util;

import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;

public class DescriptionReviewBridge {
	
	public List<String> getReviewsForAsin(String file, String asin) {
		int type = -1;
		for(int i=0; i<AmazonFileResources.types.length; i++) {
			if(file.contains(AmazonFileResources.types[i])) {
				type = i;
				break;
			}
		}
		if(type == -1) {
			System.err.println("File could not be found!");
			return null;
		}
		
		Scanner scan = new Scanner(AmazonFileResources.reviewFiles[type]);
		List<String> reviews = new ArrayList<>();
		while(scan.hasNextLine()) {
			String line = scan.nextLine();
			if(line.contains("\"asin\": \"" + asin + "\"")) {
				String reviewToken = "\"words\": ";
				String review = line.substring(line.indexOf(reviewToken)+reviewToken.length()+1);
				reviews.add(review);
			}
		}
		scan.close();
		
		return reviews;
	}

}
