package co.nlu.models;

/**
 * @author Min Xia
 * @date Oct 2, 2012
 */

public class Token {
	private int id;
	private String word;
	private String lemma;
	private String ner;
	private String pos;
	private String norner;
	private boolean hasNormalNer;
	
	
	public int getId() {
		return id;
	}
	public void setId(int id) {
		this.id = id;
	}
	public String getWord() {
		return word;
	}
	public void setWord(String word) {
		this.word = word;
	}
	public String getLemma() {
		return lemma;
	}
	public void setLemma(String lemma) {
		this.lemma = lemma;
	}
	public String getNer() {
		return ner;
	}
	public void setNer(String ner) {
		this.ner = ner;
	}
	public String getPos() {
		return pos;
	}
	public void setPos(String pos) {
		this.pos = pos;
	}
	public String getNorner() {
		return norner;
	}
	public void setNorner(String norner) {
		this.norner = norner;
	}
	public boolean isHasNormalNer() {
		return hasNormalNer;
	}
	public void setHasNormalNer(boolean hasNormalNer) {
		this.hasNormalNer = hasNormalNer;
	}
	
}
