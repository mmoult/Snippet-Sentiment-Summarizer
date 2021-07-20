package util;

public class FileResources {
	public static String fileRoot = "C:\\Users\\moult\\Development\\dataset\\";
	public static String[] types = {
			"Appliances",	//0
			"Movie",		//1
			"Music",		//2
			"Sports",		//3
			"Books",		//4
			"Electronics",	//5
			"Home",			//6
			"Cars"};		//7
	
	public static String[] metaFiles;
	public static String[] reviewFiles;
	static {
		metaFiles = new String[types.length];
		reviewFiles = new String[types.length];
		for(int i=0; i<types.length; i++) {
			metaFiles[i] = fileRoot + "meta" + types[i] + ".txt";
			reviewFiles[i] = fileRoot + "review" + types[i] + ".txt";
		}
	}

}
