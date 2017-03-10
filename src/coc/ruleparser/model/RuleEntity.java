package coc.ruleparser.model;

import java.util.Vector;

public class RuleEntity {
	private String rulename;
	private RuleLHSEntity lhs ;
	private RuleRHSEntity rhs ;
	
	private Vector patterns = new Vector();//convert to LHS
	private Vector actions = new Vector();//convert to RHS
	
	public RuleEntity(String name, Vector patterns, Vector actions){
		this.rulename = name;
		this.patterns = patterns;
		this.actions = actions;
		
		lhs = new RuleLHSEntity(name,this);
		rhs = new RuleRHSEntity(name,this);
//		lhs = new RuleLHSEntity(rulename);
//		rhs = new RuleRHSEntity(rulename);
//		initialize();
	}
	
	public void initialize(){
		convertLHS();
		convertRHS();
	}
	
	/*
	 * set grammars
	 */
	public void convertLHS(){
		lhs.convert(patterns);
	}
	
	/*
	 * set actions
	 */
	public void convertRHS(){
		rhs.convert(actions);
	}
	
	public String getRulename() {
		return rulename;
	}

	public RuleLHSEntity getLhs() {
		return lhs;
	}

	public RuleRHSEntity getRhs() {
		return rhs;
	}

	public Vector getPatterns() {
		return patterns;
	}

	public Vector getActions() {
		return actions;
	}
}
