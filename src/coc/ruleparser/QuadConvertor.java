package coc.ruleparser;

import java.util.Iterator;
import java.util.Map;
import java.util.Vector;

import coc.ruleparser.model.RuleEntity;

public class QuadConvertor {
	private Map ruleEntityMap = ParseManager.instance().getRuleEntityMap();
	
	public String getDataTypes(){
		return "";
	}
	
	public String getDataPropScript(){
		return "";
	}
	
	/*
	 *  http://www.cyberobject.com/ontologies/2012/9/Ontology1351714961073.owl#imply
		:imply rdf:type owl:ObjectProperty ;
        rdfs:domain :td_gov_owl_LHS ;
        rdfs:range :td_gov_owl_RHS .
	 */
	public String getObjectPropScript(){
		return getObjectPropImplyScript();
	}
	
	public String getObjectPropImplyScript(){
		StringBuffer sb = new StringBuffer();
		Iterator iter = ruleEntityMap.values().iterator();
		RuleEntity entity;
		
		sb.append("####### imply");
		sb.append("\n");
		
		while(iter!=null && iter.hasNext()){
			entity = (RuleEntity)iter.next();
			sb.append(":imply rdf:type owl:ObjectProperty ;");
			sb.append("\n");
			sb.append("       ");
			sb.append("rdfs:domain :");
			sb.append(entity.getLhs().getLeftquad().getQuadName());
			sb.append(" ;");
			sb.append("       ");
			sb.append("rdfs:range :");
			sb.append(entity.getRhs().getRightquad().getQuadName());
			sb.append(" .");
			sb.append("\n");
		}
		
		return sb.toString();
	}
	/*
	 * 
	 */
	public String getClassScript(){
		StringBuffer sb = new StringBuffer();
		Iterator iter = ruleEntityMap.values().iterator();
		RuleEntity entity;
		while(iter!=null && iter.hasNext()){
			entity = (RuleEntity)iter.next();
			sb.append("###").append(entity.getLhs().getLeftquad().getQuadName());
			sb.append("\n");
			sb.append(entity.getLhs().getLeftquad().getClassOWLContent());
			sb.append("\n");
			
			sb.append("###").append(entity.getRhs().getRightquad().getQuadName());
			sb.append("\n");
			sb.append(entity.getRhs().getRightquad().getClassOWLContent());
			sb.append("\n");
		}
		return sb.toString() ;
	}
	
	/*
	 * 
	 */
	public String getIndividualScript(){
		StringBuffer sb = new StringBuffer();
		Iterator iter = ruleEntityMap.values().iterator();
		RuleEntity entity;
		Vector left_vec;
		Vector right_vec;
		while(iter!=null && iter.hasNext()){
			entity = (RuleEntity)iter.next();
			
			left_vec = entity.getLhs().getLeftquad().getQuadIndiv_Owls();
			sb.append("\n");
			for(int i=0 ; i<left_vec.size() ; i++ ){
				sb.append((String)left_vec.get(i));
				sb.append("\n");
			}
			sb.append("\n");
			
			right_vec = entity.getRhs().getRightquad().getQuadIndiv_Owls();
			sb.append("\n");
			for(int i=0 ; i<right_vec.size() ; i++ ){
				sb.append((String)right_vec.get(i));
				sb.append("\n");
			}
			sb.append("\n");
		}
		
		return sb.toString();
	}
}
