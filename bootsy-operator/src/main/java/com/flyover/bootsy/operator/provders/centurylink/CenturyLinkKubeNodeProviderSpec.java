/**
 * 
 */
package com.flyover.bootsy.operator.provders.centurylink;

import com.flyover.bootsy.core.k8s.KubeNodeProviderSpec;
import com.flyover.bootsy.core.k8s.SecretRef;

/**
 * @author mramach
 *
 */
public class CenturyLinkKubeNodeProviderSpec extends KubeNodeProviderSpec {

	private String accountAlias;
	private String datacenter;
	private String group;
	private String network;
	private String os;
	private Integer cpu;
	private Integer memoryGB;
	private SecretRef credentialsSecret;
	
	public String getAccountAlias() {
		return accountAlias;
	}
	
	public void setAccountAlias(String accountAlias) {
		this.accountAlias = accountAlias;
	}

	public String getDatacenter() {
		return datacenter;
	}

	public void setDatacenter(String datacenter) {
		this.datacenter = datacenter;
	}

	public String getGroup() {
		return group;
	}

	public void setGroup(String group) {
		this.group = group;
	}

	public String getNetwork() {
		return network;
	}

	public void setNetwork(String network) {
		this.network = network;
	}

	public String getOs() {
		return os;
	}

	public void setOs(String os) {
		this.os = os;
	}

	public Integer getCpu() {
		return cpu;
	}

	public void setCpu(Integer cpu) {
		this.cpu = cpu;
	}

	public Integer getMemoryGB() {
		return memoryGB;
	}

	public void setMemoryGB(Integer memoryGB) {
		this.memoryGB = memoryGB;
	}

	public SecretRef getCredentialsSecret() {
		return credentialsSecret;
	}

	public void setCredentialsSecret(SecretRef credentialsSecret) {
		this.credentialsSecret = credentialsSecret;
	}
	
}
