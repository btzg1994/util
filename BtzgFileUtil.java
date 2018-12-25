package util;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;
import java.util.Properties;
import java.util.Vector;
import java.util.regex.Pattern;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import java.util.zip.ZipOutputStream;

import org.apache.commons.io.FileUtils;
import org.apache.commons.net.ftp.FTPClient;
import org.apache.commons.net.ftp.FTPFile;
import org.apache.commons.net.ftp.FTPReply;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.jcraft.jsch.Channel;
import com.jcraft.jsch.ChannelSftp;
import com.jcraft.jsch.ChannelSftp.LsEntry;
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
	     * 压缩成ZIP 方法1
	     * @param srcDir 压缩文件夹路径 
	     * @param out    压缩文件输出流
	     * @param KeepDirStructure  是否保留原来的目录结构,true:保留目录结构; 
	     *                          false:所有文件跑到压缩包根目录下(注意：不保留目录结构可能会出现同名文件,会压缩失败)
	     * @throws RuntimeException 压缩失败会抛出运行时异常
	     */
	    public static void toZip(String srcDir, OutputStream out, boolean KeepDirStructure)
	            throws RuntimeException{
	        
	        long start = System.currentTimeMillis();
	        ZipOutputStream zos = null ;
	        try {
	            zos = new ZipOutputStream(out);
	            File sourceFile = new File(srcDir);
	            compress(sourceFile,zos,sourceFile.getName(),KeepDirStructure);
	            long end = System.currentTimeMillis();
	            System.out.println("压缩完成，耗时：" + (end - start) +" ms");
	        } catch (Exception e) {
	            throw new RuntimeException("zip error from ZipUtils",e);
	        }finally{
	            if(zos != null){
	                try {
	                    zos.close();
	                } catch (IOException e) {
	                    e.printStackTrace();
	                }
	            }
	        }
	        
	    }
	    
	    /**
	     * 压缩成ZIP 方法2
	     * @param srcFiles 需要压缩的文件列表
	     * @param out           压缩文件输出流
	     * @throws RuntimeException 压缩失败会抛出运行时异常
	     */
	    public static void toZip(List<File> srcFiles , OutputStream out)throws RuntimeException {
	        long start = System.currentTimeMillis();
	        ZipOutputStream zos = null ;
	        try {
	            zos = new ZipOutputStream(out);
	            for (File srcFile : srcFiles) {
	                byte[] buf = new byte[2 * 1024];
	                zos.putNextEntry(new ZipEntry(srcFile.getName()));
	                int len;
	                FileInputStream in = new FileInputStream(srcFile);
	                while ((len = in.read(buf)) != -1){
	                    zos.write(buf, 0, len);
	                }
	                zos.closeEntry();
	                in.close();
	            }
	            long end = System.currentTimeMillis();
	            System.out.println("压缩完成，耗时：" + (end - start) +" ms");
	        } catch (Exception e) {
	            throw new RuntimeException("zip error from ZipUtils",e);
	        }finally{
	            if(zos != null){
	                try {
	                    zos.close();
	                } catch (IOException e) {
	                    e.printStackTrace();
	                }
	            }
	        }
	    }
	    
	    
	    /**
	     * 递归压缩方法
	     * @param sourceFile 源文件
	     * @param zos        zip输出流
	     * @param name       压缩后的名称
	     * @param KeepDirStructure  是否保留原来的目录结构,true:保留目录结构; 
	     *                          false:所有文件跑到压缩包根目录下(注意：不保留目录结构可能会出现同名文件,会压缩失败)
	     * @throws Exception
	     */
	    private static void compress(File sourceFile, ZipOutputStream zos, String name,
	            boolean KeepDirStructure) throws Exception{
	        byte[] buf = new byte[2 * 1024];
	        if(sourceFile.isFile()){
	            // 向zip输出流中添加一个zip实体，构造器中name为zip实体的文件的名字
	            zos.putNextEntry(new ZipEntry(name));
	            // copy文件到zip输出流中
	            int len;
	            FileInputStream in = new FileInputStream(sourceFile);
	            while ((len = in.read(buf)) != -1){
	                zos.write(buf, 0, len);
	            }
	            // Complete the entry
	            zos.closeEntry();
	            in.close();
	        } else {
	            File[] listFiles = sourceFile.listFiles();
	            if(listFiles == null || listFiles.length == 0){
	                // 需要保留原来的文件结构时,需要对空文件夹进行处理
	                if(KeepDirStructure){
	                    // 空文件夹的处理
	                    zos.putNextEntry(new ZipEntry(name + "/"));
	                    // 没有文件，不需要文件的copy
	                    zos.closeEntry();
	                }
	                
	            }else {
	                for (File file : listFiles) {
	                    // 判断是否需要保留原来的文件结构
	                    if (KeepDirStructure) {
	                        // 注意：file.getName()前面需要带上父文件夹的名字加一斜杠,
	                        // 不然最后压缩包中就不能保留原来的文件结构,即：所有文件都跑到压缩包根目录下了
	                        compress(file, zos, name + "/" + file.getName(),KeepDirStructure);
	                    } else {
	                        compress(file, zos, file.getName(),KeepDirStructure);
	                    }
	                    
	                }
	            }
	        }
	    }
	    
	    /**
	     * 解压
	     * @param srcfile
	     * @param toDir
	     * @throws Exception
	     */
	    public static void unZip(File srcfile,File toDir) throws Exception{
	         if (!srcfile.getName().endsWith(".zip")) { 
	        	 throw new Exception();
	         }  
	         ZipFile zipFile = new ZipFile(srcfile);//加载zip文件
	         
	         System.out.println(zipFile.getName()+" 共有文件数 "+zipFile.size());//打印zip文件包含的文件数  文件夹也包括在内  
	         ZipEntry zipentry=null;//声明一个zip文件包含文件单一的实体对象  
	         
	         Enumeration<?> e = zipFile.entries();//返回 ZIP文件条目的枚举。  
	         while (e.hasMoreElements()) {//测试此枚举是否包含更多的元素。  
	           zipentry  = (ZipEntry) e.nextElement();  
	           if (zipentry.isDirectory()) {//是否为文件夹而非文件  
	                 
	               File file = new File(toDir,zipentry.getName());  
	               file.mkdir();//创建文件夹                  
	           }else{  
	               InputStream input =zipFile.getInputStream(zipentry);//得到当前文件的文件流  
	               File f = new File(toDir , zipentry.getName());//创建当前文件  
	               FileOutputStream fout = new FileOutputStream(f);//声明一个输出流  
	               byte [] bytes = new byte[1024];//每次读1kb  
	               int len = 0;
	               while ((len = input.read(bytes)) != -1) {  
	                   fout.write(bytes, 0, len);
	               }  
	               input.close();  
	               fout.close();  
	               System.out.println(zipentry.getName()+"解压成功...");  
	           }  
	         }  
	         zipFile.close();  
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
