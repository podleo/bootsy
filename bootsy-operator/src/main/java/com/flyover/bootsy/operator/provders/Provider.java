/**
 * 
 */
package com.flyover.bootsy.operator.provders;

import com.flyover.bootsy.operator.k8s.KubeNode;
import com.flyover.bootsy.operator.k8s.KubeNodeProvider;

/**
 * @author mramach
 *
 */
public interface Provider {
	
	String name();

	boolean instanceCreated(KubeNodeProvider knp, KubeNode kn);

	void createInstance(KubeNodeProvider knp, KubeNode kn);

	boolean instanceReady(KubeNodeProvider knp, KubeNode kn);

}
