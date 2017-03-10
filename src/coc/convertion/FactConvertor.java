package coc.convertion;

import java.util.Enumeration;
import java.util.Map;
import java.util.Vector;

import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang.StringUtils;

import co.nlu.models.Sentence;
import coc.agent.engine.Fact;
import coc.convertion.entity.FactEntity;

public class FactConvertor {
	private static final String FACT_PREFIX = "_fact";
	
	private static final String DATA_PROPS_SUFFIX = "owl:DatatypeProperty";
	
	private static final String OBJECT_PROPS_SUFFIX = "owl:ObjectProperty";
	
	private static final String CLASS_SUFFIX = "owl:Class";
	
	private static final String INDIVIDUAL_SUFFIX = "owl:NamedIndividual";
	
	private static final String DATATYPES_SUFFIX = "TBD";//TODO
	
	private Vector<Fact> enumer = null;
	private Vector<FactEntity> allfacts = null;
	private Map<Integer, Sentence> sentenceMap = null;
	public FactConvertor(Vector vec, Map<Integer, Sentence> sentences){
		super();
		enumer = vec;
		sentenceMap = sentences;
		this.allfacts = getAllFactsForConvert(vec);
	}
	
	//get all fact which is needed for convert to OWL
	public Vector<FactEntity> getAllFactsForConvert(Vector v){
		Vector<FactEntity> vec = new Vector<FactEntity>();
		
		Fact mfact = null;
		for (int i=0 ; i < v.size() ; i++) {
			mfact = (Fact) v.get(i);
			if(FACT_PREFIX.equalsIgnoreCase(mfact.getName())){
				vec.add(new FactEntity(mfact.toStringWithParens()));
			}
		}
		
		return vec;
	}
	
	//owl:DatatypeProperty
	public synchronized Vector<FactEntity> getAllDataTypesFacts(){
		Vector vec = new Vector();
		FactEntity entity;
		for (int i = 0 ; i <allfacts.size() ; i++){
			entity = allfacts.get(i);
			if(entity!=null && DATATYPES_SUFFIX.equalsIgnoreCase(entity.getCategory())){
				vec.add(entity);
			}
		}
		
		return vec;
	}
	
	//owl:DatatypeProperty  
	public synchronized Vector<FactEntity> getDataTypePropFacts(){
		Vector vec = new Vector();
		FactEntity entity;
		for (int i = 0 ; i <allfacts.size() ; i++){
			entity = allfacts.get(i);
			if(entity!=null && DATA_PROPS_SUFFIX.equalsIgnoreCase(entity.getCategory())){
				vec.add(entity);
			}
		}
		
		return vec;
	}
	
//	owl:ObjectProperty 
	public synchronized Vector<FactEntity> getObjectPropFacts(){
		Vector vec = new Vector();
		FactEntity entity;
		for (int i = 0 ; i <allfacts.size() ; i++){
			entity = allfacts.get(i);
			if(entity!=null && OBJECT_PROPS_SUFFIX.equalsIgnoreCase(entity.getCategory())){
				vec.add(entity);
			}
		}
		
		return vec;
	}
	
//	owl:Class 
	public synchronized Vector<FactEntity> getClassFacts(){
		Vector vec = new Vector();
		FactEntity entity;
		for (int i = 0 ; i <allfacts.size() ; i++){
			entity = allfacts.get(i);
			if(entity!=null && CLASS_SUFFIX.equalsIgnoreCase(entity.getCategory())){
				vec.add(entity);
			}
		}
		
		return vec;
	}
	
//	owl:NamedIndividual 
	public synchronized Vector<FactEntity> getNamedIndividualFacts(){
		Vector vec = new Vector();
		FactEntity entity;
		for (int i = 0 ; i <allfacts.size() ; i++){
			entity = allfacts.get(i);
			if(entity!=null && INDIVIDUAL_SUFFIX.equalsIgnoreCase(entity.getCategory())){
				vec.add(entity);
			}
		}
		
		return vec;
	}
	
	public String verifySlot(String str){
		if(str.indexOf(":")<0){
			if(OBJECT_PROPS_SUFFIX.equals(str)){
				str = "owl:" + str;
			}else if(this.DATA_PROPS_SUFFIX.equals(str)){
				str = "owl:" + str;
			}else{
				str = ":" + str ;
			}
			
			return str;
		}
		
		return str;
	}
	
	public String getDataTypeScript(){
		return "#TBD";
	}
	
	//Convert ObjectProperties owl
	//(_fact hasPERSON rdf:type ObjectProperty)
	//(_fact hasNUMBER rdf:type ObjectProperty)
	private static final String Object_Prop_Namespace = "<http://attempto.ifi.uzh.ch/ace_lexicon#TV_pl>";
	
	public String getObjectPropScript(){
		StringBuffer sb = new StringBuffer();
		Vector<FactEntity> vec = this.getObjectPropFacts();
		
		FactEntity current;
		for (int i = 0 ; i < vec.size() ; i++){
			current = (FactEntity)vec.get(i);
			sb.append(verifySlot(current.getName()));
			sb.append(" ");
			sb.append(verifySlot(current.getType()));
			sb.append(" ");
			sb.append(verifySlot(current.getCategory()));
			sb.append(" ");
			sb.append(".");
			sb.append("\n");
		}
		return sb.toString();
	}
	
	//Convert DataProperties owl
	private static final String Data_Prop_Namespace = "<http://attempto.ifi.uzh.ch/ace_lexicon#TV_pl>";
	
	public String getDataPropScript(){
		StringBuffer sb = new StringBuffer();
		Vector<FactEntity> vec = this.getDataTypePropFacts();
		
		FactEntity current;
		for (int i = 0 ; i < vec.size() ; i++){
			current = (FactEntity)vec.get(i);
			sb.append(verifySlot(current.getName()));
			sb.append(" ");
			sb.append(verifySlot(current.getType()));
			sb.append(" ");
			sb.append(verifySlot(current.getCategory()));
			sb.append(" ");
			sb.append(".");
			sb.append("\n");
		}
		return sb.toString();
	}
	
	//Convert Class owl
	private static final String Class_Namespace = "<http://attempto.ifi.uzh.ch/ace_lexicon#CN_sg>";
	
	public String getClassScript(){
		StringBuffer sb = new StringBuffer();
		Vector<FactEntity> vec = this.getClassFacts();
		
		FactEntity current;
		for (int i = 0 ; i < vec.size() ; i++){
			current = (FactEntity)vec.get(i);
			sb.append(verifySlot(current.getName()));
			sb.append(" ");
			sb.append(verifySlot(current.getType()));
			sb.append(" ");
			sb.append(verifySlot(current.getCategory()));
			sb.append(" ");
			sb.append(".");
			sb.append("\n");
		}
		return sb.toString();
	}
	
	/*
	 * (_fact PERSON rdf:type owl:Class)
	 * (_fact NUMBER rdf:type owl:Class)
	 * return PERSON , NUMBER
	 */
	private Vector getAllClassName(){
		Vector names = new Vector();
		
		FactEntity factentity;
		Vector v = this.getClassFacts();
		for (int i=0 ; i<v.size() ; i++){
			factentity = (FactEntity)v.get(i);
			names.add(factentity.getName());
		}
		
		return names;
	}
	
	/*
	 * (_fact PERSON rdf:type owl:Class)
	 * (_fact John-1x1 rdf:type PERSON)
	 * return John-1x1
	 */
	private Vector getAllIndividualBelong2Class(String classname){
		Vector v = this.getAllFactsForConvert(enumer);
		CollectionUtils.removeAll(v, this.getAllDataTypesFacts());
		CollectionUtils.removeAll(v, this.getClassFacts());
		CollectionUtils.removeAll(v, this.getDataTypePropFacts());
//		CollectionUtils.removeAll(v, this.getNamedIndividualFacts());
		CollectionUtils.removeAll(v, this.getObjectPropFacts());
		
		Vector<String> indivs = new Vector<String>();
		FactEntity factvalue;
		for(int i=0 ; i< v.size() ; i++){
			factvalue = (FactEntity)v.get(i);
			if(classname.equalsIgnoreCase(factvalue.getCategory())){
				indivs.add(factvalue.getName());
			}
		}
		
		return indivs;
	}
	
	/*
	 * (_fact 30-1x3 rdf:type NUMBER)
	 * return NUMBER
	 */
	private String getClassNameByIndividual(String individual){
		Vector v = this.getAllFactsForConvert(enumer);
		
		CollectionUtils.removeAll(v, this.getAllDataTypesFacts());
		CollectionUtils.removeAll(v, this.getClassFacts());
		CollectionUtils.removeAll(v, this.getDataTypePropFacts());
		CollectionUtils.removeAll(v, this.getNamedIndividualFacts());
		CollectionUtils.removeAll(v, this.getObjectPropFacts());
		
		FactEntity factvalue;
		for(int i=0 ; i< v.size() ; i++){
			factvalue = (FactEntity)v.get(i);
			if(individual.equalsIgnoreCase(factvalue.getName())){
				return factvalue.getCategory();
			}
		}
		
		return "";
	}
	
	/*
	 * (_fact John-1x1 rdf:type owl:NamedIndividual)
	 * return John-1x1
	 */
	private Vector<String> getAllNamedIndividuals(){
		return getAllIndividualBelong2Class(INDIVIDUAL_SUFFIX);
	}
	
	private Vector<FactEntity> getAllFactByEntityName(String name){
		Vector<FactEntity> vec = new Vector<FactEntity>();
		FactEntity entity;
		for(int i=0 ; i< allfacts.size() ; i++){
			entity = allfacts.get(i);
			if(name.equalsIgnoreCase(entity.getName())){
				vec.add(entity);
			}
		}
		
		return vec;
	}
	
	/*
	 * if type exist in $"Data properties" , then do not append ':'
	 * 
	 * :john-1x1 :hasAGE "5" .
	 */
	private String verifyIndivSlotCategory(FactEntity enti){
		String cat = enti.getCategory();
		String type = enti.getType();
		
		Vector<FactEntity> vec = this.getDataTypePropFacts();
		String temp ;
		
		if(cat.indexOf(":")>=0){
			return cat;
		}else{
			for(int i=0 ; i< vec.size() ; i++){
				temp = vec.get(i).getName();
				
				if(type.equalsIgnoreCase(temp)){
					return cat;
				}
			}
		}
			
		return ":"+ cat;
	}
	
	/*
	 * (_fact John-1x1 rdf:type PERSON)
	 * (_fact John-1x1 rdf:type owl:NamedIndividual)
	 * return John-1x1 rdf:type PERSON,owl:NamedIndividual;
	 * 
	 * (_fact John-1x1 hasNUMBER 30-1x3)
	 *  may also return :hasNUMBER :30.
	 *  Add label for every individual: rdfs:label "$sentenceID"^^xsd:string ;
	 */
	private String getCombinedIndividuals(String individual){
		StringBuffer sb = new StringBuffer();
		Vector<FactEntity> vec = getAllFactByEntityName(individual);
		
		FactEntity entity;
		for(int i=0 ; i<vec.size() ; i++){
			entity = vec.get(i);
//			if(vec.size() == 1){
				sb.append(this.verifySlot(entity.getName()));
				sb.append(" ");
				sb.append(this.verifySlot(entity.getType()));
				sb.append(" ");
				sb.append(this.verifyIndivSlotCategory(entity));
				sb.append(" .");
				sb.append("\n");
//			}
//			else{
//				if(i == 0){
//					sb.append(this.verifySlot(entity.getName()));
//					sb.append(" ");
//					sb.append(this.verifySlot(entity.getType()));
//					sb.append(" ");
//					sb.append(this.verifySlot(entity.getCategory()));
//					sb.append(" ;");
//					sb.append("\n");
//				}else if(i<vec.size()-1){
//					sb.append("      ");
//					sb.append(this.verifySlot(entity.getType()));
//					sb.append(" ");
//					sb.append(this.verifySlot(entity.getCategory()));
//					sb.append(" ;");
//					sb.append("\n");
//				}else{
//					sb.append("      ");
//					sb.append(this.verifySlot(entity.getType()));
//					sb.append(" ");
//					sb.append(this.verifySlot(entity.getCategory()));
//					sb.append(" .");
//					sb.append("\n");
//				}
//			}

		}
		//Add label for every individual: rdfs:label "$sentenceID"^^xsd:string ;
		sb.append(this.verifySlot(individual));
		sb.append(" ");
		sb.append("rdfs:label");
		sb.append(" ");
		sb.append("\"");
		Sentence sen = this.getSentenceByIndivNameSuffix(individual);
		sb.append(StringUtils.remove(String.valueOf(sen.getIndex()), '-'));
		sb.append("\"");
//		sb.append("^^");
//		sb.append("xsd:string");
		sb.append(" .");
		sb.append("\n");
		//End label
		return sb.toString() ;
	}
	
	/*
	 * in solution section
	 * John's dog --> John_s_dog.
	 */
	public String parseDirtyPunctuation(String text){
		//TODO
		String str = StringUtils.replace(text, "'s", "s");
		return str;
	}
	
	/*
	   :John_is_30 rdf:type :COC_Answer .
       :John_is_30 rdf:type owl:NamedIndividual .
       :John_is_30 rdfs:label "1"^^xsd:string .
       :John_is_30 rdfs:comment "John is 30"^^xsd:string ;
	 */
	public String getLabelScript4EachSentence(String individual){
		StringBuffer sb = new StringBuffer();
		Sentence sen = this.getSentenceByIndivNameSuffix(individual);
		String text = sen.getText();
		//replace all SPACE to "_" , the last element is always punctuation, ignore it ? //TODO
		String[] args = text.split(" ");
		String text_name = "";
		for(int i=0 ; i<args.length-1 ; i++){
			if(StringUtils.isNotBlank(args[i])){
				if("".equalsIgnoreCase(text_name)){
					text_name = args[i];
				}else{
					text_name = text_name + "_"+ args[i];
				}
			}
		}
		//TODO remove punctuation . , ; 
		text_name = StringUtils.remove(text_name, '.');
		text_name = StringUtils.remove(text_name, ',');
		text_name = StringUtils.remove(text_name, ';');
		
		text_name = parseDirtyPunctuation(text_name);
		
		sb.append(this.verifySlot(text_name)).append(" ").append("rdf:type").append(" ").append(":__SOLUTION").append(" .");
		sb.append("\n");
		
		sb.append(this.verifySlot(text_name)).append(" ").append("rdf:type").append(" ").append("owl:NamedIndividual").append(" .");
		sb.append("\n");
		
		sb.append(this.verifySlot(text_name)).append(" ").append("rdfs:label").append(" ")
			.append("\"")
			.append(StringUtils.remove(String.valueOf(sen.getIndex()), '-'))
			.append("\"")
//			.append("^^")
//			.append("xsd:string")
			.append(" .");
		sb.append("\n");
		
		sb.append(this.verifySlot(text_name)).append(" ").append("rdfs:comment").append(" ")
		.append("\"")
		.append(text)
		.append("\"")
//		.append("^^")
//		.append("xsd:string")
		.append(" .");
		sb.append("\n");
		
		return sb.toString();
	}
	
	/**
	 * important 
	 * (_fact John-1x1 hasNUMBER 30-1x3)
	 * provide John/hasNUMBER/30-1x3
	 */
	
	//Convert Individual owl
	private static final String Individual_Namespace = "<http://attempto.ifi.uzh.ch/ace_lexicon#PN_sg>";
	
	public String getIndividualScript(){
		StringBuffer sb = new StringBuffer();
		Vector<Sentence> sens = new Vector<Sentence>();
		Sentence s = null;
		Vector<String> indv_names = getAllNamedIndividuals();
		String indiv = "";
		for (int i = 0 ; i < indv_names.size() ; i++){
			indiv = indv_names.get(i);
			s = this.getSentenceByIndivNameSuffix(indiv);
			sb.append(getCombinedIndividuals(indiv));
			sb.append("\n");
			if(!sens.contains(s)){
				sb.append(getLabelScript4EachSentence(indiv));
				sb.append("\n");
				sens.add(s);
			}
			
		}
		return sb.toString();
	}
	
	public static String getOWLDataContentByFact(Enumeration fen){
		String owlcontent = "";
		
		return owlcontent;
	}
	
	/*
	 * individual = John-2x1
	 * individualNameSuffix = 2
	 * TODO: This pattern is related with Processor.generateFacts(..)
	 */
	public Sentence getSentenceByIndivNameSuffix(String name){
		Sentence sen = null;
		
		if(StringUtils.isNotBlank(name)){
			int idx1 = name.lastIndexOf("_");
			int idx2 = name.indexOf("x", idx1);
			if(idx2 > idx1){
				int index = Integer.valueOf(name.substring(idx1+1, idx2));
				
				return this.sentenceMap.get(index);
			}
		}
		
		
		
		return sen;
	}
	
	public String getSentenceText(Sentence sentence){
		return sentence.getText();
	}
}
