package co.nlu.utils;

public class XmlEscape {
	private static String human[] = { "&", "'", "\"", "<", ">" };

	private static String xmls[] = { "&amp;", "&apos;", "&quot;", "&lt;",
			"&gt;" };

	public static String escapeXml(String in) {
		StringBuffer sbuf = new StringBuffer(in);
		int idx = 0;
		for (int ii = 0; ii < human.length; ii++) {
			idx = -1;
			while ((idx = sbuf.indexOf(human[ii], idx + 1)) >= 0) {
				if (ii == 0) {
					boolean already = false;
					String ss = sbuf.substring(idx, idx
							+ Math.min(6, sbuf.length() - idx));
					for (int jj = 0; jj < xmls.length; jj++) {
						if (ss.startsWith(xmls[jj])) {
							already = true;
							break;
						}
					}
					if (!already) {
						sbuf.replace(idx, idx + 1, xmls[ii]);
					}
				} else {
					sbuf.replace(idx, idx + 1, xmls[ii]);
				}
			}
		}
		return sbuf.toString();
	}

	public static String escapeHuman(String in) {
		StringBuffer sbuf = new StringBuffer(in);
		int idx = -1;
		for (int ii = 0; ii < xmls.length; ii++) {
			while ((idx = sbuf.indexOf(xmls[ii])) >= 0) {
				sbuf.replace(idx, idx + xmls[ii].length(), human[ii]);
			}
		}
		return sbuf.toString();
	}
	 public static String checkValue(String s)
	 {
	  if (s == null)
	   return "";
	  return s;
	 }
	 public static String escapeSpecialCharByString(String input, String start, String end) {
	     StringBuffer sbuf = new StringBuffer(checkValue(input));
	     try {     
	   int i = sbuf.indexOf(start) + start.length();
	   int j = sbuf.indexOf(end);
	   if ((i > 0) && (j > 0) && (j > i)) {
	    String temp = sbuf.substring(i, j);
	    temp = escapeXml(temp);
	    sbuf.replace(i, j, temp);
	    input = sbuf.toString();
	   }
	  } catch (Exception e) {
	  }
	  return input;
	 }

}
