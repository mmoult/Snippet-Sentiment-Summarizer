package util;

import java.io.IOException;

public class ApiDictionary {
	
	public static boolean findWord(String term) {
		//https://dictionaryapi.dev/
		//https://github.com/meetDeveloper/freeDictionaryAPI
		//https://api.dictionaryapi.dev/api/v2/entries/en_US/hello
		try {
			String response = HttpCommunicator
					.getFromUrl("https://api.dictionaryapi.dev/api/v2/entries/en_US/"+term);
			if(response.contains("\"title\":\"No Definitions Found\""))
				return false;
			return true;
		} catch (IOException e) {
			e.printStackTrace();
		}
		return false;
	}

}
