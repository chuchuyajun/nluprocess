package coc.ruleparser.model;

import java.util.Enumeration;
import java.util.Hashtable;
import java.util.Vector;

public class RHSQuad extends Quad {
	private Vector rhs_owls = new Vector();
	private String classOWLcontent;
	public RHSQuad(String name) {
		super(name);
	}
	
	public RHSQuad(String name,RuleEntity enti) {
		super(name);
		super.entity = enti;
	}
	
	/*
	 * td_gov_owl_RHS
	 */
	public String getRHSQuadName(){
		return this.entity.getRulename()+ "_" + "RHS";
	}
	
	public void setRHSOWLS(){
		Hashtable grammars = this.entity.getLhs().getGrammars();
		
		Enumeration enu = grammars.keys();
		
		Vector tempVec;
		String key;
		while(enu!=null && enu.hasMoreElements()){
			key = (String)enu.nextElement();
			tempVec = (Vector)grammars.get(key);
			
			rhs_owls.add(getIndivOWLContent(tempVec));
		}
	}
	
	public String getClassOWLContent(){
		StringBuffer sb = new StringBuffer();
		
		sb.append(":");
		sb.append(getRHSQuadName());
		sb.append(" rdf:type").append(" owl:Class").append(" .");
		sb.append("\n");
		
		sb.append(":");
		sb.append(getRHSQuadName());
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
		sb.append(this.getRHSQuadName()).append(" .");
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
	
	public Vector getRhs_owls() {
		return rhs_owls;
	}

	@Override
	public void setOWL() {
		// TODO Auto-generated method stub
		this.setRHSOWLS();
	}

	@Override
	public Vector getOWLContent() {
		// TODO Auto-generated method stub
		return this.rhs_owls;
	}

	@Override
	public String getQuadName() {
		// TODO Auto-generated method stub
		return this.getRHSQuadName();
	}

	@Override
	public String getQuadIndivOWLContent() {
		// TODO Auto-generated method stub
		return this.getQuadIndivOWLContent();
	}

	@Override
	public Vector getQuadIndiv_Owls() {
		// TODO Auto-generated method stub
		return this.rhs_owls;
	}

}
