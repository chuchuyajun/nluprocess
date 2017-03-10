package co.nlu.utils;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.Iterator;
import java.util.Properties;
import org.apache.log4j.Logger;
import org.apache.log4j.PropertyConfigurator;

public class Log {
	private static Logger log;

	static{
		create();
	}
	
	public static void create() {
		create("config/log4j.properties");
	}
	public static void create(String log4jProp) {
		Properties prop = new Properties();

		try {
			prop.load(new FileInputStream(log4jProp));

			String dir ="logs\\";
			if (dir != null && !dir.equals("")) {
				
				for (Iterator iter =prop.keySet().iterator(); iter.hasNext();) {
					String key = (String) iter.next();
					if (key.matches("log4j\\.appender\\..*\\.File")){
						String fileName = prop.getProperty(key);
						fileName = dir + fileName;
						prop.setProperty(key, fileName);
					}
					
				}
			}

			PropertyConfigurator.configure(prop);
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}

		log = Logger.getLogger("CO-NLU");
	}

	public static void debug(Object message) {
		log.debug(message);
	}

	public static void info(Object message) {
		log.info(message);
	}

	public static void warn(Object message) {
		log.warn(message);
	}

	public static void warn(Object message, Exception exception) {
		log.warn(message, exception);
	}

	public static void error(Object message) {
		log.error(message);
	}

	public static void error(Object message, Throwable exception) {
		log.error(message, exception);
	}

	public static void fatal(Object message) {
		log.fatal(message);
	}

	public static void fatal(Object message, Throwable exception) {
		log.fatal(message, exception);
	}

	public static boolean isDebugEnabled() {
		return log.isDebugEnabled();
	}

	public static boolean isInfoEnabled() {
		return log.isInfoEnabled();
	}

	public static void main(String[] args) {
		Log.create();
		Log.debug("aaaa");
	}
}
