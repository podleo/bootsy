/**
 * 
 */
package com.flyover.bootsy.operator.provders.azure;

import java.io.ByteArrayOutputStream;
import java.util.Arrays;
import java.util.Base64;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.flyover.bootsy.core.k8s.KubeMeta;
import com.flyover.bootsy.core.k8s.KubeNode;
import com.flyover.bootsy.core.k8s.KubeNodeConnector;
import com.flyover.bootsy.core.k8s.KubeNodeProvider;
import com.flyover.bootsy.core.k8s.Secret;
import com.flyover.bootsy.core.k8s.SecretRef;
import com.flyover.bootsy.operator.k8s.KubeAdapter;
import com.flyover.bootsy.operator.provders.AbstractProvider;
import com.jcraft.jsch.JSch;
import com.jcraft.jsch.KeyPair;
import com.microsoft.azure.credentials.ApplicationTokenCredentials;
import com.microsoft.azure.management.Azure;
import com.microsoft.azure.management.compute.KnownLinuxVirtualMachineImage;
import com.microsoft.azure.management.compute.PowerState;
import com.microsoft.azure.management.compute.VirtualMachine;
import com.microsoft.azure.management.compute.VirtualMachineSizeTypes;
import com.microsoft.azure.management.network.Network;
import com.microsoft.azure.management.network.NetworkInterface;
import com.microsoft.azure.management.resources.ResourceGroup;
import com.microsoft.azure.management.resources.fluentcore.arm.Region;
import com.microsoft.rest.LogLevel;
import com.microsoft.rest.ServiceCallback;

/**
 * @author mramach
 *
 */
@SuppressWarnings("unchecked")
public class AzureProvider extends AbstractProvider {

	private static final Logger LOG = LoggerFactory.getLogger(AzureProvider.class);
	private static final ObjectMapper MAPPER = new ObjectMapper(); 
	
	@Autowired
	private KubeAdapter kubeAdapter;
	
	public AzureProvider() {
		super("azure");
	}

	@Override
	public boolean instanceCreated(KubeNodeProvider knp, KubeNode kn) {
		return Arrays.asList("creating", "created", "ready").contains(getInstanceInfo(kn).getStatus());
	}
	
	@Override
	public KubeNode createInstance(KubeNodeProvider knp, KubeNode kn) {
		
		AzureKubeNodeProviderSpec config = MAPPER
				.convertValue(knp.getSpec(), AzureKubeNodeProviderSpec.class);
		
		// ensure that the api credentials secret is present
		SecretRef secretRef = config.getCredentialsSecret();
		
		if(secretRef == null) {
			LOG.error("secretRef is required on a azure provider"); return kn;
		}
		
		Secret credentials = kubeAdapter.getSecret(secretRef.getNamespace(), secretRef.getName());
		
		if(credentials == null) {
			LOG.error("credentials secret in namespace {} with name {} not found", secretRef.getNamespace(), secretRef.getName()); return kn;
		}
		
		LOG.debug(String.format("creating new public and private keys"));
		
		ByteArrayOutputStream publicKey = new ByteArrayOutputStream();
		ByteArrayOutputStream privateKey = new ByteArrayOutputStream();
		
		try {
			
			KeyPair keyPair = KeyPair.genKeyPair(new JSch(), KeyPair.RSA, 2048);
			keyPair.writePublicKey(publicKey, "bootsy");
			keyPair.writePrivateKey(privateKey);
			
		} catch (Exception e) {
			throw new RuntimeException("failed to create bootstrap ssh keypair", e);
		}
		
		ApplicationTokenCredentials azureCredentials = new ApplicationTokenCredentials(
			config.getClient(), 
			config.getTenant(), 
			credentials.getData().get("key"), 
			null);
		
		Azure azure = Azure.configure()
	        .withLogLevel(LogLevel.BASIC)
	        .authenticate(azureCredentials)
	        .withSubscription(config.getSubscription());
		
		ResourceGroup resourceGroup = azure.resourceGroups()
			.getByName(config.getResourceGroup());
		
		Network network = azure.networks()
			.getByResourceGroup(config.getResourceGroup(), config.getNetwork());
		
		NetworkInterface nic = azure.networkInterfaces()
		    .define(kn.getMetadata().getName())
		    .withRegion(Region.findByLabelOrName(config.getRegion()))
		    .withExistingResourceGroup(resourceGroup)
		    .withExistingPrimaryNetwork(network)
		    .withSubnet(config.getSubnet())
		    .withPrimaryPrivateIPAddressDynamic()
		    .create();
		
		azure.virtualMachines()
		    .define(kn.getMetadata().getName())
		    .withRegion(Region.findByLabelOrName(config.getRegion()))
		    .withExistingResourceGroup(resourceGroup)
		    .withExistingPrimaryNetworkInterface(nic)
		    .withPopularLinuxImage(KnownLinuxVirtualMachineImage.UBUNTU_SERVER_16_04_LTS)
		    .withRootUsername("bootsy")
		    .withSsh(publicKey.toString())
		    .withComputerName(kn.getMetadata().getName())
		    .withSize(VirtualMachineSizeTypes.STANDARD_D3_V2)
		    .createAsync(new ServiceCallback<VirtualMachine>() {
				
				@Override
				public void success(VirtualMachine result) {
					LOG.debug("successfully created vm {}", result.name());
				}
				
				@Override
				public void failure(Throwable t) {
					LOG.error("failed to create vm", t);
				}
				
			});
		
		// set the server id for future reference
		InstanceInfo info = new InstanceInfo();
		info.setStatus("creating");
		
		setInstanceInfo(kn, info);

		// create an auth secret for future connections
		Secret authSecret = new Secret();
		authSecret.setMetadata(new KubeMeta());
		authSecret.getMetadata().setNamespace("bootsy");
		authSecret.getMetadata().setName(UUID.randomUUID().toString());
		authSecret.getData().put("publicKey", Base64.getEncoder().encodeToString(publicKey.toByteArray()));
		authSecret.getData().put("privateKey", Base64.getEncoder().encodeToString(privateKey.toByteArray()));
		
		kubeAdapter.createSecret(authSecret);
		
		// update the kube node spec with the connection details
		SecretRef ref = new SecretRef();
		ref.setNamespace("bootsy");
		ref.setName(authSecret.getMetadata().getName());
		
		kn.getSpec().setConnector(new KubeNodeConnector());
		kn.getSpec().getConnector().setAuthSecret(ref);
		
		return kubeAdapter.updateKubeNode(kn);
		
	}

	@Override
	public boolean instanceReady(KubeNodeProvider knp, KubeNode kn) {
		
		AzureKubeNodeProviderSpec config = MAPPER
				.convertValue(knp.getSpec(), AzureKubeNodeProviderSpec.class);
		
		if(Arrays.asList("ready").contains(getInstanceInfo(kn).getStatus())) {
			return true;
		}
		
		// ensure that the api credentials secret is present
		SecretRef secretRef = config.getCredentialsSecret();
		
		if(secretRef == null) {
			LOG.error("secretRef is required on a azure provider"); return false;
		}
		
		Secret credentials = kubeAdapter.getSecret(secretRef.getNamespace(), secretRef.getName());
		
		if(credentials == null) {
			LOG.error("credentials secret in namespace {} with name {} not found", secretRef.getNamespace(), secretRef.getName()); return false;
		}
		
		ApplicationTokenCredentials azureCredentials = new ApplicationTokenCredentials(
			config.getClient(), 
			config.getTenant(), 
			credentials.getData().get("key"), 
			null);
		
		Azure azure = Azure.configure()
	        .withLogLevel(LogLevel.BASIC)
	        .authenticate(azureCredentials)
	        .withSubscription(config.getSubscription());
		
		VirtualMachine vm = azure.virtualMachines()
			.getByResourceGroup(config.getResourceGroup(), kn.getMetadata().getName());
		
		boolean isReady = PowerState.RUNNING.equals(vm.powerState());
		
		if(isReady) {
			
			// set the instance state for future reference
			InstanceInfo info = getInstanceInfo(kn);
			info.setStatus("ready");
			
			setInstanceInfo(kn, info);
			
			kn.getSpec().setIpAddress(vm.getPrimaryNetworkInterface().primaryPrivateIP());
			
			kubeAdapter.updateKubeNode(kn);
			
		}
		
		return isReady;
		
	}
	
	@Override
	public KubeNode restartInstance(KubeNodeProvider knp, KubeNode kn) {
	
		AzureKubeNodeProviderSpec config = MAPPER
				.convertValue(knp.getSpec(), AzureKubeNodeProviderSpec.class);
		
		// ensure that the api credentials secret is present
		SecretRef secretRef = config.getCredentialsSecret();
		
		if(secretRef == null) {
			LOG.error("secretRef is required on a azure provider"); return kn;
		}
		
		Secret credentials = kubeAdapter.getSecret(secretRef.getNamespace(), secretRef.getName());
		
		if(credentials == null) {
			LOG.error("credentials secret in namespace {} with name {} not found", secretRef.getNamespace(), secretRef.getName()); return kn;
		}
		
		ApplicationTokenCredentials azureCredentials = new ApplicationTokenCredentials(
			config.getClient(), 
			config.getTenant(), 
			credentials.getData().get("key"), 
			null);
		
		Azure azure = Azure.configure()
	        .withLogLevel(LogLevel.BASIC)
	        .authenticate(azureCredentials)
	        .withSubscription(config.getSubscription());
		
		VirtualMachine vm = azure.virtualMachines()
			.getByResourceGroup(config.getResourceGroup(), kn.getMetadata().getName());
		
		vm.restartAsync().await(10, TimeUnit.MINUTES);
		
		return kn;
	}

	private InstanceInfo getInstanceInfo(KubeNode kn) {
		return MAPPER.convertValue(kn.getSpec().getInstanceInfo(), InstanceInfo.class);
	}
	
	private void setInstanceInfo(KubeNode kn, InstanceInfo instanceInfo) {
		kn.getSpec().setInstanceInfo(MAPPER.convertValue(instanceInfo, Map.class));
	}

}
