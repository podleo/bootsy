/**
 * 
 */
package com.flyover.bootsy.operator.k8s;

/**
 * @author mramach
 *
 */
public class KubeNodeProviderSpec extends Model {
	
	private String type;

	public String getType() {
		return type;
	}

	public void setType(String type) {
		this.type = type;
	}

}
