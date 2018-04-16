/**
 * 
 */
package com.flyover.bootsy.operator;

import java.io.File;
import java.security.MessageDigest;
import java.util.Base64;
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
import com.flyover.bootsy.operator.ssh.Connection;

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
		
		// ensure that all kn specs are updated with the latest checksum and packages.
		String checksum = checksum(knc.getSpec().getPackages());
		
		nodes.stream()
			.filter(n -> !checksum.equals(n.getSpec().getPackages().getChecksum()))
			.map(n -> {
				
				n.getSpec().getPackages().setPackages(knc.getSpec().getPackages());
				n.getSpec().getPackages().setChecksum(checksum);
				
				return n;
				
			})
			.forEach(kubeAdapter::updateKubeNode);
		
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
			
			LOG.debug("an instance will be created for KubeNode {}", kn.getMetadata().getName());
			// create the instance using the requested provider
			kn = provider.createInstance(knp, kn);
			
		}
		
		if(!provider.instanceReady(knp, kn)) {
			
			LOG.debug("an instance is not yet ready for KubeNode {}", kn.getMetadata().getName());
			
			return;
			
		}
		
		// check to see if the node has been prepped for initialization
		if(!kn.getSpec().isDockerReady()) {
		
			LOG.debug("prepping docker for KubeNode {}", kn.getMetadata().getName());
			
			try {
				
				new Connection(kubeAdapter, kn).raw("sudo apt-get update");	
				new Connection(kubeAdapter, kn).raw("sudo apt-get install apt-transport-https ca-certificates curl software-properties-common -y");
				new Connection(kubeAdapter, kn).raw("curl -fsSL https://download.docker.com/linux/ubuntu/gpg | sudo apt-key add -");
				new Connection(kubeAdapter, kn).raw("sudo add-apt-repository \"deb [arch=amd64] https://download.docker.com/linux/ubuntu $(lsb_release -cs) stable\"");
				new Connection(kubeAdapter, kn).raw("sudo apt-get update");	
				new Connection(kubeAdapter, kn).raw("sudo apt-get install docker-ce=17.12.0~ce-0~ubuntu -y");
				deployDockerConfig(kn);
				new Connection(kubeAdapter, kn).raw("sudo systemctl restart docker");

				// mark docker as ready
				kn.getSpec().setDockerReady(true);
				// update the spec
				kn = kubeAdapter.updateKubeNode(kn);
				
			} catch (Exception e) {
				LOG.error("failed during docker installation {}", e.getMessage()); 
				// stop node actions
				return;
			}
			
		}
		
		// if packages are not installed
		if(kn.getSpec().isDockerReady() && 
			!kn.getSpec().getPackages().getChecksum().equalsIgnoreCase(kn.getSpec().getChecksum())) {
			
			KubeNode kni = kn;
			
			try {
				
				LOG.debug("installing packages for KubeNode {}", kn.getMetadata().getName());
				
				kn.getSpec().getPackages().getPackages()
					.forEach(p -> new Connection(kubeAdapter, kni).raw(String.format("sudo apt-get install %s -y", p)));
				
				kn.getSpec().setChecksum(kn.getSpec().getPackages().getChecksum());
				
				// update latest applied checksum
				kubeAdapter.updateKubeNode(kn);
				
			} catch (Exception e) {
				LOG.error("failed during package installation {}", e.getMessage()); 
				// stop node actions
				return;
			}
			
		}
		
		// check to see if the kubelet has been initialized on the node.
		if(!kn.getSpec().isKubeletReady()) {
			
			LOG.debug("prepping kubelet for KubeNode {}", kn.getMetadata().getName());
			
			// fetch master node ip address
			KubeNode master = kubeAdapter.getKubeNodes(Collections.singletonMap("type", "master"))
				.getItems().stream().findFirst().orElse(null);
			
			if(master == null) {
				LOG.error("failed during kubelet installation, unable to determine kubernetes master node.");
				// stop node actions
				return ;
			}
			
			String masterIpAddress = master.getSpec().getIpAddress();
			
			try {
				
				new Connection(kubeAdapter, kn).raw("sudo docker pull portr.ctnr.ctl.io/markramach/bootsy-cmd:latest");
				new Connection(kubeAdapter, kn).raw(String.format(
						"sudo docker run -d --net=host -v /etc:/etc -v /root:/root -v /var/run:/var/run " + 
						"portr.ctnr.ctl.io/markramach/bootsy-cmd:latest --init --type=node --api-server-endpoint=http://%s:8080", 
							masterIpAddress));
			
				// mark kubelet as ready
				kn.getSpec().setKubeletReady(true);
				// update the spec
				kn = kubeAdapter.updateKubeNode(kn);
				
			} catch (Exception e) {
				LOG.error("failed during kubelet installation {}", e.getMessage());
				// stop node actions
				return ;
			}
			
		}
		
	}
	
	private void createKubeNode(KubeNodeController knc) {
		
		LOG.debug("creating new KubeNode resource with selectors {}", knc.getSpec().getSelectors());
		
		KubeNode kn = new KubeNode();
		kn.getMetadata().setGenerateName("node-");
		kn.getMetadata().setLabels(knc.getSpec().getSelectors());
		kn.getSpec().setType("node");
		kn.getSpec().setProvider(knc.getSpec().getProvider());
		kn.getSpec().getPackages().setChecksum(checksum(knc.getSpec().getPackages()));
		kn.getSpec().getPackages().setPackages(knc.getSpec().getPackages());
		
		kubeAdapter.createKubeNode(kn);
		
	}
	
	private void deployDockerConfig(KubeNode kn) {
		
		LOG.debug(String.format("deploying docker config"));
		
		try {
			
			new Connection(kubeAdapter, kn).put(new File("/docker-daemon.json"), "/etc/docker/daemon.json");
			
			LOG.debug(String.format("daemon.json created at /etc/docker/daemon.json"));
			
		} catch (Exception e) {
			throw new RuntimeException("failed to write daemon.json file.", e);
		}
		
	}
	
	private String checksum(List<String> values) {
		
		try {
			
			MessageDigest md = MessageDigest.getInstance("MD5");
			values.forEach(n -> md.update(n.getBytes()));
			String checksum = Base64.getUrlEncoder().encodeToString(md.digest());
			
			return checksum;
			
		} catch (Exception e) {
			throw new RuntimeException("Failed to generate checksum.", e);
		}
		
	}
	
}
