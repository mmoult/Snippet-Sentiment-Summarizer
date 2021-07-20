package classifying;

import java.util.List;

public interface PartOfSpeechTagger {
	
	public enum PartOfSpeech {
		CONJ, CARDINAL, ARTICLE, PREP, ADJ, 
		ADJ_COMPARE, ADJ_SUPER, VERB, NOUN, NOUN_PL,
		NOUN_PROPER, NOUN_PROPER_PL, PRON, 
		PRON_POS, ADV, ADV_COMPARE, ADV_SUPER,
		WH_INTEROG, EXTRA, GENITIVE_MARK, PREFIX
	};
	
	
	
	public List<Pair<String, PartOfSpeech>> identify(String sentence);

}
