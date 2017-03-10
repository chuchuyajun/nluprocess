package co.nlu.utils;

import java.security.MessageDigest;

/**
 * @author Min Xia
 * @date Oct 31, 2012 1:27:58 PM
 */

public class StringUtil {

	public final static String MD5(String str) {
		String des = "";
		String tmp = null;
		try 
		{
			MessageDigest md5 = MessageDigest.getInstance("md5");
			md5.update(str.getBytes());
			byte[] nStr = md5.digest();
			for (int i = 0; i < nStr.length; i++) {
				tmp = (Integer.toHexString(nStr[i] & 0xFF));
				if (tmp.length() == 1) {
					des += "0";
				}
				des += tmp;
			}
			return des;
		} catch (Exception e) {
			return "";
		}
	}
	
	 public static void main(String[] args) {  
		  System.out.print(StringUtil.MD5("QATest"));  
		 }  

}
