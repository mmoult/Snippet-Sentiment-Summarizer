package parsing;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Scanner;

public class ParseWordNet {
	
	public static void main(String args[]) {
		new ParseWordNet();
	}
	
	public ParseWordNet() {
		try {
			Scanner scan = new Scanner(new File("C:\\Users\\moult\\Development\\dataset\\SentiWordNet_3.0.0.txt"));
			FileWriter write = new FileWriter(new File("C:\\Users\\moult\\Development\\dataset\\trimmed-senti-list.txt"));
			
			//for each line, we will only keep it if it has a sentiment value (or comment denoted by #)
			while(scan.hasNextLine()) {
				String line = scan.nextLine();
				if(line.charAt(0) == '#') {
					write.write(line+ '\n');
					continue;
				}
				
				Scanner tokens = new Scanner(line);
				//the first token is the part of speech
				tokens.next();
				tokens.next(); //then it is the id
				
				//then comes the pos score then neg score. If either are nonzero, we keep this line
				double posScore = tokens.nextDouble();
				double negScore = tokens.nextDouble();
				if(posScore != 0 || negScore != 0) {
					write.write(line + '\n');
				}
				tokens.close();
			}
			
			scan.close();
			write.flush();
			write.close();
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
		
	}

}
