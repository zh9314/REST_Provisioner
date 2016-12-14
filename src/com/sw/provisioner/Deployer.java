package com.sw.provisioner;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.StringWriter;

import javax.ws.rs.Consumes;
import javax.ws.rs.POST;
import javax.ws.rs.Path;

import org.dom4j.Document;
import org.dom4j.DocumentHelper;
import org.dom4j.Element;
import org.dom4j.io.XMLWriter;

@Path("/deploy")
public class Deployer {

	
	/*
	 *  xmlmsg: string for uploading topology files.  
	 *  Example
	 *  <deploy>
	 *      <user>abc</user>
	 *      <pwd>1234</pwd>
	 *      <action>12331323231</action>
	 *  </deploy>
	 *  
	 */
	@Path("/kubernetes")
	@POST
	@Consumes("text/xml")
	public String deployKubernetes(String xmlmsg) {
		Document doc = null;
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
			if(!rootElt.getName().equals("deploy")){
				statusElt.setText("Fail");
				infoElt.setText("The root element should be 'deploy' in input string!");
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
			String action = actionElt.getTextTrim();
			String pyParameterPath = CommonTool.generateRootDir()+"users/"+user+"/files/"+action+"/file_kubernetes";
			String pyPath = CommonTool.generateRootDir()+"bin/deploy/dctl.py";
			
			System.out.println("Execute deploying python script!");
			Process p = Runtime.getRuntime().exec("python "+pyPath+" -i "+pyParameterPath+" -o kubernetes");
		    p.waitFor();
		    String adminConfPath = CommonTool.generateRootDir()+"bin/deploy/admin.conf";
		    File confF = new File(adminConfPath);
		    if(!confF.exists()){
		    	statusElt.setText("Fail");
				infoElt.setText("There is no result file after installing kubernetes");
				xw = new XMLWriter(sw);
				xw.write(rDoc);
				xw.close();
				return sw.toString();
		    }
		    String content = CommonTool.getContentFromFile(adminConfPath);
		    
		    statusElt.setText("Success");
			infoElt.setText("Deploying kubernetes is executed! Action number: "+action);
			Element fileElt = rDocRootElt.addElement("file");
			fileElt.setText(content);
			
			String targetPath = CommonTool.generateRootDir()+"users/"+user+"/files/"+action+"/admin.conf";
			p = Runtime.getRuntime().exec("mv "+adminConfPath+" "+targetPath);
		    p.waitFor();
		    
		}
		catch (Exception e) {
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
		xw = new XMLWriter(sw);
		try {
			xw.write(rDoc);
			xw.close();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return sw.toString();
	}
	

}
