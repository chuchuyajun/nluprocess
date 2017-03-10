package coc.ruleparser.model;

import java.util.Vector;

import org.apache.commons.lang.StringUtils;

import coc.ruleparser.ParseManager;

public abstract class Quad {
	protected String rulename;
	
	public String getRulename() {
		return rulename;
	}

	protected RuleEntity entity;
	
	protected static String[] masks = new String[]{"_fact", "rdf:type", "owl:NamedIndividual"};
	protected static String[] unmasks = new String[]{"TTL", "rdf", "indv"};
	
	public Quad(String rulename){
		this.rulename = rulename;
//		this.entity = ParseManager.instance().getRuleEntityByName(rulename);
	}
	
	public abstract void setOWL();
	
	public abstract String getQuadName();
	
	public abstract Vector getQuadIndiv_Owls();
	
	public abstract String getQuadIndivOWLContent();
	
	public abstract Vector getOWLContent();
	
	public abstract String getClassOWLContent();
	
	/*
	 * v_rdf_indiv
	 */
	public String getIndivOWLName(Vector vec){
		StringBuffer sb = new StringBuffer();
		String word = null;
		for(int i=0 ; i< vec.size() ; i++){
			word = vec.get(i).toString().trim();
			//Remove '(' and ')' from the first/last word
//			if(i==0){
				word = StringUtils.remove(word, '(');
				word = StringUtils.remove(word, ')');
				
				word = removeRegexChar(word);
//			}else if( i == vec.size()-1){
//				word = StringUtils.remove(word, ')');
//			}
			if(!isUselssKeyword(word)){
				if(!isNLUkeyword(word)){
					sb.append(ummaskWord(word));
				}else{
					sb.append(ummaskWord(word).toLowerCase());
				}
				
				if(i != (vec.size() - 1)){
					sb.append("_");
				}
			}
		}
		return sb.toString();
	}
	
	/*
	 * there maybe exist some regex char " ^ * |
	 * replace them with _
	 */
	public String removeRegexChar(String str){
		String s = str;
		String[] chars = new String[]{"\"","|",":", "-", "^", "*" };
		
		for (int i=0 ; i < chars.length ; i++){
			s = StringUtils.replace(s, chars[i], "_");
		}
		return s;
	}
	
	public boolean isUselssKeyword(String word){
		if(word == null || word.trim().length() ==0){
			return false;
		}else{
			if("assert".equalsIgnoreCase(word)){
				return true;
			}
		}
		
		return false;
	}
	

	
	/*
	 * TTL , TD , LEMA, QUADS
	 */
	public String getIndivOWLClassName(Vector vec){
		String word;
		for(int i=0 ; i<vec.size() ; i++){
			word = vec.get(i).toString().trim();
			word = StringUtils.remove(word, '(');
			word = StringUtils.remove(word, ')');
			if(isNLUkeyword(word)){
				return ummaskWord(word);
			}
		}
		
		return "BASIC";
	}
	
	public String ummaskWord(String word){
		if(word == null){
			return "";
		}else{
			if(word.equalsIgnoreCase("_fact")){
				return "TTL";
			}else if(word.equalsIgnoreCase("rdf:type")){
				return "rdf";
			}else if(word.equalsIgnoreCase("_td")){
				return "TD";
			}else if(word.equalsIgnoreCase("_lema")){
				return "LEMA";
			}else if(word.equalsIgnoreCase("_pos")){
				return "POS";
			}else if(word.equalsIgnoreCase("_ner")){
				return "NER";
			}else if(word.equalsIgnoreCase("owl:NamedIndividual")){
				return "indiv";
			}else if(word.startsWith("?")){
				return "var";
			}else{
				return word;
			}

		}
	}
	
	public boolean isNLUkeyword(String word){
		if(word.equalsIgnoreCase("_td") || word.equalsIgnoreCase("_lema")
				|| word.equalsIgnoreCase("_pos")
				|| word.equalsIgnoreCase("_fact")
				|| word.equalsIgnoreCase("_ner")){
			return true;
		}
		return false;
	}
}
