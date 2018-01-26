/**
 * 
 */
package com.flyover.bootsy.operator.k8s;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * @author mramach
 *
 */
public class KubeNodeControllerSpec extends Model {
	
	private int count;
	private String provider;
	private Map<String, String> selectors = new LinkedHashMap<>();

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
	
}
