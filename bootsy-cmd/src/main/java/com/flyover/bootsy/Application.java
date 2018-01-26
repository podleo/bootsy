package com.flyover.bootsy;

import static net.sourceforge.argparse4j.impl.Arguments.*;

import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.StringWriter;
import java.net.InetAddress;
import java.net.MalformedURLException;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.Arrays;
import java.util.Collections;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.stream.IntStream;

import net.schmizz.sshj.SSHClient;
import net.schmizz.sshj.connection.ConnectionException;
import net.schmizz.sshj.connection.channel.direct.Session;
import net.schmizz.sshj.connection.channel.direct.Session.Command;
import net.schmizz.sshj.transport.TransportException;
import net.schmizz.sshj.userauth.UserAuthException;
import net.sourceforge.argparse4j.ArgumentParsers;
import net.sourceforge.argparse4j.helper.HelpScreenException;
import net.sourceforge.argparse4j.inf.ArgumentParser;
import net.sourceforge.argparse4j.inf.MutuallyExclusiveGroup;
import net.sourceforge.argparse4j.inf.Namespace;

import org.apache.commons.io.IOUtils;
import org.apache.velocity.Template;
import org.apache.velocity.VelocityContext;
import org.apache.velocity.app.VelocityEngine;
import org.apache.velocity.runtime.RuntimeConstants;
import org.apache.velocity.runtime.resource.loader.ClasspathResourceLoader;
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
import com.github.dockerjava.api.DockerClient;
import com.github.dockerjava.api.command.CreateContainerResponse;
import com.github.dockerjava.api.model.AccessMode;
import com.github.dockerjava.api.model.Bind;
import com.github.dockerjava.api.model.Container;
import com.github.dockerjava.api.model.PortBinding;
import com.github.dockerjava.api.model.RestartPolicy;
import com.github.dockerjava.api.model.Volume;
import com.github.dockerjava.core.DefaultDockerClientConfig;
import com.github.dockerjava.core.DockerClientBuilder;
import com.github.dockerjava.core.DockerClientConfig;
import com.github.dockerjava.core.command.PullImageResultCallback;
import com.jcraft.jsch.JSch;
import com.jcraft.jsch.KeyPair;

/**
 * @author mramach
 *
 */
public class Application {
	
	private static final Logger LOG = LoggerFactory.getLogger(Application.class);
	
    public static void main( String[] args ) throws Exception {
        
    	ArgumentParser parser = ArgumentParsers.newFor("bootsy").build()
    			.defaultHelp(true)
    			.description("A Kubernetes cluster bootstrap tool.");
    	
    	MutuallyExclusiveGroup group = parser.addMutuallyExclusiveGroup();
    	
    	group.addArgument("--init")
    		.help("Initialize a Kubernetes cluster on the current host.")
    		.action(storeTrue());
    	group.addArgument("--destroy")
			.help("Destory a Kubernetes cluster on the current host.")
			.action(storeTrue());
    	
    	try {
    		
			Namespace namespace = parser.parseArgs(args);
			
			if(namespace.getBoolean("init")) {
				
				new Cluster().init();
				
			} else if(namespace.getBoolean("destroy")) {
				
				new Cluster().destroy();
				
			} else {
				
				parser.printHelp();
				
			}
			
		} catch (HelpScreenException e) {
			// do nothing, just displaying help
		}
    	
    }
    
    public static class Cluster {
    	
    	private DockerClient docker;
    	
    	public Cluster() {

    		DockerClientConfig config = DefaultDockerClientConfig.createDefaultConfigBuilder()
    				.withDockerConfig(null)
    				.withRegistryUrl("https://portr.ctnr.ctl.io/v2")
    					.build();
    		
    		docker = DockerClientBuilder.getInstance(config).build();
    		
    	}
    	
    	public void init() {
    		
    		// verify docker is present on the host.
    		verifyDockerRunning();
    		// bootstrap host with ssh credentials
    		bootstrapSsh();
    		// pull etcd image
    		pullImage("quay.io/coreos/etcd:v2.3.8");
    		// pull kubernetes image
    		pullImage("portr.ctnr.ctl.io/k8sops/kube-base:1.7.11");
    		// start etcd container
    		startEtcd();
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
    		deployKubelet("master=true");
    		// deploy kube-proxy
    		deployKubeProxy();
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
    	
    	private void deployKubernetesBinaries() {
    		
    		LOG.debug(String.format("deploying kubernetes binaries 1.7.11 "));
    		
			Volume bin = new Volume("/usr/bin");
			Volume opt = new Volume("/opt");
			
    		CreateContainerResponse res = docker.createContainerCmd("portr.ctnr.ctl.io/k8sops/kube-base:1.7.11")
    			.withVolumes(bin, opt)
    			.withBinds(
    				new Bind("/usr/bin", bin),
    				new Bind("/opt", opt))
    			.withEntrypoint("/bin/bash")
    			.withCmd("./copy.sh")
    			.withName("kube-binaries-1.7.11")
    				.exec();
    		
    		LOG.debug(String.format("kube-binaries-1.7.11 container created id: %s", res.getId()));
    		
    		docker.startContainerCmd(res.getId()).exec();
    		
    		LOG.debug(String.format("kube-binaries-1.7.11 container started id: %s", res.getId()));
    		
    		try {
    			
				IntStream.range(0, 10)
					.mapToObj(i -> docker.inspectContainerCmd(res.getId()).exec().getState().getRunning())
					.map(r -> { if(r) { try { Thread.sleep(3000); } catch(Exception e){}; } return r; })
					.filter(r -> !r)
					.findFirst()
						.orElseThrow(() -> new RuntimeException("kubernetes binaries installation may have failed."));
				
			} finally {
				
				if(docker.inspectContainerCmd(res.getId()).exec().getState().getRunning()) {
					docker.stopContainerCmd(res.getId()).exec();
				}
				
				docker.removeContainerCmd(res.getId()).exec();
				
				LOG.debug(String.format("kube-binaries-1.7.11 container removed id: %s", res.getId()));
				
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
			
			Template template = velocityEngine.getTemplate("kube-node-crd.yaml");
			
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

		private void removeContainer(String name) {
			
			Optional<Container> optional = findContainer(name);
    		
    		if(optional.isPresent()) {
    			
    			LOG.debug(String.format("%s container found", name));
    			
    			Container target = optional.get();
    			
    			if(docker.inspectContainerCmd(target.getId()).exec().getState().getRunning()) {
    				docker.stopContainerCmd(target.getId()).exec();
    			}
    			
    			LOG.debug(String.format("%s container stopped", name));
    			
    			docker.removeContainerCmd(target.getId()).exec();
    			
    			LOG.debug(String.format("%s container removed", name));
    			
    		} else {
    			
    			LOG.debug(String.format("%s container not found", name));
    			
    		}
    		
		}

		private Optional<Container> findContainer(String name) {
			
			return docker.listContainersCmd().exec().stream()
    			.filter(c -> Arrays.asList(c.getNames()).contains(String.format("/%s", name)))
    				.findFirst();
			
		}
		
		private void deployKubelet(String labels) {
			
    		LOG.debug(String.format("deploying kubelet service "));
    		
    		VelocityEngine velocityEngine = new VelocityEngine();
			velocityEngine.setProperty(RuntimeConstants.RESOURCE_LOADER, "classpath"); 
			velocityEngine.setProperty("classpath.resource.loader.class", ClasspathResourceLoader.class.getName());
			velocityEngine.init();
			
			Template kubeletService = velocityEngine.getTemplate("kubelet.service");
			
			VelocityContext context = new VelocityContext();
			context.put("node_labels", labels);
			
			StringWriter kubeletServiceValue = new StringWriter();
			
			kubeletService.merge(context, kubeletServiceValue);
			
			Path kubelet = java.nio.file.Paths.get("/etc/systemd/system/kubelet.service");
			
			try {
				
				kubelet.toFile().createNewFile();
				
				Files.write(kubelet, kubeletServiceValue.toString().getBytes(), StandardOpenOption.TRUNCATE_EXISTING);
				
				LOG.debug(String.format("kubelet.service created at /etc/systemd/system"));
				
			} catch (IOException e) {
				throw new RuntimeException("failed to write kubelet.service file.", e);
			}
			
		}
		
		private void startKubelet() {
			startService("kubelet.service");			
		}
		
		private void stopKubelet() {
			stopService("kubelet.service");
		}
		
		private void deployKubeProxy() {
			
    		LOG.debug(String.format("deploying kube-proxy service "));
    		
    		VelocityEngine velocityEngine = new VelocityEngine();
			velocityEngine.setProperty(RuntimeConstants.RESOURCE_LOADER, "classpath"); 
			velocityEngine.setProperty("classpath.resource.loader.class", ClasspathResourceLoader.class.getName());
			velocityEngine.init();
			
			Template kubeletService = velocityEngine.getTemplate("kube-proxy.service");
			
			VelocityContext context = new VelocityContext();
			
			StringWriter kubeletServiceValue = new StringWriter();
			
			kubeletService.merge(context, kubeletServiceValue);
			
			Path kubelet = java.nio.file.Paths.get("/etc/systemd/system/kube-proxy.service");
			
			try {
				
				kubelet.toFile().createNewFile();
				
				Files.write(kubelet, kubeletServiceValue.toString().getBytes(), StandardOpenOption.TRUNCATE_EXISTING);
				
				LOG.debug(String.format("kube-proxy.service created at /etc/systemd/system"));
				
			} catch (IOException e) {
				throw new RuntimeException("failed to write kube-proxy.service file.", e);
			}
			
		}
		
		private void startKubeProxy() {
			startService("kube-proxy.service");
		}
		
		private void stopKubeProxy() {
			stopService("kube-proxy.service");
		}

		private void startService(String service) {
			
			LOG.debug(String.format("starting %s ", service));
			
    		try {
				
				SSHClient ssh = connect();
				execute(ssh.startSession(), String.format("systemctl enable %s", service));
				execute(ssh.startSession(), String.format("systemctl daemon-reload"));
				execute(ssh.startSession(), String.format("systemctl restart %s", service));
				ssh.close();
				
				LOG.debug(String.format("%s started ", service));
				
			} catch (Exception e) {
				throw new RuntimeException(String.format("failed to start %s", service), e);
			}
    		
		}
		
		private void stopService(String service) {
			
			LOG.debug(String.format("stopping %s ", service));
			
    		try {
				
				SSHClient ssh = connect();
				execute(ssh.startSession(), String.format("systemctl stop %s", service));
				ssh.close();
				
				LOG.debug(String.format("%s stopped ", service));
				
			} catch (Exception e) {
				throw new RuntimeException(String.format("failed to stop %s", service), e);
			}
    		
		}

		private SSHClient connect() throws IOException, UserAuthException, TransportException {
			
			SSHClient ssh = new SSHClient();
			ssh.addHostKeyVerifier((h, p, k) -> true);
			ssh.connect(getIpAddress());
			ssh.authPublickey("root", "/root/.ssh/bootsy_rsa");
			
			return ssh;
			
		}

		private void execute(Session session, String cmd) throws ConnectionException, TransportException, IOException {
			
			Command c = session.exec(cmd);
			c.join(5L, TimeUnit.SECONDS);
			
			IOUtils.readLines(new InputStreamReader(c.getInputStream())).stream()
				.forEach(l -> LOG.debug(l));
			
			session.close();
			
		}

		private void startKubeScheduler() {
			
    		
    		CreateContainerResponse res = docker.createContainerCmd("portr.ctnr.ctl.io/k8sops/kube-base:1.7.11")
    			.withNetworkMode("host")
    			.withEntrypoint("kube-scheduler")
    			.withCmd(
    				"--address=0.0.0.0",
    				"--master=http://localhost:8080")
    			.withName("kube-scheduler")
    			.withRestartPolicy(RestartPolicy.alwaysRestart())
    				.exec();
    		
    		LOG.debug(String.format("kube-scheduler container created id: %s", res.getId()));
    		
    		docker.startContainerCmd(res.getId()).exec();
    		
    		LOG.debug(String.format("kube-scheduler container started id: %s", res.getId()));
    		
		}
		
		private void startKubeControllerManager() {
			
    		
    		CreateContainerResponse res = docker.createContainerCmd("portr.ctnr.ctl.io/k8sops/kube-base:1.7.11")
    			.withNetworkMode("host")
    			.withEntrypoint("kube-controller-manager")
    			.withCmd(
    				"--address=0.0.0.0",
    				"--master=http://localhost:8080",
    				"--root-ca-file=/var/lib/k8s/ca.crt",
    				"--service-account-private-key-file=/var/lib/k8s/server.key")
    			.withName("kube-controller-manager")
    			.withRestartPolicy(RestartPolicy.alwaysRestart())
    				.exec();
    		
    		LOG.debug(String.format("kube-controller-manager container created id: %s", res.getId()));
    		
    		docker.startContainerCmd(res.getId()).exec();
    		
    		LOG.debug(String.format("kube-controller-manager container started id: %s", res.getId()));
    		
		}		

		private void startKubeApiServer() {
			
    		CreateContainerResponse res = docker.createContainerCmd("portr.ctnr.ctl.io/k8sops/kube-base:1.7.11")
    			.withNetworkMode("host")
    			.withPortBindings(PortBinding.parse("8080:8080"))
    			.withEntrypoint("kube-apiserver")
    			.withCmd(
    				"--address=0.0.0.0",
    				"--service-cluster-ip-range=192.168.253.0/24",
    				"--etcd-servers=http://localhost:4001",
    				"--storage-backend=etcd2",
    				"--allow-privileged=true",
    				"--admission-control=ServiceAccount",
    				"--client-ca-file=/var/lib/k8s/ca.crt",
    				"--tls-cert-file=/var/lib/k8s/server.cert",
    				"--tls-private-key-file=/var/lib/k8s/server.key")
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
    	
    	private void pullImage(String image) {
    		
    		try {
    			
    			LOG.debug(String.format("pulling docker image %s", image));
    			
				docker.pullImageCmd(image)
					.exec(new PullImageResultCallback())
					.awaitCompletion(5, TimeUnit.MINUTES);
				
			} catch (InterruptedException e) {
				throw new RuntimeException(String.format("Failed while attempting to pull image %s", image));
			}
    		
    	}
    	
    	private void verifyDockerRunning() {
    		
    		try {
    			
				docker.infoCmd().exec();
				
				LOG.debug(String.format("docker is running on the host"));
				
			} catch (Exception e) {
				throw new RuntimeException("Docker does not appear to be running on this host. (You may need to mount the /var/run directory.)");
			}
    		
    	}
    	
    	private void bootstrapSsh() {
    		
    		LOG.debug(String.format("bootstrapping host with ssh keys for deployment"));
    		
    		// check home dir for key files.
    		Path sshDirPath = java.nio.file.Paths.get("/root", ".ssh");
    		Path privateKeyPath = sshDirPath.resolve("bootsy_rsa");
    		Path publicKeyPath = sshDirPath.resolve("bootsy_rsa.pub");
    		Path authorizedKeysPath = sshDirPath.resolve("authorized_keys");
    		// ensure the .ssh dir exists
    		sshDirPath.toFile().mkdirs();
    		// check for the private key file
    		if(!privateKeyPath.toFile().exists()) {

    			// create a new key pair and write to files
    			LOG.debug(String.format("no existing bootstrap key detected, creating new public and private keys"));
    			
    			try {
    				
    				KeyPair keyPair = KeyPair.genKeyPair(new JSch(), KeyPair.RSA, 2048);
    				keyPair.writePublicKey(new FileOutputStream(publicKeyPath.toFile()), "bootsy");
    				keyPair.writePrivateKey(new FileOutputStream(privateKeyPath.toFile()));
    				
    			} catch (Exception e) {
    				throw new RuntimeException("failed to create bootstrap ssh keypair", e);
    			}
    			
    		}
    		
    		LOG.debug(String.format("ensuring bootstrap key file permissions are set properly"));
    		
    		// ensure the private key is secured properly
    		privateKeyPath.toFile().setWritable(true, true);
    		privateKeyPath.toFile().setReadable(false, false);
    		privateKeyPath.toFile().setReadable(true, true);
    		
    		// ensure the public key is secured properly
    		publicKeyPath.toFile().setWritable(true, true);
    		publicKeyPath.toFile().setReadable(true, false);
    		
    		LOG.debug(String.format("ensuring authorized_keys file is configured for authentication"));
    		
    		// ensure the authorized keys file exists (only created is not present)
    		try {
				authorizedKeysPath.toFile().createNewFile();
			} catch (IOException e) {
				throw new RuntimeException("failed to create authorized_keys file", e);
			}
    		
    		// ensure the authorized_keys file is secured properly
    		authorizedKeysPath.toFile().setWritable(true, true);
    		authorizedKeysPath.toFile().setReadable(false, false);
    		authorizedKeysPath.toFile().setReadable(true, true);
    		
    		try {
				// read the public key
				String publicKey = new String(Files.readAllBytes(publicKeyPath));
				// check for our key in the authorized_keys file
				Optional<String> key = Files.lines(authorizedKeysPath)
					.filter(l -> publicKey.equals(l))
					.findFirst();
				
				if(!key.isPresent()) {
					
					try(OutputStream out = Files.newOutputStream(authorizedKeysPath, StandardOpenOption.APPEND)) {

						out.write((publicKey + "\n").getBytes());
						
					} catch(Exception e) {
						throw new RuntimeException("failed to write public key to authorized_keys file", e);
					}
					
				}
				
			} catch (IOException e) {
				throw new RuntimeException("failed to configure public key", e);
			}
			
    	}
    	
    	private InetAddress getIpAddress() {
    		
    		try {
				
    			return Collections.list(NetworkInterface.getNetworkInterfaces()).stream()
					.filter(nic -> Arrays.asList("eth0", "ens160").contains(nic.getName()))
					.flatMap(nic -> Collections.list(nic.getInetAddresses()).stream())
					.filter(a -> a.isSiteLocalAddress())
					.findFirst()
						.orElseThrow(() -> new RuntimeException("failed to determine host ip address using interfaces eth0 and ens160"));
				
			} catch (SocketException e) {
				throw new RuntimeException("failed to lookup host networks", e);
			}
    		
    	}
    	
    }
    
}
