/**
 * 
 */
package com.flyover.bootsy.operator.k8s;

import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.web.client.HttpClientErrorException;
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
                .fromHttpUrl(String.format("%s/apis/bootsy.flyover.com/v1/kubenodes", endpoint))
                .queryParam("labelSelector", selectors.entrySet().stream()
                			.map(e -> String.format("%s=%s", e.getKey(), e.getValue()))
                			.collect(Collectors.joining(",")));
        
		return restTemplate.getForObject(builder.build().toUri(), KubeNodeList.class);
		
	}

	public KubeNode createKubeNode(KubeNode kn) {
		
		UriComponentsBuilder builder = UriComponentsBuilder
                .fromHttpUrl(String.format("%s/apis/bootsy.flyover.com/v1/kubenodes", 
                		endpoint, kn.getMetadata().getNamespace()));
		
		return restTemplate.exchange(builder.build().toUri(), HttpMethod.POST, 
				new HttpEntity<KubeNode>(kn), KubeNode.class).getBody();
		
	}
	
	public KubeNode updateKubeNode(KubeNode kn) {
		
		UriComponentsBuilder builder = UriComponentsBuilder
                .fromHttpUrl(String.format("%s/apis/bootsy.flyover.com/v1/kubenodes/", 
                		endpoint, kn.getMetadata().getNamespace())).path(kn.getMetadata().getName());
		
		return restTemplate.exchange(builder.build().toUri(), HttpMethod.PUT, 
				new HttpEntity<KubeNode>(kn), KubeNode.class).getBody();
		
	}
	
	public KubeNodeProvider getKubeNodeProvider(String name) {
		
		UriComponentsBuilder builder = UriComponentsBuilder
                .fromHttpUrl(String.format("%s/apis/bootsy.flyover.com/v1/kubenodeproviders/", endpoint))
                	.path(name);
		
		try {
			
			return restTemplate.exchange(builder.build().toUri(), HttpMethod.GET, 
					new HttpEntity<>(new HttpHeaders()), KubeNodeProvider.class).getBody();
			
		} catch (HttpClientErrorException e) {
			
			if(HttpStatus.NOT_FOUND.equals(e.getStatusCode())) {
				return null;
			}
			
			throw e;
			
		}
		
	}
	

	public Secret createSecret(Secret secret) {
		
		UriComponentsBuilder builder = UriComponentsBuilder
                .fromHttpUrl(String.format("%s/api/v1/namespaces/%s/secrets", 
                		endpoint, secret.getMetadata().getNamespace()));
		
		try {
			
			return restTemplate.exchange(builder.build().toUri(), HttpMethod.POST, 
					new HttpEntity<Secret>(secret), Secret.class).getBody();
			
		} catch (HttpClientErrorException e) {
			
			if(HttpStatus.CONFLICT.equals(e.getStatusCode())) {
				return secret;
			}
			
			throw e;
			
		}
		
	}
	
	public Secret getSecret(String namespace, String name) {
		
		UriComponentsBuilder builder = UriComponentsBuilder
                .fromHttpUrl(String.format("%s/api/v1/namespaces/%s/secrets/%s", endpoint, namespace, name));
		
		try {
			
			return restTemplate.exchange(builder.build().toUri(), HttpMethod.GET, 
					new HttpEntity<>(new HttpHeaders()), Secret.class).getBody();
			
		} catch (HttpClientErrorException e) {
			
			if(HttpStatus.NOT_FOUND.equals(e.getStatusCode())) {
				return null;
			}
			
			throw e;
			
		}
		
	}

}
