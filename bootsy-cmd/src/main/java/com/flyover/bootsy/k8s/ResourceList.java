/**
 * 
 */
package com.flyover.bootsy.k8s;

import java.util.LinkedList;
import java.util.List;

/**
 * @author mramach
 *
 */
public class ResourceList extends Model {

	private String kind;
	private String apiVersion;
	private String groupVersion;
	private List<Resource> resources = new LinkedList<>();

	public String getKind() {
		return kind;
	}

	public void setKind(String kind) {
		this.kind = kind;
	}

	public String getApiVersion() {
		return apiVersion;
	}

	public void setApiVersion(String apiVersion) {
		this.apiVersion = apiVersion;
	}

	public String getGroupVersion() {
		return groupVersion;
	}

	public void setGroupVersion(String groupVersion) {
		this.groupVersion = groupVersion;
	}

	public List<Resource> getResources() {
		return resources;
	}

	public void setResources(List<Resource> resources) {
		this.resources = resources;
	}

}
