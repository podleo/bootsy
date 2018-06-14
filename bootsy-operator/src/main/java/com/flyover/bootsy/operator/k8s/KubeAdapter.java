/**
 * 
 */
package com.flyover.bootsy.operator.k8s;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.security.SecureRandom;
import java.security.cert.X509Certificate;
import java.util.Arrays;
import java.util.Map;
import java.util.stream.Collectors;

import javax.annotation.PostConstruct;
import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSession;
import javax.net.ssl.SSLSocketFactory;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;

import org.apache.commons.io.IOUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.client.ClientHttpRequestInterceptor;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import com.flyover.bootsy.core.k8s.KubeCluster;
import com.flyover.bootsy.core.k8s.KubeNode;
import com.flyover.bootsy.core.k8s.KubeNodeControllerList;
import com.flyover.bootsy.core.k8s.KubeNodeList;
import com.flyover.bootsy.core.k8s.KubeNodeProvider;
import com.flyover.bootsy.core.k8s.Secret;

/**
 * @author mramach
 *
 */
public class KubeAdapter {
	
	private String endpoint;
	private RestTemplate restTemplate;
	@Value("file:/var/run/secrets/kubernetes.io/serviceaccount/token")
	private Resource token;
	
	public KubeAdapter(String endpoint) {
		this.endpoint = endpoint;
		this.restTemplate = new RestTemplate();
	}
	
	@PostConstruct
	public void afterPropertiesSet() {
		
		this.restTemplate.setRequestFactory(new SkipCertVerificationClientHttpRequestFactory());
		
		try {
			
			String t = IOUtils.toString(token.getInputStream(), "UTF-8");
			
			ClientHttpRequestInterceptor interceptor = (req, body, ex) -> {
				
				req.getHeaders().set("Authorization", String.format("Bearer %s", t));
				
				return ex.execute(req, body);
				
			};

			this.restTemplate.setInterceptors(Arrays.asList(interceptor));
			
		} catch (IOException e) {
			throw new RuntimeException("failed while attempting to read token", e);
		}
		
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
	
	public KubeCluster getKubeCluster(String name) {
		
		UriComponentsBuilder builder = UriComponentsBuilder
                .fromHttpUrl(String.format("%s/apis/bootsy.flyover.com/v1/kubeclusters/%s", endpoint, name));
        
		return restTemplate.getForObject(builder.build().toUri(), KubeCluster.class);
		
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
	
	private static class SkipCertVerificationClientHttpRequestFactory extends SimpleClientHttpRequestFactory {
		
		@Override
		protected void prepareConnection(HttpURLConnection connection, String httpMethod) throws IOException {
			
			if (connection instanceof HttpsURLConnection) {
				prepareHttpsConnection((HttpsURLConnection) connection);
			}
			
			super.prepareConnection(connection, httpMethod);
			
		}

		private void prepareHttpsConnection(HttpsURLConnection connection) {
			
			connection.setHostnameVerifier(new SkipHostnameVerifier());
			
			try {
				connection.setSSLSocketFactory(createSslSocketFactory());
			} catch (Exception ex) {}
			
		}

		private SSLSocketFactory createSslSocketFactory() throws Exception {
			
			SSLContext context = SSLContext.getInstance("TLS");
			context.init(null, new TrustManager[] { new SkipX509TrustManager() }, new SecureRandom());
			
			return context.getSocketFactory();
			
		}
		
	}
	
	private static class SkipHostnameVerifier implements HostnameVerifier {

		@Override
		public boolean verify(String s, SSLSession sslSession) {
			return true;
		}

	}

	private static class SkipX509TrustManager implements X509TrustManager {

		@Override
		public X509Certificate[] getAcceptedIssuers() {
			return new X509Certificate[0];
		}

		@Override
		public void checkClientTrusted(X509Certificate[] chain, String authType) {}

		@Override
		public void checkServerTrusted(X509Certificate[] chain, String authType) {}

	}

}
