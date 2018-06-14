/**
 * 
 */
package com.flyover.bootsy.core.k8s;

/**
 * @author mramach
 *
 */
public class KubeNode extends KubeModel<KubeNodeSpec> {

	public KubeNode() {
		super();
		setApiVersion("bootsy.flyover.com/v1");
		setKind("KubeNode");
		setMetadata(new KubeMeta());
		setSpec(new KubeNodeSpec());
	}
	
}
