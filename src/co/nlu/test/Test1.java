package co.nlu.test;

import co.nlu.process.Processor;
import co.nlu.utils.FileUtil;
import co.nlu.utils.Log;

/**
 * @author Min Xia
 * @date Oct 2, 2012
 */
public class Test1 {
	public static void main(String[] a) throws Exception
    {
		Processor proc = new Processor();
		Log.create();
		String text = FileUtil.readFileToString("src/co/nlu/resource/t1.xml");
		proc.process(text);
    }

}
