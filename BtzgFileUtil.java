package util;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import java.util.Vector;
import java.util.regex.Pattern;

import org.apache.commons.io.FileUtils;
import org.apache.commons.net.ftp.FTPClient;
import org.apache.commons.net.ftp.FTPFile;
import org.apache.commons.net.ftp.FTPReply;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.jcraft.jsch.Channel;
import com.jcraft.jsch.ChannelSftp;
import com.jcraft.jsch.ChannelSftp.LsEntry;
import com.jhlabs.image.CheckFilter;
import com.jcraft.jsch.JSch;
import com.jcraft.jsch.Session;

public class BtzgFileUtil {
		private static final Logger logger = LoggerFactory.getLogger(BtzgFileUtil.class);
	
		/**
		 * 根据首条件截取字符串到集合中
		 * @param list
		 * @param file
		 * @param start
		 * @param end
		 * @throws Exception 
		 */
	     public static List<String>  readToListByLine( File file, String start) throws Exception{
	    	 List<String> list = new ArrayList<String>();
	    	 List<String> lines = FileUtils.readLines(file,"UTF-8");
	    	 for (String line : lines) {
	    		 if(line.indexOf(start) != -1){// 起始关键字存在，则从其后面开始截取至行末
	    			 int beginIndex = line.indexOf(start) + start.length();
	    			 list.add(line.substring(beginIndex));
	    		 } 
			}
	    	 
	    	 return list;
	     }
	     
	     
	     
	     /**
	 	 * Description: 从FTP服务器下载文件
	 	 * @param host FTP服务器hostname
	 	 * @param port FTP服务器端口
	 	 * @param username FTP登录账号
	 	 * @param password FTP登录密码
	 	 * @param remotePath FTP服务器上的相对路径
	 	 * @param fileName 要下载的文件名
	 	 * @param localPath 下载后保存到本地的路径
	 	 * @return
	 	 */
	 	public static boolean downFileFromFtp(String host, int port,String username, String password, String remotePath,String fileName,String localPath) {
	 		boolean success = false;
	 		FTPClient ftp = new FTPClient();
	 		try {
	 			int reply;
	 			ftp.connect(host, port);
	 			//如果采用默认端口，可以使用ftp.connect(url)的方式直接连接FTP服务器
	 			ftp.login(username, password);//登录
	 			reply = ftp.getReplyCode();
	 			if (!FTPReply.isPositiveCompletion(reply)) {
	 				ftp.disconnect();
	 				return success;
	 			}
	 			ftp.changeWorkingDirectory(remotePath);//转移到FTP服务器目录
	 			FTPFile[] fs = ftp.listFiles();
	 			for(FTPFile ff:fs){
	 				if(ff.getName().equals(fileName)){
	 					File localFile = new File(localPath+"/"+ff.getName());
	 					
	 					OutputStream is = new FileOutputStream(localFile); 
	 					ftp.retrieveFile(ff.getName(), is);
	 					is.close();
	 				}
	 			}
	 			
	 			ftp.logout();
	 			success = true;
	 		} catch (IOException e) {
	 			e.printStackTrace();
	 		} finally {
	 			if (ftp.isConnected()) {
	 				try {
	 					ftp.disconnect();
	 				} catch (IOException ioe) {
	 				}
	 			}
	 		}
	 		return success;
	 	}
	 	
	 	/**
	 	 * Description: 从FTP服务器下载文件
	 	 * @param host FTP服务器hostname
	 	 * @param port FTP服务器端口
	 	 * @param username FTP登录账号
	 	 * @param password FTP登录密码
	 	 * @param remotePath FTP服务器上的相对路径
	 	 * @param fileName 要下载的文件名
	 	 * @param localPath 下载后保存到本地的路径
	 	 * @return
	 	 * @throws Exception 
	 	 */
	 	public static File downFileFromFtpSSH2(String host, int port, String username, String password, String remotePath,String fileName,String localPath) throws Exception {  
	        List<String> list = new ArrayList<String>();  
	        ChannelSftp sftp = null;  
	        Channel channel = null;  
	        Session sshSession = null;  
	        FileOutputStream fos = null;
	        try {  
	            JSch jsch = new JSch();  
	            jsch.getSession(username, host, port);  
	            sshSession = jsch.getSession(username, host, port);  
	            sshSession.setPassword(password);  
	            Properties sshConfig = new Properties();  
	            sshConfig.put("StrictHostKeyChecking", "no");  
	            sshSession.setConfig(sshConfig);  
	            sshSession.connect();  
	            logger.info("Session connected!");  
	            channel = sshSession.openChannel("sftp");  
	            channel.connect();  
	            logger.info("Channel connected!");  
	            sftp = (ChannelSftp) channel;  
	            
	            File localPathFile = new File(localPath);
	            if(!localPathFile.exists()){
	            	localPathFile.mkdirs();
	            }
	            
	            File localFile = new File(localPathFile, fileName); 
	            if(!localFile.exists()){
	            	localFile.createNewFile();
	            }
	            
	            fos = new FileOutputStream(localFile);
	            sftp.cd(remotePath);
	            sftp.get(fileName, fos);
	            logger.info("文件[{}]下载完毕！",fileName);
	            return localFile;  
	        } catch (Exception e) {  
	        	logger.warn("ftp异常：{}",e);
	        	return null;
	        } finally {  
	            closeChannel(sftp);  
	            closeChannel(channel);  
	            closeSession(sshSession);  
	            if(fos != null){
	            	fos.close();
	            }
	        }  
	    }  
	 	
	 	
		/**
	 	 * Description: 从FTP服务器下载文件
	 	 * @param host FTP服务器hostname
	 	 * @param port FTP服务器端口
	 	 * @param username FTP登录账号
	 	 * @param password FTP登录密码
	 	 * @param remotePath FTP服务器上的相对路径
	 	 * @param fileName 要下载的文件名
	 	 * @param localPath 下载后保存到本地的路径
	 	 * @return
	 	 * @throws Exception 
	 	 */
	 	public static List<File> downFilesFromFtpSSH2(String host, int port, String username, String password, String remotePath,String fileNameRegex,String localPath) throws Exception {  
	        List<String> list = new ArrayList<String>();  
	        ChannelSftp sftp = null;  
	        Channel channel = null;  
	        Session sshSession = null;  
	        FileOutputStream fos = null;
	        try {  
	            JSch jsch = new JSch();  
	            jsch.getSession(username, host, port);  
	            sshSession = jsch.getSession(username, host, port);  
	            sshSession.setPassword(password);  
	            Properties sshConfig = new Properties();  
	            sshConfig.put("StrictHostKeyChecking", "no");  
	            sshSession.setConfig(sshConfig);  
	            sshSession.connect();  
	            logger.info("Session connected!");  
	            channel = sshSession.openChannel("sftp");  
	            channel.connect();  
	            logger.info("Channel connected!");  
	            sftp = (ChannelSftp) channel;  
	            
	            File localPathFile = new File(localPath);
	            if(!localPathFile.exists()){
	            	localPathFile.mkdirs();
	            }
	            
	            sftp.cd(remotePath);
	            Vector vector = sftp.ls("*.TXT");
	            ArrayList<File> files = new ArrayList<File>();
	            for (Object item : vector) {
	            	LsEntry entry = (LsEntry) item;
	            	String filename = entry.getFilename();
	            	 
	            	if(Pattern.matches(fileNameRegex, filename)){
	            		File localFile = new File(localPathFile, filename); 
	 		            if(!localFile.exists()){
	 		            	localFile.createNewFile();
	 		            }
	 	            	
	 	            	fos = new FileOutputStream(localFile);
	            		sftp.get(filename, fos);
	            		fos.close();
		            	files.add(localFile);
		            	logger.info("文件[{}]下载完毕！",filename);
	            	}
	            	
				}
	            
	            return files;  
	        } catch (Exception e) {  
	        	logger.warn("ftp异常：{}",e);
	        	return null;
	        } finally {  
	            closeChannel(sftp);  
	            closeChannel(channel);  
	            closeSession(sshSession);  
	        }  
	    }  
	 	
	  
	    private static void closeChannel(Channel channel) {  
	        if (channel != null) {  
	            if (channel.isConnected()) {  
	                channel.disconnect();  
	            }  
	        }  
	    }  
	  
	    private static void closeSession(Session session) {  
	        if (session != null) {  
	            if (session.isConnected()) {  
	                session.disconnect();  
	            }  
	        }  
	    }  
	    
}
