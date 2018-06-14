/**
 * 
 */
package com.flyover.bootsy.core.k8s;


/**
 * @author mramach
 *
 */
public class SecretRef extends Model {

	private String namespace;
	private String name;
	
	public String getNamespace() {
		return namespace;
	}
	
	public void setNamespace(String namespace) {
		this.namespace = namespace;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}	
	
}
