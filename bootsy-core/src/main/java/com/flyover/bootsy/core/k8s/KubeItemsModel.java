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
public class KubeItemsModel<T> extends Model {

	private String kind;
	private String apiVersion;
	private KubeMeta metadata;
	private List<T> items = new LinkedList<T>();

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

	public KubeMeta getMetadata() {
		return metadata;
	}

	public void setMetadata(KubeMeta metadata) {
		this.metadata = metadata;
	}
	
	public List<T> getItems() {
		return items;
	}

	public void setItems(List<T> items) {
		this.items = items;
	}

}
