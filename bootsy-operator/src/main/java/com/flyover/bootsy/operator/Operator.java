/**
 * 
 */
package com.flyover.bootsy.operator;

import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.stream.IntStream;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;

import com.flyover.bootsy.operator.k8s.KubeAdapter;
import com.flyover.bootsy.operator.k8s.KubeNode;
import com.flyover.bootsy.operator.k8s.KubeNodeController;
import com.flyover.bootsy.operator.k8s.KubeNodeProvider;
import com.flyover.bootsy.operator.provders.Provider;

/**
 * @author mramach
 *
 */
public class Operator {
	
	private static final Logger LOG = LoggerFactory.getLogger(Operator.class);
	
	@Autowired
	private KubeAdapter kubeAdapter;
	@Autowired
	private List<Provider> providers;

	@Scheduled(fixedDelay = 5000L, initialDelay = 0L)
	public void controllerLoop() {
		
		LOG.debug("processing kube node controllers");
		
		kubeAdapter.getKubeNodeControllers().getItems()
			.stream().forEach(this::processKubeNodeController);
		
	}
	
	@Scheduled(fixedDelay = 5000L, initialDelay = 0L)
	public void nodeControllerLoop() {
		
		LOG.debug("processing kube nodes");
		
		kubeAdapter.getKubeNodes(Collections.emptyMap()).getItems()
			.stream().forEach(this::processKubeNode);
		
	}
	
	private void processKubeNodeController(KubeNodeController knc) {
		
		LOG.debug("processing node controller {}", knc.getMetadata().getName());
		
		List<KubeNode> nodes = kubeAdapter.getKubeNodes(knc.getSpec().getSelectors()).getItems();
		
		LOG.debug("{}/{} requested nodes found", nodes.size(), knc.getSpec().getCount());
		
		if(nodes.size() < knc.getSpec().getCount()) {
			
			// create additional KubeNode resources
			IntStream.range(nodes.size(), knc.getSpec().getCount())
				.forEach(i -> this.createKubeNode(knc));
			
		} else if(nodes.size() > knc.getSpec().getCount()) {
			
			// delete excess KubeNodes (logical delete so additional controller loop can process delete)
			
		}
		
	}
	
	private void processKubeNode(KubeNode kn) {
		
		LOG.debug("processing node node {}", kn.getMetadata().getName());
		
		KubeNodeProvider knp = kubeAdapter.getKubeNodeProvider(
			Optional.ofNullable(kn.getSpec().getProvider()).orElse("not_provided"));
		
		if(knp == null) {
			
			LOG.debug("provider with name {} not found could not process node {}", 
				kn.getSpec().getProvider(), kn.getMetadata().getName());
			
			return;
			
		}
		
		Provider provider = providers.stream()
			.filter(p -> p.name().equals(knp.getSpec().getType()))
			.findFirst()
				.orElse(null);
		
		if(provider == null) {
			
			LOG.debug("provider type with name {} is not currently available in the system", knp.getSpec().getType());
				
			return;
			
		}
		
		if(!provider.instanceCreated(knp, kn)) {
			
			LOG.debug("an istance will be created for KubeNode {}", kn.getMetadata().getName());
			// create the instance using the requested provider
			provider.createInstance(knp, kn);
			
		}
		
		if(!provider.instanceReady(knp, kn)) {
			
			LOG.debug("an istance is not yet ready for KubeNode {}", kn.getMetadata().getName());
			
			return;
			
		}
		
		// check to see if the node has been prepped for initialization
		
		
	}
	
	private void createKubeNode(KubeNodeController knc) {
		
		LOG.debug("creating new KubeNode resource with selectors {}", knc.getSpec().getSelectors());
		
		KubeNode kn = new KubeNode();
		kn.getMetadata().setGenerateName("node-");
		kn.getMetadata().setLabels(knc.getSpec().getSelectors());
		kn.getSpec().setType("node");
		kn.getSpec().setProvider(knc.getSpec().getProvider());
		
		kubeAdapter.createKubeNode(kn);
		
	}
	
}
