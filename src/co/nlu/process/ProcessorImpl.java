package co.nlu.process;

import java.io.File;
import java.util.Map;

/**
 * @author Min Xia
 * @date Oct 2, 2012
 */
public interface ProcessorImpl {
	
	public void process(String text);
	public void process(File file);

}
