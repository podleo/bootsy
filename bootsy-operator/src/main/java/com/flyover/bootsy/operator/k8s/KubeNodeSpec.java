/**
 * 
 */
package com.flyover.bootsy.operator.k8s;

/**
 * @author mramach
 *
 */
public class KubeNodeSpec extends Model {

	private String type;
	private String provider;

	public String getType() {
		return type;
	}

	public void setType(String type) {
		this.type = type;
	}

	public String getProvider() {
		return provider;
	}

	public void setProvider(String provider) {
		this.provider = provider;
	}
	
}
