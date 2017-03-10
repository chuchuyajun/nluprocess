package coc.ruleparser.model;

import java.util.Hashtable;
import java.util.Vector;

import org.apache.commons.lang.StringUtils;

import coc.agent.engine.Funcall;
import coc.agent.engine.Pattern;

import co.nlu.utils.Log;

public class RuleRHSEntity {
	private Hashtable actions = new Hashtable();
	private RuleEntity parent_entity;
	private Quad rightquad;
	public Quad getRightquad() {
		return rightquad;
	}

	private String rulename;
	private static String[] uselessChars = new String[]{"(", ")", "\"", "assert"};

	public RuleRHSEntity(String name){
		this.rulename = name;
		this.rightquad = new RHSQuad(rulename); 
	}
	
	public RuleRHSEntity(String name,RuleEntity enti){
		this.rulename = name;
		this.parent_entity = enti;
		this.rightquad = new RHSQuad(rulename, enti); 
	}
	/*
	 * Vec : all the RHS content of each rule
	 * almost same handle with LHS, will ignore "assert" additional
	 */
	public void convert(Vector vec){
		String line;
		Funcall p ; 
		for (int i=0 ; i< vec.size() ; i++){
			p = (Funcall)vec.get(i);
			line = p.toString();
			
			if(line!=null && "".equalsIgnoreCase(line)){
				actions.put(line, parseLine(line));
			}
		}
		
		rightquad.setOWL();
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

	public Hashtable getActions() {
		return actions;
	}
	
}
