/**
 * 
 */
package com.flyover.bootsy;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.io.StringWriter;
import java.net.InetAddress;
import java.net.MalformedURLException;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.security.KeyPair;
import java.security.cert.X509Certificate;
import java.util.Map;
import java.util.UUID;
import java.util.stream.IntStream;

import org.apache.velocity.Template;
import org.apache.velocity.VelocityContext;
import org.apache.velocity.app.VelocityEngine;
import org.apache.velocity.runtime.RuntimeConstants;
import org.apache.velocity.runtime.resource.loader.ClasspathResourceLoader;
import org.bouncycastle.asn1.x500.X500Name;
import org.bouncycastle.asn1.x509.GeneralName;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import com.flyover.bootsy.k8s.Generic;
import com.flyover.bootsy.k8s.GenericItems;
import com.flyover.bootsy.k8s.Paths;
import com.flyover.bootsy.k8s.Resource;
import com.flyover.bootsy.k8s.ResourceList;
import com.github.dockerjava.api.command.CreateContainerResponse;
import com.github.dockerjava.api.model.AccessMode;
import com.github.dockerjava.api.model.Bind;
import com.github.dockerjava.api.model.PortBinding;
import com.github.dockerjava.api.model.RestartPolicy;
import com.github.dockerjava.api.model.Volume;

/**
 * @author mramach
 *
 */
public class K8sMaster extends K8sServer {
	
	private static final Logger LOG = LoggerFactory.getLogger(K8sMaster.class);
	
	public K8sMaster() {
		super();
	}
	
	public void init(String apiServerEndpoint) {
		
		// verify docker is present on the host.
		verifyDockerRunning();
		// bootstrap host with ssh credentials
		bootstrapSsh();
		// pull etcd image
		pullImage("quay.io/coreos/etcd:v2.3.8");
		// pull kubernetes image
		pullImage("portr.ctnr.ctl.io/markramach/kube-base:1.7.11");
		// pull bootsy image
		pullImage("portr.ctnr.ctl.io/markramach/bootsy-cmd:0.0.1-SNAPSHOT");
		// pull bootsy operator image
		pullImage("portr.ctnr.ctl.io/markramach/bootsy-operator:0.0.1-SNAPSHOT");
		// start etcd container
		startEtcd();
		// gernerate ca.crt, server.key, server.crt and write to the host file system.
		provisionCertificates();
		// start kube-apiserver
		startKubeApiServer();
		// start kube-controller-manager
		startKubeControllerManager();
		// start kube-scheduler
		startKubeScheduler();
		// wait for api to become available
		waitForApiServer();
		// deploy weave components
		deployWeaveComponents();
		// deploy bootsy components
		deployBootsyComponents();
		// deploy kubectl
		deployKubernetesBinaries();
		// deploy kubelet
		deployKubelet(apiServerEndpoint, "master=true");
		// deploy kube-proxy
		deployKubeProxy(apiServerEndpoint);
		// start kubelet
		startKubelet();
		// start kube-proxy
		startKubeProxy();

	}
	
	public void destroy() {
		
		stopKubelet();
		stopKubeProxy();
		removeContainer("kube-scheduler");
		removeContainer("kube-controller-manager");
		removeContainer("kube-apiserver");
		removeContainer("etcd");
		
	}
	
	private void provisionCertificates() {
		
		try {
			
			Path dir = java.nio.file.Paths.get("/etc/k8s");
			
			if(!dir.toFile().exists()) {
				Files.createDirectories(dir);
			}
			
			Path caKeyPath = dir.resolve("ca.key");
			Path caCertPath = dir.resolve("ca.crt");
			Path serverKeyPath = dir.resolve("server.key");
			Path serverCertPath = dir.resolve("server.crt");
			
			ByteArrayOutputStream out = new ByteArrayOutputStream();
			
			KeyPair caKey = SSL.generateRSAKeyPair();
			
			SSL.write(out, caKey);
			
			Files.write(caKeyPath, out.toByteArray(), StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
			
			out.reset();
			
			X509Certificate caCert = SSL.generateV1Certificate(caKey, String.format("192.168.253.1"));
			
			SSL.write(out, caCert);
			
			Files.write(caCertPath, out.toByteArray(), StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
			
			out.reset();
			
			KeyPair serverKey = SSL.generateRSAKeyPair();
			SSL.write(out, serverKey);
			
			Files.write(serverKeyPath, out.toByteArray(), StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
			
			out.reset();
			
			X500Name subject = new X500Name(String.format("C=US, ST=WA, L=Seattle, O=bootsy, OU=bootsy, CN=%s", "192.168.253.1"));
			
			X509Certificate[] serverChain = SSL.buildChain(caKey, caCert, serverKey, "192.168.253.1", subject,
					new GeneralName(GeneralName.iPAddress, getIpAddress().getHostAddress()),
					new GeneralName(GeneralName.iPAddress, "192.168.253.1"),
					new GeneralName(GeneralName.iPAddress, "127.0.0.1"),
					new GeneralName(GeneralName.dNSName, "kubernetes"),
					new GeneralName(GeneralName.dNSName, "kubernetes.default"),
					new GeneralName(GeneralName.dNSName, "kubernetes.default.svc"),
					new GeneralName(GeneralName.dNSName, "kubernetes.default.svc.cluster.local"),
					new GeneralName(GeneralName.dNSName, "kubernetes.default.cluster.local"),
					new GeneralName(GeneralName.dNSName, "kubernetes.default.skydns.local"));
			
			SSL.write(out, serverChain);
			
			Files.write(serverCertPath, out.toByteArray(), StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
			
		} catch (Exception e) {
			e.printStackTrace();
		}
		
	}
	
	private void deployWeaveComponents() {
		
		LOG.info("creating weave networking components");
		
		ObjectMapper mapper = new ObjectMapper(new YAMLFactory());
		
		InputStream input = Thread.currentThread().getContextClassLoader()
			.getResourceAsStream("weave-daemonset-k8s-1.7.yaml");
		
		try {
			
			GenericItems items = mapper.readValue(input, GenericItems.class);
			
			items.getItems().stream().forEach(this::createGenericKubeComponent);
			
		} catch (Exception e) {
			LOG.error("failed to read weave components from yaml.", e);
		}
		
	}
	
	private void deployBootsyComponents() {
		
		LOG.info("creating bootsy components");
		
		VelocityEngine velocityEngine = new VelocityEngine();
		velocityEngine.setProperty(RuntimeConstants.RESOURCE_LOADER, "classpath"); 
		velocityEngine.setProperty("classpath.resource.loader.class", ClasspathResourceLoader.class.getName());
		velocityEngine.init();
		
		Template template = velocityEngine.getTemplate("bootsy-specs.yaml");
		
		VelocityContext context = new VelocityContext();
		context.put("ip_address", getIpAddress().getHostAddress());
		context.put("version", "1.7.11");
		context.put("auth_secret_name", UUID.randomUUID().toString());
		
		StringWriter value = new StringWriter();
		
		template.merge(context, value);
		
		ObjectMapper mapper = new ObjectMapper(new YAMLFactory());
		
		try {
			
			GenericItems items = mapper.readValue(value.toString().getBytes(), GenericItems.class);
			
			items.getItems().stream().forEach(this::createGenericKubeComponent);
			
		} catch (Exception e) {
			LOG.error("failed to read bootsy components from yaml.", e);
		}
		
	}
	
	private void createGenericKubeComponent(Generic comp) {
		
		// get the api path for the component
		
		URI uri = UriComponentsBuilder
			.fromHttpUrl("http://localhost:8080").build().toUri();
		
		Paths res = new RestTemplate().getForObject(uri, Paths.class); 
		
		String path = res.getPaths().stream()
			.filter(p -> p.contains(comp.getApiVersion()))
			.findFirst()
				.orElseThrow(() -> new RuntimeException(
					String.format("Could not determine api path for compoent with apiVersion %s and kind", 
							comp.getApiVersion(), comp.getKind())));
		
		// need to get the resource for the component for api call
		
		uri = UriComponentsBuilder
			.fromHttpUrl("http://localhost:8080")
			.path(path)
			.build()
				.toUri();
		
		ResourceList resourceList = new RestTemplate().getForObject(uri, ResourceList.class);
		
		Resource resource = resourceList.getResources().stream()
			.filter(r -> comp.getKind().equals(r.getKind()))
			.findFirst()
			.orElseThrow(() -> new RuntimeException(
					String.format("Could not api resource for compoent with apiVersion %s and kind %s", 
							comp.getApiVersion(), comp.getKind())));
		
		if(resource.isNamespaced()) {
			
			uri = UriComponentsBuilder
    			.fromHttpUrl("http://localhost:8080")
    			.path(path)
    			.path("/namespaces/")
    			.path(comp.getMetadata().getNamespace())
    			.path("/")
    			.path(resource.getName())
    			.build()
    				.toUri();
			
			createGenericKubeComponent(uri, comp);
			
		} else {
			
			uri = UriComponentsBuilder
    			.fromHttpUrl("http://localhost:8080")
    			.path(path)
    			.path("/")
    			.path(resource.getName())
    			.build()
    				.toUri();
			
			createGenericKubeComponent(uri, comp);
			
		}
		
	}
	
	private void createGenericKubeComponent(URI uri, Generic comp) {
		
		try {

			try { LOG.debug(uri.toURL().toString()); } catch (MalformedURLException e) {}
			
			new RestTemplate().postForObject(uri, comp, Map.class);
			
			LOG.info("the component with apiVersion {} kind {} namespace {} and name {} was created successfully",
				comp.getApiVersion(), comp.getKind(), comp.getMetadata().getNamespace(), comp.getMetadata().getName());
			
		} catch (HttpClientErrorException e) {
			
			if(HttpStatus.CONFLICT.equals(e.getStatusCode())) {
				
				LOG.info("the component with apiVersion {} kind {} namespace {} and name {} already exists",
					comp.getApiVersion(), comp.getKind(), comp.getMetadata().getNamespace(), comp.getMetadata().getName());
				
			} else {
				
				throw e;
				
			}
			
		}
		
	}
	
	private void waitForApiServer() {

		LOG.debug("waiting for api server to become available");
		
		IntStream.range(0, 10)
    		.mapToObj(i -> {
    			
    			URI uri = UriComponentsBuilder
    				.fromHttpUrl("http://localhost:8080/healthz").build().toUri();
    			
    			try {
    				
					new RestTemplate().getForObject(uri, String.class); 
					
					LOG.debug("api server is available");
					
					return true;
					
				} catch (Exception e) {
					
					LOG.debug("api server is not available yet {}", e.getMessage());
					
					try { Thread.sleep(6000); } catch(Exception e1){ /* do nothing*/ }
					
					return false;
					
				}
    			
    		})
    		.filter(r -> r)
    		.findFirst().orElseThrow(() -> new RuntimeException(
    				"kubernetes api server could not be reached, or is not healthy"));
		
	}

	private void startKubeScheduler() {
		
		Volume k8s = new Volume("/etc/k8s");
		
		CreateContainerResponse res = docker.createContainerCmd("portr.ctnr.ctl.io/markramach/kube-base:1.7.11")
			.withNetworkMode("host")
			.withEntrypoint("kube-scheduler")
			.withCmd(
				"--address=0.0.0.0",
				"--master=http://localhost:8080")
			.withBinds(new Bind("/etc/k8s", k8s, AccessMode.rw))
			.withName("kube-apiserver")
			.withName("kube-scheduler")
			.withRestartPolicy(RestartPolicy.alwaysRestart())
				.exec();
		
		LOG.debug(String.format("kube-scheduler container created id: %s", res.getId()));
		
		docker.startContainerCmd(res.getId()).exec();
		
		LOG.debug(String.format("kube-scheduler container started id: %s", res.getId()));
		
	}
	
	private void startKubeControllerManager() {
		
		Volume k8s = new Volume("/etc/k8s");
		
		CreateContainerResponse res = docker.createContainerCmd("portr.ctnr.ctl.io/markramach/kube-base:1.7.11")
			.withNetworkMode("host")
			.withEntrypoint("kube-controller-manager")
			.withCmd(
				"--address=0.0.0.0",
				"--master=http://localhost:8080",
				"--root-ca-file=/etc/k8s/ca.crt",
				"--service-account-private-key-file=/etc/k8s/server.key")
			.withBinds(new Bind("/etc/k8s", k8s, AccessMode.rw))
			.withName("kube-apiserver")
			.withName("kube-controller-manager")
			.withRestartPolicy(RestartPolicy.alwaysRestart())
				.exec();
		
		LOG.debug(String.format("kube-controller-manager container created id: %s", res.getId()));
		
		docker.startContainerCmd(res.getId()).exec();
		
		LOG.debug(String.format("kube-controller-manager container started id: %s", res.getId()));
		
	}		

	private void startKubeApiServer() {
		
		Volume k8s = new Volume("/etc/k8s");
		
		CreateContainerResponse res = docker.createContainerCmd("portr.ctnr.ctl.io/markramach/kube-base:1.7.11")
			.withNetworkMode("host")
			.withPortBindings(PortBinding.parse("8080:8080"))
			.withEntrypoint("kube-apiserver")
			.withCmd(
				"--address=0.0.0.0",
				"--bind-address=0.0.0.0",
				"--secure-port=443",
				"--service-cluster-ip-range=192.168.253.0/24",
				"--etcd-servers=http://localhost:4001",
				"--storage-backend=etcd2",
				"--allow-privileged=true",
				"--anonymous-auth=false",
				"--authorization-mode=RBAC",
				"--admission-control=ServiceAccount",
				"--client-ca-file=/etc/k8s/ca.crt",
				"--tls-cert-file=/etc/k8s/server.crt",
				"--tls-private-key-file=/etc/k8s/server.key")
			.withVolumes(k8s)
			.withBinds(new Bind("/etc/k8s", k8s, AccessMode.rw))
			.withName("kube-apiserver")
			.withRestartPolicy(RestartPolicy.alwaysRestart())
				.exec();
		
		LOG.debug(String.format("kube-apiserver container created id: %s", res.getId()));
		
		docker.startContainerCmd(res.getId()).exec();
		
		LOG.debug(String.format("kube-apiserver container started id: %s", res.getId()));
		
	}
	
	private void startEtcd() {
		
		Volume etcdData = new Volume("/data01/etcd");
		
		CreateContainerResponse res = docker.createContainerCmd("quay.io/coreos/etcd:v2.3.8")
			.withNetworkMode("host")
			.withPortBindings(PortBinding.parse("4001:4001"))
			.withVolumes(etcdData)
			.withBinds(new Bind("/data01/etcd", etcdData, AccessMode.rw))
			.withCmd("-data-dir=/data01/etcd")
			.withName("etcd")
			.withRestartPolicy(RestartPolicy.alwaysRestart())
				.exec();
		
		LOG.debug(String.format("etcd container created id: %s", res.getId()));
		
		docker.startContainerCmd(res.getId()).exec();
		
		LOG.debug(String.format("etcd container started id: %s", res.getId()));
		
	}

}
