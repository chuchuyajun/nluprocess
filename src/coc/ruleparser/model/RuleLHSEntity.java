package coc.ruleparser.model;

import java.util.Hashtable;
import java.util.Vector;

import org.apache.commons.lang.StringUtils;

import coc.agent.engine.Pattern;

import co.nlu.utils.Log;

public class RuleLHSEntity {
	private Hashtable grammars = new Hashtable();
	private String rulename;
	private RuleEntity parent_entity;
	private Quad leftquad ; 
	public Quad getLeftquad() {
		return leftquad;
	}

	private static String[] uselessChars = new String[]{"(", ")", "\""};

	public RuleLHSEntity(String name){
		this.rulename = name;
		this.leftquad = new LHSQuad(rulename); 
	}
	
	public RuleLHSEntity(String name, RuleEntity enti){
		this.rulename = name;
		this.parent_entity = enti;
		this.leftquad = new LHSQuad(rulename, enti); 
	}
	/*
	 * Vec : all the LHS content of each rule
	 * example String
	 *    (_td attr ?be ?wh)
	 *    (_fact ?gov rdf:type owl:NamedIndividual)
	 * will convert to Map
	 *    ("key1", "_td, attr, be , wh" )
	 *    ("key2", "_fact, gov, rdf:type , owl:NamedIndividual" )
	 */
	public void convert(Vector vec){
		String line;
		Pattern p;
		for (int i=0 ; i< vec.size() ; i++){
			p = (Pattern)vec.get(i);
			line = p.toString();
			if(line!=null && !"".equalsIgnoreCase(line)){
				grammars.put(line, parseLine(line));
			}
		}
		
		leftquad.setOWL();
	}
	
	private Vector parseLine(String line){
		Vector vec = new Vector();
		String[] strs;
		try{
			removeUselessChar(line);
			strs = line.split(" ") ;
			
			for(int i=0 ; i<strs.length ; i++){
				vec.add(strs[i]);
			}
		}catch(Exception ex){
			Log.debug(ex.getMessage());
		}
		return vec;
	}
	
	private String removeUselessChar(String line){
		for(int i=0 ; i<uselessChars.length; i++){
			StringUtils.remove(line, uselessChars[i]);
		}

		return line;
	}
	
	public Hashtable getGrammars() {
		return grammars;
	}
}
