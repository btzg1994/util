package util;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.*;
import java.util.Map.Entry;
import java.util.regex.Pattern;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import java.util.zip.ZipOutputStream;

import javax.servlet.http.HttpServletRequest;

import com.github.gserv.serv.commons.util.JsonMapper;
import org.apache.commons.io.FileUtils;
import org.apache.commons.net.ftp.FTPClient;
import org.apache.commons.net.ftp.FTPFile;
import org.apache.commons.net.ftp.FTPReply;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.github.gserv.serv.commons.encry.HashUtils;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.JsonPrimitive;
import com.jcraft.jsch.Channel;
import com.jcraft.jsch.ChannelSftp;
import com.jcraft.jsch.JSch;
import com.jcraft.jsch.Session;
import com.jcraft.jsch.ChannelSftp.LsEntry;

public class MyTools {
	private static final Logger logger = LoggerFactory.getLogger(MyTools.class);
	
	/**
	 * 根据首条件截取字符串到集合中
	 * @param file
	 * @param start
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
    
    
    
    

	
	/**
	 * request.getScheme() + "://" + request.getServerName() + ":" + request.getServerPort() + request.getContextPath() +"/"
	 * @param request
	 * @return
	 */
	
	public static String gotRealPath(HttpServletRequest request){
		return request.getScheme() + "://" + request.getServerName() + ":" + request.getServerPort() + request.getContextPath() +"/";
	}
	
	
	/**
	 * request.getScheme() + "://" + request.getServerName() + request.getContextPath() +"/"
	 * @param request
	 * @return
	 */
	public static String gotRealPathWithoutPort(HttpServletRequest request){
		return request.getScheme() + "://" + request.getServerName() + request.getContextPath() +"/";
	}
	
	
	/**
	 * 使用指定的字符数组，生成指定数量的指定长度的字符串，且字符串不重复
	 * @param length 要生成字符串的长度
	 * @param count 要生成字符串的个数
	 * @param charArr 构成字符串的字符数组
	 * @return
	 */
	public static HashSet<String> randomStr(int length,long count,char[] charArr){
		//原始长度
		int charArrLength = charArr.length;
		//charArr去重
		HashSet<Character> charSet = new HashSet<Character>();
		for (Character c : charArr) {
			charSet.add(c);
		}
		//获取去重后的长度
		int realLength = charSet.size();
		//检查入参合法性
		double maxCount = Math.pow(realLength, length);
		if(count > maxCount){
			count =(long) maxCount;
		}
		//创建容器
		HashSet<String> strSet = new HashSet<String>();
		//循环生成指定数量的字符串
		for(int i = 0; i < count; i++){
			StringBuilder sb = new StringBuilder();
			for(int j = 0; j < length; j++){
				//随机生成索引
				double tmp = Math.random() * charArrLength;
				int randomIndex = (int)Math.floor(tmp);
				//根据索引取值，并组装字符串
				sb.append(charArr[randomIndex]);
			}
			boolean isNotExist = strSet.add(sb.toString());
			if(!isNotExist){
				//如果已经存在
				i--;
			}
		}
		return strSet;
	}
	
	/**
	 * 获取指定位数的随机数
	 * @param length
	 * @return
	 */
	public static String randomNum(int length){
		StringBuilder sb = new StringBuilder();
		for(int i = 0; i < length; i++){
			double random = Math.random();
			int randomNum = (int) Math.floor(((random*10) % 10));
			sb.append(randomNum);
		}
		return sb.toString();
	}
	
	
	/**
     * 使用Gson拍平json字符串，即当有多层json嵌套时，可以把多层的json拍平为一层
     * @param map
     * @param json
     * @param parentKey
     */
    public static void Json2SimpleMap(Map map, String json, String parentKey){
        JsonElement jsonElement = new JsonParser().parse(json);
        if (jsonElement.isJsonObject()) {
            JsonObject jsonObject = jsonElement.getAsJsonObject();
            parseJson2Map(map,jsonObject,parentKey);
            //传入的还是一个json数组
        }else if (jsonElement.isJsonArray()){
            JsonArray jsonArray = jsonElement.getAsJsonArray();
            Iterator<JsonElement> iterator = jsonArray.iterator();
            while (iterator.hasNext()){
                parseJson2Map(map,iterator.next().getAsJsonObject(),parentKey);
            }
        }else if (jsonElement.isJsonPrimitive()){
            System.out.println("please check the json format!");
        }else if (jsonElement.isJsonNull()){
        }
    }

	
	
	public static void parseJson2Map(Map map,JsonObject jsonObject,String parentKey){
        for (Map.Entry<String, JsonElement> object : jsonObject.entrySet()) {
            String key = object.getKey();
            JsonElement value = object.getValue();
            String fullkey = (null == parentKey || parentKey.trim().equals("")) ? key : parentKey.trim() + "." + key;
            //判断对象的类型，如果是空类型则安装空类型处理
            if (value.isJsonNull()){
            	put(map, fullkey, null);
                continue;
            //如果是JsonObject对象则递归处理
            }else if (value.isJsonObject()){
                parseJson2Map(map,value.getAsJsonObject(),fullkey);
            //如果是JsonArray数组则迭代，然后进行递归
            }else if (value.isJsonArray()){
                JsonArray jsonArray = value.getAsJsonArray();
                Iterator<JsonElement> iterator = jsonArray.iterator();
                while (iterator.hasNext()) {
                    JsonElement jsonElement1 = iterator.next();
                    parseJson2Map(map, jsonElement1.getAsJsonObject(), fullkey);
                }
                continue;
             // 如果是JsonPrimitive对象则获取当中的值,则还需要再次进行判断一下
            }else if (value.isJsonPrimitive()){
                try {
                    JsonElement element = new JsonParser().parse(value.getAsString());
                    if (element.isJsonNull()){
                    	put(map, fullkey, value.getAsString());
                    }else if (element.isJsonObject()) {
                        parseJson2Map(map, element.getAsJsonObject(), fullkey);
                    } else if (element.isJsonPrimitive()) {
                        JsonPrimitive jsonPrimitive = element.getAsJsonPrimitive();

                        if (jsonPrimitive.isNumber()) {
                        	put(map, fullkey, jsonPrimitive.getAsNumber());
                        } else {
                        	put(map, fullkey, jsonPrimitive.getAsString());
                        }
                    } else if (element.isJsonArray()) {
                        JsonArray jsonArray = element.getAsJsonArray();
                        Iterator<JsonElement> iterator = jsonArray.iterator();
                        while (iterator.hasNext()) {
                            parseJson2Map(map, iterator.next().getAsJsonObject(), fullkey);
                        }
                    }
                }catch (Exception e){
                    put(map,fullkey,value.getAsString());
                }
            }
        }
    }
	
	public static final String JSON_ARRAY_SEPERATOR = "_$_";
	
	public static void put(Map map,String key,Object value){
		//put之前检测该key是否已经存在，存在则通过修改命名去重
		int i =1;
		while(map.containsKey(key)){
			if(key.indexOf(JSON_ARRAY_SEPERATOR) != -1){
				key = key.substring(0,key.indexOf(JSON_ARRAY_SEPERATOR));
			}
			key += JSON_ARRAY_SEPERATOR + i;
			i++;
		}
		map.put(key, value);
	}
	
	/**
	 * 获取当前秒数
	 * @return
	 */
	public static String gotSecondsStr(){
		return new Date().getTime()/1000+"";
	}
	
	
	
	
	public static boolean isPhoneNumber(String phone){
		if(phone == null){
			return false;
		}else{
			String regex = "^1[0-9]{10}$";
			return phone.matches(regex);
		}
	}

	public static String fileCompare(File f1,File f2) throws IOException {
        List<String> list1 = FileUtils.readLines(f1,"UTF-8");
        List<String> list2 = FileUtils.readLines(f2, "UTF-8");
        ArrayList<String> list2_cp = new ArrayList<>();
        list2_cp.addAll(list2);

        HashMap<String, Object> map = new HashMap<>();

        list2.removeAll(list1);
        map.put("f2 gt f1",list2);
        list1.removeAll(list2_cp);
        map.put("f1 gt f2",list1);
        return JsonMapper.toJsonString(map);
    }
	
	/**
	 * 文件有重复
	 * @param f1
	 * @param f2
	 * @return
	 * @throws IOException
	 */
	public static String fileCompare2(File f1,File f2)throws IOException {
		HashMap<String,Integer> map1 = new HashMap<String, Integer>();
		HashMap<String,Integer> map2 = new HashMap<String, Integer>();
		HashMap<String,Integer> map3 = new HashMap<String, Integer>();
		
		
		List<String> list1 = FileUtils.readLines(f1,"UTF-8");
		List<String> list2 = FileUtils.readLines(f2, "UTF-8");
		
		for (String string : list1) {
			map1.put(string, (map1.get(string) == null ? 0 : map1.get(string)) +1);
		}
		
		
        for (String string2 : list2) {
        	map2.put(string2, (map2.get(string2) == null ? 0 : map2.get(string2))+1);
		}
        
        for (String string2 : list2) {
        	map1.put(string2, (map1.get(string2) == null ? 0 : map1.get(string2))-1);
        }
        
        
        Set<Entry<String,Integer>> entrySet = map1.entrySet();
        for (Entry<String, Integer> entry : entrySet) {
			String key = entry.getKey();
			Integer value = entry.getValue();
			if(value != 0){
				map3.put(key, value);
			}
		}
        
        return JsonMapper.toJsonString(map3);
        
        
        
        
	}







	
	public static String convertNum2HZ(Integer num){
		switch (num) {
			case 0:
				return "零";
			case 1:
				return "壹";
			case 2:
				return "贰";
			case 3:
				return "叁";
			case 4:
				return "肆";
			case 5:
				return "伍";
			case 6:
				return "陆";
			case 7:
				return "柒";
			case 8:
				return "捌";
			case 9:
				return "玖";
				default: return "零";
		}
	}
	
	
	
	
	
	
	public static void main(String[] args) {
		
		char[] chars = {'3','4','5','6','7','8','9',
						'a','b','c','d','e','f','g','h','i','j','k','m','n','p','q','r','s','t','u','v','w','x','y',
						'A','B','C','D','E','F','G','H','J','K','L','M','N','P','Q','R','S','T','U','V','W','X','Y'};
		int length = 8;
		int count = 68500;
		HashSet<String> randomStr = randomStr(length, count, chars);
		
		File file1 = new File("D:/code.txt");
		File file2 = new File("D:/encryption.txt");
		if(!file1.exists()){
			try {
				file1.createNewFile();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
		if(!file2.exists()){
			try {
				file2.createNewFile();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
		
		String salt = "3d9481b585e3d6391a9646c880be6c7a";
		for (String string : randomStr) {
			try {
				FileUtils.write(file1, string+"\n", true);
				
				String encryption = HashUtils.md5(string+salt);
				FileUtils.write(file2, encryption+"\n", true);
				
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
		

	}
    
    
}
