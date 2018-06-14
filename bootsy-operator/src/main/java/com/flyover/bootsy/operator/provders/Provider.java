/**
 * 
 */
package com.flyover.bootsy.operator.provders;

import com.flyover.bootsy.core.k8s.KubeNode;
import com.flyover.bootsy.core.k8s.KubeNodeProvider;

/**
 * @author mramach
 *
 */
public interface Provider {
	
	String name();

	boolean instanceCreated(KubeNodeProvider knp, KubeNode kn);

	KubeNode createInstance(KubeNodeProvider knp, KubeNode kn);

	KubeNode restartInstance(KubeNodeProvider knp, KubeNode kn);
	
	boolean instanceReady(KubeNodeProvider knp, KubeNode kn);

}
