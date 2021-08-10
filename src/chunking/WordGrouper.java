package chunking;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import classifying.Pair;

public class WordGrouper extends FacetGrouper {

	@Override
	public List<TermOcc> categorize(List<TermOcc> terms) {
		List<Pair<Set<String>, Integer>> groups = new ArrayList<>();
		Map<String, Pair<Set<String>, Integer>> wordsUsed = new HashMap<>();
		
		for(TermOcc term: terms) {
			String[] words = term.text.split(" ");
			
			Pair<Set<String>, Integer> foundGroup = null;
			for(String word: words) {
				if(wordsUsed.containsKey(word)) {
					foundGroup = wordsUsed.get(word);
					break;
				}
			}
			if(foundGroup != null) {
				//now add all the words in this to the group and to
				// the used words
				foundGroup.second += term.occurrences;
				for(String word: words) {
					if(!wordsUsed.containsKey(word))
						wordsUsed.put(word, foundGroup);
					if(!foundGroup.first.contains(word))
						foundGroup.first.add(word);
				}
			}else {
				//We need to create a new group
				foundGroup = new Pair<Set<String>, Integer>(new HashSet<>(), term.occurrences);
				for(String word: words) {
					if(!wordsUsed.containsKey(word))
						wordsUsed.put(word, foundGroup);
					if(!foundGroup.first.contains(word))
						foundGroup.first.add(word);
				}
				groups.add(foundGroup);
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

}
