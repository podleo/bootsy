/**
 * 
 */
package com.flyover.bootsy.k8s;

/**
 * @author mramach
 *
 */
public class Resource extends Model {

	private String name;
	private String kind;
	private boolean namespaced;
	
	public boolean isNamespaced() {
		return namespaced;
	}

	public void setNamespaced(boolean namespaced) {
		this.namespaced = namespaced;
	}

	public String getName() {
		return name;
	}
	
	public void setName(String name) {
		this.name = name;
	}
	
	public String getKind() {
		return kind;
	}
	
	public void setKind(String kind) {
		this.kind = kind;
	}
	
}
