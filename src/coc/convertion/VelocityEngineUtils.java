package coc.convertion;

import java.io.File;
import java.io.StringWriter;
import java.util.Calendar;
import java.util.Date;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Properties;
import java.util.Vector;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.io.FileUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.velocity.Template;
import org.apache.velocity.VelocityContext;
import org.apache.velocity.app.Velocity;
import org.apache.velocity.app.VelocityEngine;

import co.nlu.models.Sentence;
import co.nlu.utils.StringUtil;
import coc.agent.engine.Fact;
import coc.ruleparser.QuadConvertor;

public class VelocityEngineUtils {
	private static VelocityEngine velocityEngine = new VelocityEngine();
	private static final String regEx = "[#@'*`]";
	static {
		Properties properties = new Properties();
		properties.setProperty(Velocity.FILE_RESOURCE_LOADER_PATH, "Ontology/input/");
		properties.setProperty(Velocity.ENCODING_DEFAULT, "UTF-8");
		properties.setProperty(Velocity.INPUT_ENCODING, "UTF-8");
		properties.setProperty(Velocity.OUTPUT_ENCODING, "UTF-8");
		try {
			velocityEngine.init(properties);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	public static String mergeTemplateIntoString (String templateLocation, Map map) {

		StringWriter writer = null; //output writer
		try {
			Template template = velocityEngine.getTemplate(templateLocation);
			VelocityContext context = new VelocityContext();
			
			for (Iterator<Entry> it=map.entrySet().iterator();it.hasNext();) {
				Entry entry = it.next();
				context.put(entry.getKey().toString(), entry.getValue());
			}
			writer = new StringWriter();
			//generate output
			template.merge(context, writer);

			return writer.toString();
		} catch (Exception e) {
			e.printStackTrace();
		} finally {
			try {
				writer.close();
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
		return null;
	}
	
	/**
	 * remove all special chars which not be inlucded in "" 
	 * @param source
	 * @return
	 */
	public static String convertSpecialChar(String source){
		String[] lines = source.split("\n");
		String line = "";
		
		StringBuffer sb = new StringBuffer();
		
		int first = 0; //the index of first "
		int last = 0; //the index of last "
		String s1 ; //the string before first "
		String s3 ; //the string after last "
		String s2 ; //the string between " " , the special char in which will remain
		
		Pattern p = Pattern.compile(regEx);
		Matcher m;
		
		for(int i = 0 ; i< lines.length ; i++){
			line = lines[i];
			if(!StringUtils.isBlank(line)){
				first = line.indexOf("\"");
				last = line.lastIndexOf("\"");
				
				if(first > -1 && last > first){
					s1 = line.substring(0, first);
					m = p.matcher(s1);
					sb.append(m.replaceAll(""));
					
					s2 = line.substring(first , last+1);
					sb.append(s2);
					
					s3 = line.substring(last+1);
					m = p.matcher(s3);
					sb.append(m.replaceAll(""));
				}else{
					m = p.matcher(line);
					sb.append(m.replaceAll(""));
				}
			}else{
				sb.append("\n");
			}
			
			sb.append("\n");
		}
		return sb.toString();
	}
	
	/**
	 * Get output from template then save to file
	 * @param templateLocation
	 * @param map raw data
	 * @param saveFilePath	Absolute Path
	 * @param encoding
	 * @param needConvert whether need convert special char   
	 * !!!Attention ---> all the comments in OWL maybe filter , Be careful to use  convert!
	 * @return
	 */
	public static boolean mergeTemplateIntoSaveFile (String templateLocation, Map map, String saveFilePath, String encoding, boolean needConvert) {

		try {
			String content = mergeTemplateIntoString(templateLocation, map);
			File file = new File(saveFilePath);
			if (!file.exists()) {
				makeParent(file);
			}
			if(needConvert){
				content = convertSpecialChar(content);
			}
			FileUtils.writeStringToFile(file, content.toString(), "UTF-8");
			return true;
		} catch (Exception e) {
			e.printStackTrace();
		}
		return false;
	}

	public static void makeParent(File file) throws Exception
	{
		File parent = file.getParentFile();
		if(parent==null)
		{
			return;
		}else if(!parent.exists())
		{
			if(!parent.mkdirs())
			{
				throw new Exception("make dir [ "+parent.getAbsolutePath()+" ] fail");
			}
		}
	}
	
	public static String getOWLVersionContent(){
		/*Calendar cal=Calendar.getInstance();
		return String.valueOf(cal.get(Calendar.YEAR)) + "-" 
				+ String.valueOf(cal.get(Calendar.MONTH)+1) + "-"
				+ String.valueOf(cal.get(Calendar.DAY_OF_MONTH));*/
		return "QATest";
	}
	
	public static String getOWLFileNameContent(){
//		return "Ontology" + String.valueOf(System.currentTimeMillis()) + ".owl";
		return "Ontology" + StringUtil.MD5(getOWLVersionContent()) + ".owl";
	}
	
	public static Map getInputMap(Vector vec, Map<Integer, Sentence> sentenceMap){
		FactConvertor convertor = new FactConvertor(vec, sentenceMap);
		Map map = new HashMap();
		
		map.put("version", getOWLVersionContent());
		map.put("doc", getOWLFileNameContent());
		map.put("datatypes", convertor.getDataTypeScript()); //Curernt is #TBD , so do not convert
		map.put("dataprops", convertSpecialChar(convertor.getDataPropScript()));
		map.put("objectprops", convertSpecialChar(convertor.getObjectPropScript()));
		map.put("classes", convertSpecialChar(convertor.getClassScript()));
		map.put("individuals", convertSpecialChar(convertor.getIndividualScript()));

		return map;
	}
	
	public static void getOutputOWL(Vector vec, Map<Integer, Sentence> sentenceMap){
		try {
			Map map = getInputMap(vec, sentenceMap);
			mergeTemplateIntoSaveFile("Ontology.vm", map, "Ontology/output/" + getOWLFileNameContent(), "UTF-8", false);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	
	public static String getOutputOWLContent(Vector vec, Map<Integer, Sentence> sentenceMap){
		String owlcontent = null;
		try {
			Map map = getInputMap(vec, sentenceMap);
			owlcontent =mergeTemplateIntoString("Ontology.vm", map);
			return owlcontent;
		} catch (Exception e) {
			e.printStackTrace();
		}
		return "";
	}
	
	public static Map getQuadInputMap(){
		QuadConvertor convertor = new QuadConvertor();
		Map map = new HashMap();
		
		map.put("version", getOWLVersionContent());
		map.put("doc", getOWLFileNameContent());
		map.put("datatypes", convertor.getDataTypes());
		map.put("dataprops", convertor.getDataPropScript());
		map.put("objectprops", convertor.getObjectPropScript());
		map.put("classes", convertor.getClassScript());
		map.put("individuals", convertor.getIndividualScript());

		return map;
	}
	
	public static void getQuadsOutputOWL(){
		try {
			Map map = getQuadInputMap();
			mergeTemplateIntoSaveFile("Quads-Ontology.vm", map, "Ontology/output/" + "Quads-" + getOWLFileNameContent(), "UTF-8", false);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	
	public static void main(String[] args){
		Map map = new HashMap();
//		map.put("version", getOWLVersionContent());
//		map.put("doc", getOWLFileNameContent());
//		map.put("data", getOWLFileNameContent());
		System.out.println(mergeTemplateIntoString("Ontology.vm", map));
	}
}
