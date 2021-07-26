package parsing;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.Writer;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Scanner;
import java.util.Set;

import util.AmazonFileResources;

public class DataCleaner {
	protected static char sentenceSplitter = '|';
	private Set<String> stopWords;
	
	public static int findEndOfQuoteDelimitedField(String line, int startIndex) {
		boolean started = false;
		for(int i=startIndex; i<line.length(); i++) {
			char c = line.charAt(i);
			if(c == '\\') {
				i++; //skip over the next
				continue;				
			}
			if(c == '"') {
				if(started)
					return i;
				else
					started = true;
			}
		}
		return -1;
	}
	public static String getStringField(String line, String fieldName, boolean sentenceDivision) {
		String findToken = "\"" + fieldName + "\": \"";
		if(!line.contains(findToken))
			return null;
		
		int start = line.indexOf(findToken) + findToken.length();
		return getStringField(line, start-1, sentenceDivision);
	}
	public static String getStringField(String line, int startIndex, boolean sentenceDivision) {
		startIndex = line.indexOf('"', startIndex);
		int endIndex = findEndOfQuoteDelimitedField(line, startIndex);
		return removePunctuation(line.substring(startIndex+1, endIndex), sentenceDivision);
	}
	
	public Set<String> getStopWords() throws FileNotFoundException {
		Scanner scan = new Scanner(new File(AmazonFileResources.fileRoot+"stopwords.txt"));
		HashSet<String> stopWords = new HashSet<>();
		
		while(scan.hasNext()) {
			stopWords.add(scan.next().toLowerCase());
		}
		scan.close();
		return stopWords;
	}
	
	public static String removePunctuation(String reviewLine, boolean sentenceDivision) {
		//remove some multi-character punctuation
		reviewLine = reviewLine.replace("\\\"", "");
		
		//convert all delimiter characters into spaces
		//like for example \n or /
		reviewLine = reviewLine.replace('/', ' ');
		reviewLine = reviewLine.replace("\\n", " ");
		reviewLine = reviewLine.replace("\\t", " ");
		reviewLine = reviewLine.replace('\\', ' ');
		reviewLine = reviewLine.replace("--", " ");
		reviewLine = reviewLine.replace(":", " ");
		reviewLine = reviewLine.replace(" - ", " ");
		reviewLine = reviewLine.replace(" ' ", " ");
		reviewLine = reviewLine.replace("...", ".");
		reviewLine = reviewLine.replace('|', ' ');
		reviewLine = removeHtmlTags(reviewLine);
		
		//all the different punctuation to remove
		char[] sentPunctuation = {'.', '!', '?', ';'};
		for(int i=0; i<sentPunctuation.length; i++) {
			char c = sentPunctuation[i];
			int index = reviewLine.indexOf(c);
			while(index != -1) {
				reviewLine = reviewLine.substring(0, index) +
						//sentence punctuation marks
						((sentenceDivision)? " " + sentenceSplitter + " " : ' ') +
						reviewLine.substring(index + 1);
				index = reviewLine.indexOf(c);
			}
		}
		
		//All other non-letter or number characters (except - ' |) should be stripped at this point
		StringBuilder line = new StringBuilder();
		boolean lastSpace = false; //cannot have more than 1 space in a row
		boolean emptySentence = true; //a sentence must have more than space to remain
		for(char c: reviewLine.toCharArray()) {
			if(Character.isWhitespace(c) && !lastSpace) {
				lastSpace = true;
			}else if(Character.isLetterOrDigit(c) || c=='-' || c=='\'') {
				lastSpace = false;
				emptySentence = false;
			}else if(!emptySentence && c =='|') {
				emptySentence = true;
				lastSpace = false;
			}else 
				continue;
			
			//if it did meet one of the criteria, append
			line.append(c);
		}
		return line.toString();
	}
	public static String removeHtmlTags(String line) {
		line = line.replace("&nbsp;", "_")
					.replace("&rsquo;", "'")
					.replace("&lsquo;", "'")
					.replace("&mdash;", "-")
					.replace("&amp;", "&");
		
		int start = line.indexOf('<');
		int end = line.indexOf('>');
		while(start != -1 && end != -1) {
			if(end < start)
				line = line.substring(0, start) + line.substring(start+1);
			else
				line = line.substring(0, start) + line.substring(end+1);
			start = line.indexOf('<');
			end = line.indexOf('>');
		}
		
		start = line.indexOf('&');
		end = line.indexOf(';');
		while(start != -1 && end != -1) {
			//check that it really is an escaped sequence
			if(end < start) {
				end = line.indexOf(';', end+1);
				continue;
			}
			//there should be no spaces between the start and the end
			boolean valid = true;
			for(int i=start; i<end; i++) {
				if(Character.isWhitespace(line.charAt(i))) {
					valid = false;
					break;
				}
			}
			if(valid) {
				line = line.substring(0, start) + line.substring(end+1);
				start = line.indexOf('&', start+1);
				end = line.indexOf(';', start+1);
			}else {
				//it was an invalid pairing, so continue forward
				//since there was a space, we may need to advance the start forward
				start = line.indexOf('&', start+1);
			}
		}
		
		//if we find some common css (that is uncommon as typical words), we just truncate the line
		//to cut it out, assuming that the rest is formatting garbage
		line = truncateOn("padding: ", line);
		line = truncateOn("<span ", line);
		return line;
	}
	
	private static String truncateOn(String endToken, String line) {
		if(line.contains(endToken))
			return line.substring(0, line.indexOf(endToken));
		return line;
	}
	
	public String combineSet(String line, int startIndex, boolean sentenceDivision) {
		//we assume that we started on the first [ and we want to go to the ]
		//the list will be composed of strings.
		
		StringBuilder set = new StringBuilder();
		for(int i=startIndex; i<line.length(); i++) {
			char c = line.charAt(i);
			if(c == ']')
				break;
			
			if(c == '"') {
				int end = findEndOfQuoteDelimitedField(line, i);
				if(end == -1)
					end = line.length();
			
				String field = line.substring(i+1, end);
				if(!(set.length()==0)) //isEmpty not working...
					set.append(' ');
				set.append(removePunctuation(field, sentenceDivision));
				i = end;
			}
		}
		
		return set.toString();
	}
	
	public List<String> filterStopWords(String wordList) {
		if(stopWords == null) {
			try {
				stopWords = getStopWords();
			} catch (FileNotFoundException e) {
				e.printStackTrace();
			}
		}
		
		String[] tokens = wordList.split(" ");
		List<String> okTokens = new ArrayList<>();
		for(String token: tokens) {
			if(token.length() == 0)
				continue;
			//if it is a single character and not a letter or digit, then some odd punctuation
			//we allow | to go through since it is a sentence break. As normal punctuation, it would have
			//already been filtered out
			if(token.length() == 1 && !Character.isLetterOrDigit(token.charAt(0)) && token.charAt(0) != '|')
				continue;
			
			//filter all numbers
			try {
				Integer.parseInt(token);
				continue;
			}catch(NumberFormatException e) {
				//keep going and add this non-number token
			}
			
			if(!stopWords.contains(token)) {
				okTokens.add(token);
			}
		}
		return okTokens;
	}
	
	public static String condenseSpaces(String line) {
		return line.replaceAll("\\p{javaWhitespace}+", " ");
	}
	
	public boolean isStopWord(String word) {
		if(stopWords == null) {
			try {
				stopWords = getStopWords();
			} catch (FileNotFoundException e) {
				e.printStackTrace();
			}
		}
		
		return stopWords.contains(word.toLowerCase());
	}
	
	protected void writeWords(String wordList, Writer out) throws IOException {
		String[] words = condenseSpaces(wordList).split(" ");
		boolean first = true;
		for(String word: words) {
			word = word.trim();
			if(word.length()>0 && word.charAt(0) == '\'')
				word = word.substring(1);
			if(word.length()>0 && word.charAt(word.length()-1) == '\'')
				word = word.substring(0, word.length()-1);
			if(word.isEmpty())
				continue;
			
			if(!first)
				out.write(' ');
			else
				first = false;
			out.write(word);
		}
		out.write("\n");
	}
	

}
