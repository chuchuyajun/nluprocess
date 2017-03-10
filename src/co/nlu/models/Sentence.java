package co.nlu.models;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @author Min Xia
 * @date Oct 2, 2012
 */

public class Sentence {
	
	private int index = -1;;
	private Map tokenRet = new HashMap();
	private List depRet = new ArrayList();
	private String text;
	
	
	public int getIndex() {
		return index;
	}
	public void setIndex(int index) {
		this.index = index;
	}
	public Map getTokenRet() {
		return tokenRet;
	}
	public void setTokenRet(Map tokenRet) {
		this.tokenRet = tokenRet;
	}
	public void clear() {
		index = -1;
		tokenRet.clear();
		depRet.clear();
	}
	public List getDepRet() {
		return depRet;
	}
	public void setDepRet(List depRet) {
		this.depRet = depRet;
	}
	public String getText() {
		return text;
	}
	public void setText(String text) {
		this.text = text;
	}
	
}
