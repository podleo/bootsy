/**
 * 
 */
package com.flyover.bootsy.operator.provders.bootstrap;

import com.flyover.bootsy.core.k8s.KubeNode;
import com.flyover.bootsy.core.k8s.KubeNodeProvider;
import com.flyover.bootsy.operator.provders.Provider;

/**
 * @author mramach
 *
 */
public class BootstrapProvider implements Provider {

	@Override
	public String name() {
		return "bootstrap";
	}

	@Override
	public boolean instanceCreated(KubeNodeProvider knp, KubeNode kn) {
		return true;
	}

	@Override
	public KubeNode createInstance(KubeNodeProvider knp, KubeNode kn) {
		return kn;
	}

	@Override
	public KubeNode restartInstance(KubeNodeProvider knp, KubeNode kn) {
		return kn;
	}

	@Override
	public boolean instanceReady(KubeNodeProvider knp, KubeNode kn) {
		return true;
	}

}
