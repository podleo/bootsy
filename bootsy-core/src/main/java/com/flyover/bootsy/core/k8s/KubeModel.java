/**
 * 
 */
package com.flyover.bootsy.core.k8s;

/**
 * @author mramach
 *
 */
public class KubeModel<T> extends Model {

	private String kind;
	private String apiVersion;
	private KubeMeta metadata;
	private T spec;

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

	public T getSpec() {
		return spec;
	}

	public void setSpec(T spec) {
		this.spec = spec;
	}

}
