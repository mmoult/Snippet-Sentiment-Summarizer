package coreference;

import util.PythonBridge;

public class CoreferenceResolver {
	
	public static String resolve(String sentence) {
		return PythonBridge.launch("coreference\\Coreference.py", sentence);
	}

}
