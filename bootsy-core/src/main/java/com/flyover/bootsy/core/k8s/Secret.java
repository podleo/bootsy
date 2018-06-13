/**
 * 
 */
package com.flyover.bootsy.core.k8s;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * @author mramach
 *
 */
public class Secret extends KubeModel<Void> {
	
	private String type;
	private Map<String, String> data = new LinkedHashMap<String, String>();
	
	public Secret() {
		setKind("Secret");
		setApiVersion("v1");
	}

	public String getType() {
		return type;
	}

	public void setType(String type) {
		this.type = type;
	}

	public Map<String, String> getData() {
		return data;
	}

	public void setData(Map<String, String> data) {
		this.data = data;
	}

}
