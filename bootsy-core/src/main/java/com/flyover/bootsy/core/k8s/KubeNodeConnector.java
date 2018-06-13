/**
 * 
 */
package com.flyover.bootsy.core.k8s;

/**
 * @author mramach
 *
 */
public class KubeNodeConnector extends Model {
	
	private String type = "ssh";
	private SecretRef authSecret;
	
	public String getType() {
		return type;
	}
	
	public void setType(String type) {
		this.type = type;
	}
	
	public SecretRef getAuthSecret() {
		return authSecret;
	}
	
	public void setAuthSecret(SecretRef authSecret) {
		this.authSecret = authSecret;
	}

}
