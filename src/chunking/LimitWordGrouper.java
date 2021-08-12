package chunking;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import classifying.Pair;

public class LimitWordGrouper extends FacetGrouper {
	private static final double REQ_THRESH = .5;

	@Override
	public List<TermOcc> categorize(List<TermOcc> terms) {
		List<Pair<Set<String>, Integer>> groups = new ArrayList<>();
		Map<String, List<Pair<Set<String>, Integer>>> wordsUsed = new HashMap<>();
		
		//To be more accurate, we need to sort the terms in ascending word order.
		// However, it does not have too much effect, so we may omit sorting for speed.
		terms.sort((TermOcc me, TermOcc other) -> {
			return Integer.compare(countCharOccs(me.text, ' '), countCharOccs(other.text, ' '));
		});
		//
		
		for(TermOcc term: terms) {
			String[] words = term.text.split(" ");
			
			Pair<Set<String>, Integer> foundGroup = null;
			for(int i=0; i<words.length; i++) {
				String word = words[i];
				if(wordsUsed.containsKey(word)) {
					//We potentially have some different groups tied to this one word
					for(Pair<Set<String>, Integer> group: wordsUsed.get(word)) {
						//We want to make sure that enough of the words in the found
						// group are present in this term
						int reqWords = (int)Math.round(REQ_THRESH * group.first.size());
						int found = 1;
						if(found >= reqWords) {
							foundGroup = group;
							break; // We found sufficient
						}
						for(int j=i+1; j<words.length; j++) { //look through remaining words
							//If there aren't enough words to fulfill the threshold: quit early
							if(words.length-j < reqWords-found)
								break;
							
							if(group.first.contains(words[j])) {
								found++;
								if(found >= reqWords) {
									foundGroup = group;
									break; //we have enough!
								}
							}
						}
						if(found >= words.length) //All words in this matched, so it doesn't matter
							foundGroup = group;   // that the group requirement wasn't met
					}
					
					if(foundGroup != null)
						break; //otherwise, we want to keep searching
				}
			}

			boolean newGroup = foundGroup == null;
			if(!newGroup) {
				foundGroup.second += term.occurrences;
			} else {
				//We need to create a new group
				foundGroup = new Pair<Set<String>, Integer>(new HashSet<>(), term.occurrences);
				groups.add(foundGroup);
			}
			
			//Now add all the words in this to the group and to the used words
			for(String word: words) {
				boolean added = false;
				if(!wordsUsed.containsKey(word)) { //this is a new word
					//therefore, we must initialize the list
					List<Pair<Set<String>, Integer>> list = new ArrayList<>();
					list.add(foundGroup);
					added = true; //the list was added for the word
					wordsUsed.put(word, list);
				}
				if(newGroup || !foundGroup.first.contains(word)) { //new word to the group
					foundGroup.first.add(word);
					if(!added) //if already added ref for this word, don't do again
						wordsUsed.get(word).add(foundGroup);
				}
			}
		}
		
		List<TermOcc> ret = new ArrayList<>(groups.size());
		for(Pair<Set<String>, Integer> group: groups) {
			StringBuilder build = new StringBuilder();
			for(String word: group.first) {
				if(build.length() != 0)
					build.append(' ');
				build.append(word);
			}
			ret.add(new TermOcc(build.toString(), group.second));
		}
		return ret;
	}
	
	private static int countCharOccs(String str, char c) {
		int occs = 0;
		for(int i=0; i<str.length(); i++) {
			if(str.charAt(i) == c)
				occs++;
		}
		return occs;
	}

}
