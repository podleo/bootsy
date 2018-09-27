/**
 * 
 */
package com.flyover.bootsy.core.k8s;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * @author mramach
 *
 */
public class KubeletConfig extends Model {
	
	@JsonProperty(value = "cluster-dns")
	private String clusterDns = "";

	public String getClusterDns() {
		return clusterDns;
	}

	public void setClusterDns(String clusterDns) {
		this.clusterDns = clusterDns;
	}

}
