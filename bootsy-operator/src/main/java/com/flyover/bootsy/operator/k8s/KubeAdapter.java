/**
 * 
 */
package com.flyover.bootsy.operator.k8s;

import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

/**
 * @author mramach
 *
 */
public class KubeAdapter {
	
	private String endpoint;
	private RestTemplate restTemplate;
	
	public KubeAdapter(String endpoint) {
		this.endpoint = endpoint;
		this.restTemplate = new RestTemplate();
	}
	
	public KubeNodeControllerList getKubeNodeControllers() {
		
		UriComponentsBuilder builder = UriComponentsBuilder
                .fromHttpUrl(String.format("%s/apis/bootsy.flyover.com/v1/kubenodecontrollers", endpoint));
        
		return restTemplate.getForObject(builder.build().toUri(), KubeNodeControllerList.class);
		
	}
	
	public KubeNodeList getKubeNodes(Map<String, String> selectors) {
		
		UriComponentsBuilder builder = UriComponentsBuilder
                .fromHttpUrl(String.format("%s/apis/bootsy.flyover.com/v1/kubenodecontrollers", endpoint))
                .queryParam("labelSelector", selectors.entrySet().stream()
                			.map(e -> String.format("%s=%s", e.getKey(), e.getValue()))
                			.collect(Collectors.joining(",")));
        
		return restTemplate.getForObject(builder.build().toUri(), KubeNodeList.class);
		
	}

}
