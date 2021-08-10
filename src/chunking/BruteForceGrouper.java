package chunking;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

public class BruteForceGrouper extends FacetGrouper {

	@Override
	public List<TermOcc> categorize(List<TermOcc> terms) {
		//this will be an (intentionally) naive approach to the problem
		
		//We will keep a running set of the categories. We try to add
		//each new term in the given list. To do so, we must compare
		//each word to every word in the return set. If the word is
		//subsumed by one in the return set, this is added to that
		//group. If the word in the return set is subsumed by the
		//word processed, then this word replaces that, and we must
		//check against all other words currently in the set
		
		Set<TermOcc> ret = new HashSet<>();
		terms: for(TermOcc term: terms) {
			Iterator<TermOcc> i = ret.iterator();
			//to modify in ret. First is add, all others are to remove.
			List<TermOcc> modify = new ArrayList<>();
			while(i.hasNext()) {
				TermOcc rett = i.next();
				if(rett.text.contains(term.text)) {
					//the term was subsumed
					//That means that the number of occurrences should change
					rett.occurrences += term.occurrences;
					continue terms; 
				}if(term.text.contains(rett.text)) {
					//This is where it gets complicated. We are going
					// to replace rett in ret with term. However, we
					// also need to check all the remaining strings
					// in ret to see if they are subsumed by term.
					//Thankfully, we can continue from where we left
					// off, since we already know previous strings
					// in ret are not subsumed by term.
					modify.add(term);
					modify.add(rett);
					term.occurrences += rett.occurrences;
					while(i.hasNext()) {
						rett = i.next();
						//it should not be possible for term to be contained in rett
						if(term.text.contains(rett.text)) {
							modify.add(rett);
							term.occurrences += rett.occurrences;
						}
					}
					break;
				}
			}
			if(!modify.isEmpty()) {
				//the first is the one to add
				ret.add(modify.get(0));
				for(int j=1; j<modify.size(); j++)
					ret.remove(modify.get(j));
				continue;
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
