package util;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;

public class PythonBridge {
	//If Python is already saved on the path, we do not need to provide a specific location
	final static String pythPath = "python";
	
	public static String launch(String filePath, String... args) {
		//I have tried it both ways, and it does not seem faster to reuse a static processBuilder.
		List<String> params = new ArrayList<>(2 + args.length);
		params.add(pythPath);
		params.add("src\\" + filePath);
		for(String arg: args)
			params.add(arg);
		
		ProcessBuilder processBuilder = new ProcessBuilder(params);
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
            	throw new RuntimeException(filePath + " program failed! Error code: " + exitCode);
            
            return output.toString();
        } catch (IOException e) {
            e.printStackTrace();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        return null;
	}

}
