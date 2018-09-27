/**
 * 
 */
package com.flyover.bootsy.core.k8s;

import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

/**
 * @author mramach
 *
 */
public class KubeNodeControllerSpec extends Model {
	
	private int count;
	private String provider;
	private Map<String, String> selectors = new LinkedHashMap<>();
	private Map<String, String> labels = new LinkedHashMap<>();
	private List<String> packages = new LinkedList<>();
	private KubeletConfig kubelet = new KubeletConfig();
	
	public int getCount() {
		return count;
	}

	public void setCount(int count) {
		this.count = count;
	}

	public String getProvider() {
		return provider;
	}

	public void setProvider(String provider) {
		this.provider = provider;
	}

	public Map<String, String> getSelectors() {
		return selectors;
	}

	public void setSelectors(Map<String, String> selectors) {
		this.selectors = selectors;
	}

	public Map<String, String> getLabels() {
		return labels;
	}

	public void setLabels(Map<String, String> labels) {
		this.labels = labels;
	}

	public List<String> getPackages() {
		return packages;
	}

	public void setPackages(List<String> packages) {
		this.packages = packages;
	}
	
	public KubeletConfig getKubelet() {
		return kubelet;
	}

	public void setKubelet(KubeletConfig kubelet) {
		this.kubelet = kubelet;
	}
	
}
