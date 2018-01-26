/**
 * 
 */
package com.flyover.bootsy.operator;

import java.util.List;

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
	
	private void processKubeNodeController(KubeNodeController knc) {
		
		LOG.debug("processing node controller {}", knc.getMetadata().getName());
		
		List<KubeNode> nodes = kubeAdapter.getKubeNodes(knc.getSpec().getSelectors()).getItems();
		
		if(nodes.size() < knc.getSpec().getCount()) {
			
			LOG.debug("{}/{} requested nodes created", nodes.size(), knc.getSpec().getCount());
			
			// create additional KubeNode resources
			
		} else if(nodes.size() > knc.getSpec().getCount()) {
			
			LOG.debug("{}/{} requested nodes created", nodes.size(), knc.getSpec().getCount());
			
			// delete excess KubeNodes (logica delete so additional controller loop can process delete)
			
		}
		
	}
	
}
