package coc.ruleparser;

import java.util.Enumeration;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.Map;
import java.util.Vector;

import coc.Agent;
import coc.agent.engine.Defrule;
import coc.agent.engine.Rete;
import coc.convertion.VelocityEngineUtils;
import coc.ruleparser.model.RuleEntity;

public class ParseManager {
	private static ParseManager _self;
	private Rete rete ;
	private Agent agent ;
	private Map ruleEntityMap = new HashMap();
	
	public Map getRuleEntityMap() {
		return ruleEntityMap;
	}

	private Map QuadsMap = new HashMap();
	
	public ParseManager(){
		agent = new Agent();
		rete = agent.getEngine();
		
		removeOutputRouter();
	}
	
	public static synchronized ParseManager instance(){
		if (_self == null){
			_self = new ParseManager();
		}
		
		return _self;
	}
	

	/*
	 * parse rule file to a Entity map which include collection of Quads
	 */
	private Map parseRule2Entity(){
		Map map = new HashMap();
		RuleEntity entity ;
		Vector names = getRuleNames();
		Defrule rule;
		for(int i = 0 ; i< names.size() ; i++){
			rule = (Defrule)findDefrule((String)names.get(i));
			entity = new RuleEntity((String)names.get(i), rule.getRulePatts(), rule.getRuleActions());
			entity.initialize();

			map.put((String)names.get(i), entity);
			
		}
		
		return map;
	}
	
	public RuleEntity getRuleEntityByName(String rulename){
		return (RuleEntity)(this.ruleEntityMap.get(rulename));
	}
	
	public void removeOutputRouter(){
		rete.removeOutputRouter("t");
		rete.removeOutputRouter("WSTDOUT");
		rete.removeOutputRouter("WSTDERR");
		rete.removeOutputRouter("WSTDOUT_FACT");
	}
	
	public Vector getRuleNames(){
		Hashtable rules = rete.getm_rule();
		Vector vec = new Vector();
		
		Enumeration en = rules.keys();
		
		while(en!=null && en.hasMoreElements()){
			vec.add(en.nextElement().toString());
		}
		
		return vec;
	}
	
	  /**
	   * Find a defrule object with a certain name
	   * @param name 
	   * @return 
	   */
	  public Defrule findDefrule(String name){
	    return (Defrule)(rete.getm_rule().get(name));
	  }

	  public void initialize(){
		  ruleEntityMap = this.parseRule2Entity();
	  }
	  
	  public static void main(String[] args){
		  ParseManager paser = ParseManager.instance();
		  paser.initialize();
		  
		  VelocityEngineUtils.getQuadsOutputOWL();
	  }
}
