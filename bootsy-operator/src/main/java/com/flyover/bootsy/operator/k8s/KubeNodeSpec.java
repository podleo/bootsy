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
public class KubeNodeSpec extends Model {

	private String type;
	private String provider;
	private KubeNodeConnector connector;
	private String ipAddress;
	private boolean dockerReady = false;
	private boolean kubeletReady = false;
	private Map<String, Object> instanceInfo = new LinkedHashMap<>();

	public String getType() {
		return type;
	}

	public void setType(String type) {
		this.type = type;
	}

	public String getProvider() {
		return provider;
	}

	public void setProvider(String provider) {
		this.provider = provider;
	}

	public Map<String, Object> getInstanceInfo() {
		return instanceInfo;
	}

	public void setInstanceInfo(Map<String, Object> instanceInfo) {
		this.instanceInfo = instanceInfo;
	}

	public KubeNodeConnector getConnector() {
		return connector;
	}

	public void setConnector(KubeNodeConnector connector) {
		this.connector = connector;
	}

	public String getIpAddress() {
		return ipAddress;
	}

	public void setIpAddress(String ipAddress) {
		this.ipAddress = ipAddress;
	}

	public boolean isDockerReady() {
		return dockerReady;
	}

	public void setDockerReady(boolean dockerReady) {
		this.dockerReady = dockerReady;
	}

	public boolean isKubeletReady() {
		return kubeletReady;
	}

	public void setKubeletReady(boolean kubeletReady) {
		this.kubeletReady = kubeletReady;
	}
	
}
