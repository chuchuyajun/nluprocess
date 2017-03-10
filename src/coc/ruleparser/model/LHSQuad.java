package coc.ruleparser.model;

import java.util.Enumeration;
import java.util.Hashtable;
import java.util.Vector;

import org.apache.commons.lang.StringUtils;

public class LHSQuad extends Quad {
	private Vector lhs_indiv_owls = new Vector();
	private String lhs_class_owls ;
	
	public LHSQuad(String rulename) {
		super(rulename);
	}
	
	public LHSQuad(String rulename, RuleEntity enti) {
		super(rulename);
		this.entity = enti;
	}
	
	/*
	 * td_gov_owl_LHS
	 */
	public String getLHSQuadName(){
		return this.entity.getRulename()+ "_" + "LHS";
	}
	
	public void setLHSOWLS(){
		Hashtable grammars = this.entity.getLhs().getGrammars();
		
		Enumeration enu = grammars.keys();
		
		Vector tempVec;
		String key;
		while(enu!=null && enu.hasMoreElements()){
			key = (String)enu.nextElement();
			tempVec = (Vector)grammars.get(key);
			
			lhs_indiv_owls.add(getIndivOWLContent(tempVec));
		}
	}
	
	public String getClassOWLContent(){
		StringBuffer sb = new StringBuffer();
		
		sb.append(":");
		sb.append(getLHSQuadName());
		sb.append(" rdf:type").append(" owl:Class").append(" .");
		sb.append("\n");
		
		sb.append(":");
		sb.append(getLHSQuadName());
		sb.append(" rdfs:subClassOf :Quads .");
		sb.append("\n");
		
		return sb.toString();
	}
	
	public String getIndivOWLContent(Vector vec){
		StringBuffer sb = new StringBuffer();
		sb.append(":");
		sb.append(this.getIndivOWLName(vec));
		sb.append(" ");
		sb.append("rdf:type");
		sb.append(" :");
		sb.append(this.getIndivOWLClassName(vec));
		sb.append(" .");
		sb.append("\n");
		
		sb.append(":");
		sb.append(this.getIndivOWLName(vec));
		sb.append(" ");
		sb.append("rdf:type");
		sb.append(" :");
		sb.append(this.getLHSQuadName()).append(" .");
		sb.append("\n");
		
		sb.append(":");
		sb.append(this.getIndivOWLName(vec));
		sb.append(" ");
		sb.append("rdf:type");
		sb.append(" ");
		sb.append("owl:NamedIndividual").append(" .");
		sb.append("\n");
		return sb.toString();
	}

	


	
	public Vector getLhs_owls() {
		return lhs_indiv_owls;
	}

	@Override
	public void setOWL() {
		// TODO Auto-generated method stub
		this.setLHSOWLS();
	}

	@Override
	public Vector getOWLContent() {
		// TODO Auto-generated method stub
		return this.lhs_indiv_owls;
	}

	@Override
	public String getQuadName() {
		// TODO Auto-generated method stub
		return this.getLHSQuadName();
	}

	@Override
	public String getQuadIndivOWLContent() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Vector getQuadIndiv_Owls() {
		// TODO Auto-generated method stub
		return this.lhs_indiv_owls;
	}
}
