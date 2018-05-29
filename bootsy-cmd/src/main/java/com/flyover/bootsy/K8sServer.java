package com.flyover.bootsy;

import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.StringWriter;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.security.PrivateKey;
import java.security.cert.X509Certificate;
import java.util.Arrays;
import java.util.Collections;
import java.util.Optional;
import java.util.concurrent.TimeUnit;
import java.util.stream.IntStream;

import net.schmizz.sshj.SSHClient;
import net.schmizz.sshj.connection.ConnectionException;
import net.schmizz.sshj.connection.channel.direct.Session;
import net.schmizz.sshj.connection.channel.direct.Session.Command;
import net.schmizz.sshj.transport.TransportException;
import net.schmizz.sshj.userauth.UserAuthException;

import org.apache.commons.io.IOUtils;
import org.apache.velocity.Template;
import org.apache.velocity.VelocityContext;
import org.apache.velocity.app.VelocityEngine;
import org.apache.velocity.runtime.RuntimeConstants;
import org.apache.velocity.runtime.resource.loader.ClasspathResourceLoader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.github.dockerjava.api.DockerClient;
import com.github.dockerjava.api.command.CreateContainerResponse;
import com.github.dockerjava.api.model.Bind;
import com.github.dockerjava.api.model.Container;
import com.github.dockerjava.api.model.Volume;
import com.github.dockerjava.core.DefaultDockerClientConfig;
import com.github.dockerjava.core.DockerClientBuilder;
import com.github.dockerjava.core.DockerClientConfig;
import com.github.dockerjava.core.command.PullImageResultCallback;
import com.jcraft.jsch.JSch;
import com.jcraft.jsch.KeyPair;

public class K8sServer {
	
	private static final Logger LOG = LoggerFactory.getLogger(K8sMaster.class);

	protected DockerClient docker;

	public K8sServer() {

		DockerClientConfig config = DefaultDockerClientConfig.createDefaultConfigBuilder()
				.withDockerConfig(null)
				.withRegistryUrl("https://portr.ctnr.ctl.io/v2")
					.build();
		
		docker = DockerClientBuilder.getInstance(config).build();
		
	}

	protected void removeContainer(String name) {
		
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

	protected void deployKubeletKubeconfig(String apiServerEndpoint) {
		
		LOG.debug(String.format("deploying kubelet kubeconfig "));
		
		VelocityEngine velocityEngine = new VelocityEngine();
		velocityEngine.setProperty(RuntimeConstants.RESOURCE_LOADER, "classpath"); 
		velocityEngine.setProperty("classpath.resource.loader.class", ClasspathResourceLoader.class.getName());
		velocityEngine.init();
		
		Template kubeletService = velocityEngine.getTemplate("kubeconfig.kubelet");
		
		VelocityContext context = new VelocityContext();
		context.put("api_server_endpoint", apiServerEndpoint);
		context.put("ip_address", getIpAddress().getHostAddress());
		
		StringWriter kubeletServiceValue = new StringWriter();
		
		kubeletService.merge(context, kubeletServiceValue);
		
		Path k8s = java.nio.file.Paths.get("/etc/k8s");
		Path kubeconfig = k8s.resolve("kubeconfig.kubelet");
		
		try {
			
			Files.createDirectories(k8s);
			
			Files.write(kubeconfig, kubeletServiceValue.toString().getBytes(), 
					StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
			
			LOG.debug(String.format("kubeconfig created at /etc/k8s/kubeconfig.kubelet"));
			
		} catch (IOException e) {
			throw new RuntimeException("failed to write kubelet kubeconfig file.", e);
		}
		
	}
	
	protected void deployKubeProxyKubeconfig(String apiServerEndpoint) {
		
		LOG.debug(String.format("deploying kube-proxy kubeconfig "));
		
		VelocityEngine velocityEngine = new VelocityEngine();
		velocityEngine.setProperty(RuntimeConstants.RESOURCE_LOADER, "classpath"); 
		velocityEngine.setProperty("classpath.resource.loader.class", ClasspathResourceLoader.class.getName());
		velocityEngine.init();
		
		Template kubeletService = velocityEngine.getTemplate("kubeconfig.kube-proxy");
		
		VelocityContext context = new VelocityContext();
		context.put("api_server_endpoint", apiServerEndpoint);
		context.put("ip_address", getIpAddress().getHostAddress());
		
		StringWriter kubeletServiceValue = new StringWriter();
		
		kubeletService.merge(context, kubeletServiceValue);
		
		Path k8s = java.nio.file.Paths.get("/etc/k8s");
		Path kubeconfig = k8s.resolve("kubeconfig.kube-proxy");
		
		try {
			
			Files.createDirectories(k8s);
			
			Files.write(kubeconfig, kubeletServiceValue.toString().getBytes(), 
					StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
			
			LOG.debug(String.format("kubeconfig created at /etc/k8s/kubeconfig.kube-proxy"));
			
		} catch (IOException e) {
			throw new RuntimeException("failed to write kube-proxy kubeconfig file.", e);
		}
		
	}
	
	protected void deployKubelet(String apiServerEndpoint, String labels) {
		
		LOG.debug(String.format("deploying kubelet service "));
		
		VelocityEngine velocityEngine = new VelocityEngine();
		velocityEngine.setProperty(RuntimeConstants.RESOURCE_LOADER, "classpath"); 
		velocityEngine.setProperty("classpath.resource.loader.class", ClasspathResourceLoader.class.getName());
		velocityEngine.init();
		
		Template kubeletService = velocityEngine.getTemplate("kubelet.service");
		
		VelocityContext context = new VelocityContext();
		context.put("node_labels", labels);
		context.put("api_server_endpoint", apiServerEndpoint);
		context.put("ip_address", getIpAddress().getHostAddress());
		
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

	protected void startKubelet() {
		startService("kubelet.service");			
	}

	protected void stopKubelet() {
		stopService("kubelet.service");
	}

	protected void deployKubeProxy(String apiServerEndpoint) {
		
		LOG.debug(String.format("deploying kube-proxy service "));
		
		VelocityEngine velocityEngine = new VelocityEngine();
		velocityEngine.setProperty(RuntimeConstants.RESOURCE_LOADER, "classpath"); 
		velocityEngine.setProperty("classpath.resource.loader.class", ClasspathResourceLoader.class.getName());
		velocityEngine.init();
		
		Template kubeletService = velocityEngine.getTemplate("kube-proxy.service");
		
		VelocityContext context = new VelocityContext();
		context.put("api_server_endpoint", apiServerEndpoint);
		context.put("ip_address", getIpAddress().getHostAddress());
		
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

	protected void startKubeProxy() {
		startService("kube-proxy.service");
	}

	protected void stopKubeProxy() {
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

	private SSHClient connect() throws IOException, UserAuthException,
			TransportException {
				
				SSHClient ssh = new SSHClient();
				ssh.addHostKeyVerifier((h, p, k) -> true);
				ssh.connect(getIpAddress());
				ssh.authPublickey("root", "/root/.ssh/bootsy_rsa");
				
				return ssh;
				
			}

	private void execute(Session session, String cmd)
			throws ConnectionException, TransportException, IOException {
				
				Command c = session.exec(cmd);
				c.join(5L, TimeUnit.SECONDS);
				
				IOUtils.readLines(new InputStreamReader(c.getInputStream())).stream()
					.forEach(l -> LOG.debug(l));
				
				session.close();
				
			}

	protected void pullImage(String image) {
		
		try {
			
			LOG.debug(String.format("pulling docker image %s", image));
			
			docker.pullImageCmd(image)
				.exec(new PullImageResultCallback())
				.awaitCompletion(5, TimeUnit.MINUTES);
			
		} catch (InterruptedException e) {
			throw new RuntimeException(String.format("Failed while attempting to pull image %s", image));
		}
		
	}

	protected void verifyDockerRunning() {
		
		try {
			
			docker.infoCmd().exec();
			
			LOG.debug(String.format("docker is running on the host"));
			
		} catch (Exception e) {
			throw new RuntimeException("Docker does not appear to be running on this host. (You may need to mount the /var/run directory.)");
		}
		
	}

	protected void bootstrapSsh() {
		
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
	
	protected void deployKubernetesBinaries() {
		
		LOG.debug(String.format("deploying kubernetes binaries 1.7.11 "));
		
		Volume bin = new Volume("/usr/bin");
		Volume opt = new Volume("/opt");
		
		CreateContainerResponse res = docker.createContainerCmd("portr.ctnr.ctl.io/markramach/kube-base:1.7.11")
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

	protected InetAddress getIpAddress() {
		
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

	protected static class MasterContext {
		
		private String masterIP;
		private PrivateKey caKey;
		private X509Certificate caCert;
		private PrivateKey serverKey;
		private X509Certificate[] serverCert;
		private PrivateKey kubeletKey;
		private X509Certificate[] kubeletCert;
		private PrivateKey kubeProxyKey;
		private X509Certificate[] kubeProxyCert;
		
		public MasterContext(String masterIP, PrivateKey caKey,
				X509Certificate caCert, PrivateKey serverKey,
				X509Certificate[] serverCert, PrivateKey kubeletKey,
				X509Certificate[] kubeletCert, PrivateKey kubeProxyKey,
				X509Certificate[] kubeProxyCert) {
			this.masterIP = masterIP;
			this.caKey = caKey;
			this.caCert = caCert;
			this.serverKey = serverKey;
			this.serverCert = serverCert;
			this.kubeletKey = kubeletKey;
			this.kubeletCert = kubeletCert;
			this.kubeProxyKey = kubeProxyKey;
			this.kubeProxyCert = kubeProxyCert;
		}

		public String getMasterIP() {
			return masterIP;
		}

		public PrivateKey getCaKey() {
			return caKey;
		}

		public X509Certificate getCaCert() {
			return caCert;
		}

		public PrivateKey getServerKey() {
			return serverKey;
		}

		public X509Certificate[] getServerCert() {
			return serverCert;
		}

		public PrivateKey getKubeletKey() {
			return kubeletKey;
		}

		public X509Certificate[] getKubeletCert() {
			return kubeletCert;
		}

		public PrivateKey getKubeProxyKey() {
			return kubeProxyKey;
		}

		public X509Certificate[] getKubeProxyCert() {
			return kubeProxyCert;
		}
		
	}
		
}