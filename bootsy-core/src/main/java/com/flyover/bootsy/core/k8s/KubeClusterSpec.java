/**
 * 
 */
package com.flyover.bootsy.core.k8s;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.flyover.bootsy.core.ClusterConfig;

/**
 * @author mramach
 *
 */
@JsonInclude(Include.NON_NULL)
public class KubeClusterSpec extends Model {

	private ClusterConfig config = new ClusterConfig();

	public ClusterConfig getConfig() {
		return config;
	}

	public void setConfig(ClusterConfig config) {
		this.config = config;
	}
	
}
