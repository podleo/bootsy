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
public class KubeMeta extends Model {
	
	private String generateName;
	private String name;
	private String namespace;
	private Map<String, String> labels = new LinkedHashMap<String, String>();

	public String getGenerateName() {
		return generateName;
	}

	public void setGenerateName(String generateName) {
		this.generateName = generateName;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public String getNamespace() {
		return namespace;
	}

	public void setNamespace(String namespace) {
		this.namespace = namespace;
	}

	public Map<String, String> getLabels() {
		return labels;
	}

	public void setLabels(Map<String, String> labels) {
		this.labels = labels;
	}

}
