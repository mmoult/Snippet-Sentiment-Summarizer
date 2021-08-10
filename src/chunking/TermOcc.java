package chunking;

public class TermOcc implements Comparable<TermOcc>{
	public String text;
	public int occurrences;
	
	public TermOcc(String term, int occ) {
		this.text = term;
		this.occurrences = occ;
	}
	
	//identity is the text, not the occurrences
	@Override
	public int hashCode() {
		return text.hashCode();
	}

	//should sort such by occurrence (desc)
	@Override
	public int compareTo(TermOcc o) {
		return Integer.compare(o.occurrences, occurrences);
	}
	
	@Override
	public String toString() {
		StringBuilder build = new StringBuilder();
		build.append('"');
		build.append(text);
		build.append("\"(");
		build.append(occurrences);
		build.append(')');
		return build.toString();
	}
	
}
