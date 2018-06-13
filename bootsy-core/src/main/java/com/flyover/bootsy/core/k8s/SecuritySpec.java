/**
 * 
 */
package com.flyover.bootsy.core.k8s;

import java.util.LinkedList;
import java.util.List;

/**
 * @author mramach
 *
 */
public class SecuritySpec extends Model {

	private CertSpec ca;
	private CertSpec server;
	private CertSpec kubelet;
	private CertSpec kubeProxy;
	
	public CertSpec getCa() {
		return ca;
	}

	public void setCa(CertSpec ca) {
		this.ca = ca;
	}

	public CertSpec getServer() {
		return server;
	}

	public void setServer(CertSpec server) {
		this.server = server;
	}

	public CertSpec getKubelet() {
		return kubelet;
	}

	public void setKubelet(CertSpec kubelet) {
		this.kubelet = kubelet;
	}

	public CertSpec getKubeProxy() {
		return kubeProxy;
	}

	public void setKubeProxy(CertSpec kubeProxy) {
		this.kubeProxy = kubeProxy;
	}

	public static class CertSpec extends Model {
		
		private String key;
		private List<String> cert = new LinkedList<>();
		
		public String getKey() {
			return key;
		}
		
		public void setKey(String key) {
			this.key = key;
		}

		public List<String> getCert() {
			return cert;
		}

		public void setCert(List<String> cert) {
			this.cert = cert;
		}
		
	}
	
}
