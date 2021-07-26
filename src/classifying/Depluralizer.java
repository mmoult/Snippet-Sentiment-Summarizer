package classifying;

import sentiment.PorterStemmer;

public class Depluralizer extends PorterStemmer {
	
	public String depluralize(String str) {
		//TODO we should handle the special case of -es by itself.
		//We don't merely want to stem it, we want to depluralize.
		//If the word ends in -ses or -xes, then it should most likely NOT keep the e
		//If the word ends in -ies, it should probably not keep the e since -ie words
		// are uncommon in English.
		//If the word ends in -oes, we have about a 50-50 guess at whether the final
		// e should be kept or not. ex: tornadoes vs toes
		//...
		
		return super.step1a(str);
	}
}
