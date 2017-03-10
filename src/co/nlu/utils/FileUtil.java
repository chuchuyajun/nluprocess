package co.nlu.utils;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.channels.FileChannel;
import java.util.ArrayList;

/**
 * @author Stony Zhang
 * @date Jul 25, 2011
 */
public class FileUtil {
	public static boolean delFolder(File dir) {
		boolean success = false;
		if (dir.isDirectory()) {
			String[] children = dir.list();
			for (int i = 0; i < children.length; i++) {
				success = delFolder(new File(dir, children[i]));
			}
		}
		success = dir.delete();
		return success;
	}
	
	public static boolean delFolder(String folder){
		return delFolder(new File(folder));
	}
	
	public static boolean clearFolder(File dir){
		if (dir.isDirectory()) {
			String[] children = dir.list();
			for (int i = 0; i < children.length; i++) {
				delFolder(new File(dir, children[i]));
			}
		}
		return true;
	}
	
	public static boolean clearFolder(String folder){
		return clearFolder(new File(folder));
	}

	public static void copyFile(File in, File out) throws Exception {
		FileChannel sourceChannel = new FileInputStream(in).getChannel();
		FileChannel destinationChannel = new FileOutputStream(out).getChannel();
		sourceChannel.transferTo(0, sourceChannel.size(), destinationChannel);
		// or destinationChannel.transferFrom(sourceChannel, 0,
		// sourceChannel.size());
		sourceChannel.close();
		destinationChannel.close();
	}

	public static void copyFile(File in, String folder) throws Exception {
		if(!folder.endsWith(File.separator)){
			folder+=File.separator;
		}
		File toDir=new File(folder);
		if(!toDir.exists()){
			toDir.mkdirs();
		}
		copyFile(in, new File(folder + in.getName()));
	}
	public static void copyFile(String file, String folder) throws Exception {
		File in=new File(file);
		copyFile(in, folder);
	}
	

	public static void copyFolder(String fromfolder, String toForder)
			throws Exception {
		File inFolder = new File(fromfolder);
		if (!inFolder.isDirectory()) {
			return;
		}
		File[] children = inFolder.listFiles();
		for (int i = 0; i < children.length; i++) {
			File file=children[i];
			if(file.isDirectory()){
				if(!toForder.endsWith(File.separator)){
					toForder+=File.separator;
				}
				copyFolder(file.getAbsolutePath(),toForder+file.getName());
			}else{
				copyFile(children[i], toForder);
			}
		}

	}
	
	public static void save(byte[] in, String folder, String fileName) {
		save(in,folder+fileName);
	}
	public static void save(byte[] in, String fullFileName) {
		try {
			FileOutputStream writer = new FileOutputStream(fullFileName);
			writer.write(in);
			writer.flush();
			writer.close();
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	public static void save(String fullFileName,String content) {
		save(content.getBytes(),fullFileName);
	}

	public static String[] listFilesName(String srcFolder) {
		File inFolder = new File(srcFolder);
		if (!inFolder.isDirectory()) {
			return new String[0];
		}
		
		ArrayList subfiles=new ArrayList();
		
		File[] files = inFolder.listFiles();
		for (int i=0;i<files.length;i++) {
			File subf=files[i];
			if(subf.isDirectory()){
				String[] ssubfils=listFilesName(subf.getPath());
				for (int j=0;j<ssubfils.length;j++) {
					String ssfile =ssubfils[j];
					subfiles.add(subf.getName() + "/" + ssfile);
				}
			}else{
				subfiles.add(subf.getName());
			}
		}
		
		return (String[])subfiles.toArray(new String[subfiles.size()]);
	}
	
	public static String readFileToString(String filePath) throws Exception {
		StringBuffer sbuf = new StringBuffer();
		try {
			InputStream in = new FileInputStream(filePath);
			int size = in.available();
			for (int i = 0; i < size; i++) {
				sbuf.append((char) in.read());
			}
			in.close();
		} catch (Exception e) {
			throw e;
		}
		return sbuf.toString().trim();
	}

	public static String readFileToString(File file) throws Exception {
		StringBuffer sbuf = new StringBuffer();
		try {
			InputStream in = new FileInputStream(file);
			int size = in.available();
			for (int i = 0; i < size; i++) {
				sbuf.append((char) in.read());
			}
			in.close();
		} catch (Exception e) {
			throw e;
		}
		return sbuf.toString().trim();
	}
	
	public static void delete(String file) {
		File f = new File(file);
		if(f.exists() && f.isFile()){
			f.delete();
		}
	}
	
	public static boolean fileExists(String file) {
		File f = new File(file);
		return f.exists();
	}
	
}
