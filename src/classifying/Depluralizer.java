package classifying;

public class Depluralizer {
	
	public String stem(String str) {
		//We don't merely want to stem it, we want to depluralize.
		//If the word ends in -ses or -xes, then it should most likely NOT keep the e
		if (str.endsWith("ses") || str.endsWith("xes"))
			return str.substring(0, str.length() - 2);
		
		//If the word ends in -ies, it should probably not keep the e since -ie words
		// are uncommon in English.
		// IES -> Y //changed here from I -> Y
        else if (str.endsWith("ies")) 
        	return str.substring(0, str.length() - 3)  + 'y';
		
		//If the word ends in -oes, we have about a 50-50 guess at whether the final
		// e should be kept or not. ex: tornadoes vs toes
        else if (str.endsWith("oes"))
        	return str.substring(0, str.length() - 2);
		
        else if (str.endsWith("es"))
        	return str.substring(0, str.length() - 1);
		
        else if (str.endsWith("s") && !isVowel(str.charAt(str.length()-2)))
        	return str.substring(0, str.length() - 1);
        
		//otherwise, do nothing
        return str;
	}
	
	protected boolean isVowel(char vowel) {
		vowel = Character.toLowerCase(vowel);
		return vowel == 'a' || vowel == 'e' || vowel == 'i' || vowel == 'o' || vowel == 'u';
	}
}
