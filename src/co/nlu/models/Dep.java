package co.nlu.models;

/**
 * @author Min Xia
 * @date Oct 2, 2012
 */
public class Dep {
	private String dep;
	private Governor gov;
	private Dependent depend;
	public String getDep() {
		return dep;
	}
	public void setDep(String dep) {
		this.dep = dep;
	}
	public Governor getGov() {
		return gov;
	}
	public void setGov(Governor gov) {
		this.gov = gov;
	}
	public Dependent getDepend() {
		return depend;
	}
	public void setDepend(Dependent depend) {
		this.depend = depend;
	}
}
