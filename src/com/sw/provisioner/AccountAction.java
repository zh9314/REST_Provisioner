package com.sw.provisioner;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.security.MessageDigest;

import javax.ws.rs.Consumes;
import javax.ws.rs.POST;
import javax.ws.rs.Path;

import org.dom4j.Document;
import org.dom4j.DocumentException;
import org.dom4j.DocumentHelper;
import org.dom4j.Element;

@Path("/account")
public class AccountAction {
	
	
	/*
	 *  xmlmsg: string for registering. There is "user.info" in the root dir. 
	 *  Example
	 *  <register>
	 *      <user>abc</user>
	 *      <pwd>1234</pwd>
	 *  </register>
	 */
	@Path("/register")
	@POST
	@Consumes("text/xml")
	public String register(String xmlmsg) {
		Document doc = null;
		String user = "";
		try{
			doc = DocumentHelper.parseText(xmlmsg);
			Element rootElt = doc.getRootElement();
			if(!rootElt.getName().equals("register"))
				return "Fail: The root element should be 'register' in input string!";
			Element userElt = rootElt.element("user");
			Element pwdElt = rootElt.element("pwd");
			user = userElt.getTextTrim();
			if(user.equals("") || user.contains(" "))
				return "Fail: User name "+user+" is not valid!";
			String check = findPassword(user);
			if(!check.equals("null"))
				return "Fail: The user "+user+" already exists!";
			String pwd = pwdElt.getTextTrim();
			String infoPath = CommonTool.generateRootDir()+"database/"+CommonTool.userInfo;
			FileWriter fw = new FileWriter(infoPath, true);
			String pwd_md5 = toMD5(pwd);
			fw.write(user+" "+pwd_md5+"\n");
			fw.close();
			setupUserfDir(user);
		} catch (Exception e) {
				e.printStackTrace();
				return "Fail: Execption with the input String!";
		}
		return "Success: User "+user+" is registered!";
	}
	
	
	/* 
	 *  Example
	 *  <configure>
	 *  	<user>abc</user>
	 *      <pwd>1234</pwd>
	 *      <keyid>abc</keyid>
	 *      <key>1234</key>
	 *      <loginKey domain_name="Virginia" >sddssdf</loginKey>
	 *      <loginKey domain_name="California" >sddsssdfdf</loginKey>
	 *  </configure>
	 *  
	 *  valid domain names are only:
	 *  Virginia, Ohio, California, Oregon, Ireland, Frankfurt, Tokyo, Seoul, Singapore, Sydney
	 *  Mumbai, SanPaulo 
	 */
	@Path("/configure/ec2")
	@POST
	@Consumes("text/xml")
	public String configureEC2(String xmlmsg) {
		Document doc = null;
		String user = "";
		try{
			doc = DocumentHelper.parseText(xmlmsg);
			Element rootElt = doc.getRootElement();
			if(!rootElt.getName().equals("configure"))
				return "Fail: The root element should be 'configure' in input string!";
			Element userElt = rootElt.element("user");
			Element pwdElt = rootElt.element("pwd");
			user = userElt.getTextTrim();
			String pwd = pwdElt.getTextTrim();
			if(!AccountAction.validateUser(user, pwd))
				return "Fail: Invalid user or password!";
			Element keyidElt = rootElt.element("keyid");
			Element keyElt = rootElt.element("key");
			String keyid = keyidElt.getTextTrim();
			String key = keyElt.getTextTrim();
			if(keyid.equals("") || keyid.contains(" "))
				return "Fail: AWSAccessKeyId "+keyid+" is not valid!";
			if(key.equals("") || key.contains(" "))
				return "Fail: AWSSecretKey "+key+" is not valid!";
			String rootDir = CommonTool.generateRootDir();
			String userDir = CommonTool.generateRootDir()+"users/"+user+"/";
			String confPath = userDir+"conf/ec2.conf";
			FileWriter fw = new FileWriter(confPath, false);
			fw.write("AWSAccessKeyId="+keyid+"\n");
			fw.write("AWSSecretKey="+key+"\n");
			fw.write("KeyDir="+userDir+"certs/EC2certs/\n");
			fw.write("DatabaseDir="+rootDir+"database/EC2/\n");
			fw.write("LogsDir="+rootDir+"logs/"+user+"/\n");
			Iterator iter = rootElt.elementIterator("loginKey"); 
			ArrayList<String> domains = new ArrayList<String>();
			while (iter.hasNext()) {
				Element loginKeyElt = (Element) iter.next();
				String domain = loginKeyElt.attributeValue("domain_name");
				String domainFull = validateEC2Domain(domain);
				if(domainFull.equals("null")){
					System.out.println("Error: Input domain name "+domain+" for EC2 is not validate!");  
					continue;
				}
				domains.add(domainFull);
				String loginKey = loginKeyElt.getTextTrim();
				String keyFilePath = userDir+"certs/EC2certs/"+domain+".pem";
				FileWriter keyFile = new FileWriter(keyFilePath);
				keyFile.write(CommonTool.rephaseString(loginKey));
				keyFile.close();
				try {
					Process p = Runtime.getRuntime().exec("chmod 400 "+keyFilePath);
					p.waitFor();
				} catch (IOException | InterruptedException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				} 
			}
			
			if(domains.size() == 0){
				fw.close();
				return "Warning: There is no valid domain!";
			}
			String supportDomains = "";
			for(int i = 0 ; i<domains.size() ; i++){
				supportDomains += domains.get(i);
				if(i < (domains.size()-1))
					supportDomains += ", ";
			}
			fw.write("SupportDomains="+supportDomains+"\n");
			fw.close();
			
		} catch (Exception e) {
				e.printStackTrace();
				return "Fail: Execption with the input String!";
		}
		return "Success: EC2 of "+user+" is configured!";
	}
	
	
	/* 
	 *  Example
	 *  <configure>
	 *  	<user>abc</user>
	 *      <pwd>1234</pwd>
	 *      <geniKey>1234\nfg\n</geniKey>
	 *      <geniKeyAlias>zhgeni</geniKeyAlias>
	 *      <geniKeyPass>123456</geniKeyPass>
	 *      <loginPubKey>1234\nfg\nss\n</loginPubKey>
	 *      <loginPriKey>1234\nfg\nsdfs\n</loginPriKey>
	 *  </configure>
	 *  
	 *  valid domain names are only:
	 *  Virginia, Ohio, California, Oregon, Ireland, Frankfurt, Tokyo, Seoul, Singapore, Sydney
	 *  Mumbai, SanPaulo 
	 */
	@Path("/configure/geni")
	@POST
	@Consumes("text/xml")
	public String configureGENI(String xmlmsg) {
		Document doc = null;
		String user = "";
		try{
			doc = DocumentHelper.parseText(xmlmsg);
			Element rootElt = doc.getRootElement();
			if(!rootElt.getName().equals("configure"))
				return "Fail: The root element should be 'configure' in input string!";
			Element userElt = rootElt.element("user");
			Element pwdElt = rootElt.element("pwd");
			user = userElt.getTextTrim();
			String pwd = pwdElt.getTextTrim();
			if(!AccountAction.validateUser(user, pwd))
				return "Fail: Invalid user or password!";
			String rootDir = CommonTool.generateRootDir();
			String userDir = CommonTool.generateRootDir()+"users/"+user+"/";
			
			Element keyElt = rootElt.element("geniKey");
			String key = keyElt.getText();
			String geniKeyPath = userDir+"certs/ExoGENIcerts/user.jks";
			FileWriter keyFw = new FileWriter(geniKeyPath, false);
			keyFw.write(CommonTool.rephaseString(key));
			keyFw.close();
			
			Element pubKeyElt = rootElt.element("loginPubKey");
			String pubKey = pubKeyElt.getText();
			String pubKeyPath = userDir+"certs/ExoGENIcerts/id_dsa.pub";
			FileWriter pubKeyFw = new FileWriter(pubKeyPath, false);
			pubKeyFw.write(CommonTool.rephaseString(pubKey));
			pubKeyFw.close();
			
			Element priKeyElt = rootElt.element("loginPriKey");
			String priKey = priKeyElt.getText();
			String priKeyPath = userDir+"certs/ExoGENIcerts/id_dsa";
			FileWriter priKeyFw = new FileWriter(priKeyPath, false);
			priKeyFw.write(CommonTool.rephaseString(priKey));
			priKeyFw.close();
			

			Element keyAliasElt = rootElt.element("geniKeyAlias");
			String keyAlias = keyAliasElt.getTextTrim();
			Element keyPassElt = rootElt.element("geniKeyPass");
			String keyPass = keyPassElt.getTextTrim();
			
			String confPath = userDir+"conf/geni.conf";
			FileWriter fw = new FileWriter(confPath, false);
			fw.write("UserKeyPath="+geniKeyPath+"\n");
			fw.write("KeyAlias="+keyAlias+"\n");
			fw.write("KeyPassword="+keyPass+"\n");
			fw.write("SshPubKeyPath="+pubKeyPath+"\n");
			fw.write("SshPriKeyPath="+priKeyPath+"\n");
			fw.write("APIURL=https://geni.renci.org:11443/orca/xmlrpc\n");
			fw.write("DatabaseDir="+rootDir+"database/ExoGENI/\n");
			fw.write("LogsDir="+rootDir+"logs/"+user+"/\n");
			
			fw.close();
			
		} catch (Exception e) {
				e.printStackTrace();
				return "Fail: Execption with the input String!";
		}
		return "Success: ExoGENI of "+user+" is configured!";
	}
	
	
	/*
	 *  Note: Do not need now!
	 *  xmlmsg: string for registering. 
	 *  Example
	 *  <login>
	 *      <user>abc</user>
	 *      <pwd>1234</pwd>
	 *  </login>
	 */
	@Path("/login")
	@POST
	@Consumes("text/xml")
	public String login(String xmlmsg) {
		Document doc = null;
		try{
			doc = DocumentHelper.parseText(xmlmsg);
			Element rootElt = doc.getRootElement();
			Element userElt = rootElt.element("user");
			Element pwdElt = rootElt.element("pwd");
			
		} catch (DocumentException e) {
			e.printStackTrace();
		} catch (Exception e) {
				e.printStackTrace();
		}
		return "register";
	}
	
	private String validateEC2Domain(String domain){
		String domainPath = CommonTool.generateRootDir()+"database/EC2/domains";
		File domainFile = new File(domainPath);
		try {
			BufferedReader in = new BufferedReader(new FileReader(domainFile));
			String line = null;
			while((line = in.readLine()) != null){
				String [] DM = line.split("&&");
				if(DM[0].equals(domain)){
					in.close();
					return DM[1];
				}
			}
			in.close();
		}catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} 
		return "null";
	}
	

	
	private String setupUserfDir(String userName){
		String dirName = CommonTool.generateRootDir()+"users/"+userName+"/";
		String logDir = CommonTool.generateRootDir()+"logs/"+userName+"/";
		Process p;
		try {
			p = Runtime.getRuntime().exec("mkdir "+dirName);
			p.waitFor();
			p = Runtime.getRuntime().exec("mkdir "+dirName+"certs/");
			p.waitFor();
			p = Runtime.getRuntime().exec("mkdir "+dirName+"conf/");
			p.waitFor();
			p = Runtime.getRuntime().exec("mkdir "+dirName+"files/");
			p.waitFor();
			p = Runtime.getRuntime().exec("mkdir "+dirName+"certs/EC2certs/");
			p.waitFor();
			p = Runtime.getRuntime().exec("mkdir "+dirName+"certs/ExoGENIcerts/");
			p.waitFor();
			p = Runtime.getRuntime().exec("mkdir "+logDir);
			p.waitFor();
		} catch (IOException | InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} 
		return dirName;
		
	}
	
	
	
	///if the user does not exist, return null
	private String findPassword(String user){
		String infoPath = CommonTool.generateRootDir()+"database/"+CommonTool.userInfo;
		File userFile = new File(infoPath);
		try {
			BufferedReader in = new BufferedReader(new FileReader(userFile));
			String line = null;
			while((line = in.readLine()) != null){
				String [] up = line.split(" ");
				String curuser = up[0];
				if(curuser.equals(user)){
					in.close();
					return up[1];
				}
			}
			in.close();
		}catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return "null";
	}
	
	public static String toMD5(String plainText) {
		StringBuffer buf = new StringBuffer("");
        try {
             MessageDigest md = MessageDigest.getInstance("MD5");    
             md.update(plainText.getBytes());
             byte b[] = md.digest();
             int i;
             
             for (int offset = 0; offset < b.length; offset++) {
                  i = b[offset];
                  if (i < 0)
                      i += 256;
                  if (i < 16)
                      buf.append("0");
                  buf.append(Integer.toHexString(i));
             }
        } 
        catch (Exception e) {
            e.printStackTrace();
        }
        return buf.toString();
   }
	
	public static boolean validateUser(String user, String pwd){
		String pwd_md5 = toMD5(pwd);
		String infoPath = CommonTool.generateRootDir()+"database/"+CommonTool.userInfo;
		File userFile = new File(infoPath);
		try {
			BufferedReader in = new BufferedReader(new FileReader(userFile));
			String line = null;
			while((line = in.readLine()) != null){
				String [] up = line.split(" ");
				String curuser = up[0];
				String curpwd_md5 = up[1];
				if(curuser.equals(user)){
					in.close();
					if(curpwd_md5.equals(pwd_md5))
						return true;
					else
						return false;
				}
			}
			in.close();
		}catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return false;
	}
	

}
