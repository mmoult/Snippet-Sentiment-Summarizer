package classifying;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

public class CountedWords {
	private Map<String, Integer> map;
	
	public CountedWords() {
		map = new HashMap<>();
	}
	
	public void addWord(String word) {
		if(map.containsKey(word))
			map.put(word, map.get(word) + 1);
		else
			map.put(word, 1);
	}
	
	public int getOccurrences(String word) {
		if(map.containsKey(word))
			return map.get(word);
		return 0;
	}
	
	public int getSize() {
		int size = 0;
		for(String key: map.keySet()) {
			size += map.get(key);
		}
		return size;
	}
	
	public Set<String> getDistinctWords() {
		return map.keySet();
	}
	
	public int getDistinctSize() {
		return map.size();
	}
	
	public String toString() {
		return map.toString();
	}
	
}