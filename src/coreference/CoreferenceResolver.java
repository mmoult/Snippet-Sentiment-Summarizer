package coreference;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;

public class CoreferenceResolver {
	
	public static String resolve(String sentence) {
		final String pythPath = "C:\\Users\\moult\\AppData\\Local\\Programs\\Python\\Python37\\python.exe";
		ProcessBuilder processBuilder = new ProcessBuilder(pythPath, "src\\coreference\\coreference.py", sentence);
		processBuilder.redirectErrorStream(true);
		
        try {
            Process process = processBuilder.start();

            BufferedReader reader =
                    new BufferedReader(new InputStreamReader(process.getInputStream()));

            StringBuilder output = new StringBuilder();
            String line;
            while((line = reader.readLine()) != null) {
            	output.append('\n');
            	output.append(line);
            }

            int exitCode = process.waitFor();
            if(exitCode != 0)
            	throw new RuntimeException("Coreference program failed! Error code: " + exitCode);
            
            return output.toString();
        } catch (IOException e) {
            e.printStackTrace();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        return null;
	}

}
