/**
 * 
 */
package com.flyover.bootsy.operator;

import java.util.Collections;
import java.util.List;
import java.util.stream.IntStream;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;

import com.flyover.bootsy.operator.k8s.KubeAdapter;
import com.flyover.bootsy.operator.k8s.KubeNode;
import com.flyover.bootsy.operator.k8s.KubeNodeController;

/**
 * @author mramach
 *
 */
public class Operator {
	
	private static final Logger LOG = LoggerFactory.getLogger(Operator.class);
	
	@Autowired
	private KubeAdapter kubeAdapter;

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
