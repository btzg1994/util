package com.meiso.www.util;

/**
 * 
 * @author btzg
 * @time 2019年1月7日 下午1:57:47
 */
public class BtzgMathUtil {
		
	/**
	 * 计算两对经纬度之间的距离
	 * @param lat1
	 * @param lon1
	 * @param lat2
	 * @param lon2
	 * @return
	 */
	public static double calDistance(double latitude1, double longitude1, double latitude2, double longitude2) {
		
		//转为弧度制
		double lat1 = rad(latitude1);//纬度
		double lat2 = rad(latitude2);
		
		double lon1 = rad(longitude1);
		double lon2 = rad(longitude2);
		
		double a = lat1 - lat2;// 两点纬度之差
		double b = lon1 - lon2;//两点经度之差
		
		double distance = 2 * Math.asin(Math.sqrt(Math.pow(Math.sin(a / 2), 2) + Math.cos(lat1) * Math.cos(lat2) * Math.pow(Math.sin(b / 2), 2)));//计算两点距离的公式

		distance = distance * 6378137.0;//弧长乘地球半径（半径为米）

		distance = Math.round(distance * 10000d) / 10000d;//精确距离的数值
		return distance;
	}



	private static double rad(double d){
		return d* Math.PI / 180.00;
	}
	
	public static void main(String[] args) {
		double distance = calDistance(34.36173481261341, 108.915142190911, 34.194348, 108.860103);
		System.out.println(distance);
	}
	
		
}
