/**
 * 
 */
package com.flyover.bootsy.core.k8s;

/**
 * @author mramach
 *
 */
public class KubeCluster extends KubeModel<KubeClusterSpec> {

	public KubeCluster() {
		super();
		setApiVersion("bootsy.flyover.com/v1");
		setKind("KubeCluster");
		setMetadata(new KubeMeta());
		setSpec(new KubeClusterSpec());
	}
	
}
