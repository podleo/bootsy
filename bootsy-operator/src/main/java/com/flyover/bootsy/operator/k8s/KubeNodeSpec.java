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
	private String state = "docker_install";
	private Map<String, Object> instanceInfo = new LinkedHashMap<>();
	private String checksum = "";
	private KubeNodePackageSpec packages = new KubeNodePackageSpec();

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

	public String getState() {
		return state;
	}

	public void setState(String state) {
		this.state = state;
	}

	public KubeNodePackageSpec getPackages() {
		return packages;
	}

	public void setPackages(KubeNodePackageSpec packages) {
		this.packages = packages;
	}

	public String getChecksum() {
		return checksum;
	}

	public void setChecksum(String checksum) {
		this.checksum = checksum;
	}
	
}
