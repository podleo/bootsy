/**
 * 
 */
package com.flyover.bootsy.operator.provders.azure;

import com.flyover.bootsy.core.k8s.KubeNodeProviderSpec;
import com.flyover.bootsy.core.k8s.SecretRef;

/**
 * @author mramach
 *
 */
public class AzureKubeNodeProviderSpec extends KubeNodeProviderSpec {

	private String subscription;
	private String client;
	private String tenant;
	private String resourceGroup;
	private String region;
	private String network;
	private String subnet;
	private SecretRef credentialsSecret;

	public String getSubscription() {
		return subscription;
	}

	public void setSubscription(String subscription) {
		this.subscription = subscription;
	}

	public String getClient() {
		return client;
	}

	public void setClient(String client) {
		this.client = client;
	}

	public String getTenant() {
		return tenant;
	}

	public void setTenant(String tenant) {
		this.tenant = tenant;
	}

	public SecretRef getCredentialsSecret() {
		return credentialsSecret;
	}

	public void setCredentialsSecret(SecretRef credentialsSecret) {
		this.credentialsSecret = credentialsSecret;
	}

	public String getResourceGroup() {
		return resourceGroup;
	}

	public void setResourceGroup(String resourceGroup) {
		this.resourceGroup = resourceGroup;
	}

	public String getRegion() {
		return region;
	}

	public void setRegion(String region) {
		this.region = region;
	}

	public String getNetwork() {
		return network;
	}

	public void setNetwork(String network) {
		this.network = network;
	}

	public String getSubnet() {
		return subnet;
	}

	public void setSubnet(String subnet) {
		this.subnet = subnet;
	}
	
}
