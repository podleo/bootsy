/**
 * 
 */
package com.flyover.bootsy.core.k8s;

import java.util.LinkedHashMap;
import java.util.Map;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;

/**
 * @author mramach
 *
 */
@JsonInclude(Include.NON_NULL)
public class KubeNodeSpec extends Model {

	private String type;
	private String provider;
	private KubeNodeConnector connector;
	private String ipAddress;
	private String state = "docker_install";
	private Map<String, Object> instanceInfo = new LinkedHashMap<>();
	private String checksum = "";
	private String configurationChecksum = "";
	private KubeNodePackageSpec packages = new KubeNodePackageSpec();
	private SecuritySpec security = new SecuritySpec();

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

	public String getConfigurationChecksum() {
		return configurationChecksum;
	}

	public void setConfigurationChecksum(String configurationChecksum) {
		this.configurationChecksum = configurationChecksum;
	}

	public SecuritySpec getSecurity() {
		return security;
	}

	public void setSecurity(SecuritySpec security) {
		this.security = security;
	}
	
}
