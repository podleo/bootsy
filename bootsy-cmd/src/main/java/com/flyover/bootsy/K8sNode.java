/**
 * 
 */
package com.flyover.bootsy;

import java.net.URI;
import java.util.stream.IntStream;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

/**
 * @author mramach
 *
 */
public class K8sNode extends K8sServer {
	
	private static final Logger LOG = LoggerFactory.getLogger(K8sNode.class);

	public K8sNode() {
		super();
	}
	
	public void init(String apiServerEndpoint) {
		
		// verify docker is present on the host.
		verifyDockerRunning();
		// bootstrap host with ssh credentials
		bootstrapSsh();
		// pull kubernetes image
		pullImage("portr.ctnr.ctl.io/bootsy/kube-base:1.7.11");
		// pull bootsy image
		pullImage("portr.ctnr.ctl.io/bootsy/bootsy-cmd:0.0.1-SNAPSHOT");
		// deploy kubectl
		deployKubernetesBinaries();
		// deploy kubeconfig
		deployKubeletKubeconfig(apiServerEndpoint);
		// deploy kubelet
		deployKubelet(apiServerEndpoint, "node=true");
		// deploy kubeconfig
		deployKubeProxyKubeconfig(apiServerEndpoint);
		// deploy kube-proxy
		deployKubeProxy(apiServerEndpoint);
		// start kubelet
		startKubelet();
		// start kube-proxy
		startKubeProxy();
		// wait for kubelet to become available
		waitForKubelet();

	}
	
	public void destroy() {
		
		stopKubelet();
		stopKubeProxy();
		
	}
	
	private void waitForKubelet() {

		LOG.debug("waiting for kubelet to become available");
		
		IntStream.range(0, 30)
    		.mapToObj(i -> {
    			
    			URI uri = UriComponentsBuilder
    				.fromHttpUrl("http://127.0.0.1:10248/healthz").build().toUri();
    			
    			try {
    				
					new RestTemplate().getForObject(uri, String.class); 
					
					LOG.debug("kubelet is available");
					
					return true;
					
				} catch (Exception e) {
					
					LOG.debug("kubelet is not available yet {}", e.getMessage());
					
					try { Thread.sleep(6000); } catch(Exception e1){ /* do nothing*/ }
					
					return false;
					
				}
    			
    		})
    		.filter(r -> r)
    		.findFirst().orElseThrow(() -> new RuntimeException(
    				"kubelet could not be reached, or is not healthy"));
		
	}
	
}
