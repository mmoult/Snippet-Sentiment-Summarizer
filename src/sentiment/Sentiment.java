package sentiment;

public class Sentiment {
	private String[] synonyms;
	private char partOfSpeech;
	private int id;
	private String definition;
	
	private SentiScore score;
	
	
	public Sentiment(char partOfSpeech, String[] synonyms, int id, SentiScore score, String definition) {
		this.partOfSpeech = partOfSpeech;
		this.synonyms = synonyms;
		this.id = id;
		this.score = score;
		this.definition = definition;
	}

	public String[] getSynonyms() {
		return synonyms;
	}

	public char getPartOfSpeech() {
		return partOfSpeech;
	}

	public int getId() {
		return id;
	}

	public String getDefinition() {
		return definition;
	}

	public SentiScore getScore() {
		return score;
	}
}