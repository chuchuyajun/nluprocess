package coc.convertion.entity;

import org.apache.commons.lang.StringUtils;

public class FactEntity {
	//Example: (_fact John-1x1 rdf:type owl:NamedIndividual)
	private String prefix;//_fact
	private String name;//John-1x1
	private String type;//rdf:type
	private String category;//owl:NamedIndividual
	
	public FactEntity(String fact){
		String[] args = fact.split(" ");
		this.prefix = args[0];
		this.name = args[1];
		this.type = args[2];
		this.category = args[3];
		
		if (this.prefix.startsWith("(")){
			this.prefix = StringUtils.remove(this.prefix, "(");
		}
		
		if (this.category.endsWith(")")){
			this.category = StringUtils.remove(this.category, ")");
		}
	}
	
	public String getPrefix() {
		return prefix;
	}
	public void setPrefix(String prefix) {
		this.prefix = prefix;
	}
	public String getName() {
		return name;
	}
	public void setName(String name) {
		this.name = name;
	}
	public String getType() {
		return type;
	}
	public void setType(String type) {
		this.type = type;
	}
	public String getCategory() {
		return category;
	}
	public void setCategory(String category) {
		this.category = category;
	}

}
