package com.sw.provisioner;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.StringWriter;
import java.util.Iterator;

import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;

import org.dom4j.Document;
import org.dom4j.DocumentException;
import org.dom4j.DocumentHelper;
import org.dom4j.Element;
import org.dom4j.io.XMLWriter;


@Path("/provision")
public class Provisioner {
	
	private String rootDir = "SWITCH";
	private String userInfo = "user.info";
	
	
	/*
	 *  xmlmsg: string for uploading topology files.  
	 *  Example
	 *  <upload>
	 *      <user>abc</user>
	 *      <pwd>1234</pwd>
	 *      <file name="1.yml" level=0>tosca\n</file>
	 *      <file name="2.yml" level=1>infrastructure\ntest</file>
	 *  </upload>
	 *  
	 *  note: level 0 identifies that the file is the main topology file.
	 */
	@Path("/upload")
	@POST
	@Consumes("text/xml")
	public String uploadFiles(String xmlmsg) {
		java.util.Calendar cal = java.util.Calendar.getInstance();
		long currentMili = cal.getTimeInMillis();
		Document doc = null;
		try{
			doc = DocumentHelper.parseText(xmlmsg);
			Element rootElt = doc.getRootElement();
			if(!rootElt.getName().equals("upload"))
				return "Fail: The root element should be 'upload' in input string!";
			Element userElt = rootElt.element("user");
			Element pwdElt = rootElt.element("pwd");
			String user = userElt.getTextTrim();
			String pwd = pwdElt.getTextTrim();
			if(!AccountAction.validateUser(user, pwd))
				return "Fail: Invalid user or password!";
			String fileDir = CommonTool.generateRootDir()+"users/"+user+"/files/"+currentMili+"/";
			try {
				Process p = Runtime.getRuntime().exec("mkdir "+fileDir);
				p.waitFor();
			} catch (IOException | InterruptedException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} 
			Iterator iter = rootElt.elementIterator("file"); 
			while (iter.hasNext()) {
				Element file = (Element) iter.next();
				String fname = file.attributeValue("name");
				String flvl = file.attributeValue("level");
				String content = file.getText();
				String filePath = fileDir+fname;
				if(flvl.equals("0"))
					filePath += "_main";
				FileWriter yamlFile = new FileWriter(filePath);
				yamlFile.write(CommonTool.rephaseString(content));
				yamlFile.close();
			}
		} catch (Exception e) {
				e.printStackTrace();
				return "Fail: Execption with the input String!";
		}
		
		return "Success: Infrastructure files are uploaded! Action number: "+currentMili;
	}
	
	
	/*
	 *  xmlmsg: string for uploading user public key file.  
	 *  Example
	 *  <confUserKey>
	 *      <user>abc</user>
	 *      <pwd>1234</pwd>
	 *      <userKey name="id_dsa.pub">tosca\nsdfsdf\n</userKey>
	 *      <action>12331323231</action>
	 *  </confUserKey>
	 *  
	 *  
	 */
	@Path("/confuserkey")
	@POST
	@Consumes("text/xml")
	public String confUserKey(String xmlmsg) {
		Document doc = null;
		try{
			doc = DocumentHelper.parseText(xmlmsg);
			Element rootElt = doc.getRootElement();
			if(!rootElt.getName().equals("confUserKey"))
				return "Fail: The root element should be 'confUserKey' in input string!";
			Element userElt = rootElt.element("user");
			Element pwdElt = rootElt.element("pwd");
			String user = userElt.getTextTrim();
			String pwd = pwdElt.getTextTrim();
			if(!AccountAction.validateUser(user, pwd))
				return "Fail: Invalid user or password!";
			Element actionElt = rootElt.element("action");
			String action = actionElt.getTextTrim();
			Element keyElt = rootElt.element("userKey");
			String keyName = keyElt.attributeValue("name");
			String keyContent = keyElt.getText();
			String fileDir = CommonTool.generateRootDir()+"users/"+user+"/files/"+action+"/";
			String keyFilePath = fileDir+keyName;
			
			File file = new File(fileDir);
	        String[] ls = file.list();
	        for(int i = 0 ; i<ls.length ; i++){
	        	if(ls[i].contains(".")){
	        		String [] fileTypes = ls[i].split("\\.");
		        	if(fileTypes.length > 0){
			        	int lastIndex = fileTypes.length-1;
			        	String fileType = fileTypes[lastIndex];
			        	if(fileType.equals("yml")){
			        		String toscaFile = fileDir+ls[i];
			        		changeKeyFilePath(toscaFile, keyFilePath);
			        	}
	        		}
	        	}
	        	
	        }
			
			FileWriter keyFile = new FileWriter(keyFilePath);
			keyFile.write(CommonTool.rephaseString(keyContent));
			keyFile.close();
			
			
		} catch (Exception e) {
				e.printStackTrace();
				return "Fail: Execption with the input String!";
		}
		
		return "Success: User public key file is uploaded!";
	}
	
	
	
	/*
	 *  xmlmsg: string for uploading script for GUI to use.  
	 *  Example
	 *  <confScript>
	 *      <user>abc</user>
	 *      <pwd>1234</pwd>
	 *      <script>tosca\nsdfsdf\n</script>
	 *      <action>12331323231</action>
	 *  </confScript>
	 *  
	 *  
	 */
	@Path("/confscript")
	@POST
	@Consumes("text/xml")
	public String confGUIscript(String xmlmsg) {
		Document doc = null;
		try{
			doc = DocumentHelper.parseText(xmlmsg);
			Element rootElt = doc.getRootElement();
			if(!rootElt.getName().equals("confScript"))
				return "Fail: The root element should be 'confScript' in input string!";
			Element userElt = rootElt.element("user");
			Element pwdElt = rootElt.element("pwd");
			String user = userElt.getTextTrim();
			String pwd = pwdElt.getTextTrim();
			if(!AccountAction.validateUser(user, pwd))
				return "Fail: Invalid user or password!";
			Element actionElt = rootElt.element("action");
			String action = actionElt.getTextTrim();
			Element scriptElt = rootElt.element("script");
			String fileContent = scriptElt.getText();
			String scriptPath = CommonTool.generateRootDir()+"users/"+user+"/files/"+action+"/GUI_script.sh";
			
			String fileDir = CommonTool.generateRootDir()+"users/"+user+"/files/"+action+"/";
			File file = new File(fileDir);
	        String[] ls = file.list();
	        for(int i = 0 ; i<ls.length ; i++){
	        	if(ls[i].contains(".")){
	        		String [] fileTypes = ls[i].split("\\.");
		        	if(fileTypes.length > 0){
			        	int lastIndex = fileTypes.length-1;
			        	String fileType = fileTypes[lastIndex];
			        	if(fileType.equals("yml")){
			        		String toscaFile = fileDir+ls[i];
			        		changeGUIScriptFilePath(toscaFile, scriptPath);
			        	}
	        		}
	        	}
	        	
	        }
			FileWriter scriptFile = new FileWriter(scriptPath);
			scriptFile.write(CommonTool.rephaseString(fileContent));
			scriptFile.close();
			
			
		} catch (Exception e) {
				e.printStackTrace();
				return "Fail: Execption with the input String!";
		}
		
		return "Success: script for GUI is uploaded!";
	}
	
	
	/*
	 *  xmlmsg: executing the provisioning files.  
	 *  Example
	 *  <execute>
	 *      <user>abc</user>
	 *      <pwd>1234</pwd>
	 *      <action>12331323231</action>
	 *  </execute>
	 *  
	 *  note: action is the action number feedback by the operation of upload.
	 */
	@Path("/execute")
	@POST
	@Consumes("text/xml")
	public String executeProvision(String xmlmsg) {
		Document doc = null;
		String action = "";
		String fileDir = "";
		
		Document rDoc = null;
		XMLWriter xw;
	    StringWriter sw = new StringWriter();
	    rDoc = DocumentHelper.createDocument();
		rDoc.setXMLEncoding("UTF-8");
	    Element rDocRootElt = rDoc.addElement("result");
	    Element statusElt = rDocRootElt.addElement("status");
	    Element infoElt = rDocRootElt.addElement("info");
	    
		try{
			doc = DocumentHelper.parseText(xmlmsg);
			Element rootElt = doc.getRootElement();
			if(!rootElt.getName().equals("execute")){
				statusElt.setText("Fail");
				infoElt.setText("The root element should be 'execute' in input string!");
				xw = new XMLWriter(sw);
				xw.write(rDoc);
				xw.close();
				return sw.toString();
			}
			Element userElt = rootElt.element("user");
			Element pwdElt = rootElt.element("pwd");
			String user = userElt.getTextTrim();
			String pwd = pwdElt.getTextTrim();
			if(!AccountAction.validateUser(user, pwd)){
				statusElt.setText("Fail");
				infoElt.setText("Invalid user or password!");
				xw = new XMLWriter(sw);
				xw.write(rDoc);
				xw.close();
				return sw.toString();
			}
			Element actionElt = rootElt.element("action");
			action = actionElt.getTextTrim();
			fileDir = CommonTool.generateRootDir()+"users/"+user+"/files/"+action+"/";
			File toscaDir = new File(fileDir);
			if(!toscaDir.exists()){
				statusElt.setText("Fail");
				infoElt.setText("The topology files with number "+action+" is not provided!");
				xw = new XMLWriter(sw);
				xw.write(rDoc);
				xw.close();
				return sw.toString();
			}
			String [] ls = toscaDir.list();
			String mainFilePath = "";
			for(int i = 0 ; i<ls.length ; i++){
				if(ls[i].lastIndexOf("_main") == (ls[i].length()-5)){
					mainFilePath = fileDir+ls[i];
					break;
				}
			}
			if(mainFilePath.equals("")){
				statusElt.setText("Fail");
				infoElt.setText("Something wrong with the main TOSCA file!");
				xw = new XMLWriter(sw);
				xw.write(rDoc);
				xw.close();
				return sw.toString();
			}
			System.out.println(mainFilePath);
			String jarFilePath = CommonTool.generateRootDir()+"bin/ProvisioningCore.jar";
			String logDir = CommonTool.generateRootDir()+"logs/"+user+"/";
			String ec2ConfPath = CommonTool.generateRootDir()+"users/"+user+"/conf/ec2.conf";
			String geniConfPath = CommonTool.generateRootDir()+"users/"+user+"/conf/geni.conf";
			//String geniConfPath = "";
			String option_ec2 = "ec2="+ec2ConfPath;
			String option_geni = "exogeni="+geniConfPath;
			String option1 = "logDir="+logDir;
			String option2 = "topology="+mainFilePath;
			String cmd = "java -jar "+jarFilePath+" "+option_ec2+" "+option_geni+" "+option1+" "+option2;
			System.out.println(cmd);
			//String shellPath = generateRootDir()+"bin/"+user+""+action+".sh";
			try {

				Process p = Runtime.getRuntime().exec(cmd);
				
				BufferedReader input = new BufferedReader(new InputStreamReader(
			               p.getInputStream()));

			    String line = null;

			    while ((line = input.readLine()) != null)
			    {
			            System.out.println(line);
			    }
			    p.waitFor();
			} catch (IOException | InterruptedException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
				statusElt.setText("Fail");
				infoElt.setText("The jar file cannot be executed!");
				xw = new XMLWriter(sw);
				xw.write(rDoc);
				xw.close();
				return sw.toString();
			} 
		} catch (Exception e) {
				e.printStackTrace();
				statusElt.setText("Fail");
				infoElt.setText("Execption with the input String!");
				xw = new XMLWriter(sw);
				try {
					xw.write(rDoc);
					xw.close();
				} catch (IOException e1) {
					// TODO Auto-generated catch block
					e1.printStackTrace();
				}
				return sw.toString();
		}
		
		statusElt.setText("Success");
		infoElt.setText("Provisioning is executed! Action number: "+action);
		File file = new File(fileDir);
        String[] ls = file.list();
        for(int i = 0 ; i<ls.length ; i++){
        	if(ls[i].contains("_provisioned.yml")){
        		String fileString = CommonTool.getContentFromFile(fileDir+ls[i]);
        		Element fileElt = rDocRootElt.addElement("file");
        		fileElt.setText(fileString);
        	}
        }
	    xw = new XMLWriter(sw);
	    try {
			xw.write(rDoc);
			xw.close();    
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	    
		//return "Success: Provisioning is executed! Action number: "+action;
		return sw.toString();
	}
	
	
	
 
	
	////Change the key file path in the tosca file. 
	////Because the user needs to upload their public key file into the server file system. 
	private void changeKeyFilePath(String toscaFilePath, String newKeyFilePath){
		File toscaFile = new File(toscaFilePath);
		String fileContent = "";
		try {
			BufferedReader in = new BufferedReader(new FileReader(toscaFile));
			String line = "";
			while((line = in.readLine()) != null){
				if(line.contains("publicKeyPath"))
					fileContent += ("publicKeyPath: "+newKeyFilePath+"\n");
				else
					fileContent += (line+"\n");
			}
			in.close();
			
			FileWriter fw = new FileWriter(toscaFilePath, false);
			fw.write(fileContent);
			fw.close();
		} catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
	
	
	private void changeGUIScriptFilePath(String toscaFilePath, String newScriptPath){
		File toscaFile = new File(toscaFilePath);
		String fileContent = "";
		try {
			BufferedReader in = new BufferedReader(new FileReader(toscaFile));
			String line = "";
			while((line = in.readLine()) != null){
				if(line.contains("script")){
					int index = line.indexOf("script:");
					String prefix = line.substring(0, index+7);
					fileContent += (prefix+" "+newScriptPath+"\n");
				}
				else
					fileContent += (line+"\n");
			}
			in.close();
			
			FileWriter fw = new FileWriter(toscaFilePath, false);
			fw.write(fileContent);
			fw.close();
		} catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

}
