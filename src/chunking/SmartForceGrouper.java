package chunking;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class SmartForceGrouper extends FacetGrouper {

	@Override
	public List<TermOcc> categorize(List<TermOcc> terms) {
		//We will keep a running set of the categories. We try to add
		//each new term in the given list. To do so, we must compare
		//each word to every word in the return set. If the word is
		//subsumed by one in the return set, this is added to that
		//group. Since we sort the terms first (by letter length),
		//terms added to the return set are knowably final.
		terms.sort((TermOcc o1, TermOcc o2) -> {
			//sort by descending length
			return -Integer.compare(o1.text.length(), o2.text.length());
		});
		
		Set<TermOcc> ret = new HashSet<>();
		terms: for(TermOcc term: terms) {
			for(TermOcc rett: ret) {
				if(rett.text.contains(term.text)) {
					rett.occurrences += term.occurrences;
					continue terms; //the term was subsumed
				}
			}
			//if we made it thus far, then the word was not related
			//to any currently in the set
			ret.add(term);
		}
		
		List<TermOcc> toReturn = new ArrayList<>(ret.size());
		toReturn.addAll(ret);
		return toReturn;
	}

}
