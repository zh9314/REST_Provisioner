package com.sw.provisioner;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;

public class CommonTool {
	
	public static String rootDir = "SWITCH";    ////do not contain '/'
	public static String userInfo = "user.info";
	
	public static String generateRootDir(){
		String abspath = System.getProperty("user.home");
		if(abspath.lastIndexOf('/') != abspath.length()-1)
			abspath += "/";
		return abspath+rootDir+"/";
	}
	
	public static String rephaseString(String input){
		String [] lines = input.split("\\\\n");
		String output = "";
		for(int i = 0 ; i<lines.length ; i++){
			output += lines[i];
			if(i<lines.length-1)
				output += "\n";
		}
		if(input.lastIndexOf("\\n") == input.length()-2)
			output += "\n";
		return output;
		
	}
	
	public static String getContentFromFile(String filePath){
		File toscaFile = new File(filePath);
		String fileContent = "";
		try {
			BufferedReader in = new BufferedReader(new FileReader(toscaFile));
			String line = "";
			while((line = in.readLine()) != null)
				fileContent += (line+"\\n");
			in.close();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return fileContent;
	}

}
