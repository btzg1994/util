package util;

import java.io.File;
import java.io.IOException;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;

import org.apache.commons.io.FileUtils;

import com.github.gserv.serv.commons.encry.HashUtils;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.JsonPrimitive;


public class BtzgStringUtil {
	
	
	
	public static String gotRealPath(HttpServletRequest request){
		return request.getScheme() + "://" + request.getServerName() + ":" + request.getServerPort() + request.getContextPath() +"/";
	}
	
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
                map.put(fullkey,null);
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
                        map.put(fullkey,value.getAsString());
                    }else if (element.isJsonObject()) {
                        parseJson2Map(map, element.getAsJsonObject(), fullkey);
                    } else if (element.isJsonPrimitive()) {
                        JsonPrimitive jsonPrimitive = element.getAsJsonPrimitive();

                        if (jsonPrimitive.isNumber()) {
                            map.put(fullkey, jsonPrimitive.getAsNumber());
                        } else {
                            map.put(fullkey, jsonPrimitive.getAsString());
                        }
                    } else if (element.isJsonArray()) {
                        JsonArray jsonArray = element.getAsJsonArray();
                        Iterator<JsonElement> iterator = jsonArray.iterator();
                        while (iterator.hasNext()) {
                            parseJson2Map(map, iterator.next().getAsJsonObject(), fullkey);
                        }
                    }
                }catch (Exception e){
                    map.put(fullkey,value.getAsString());
                }
            }
        }
    }
	
	
	public static void main(String[] args) {
		
		char[] chars = {'3','4','5','6','7','8','9',
						'a','b','c','d','e','f','g','h','i','j','k','m','n','p','q','r','s','t','u','v','w','x','y',
						'A','B','C','D','E','F','G','H','J','K','L','M','N','P','Q','R','S','T','U','V','W','X','Y'};
		int length = 8;
		int count = 68500;
		HashSet<String> randomStr = BtzgStringUtil.randomStr(length, count, chars);
		
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
