/**
 * 
 */
package com.flyover.bootsy.core;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * @author mramach
 *
 */
public class ClusterConfig {
	
	private EtcdServer etcdserver = new EtcdServer();
	private ApiServer apiserver = new ApiServer();
	private Scheduler scheduler = new Scheduler();
	private ControllerManager controllerManager = new ControllerManager();
	private Kubelet kubelet = new Kubelet();
	private KubeProxy kubeProxy = new KubeProxy();
	
	public EtcdServer getEtcdserver() {
		return etcdserver;
	}

	public void setEtcdserver(EtcdServer etcdserver) {
		this.etcdserver = etcdserver;
	}

	public ApiServer getApiserver() {
		return apiserver;
	}

	public void setApiserver(ApiServer apiserver) {
		this.apiserver = apiserver;
	}
	
	public Scheduler getScheduler() {
		return scheduler;
	}

	public void setScheduler(Scheduler scheduler) {
		this.scheduler = scheduler;
	}

	public ControllerManager getControllerManager() {
		return controllerManager;
	}

	public void setControllerManager(ControllerManager controllerManager) {
		this.controllerManager = controllerManager;
	}

	public Kubelet getKubelet() {
		return kubelet;
	}

	public void setKubelet(Kubelet kubelet) {
		this.kubelet = kubelet;
	}

	public KubeProxy getKubeProxy() {
		return kubeProxy;
	}

	public void setKubeProxy(KubeProxy kubeProxy) {
		this.kubeProxy = kubeProxy;
	}

	public static class EtcdServer {
		
		@JsonProperty(value = "data-dir")
		private String dataDir = "/data01/etcd";
		@JsonProperty(value = "cert-file")
		private String certFile = "/etc/k8s/etcd-server.crt";
		@JsonProperty(value = "key-file")
		private String keyFile = "/etc/k8s/etcd-server.key";
		@JsonProperty(value = "trusted-ca-file")
		private String trustedCaFile = "/etc/k8s/etcd-ca.crt";
		@JsonProperty(value = "ca-file")
		private String caFile = "/etc/k8s/etcd-ca.crt";
		@JsonProperty(value = "client-cert-auth")
		private boolean clientCertAuth= true;
		@JsonProperty(value = "listen-client-urls")
		private String listenClientUrls = "";
		@JsonProperty(value = "advertise-client-urls")
		private String advertiseClientUrls = "";
		
		public String getCaFile() {
			return caFile;
		}
		
		public void setCaFile(String caFile) {
			this.caFile = caFile;
		}

		public String getDataDir() {
			return dataDir;
		}

		public void setDataDir(String dataDir) {
			this.dataDir = dataDir;
		}

		public String getCertFile() {
			return certFile;
		}

		public void setCertFile(String certFile) {
			this.certFile = certFile;
		}

		public String getKeyFile() {
			return keyFile;
		}

		public void setKeyFile(String keyFile) {
			this.keyFile = keyFile;
		}

		public String getTrustedCaFile() {
			return trustedCaFile;
		}

		public void setTrustedCaFile(String trustedCaFile) {
			this.trustedCaFile = trustedCaFile;
		}

		public boolean isClientCertAuth() {
			return clientCertAuth;
		}

		public void setClientCertAuth(boolean clientCertAuth) {
			this.clientCertAuth = clientCertAuth;
		}

		public String getListenClientUrls() {
			return listenClientUrls;
		}

		public void setListenClientUrls(String listenClientUrls) {
			this.listenClientUrls = listenClientUrls;
		}

		public String getAdvertiseClientUrls() {
			return advertiseClientUrls;
		}

		public void setAdvertiseClientUrls(String advertiseClientUrls) {
			this.advertiseClientUrls = advertiseClientUrls;
		}
		
	}
	
	public static class ApiServer {
		
		@JsonProperty(value = "bind-address")
		private String bindAddress = "0.0.0.0";
		@JsonProperty(value = "secure-port")
		private int securePort = 443;
		@JsonProperty(value = "service-cluster-ip-range")
		private String serviceClusterIpRange = "192.168.253.0/24";
		@JsonProperty(value = "allow-privileged")
		private boolean allowPrivileged = true;
		@JsonProperty(value = "anonymous-auth")
		private boolean anonymousAuth = false;
		@JsonProperty(value = "authorization-mode")
		private String authorizationMode = "Node,RBAC";
		@JsonProperty(value = "admission-control")
		private String admissionControl = "NodeRestriction,ServiceAccount";
		@JsonProperty(value = "client-ca-file")
		private String clientCaFile = "/etc/k8s/ca.crt";
		@JsonProperty(value = "tls-cert-file")
		private String tlsCertFile = "/etc/k8s/server.crt";
		@JsonProperty(value = "tls-private-key-file")
		private String tlsPrivateKeyFile = "/etc/k8s/server.key";
		@JsonProperty(value = "etcd-servers")
		private String etcdServers = "";
		@JsonProperty(value = "etcd-cafile")
		private String etcdCafile = "/etc/k8s/etcd-ca.crt";
		@JsonProperty(value = "etcd-certfile")
		private String etcdCertfile = "/etc/k8s/etcd-server.crt";
		@JsonProperty(value = "etcd-keyfile")
		private String etcdKeyfile = "/etc/k8s/etcd-server.key";
		
		public String getAdmissionControl() {
			return admissionControl;
		}
		
		public void setAdmissionControl(String admissionControl) {
			this.admissionControl = admissionControl;
		}

		public String getBindAddress() {
			return bindAddress;
		}

		public void setBindAddress(String bindAddress) {
			this.bindAddress = bindAddress;
		}

		public int getSecurePort() {
			return securePort;
		}

		public void setSecurePort(int securePort) {
			this.securePort = securePort;
		}

		public String getServiceClusterIpRange() {
			return serviceClusterIpRange;
		}

		public void setServiceClusterIpRange(String serviceClusterIpRange) {
			this.serviceClusterIpRange = serviceClusterIpRange;
		}

		public boolean isAllowPrivileged() {
			return allowPrivileged;
		}

		public void setAllowPrivileged(boolean allowPrivileged) {
			this.allowPrivileged = allowPrivileged;
		}

		public boolean isAnonymousAuth() {
			return anonymousAuth;
		}

		public void setAnonymousAuth(boolean anonymousAuth) {
			this.anonymousAuth = anonymousAuth;
		}

		public String getAuthorizationMode() {
			return authorizationMode;
		}

		public void setAuthorizationMode(String authorizationMode) {
			this.authorizationMode = authorizationMode;
		}

		public String getClientCaFile() {
			return clientCaFile;
		}

		public void setClientCaFile(String clientCaFile) {
			this.clientCaFile = clientCaFile;
		}

		public String getTlsCertFile() {
			return tlsCertFile;
		}

		public void setTlsCertFile(String tlsCertFile) {
			this.tlsCertFile = tlsCertFile;
		}

		public String getTlsPrivateKeyFile() {
			return tlsPrivateKeyFile;
		}

		public void setTlsPrivateKeyFile(String tlsPrivateKeyFile) {
			this.tlsPrivateKeyFile = tlsPrivateKeyFile;
		}

		public String getEtcdServers() {
			return etcdServers;
		}

		public void setEtcdServers(String etcdServers) {
			this.etcdServers = etcdServers;
		}

		public String getEtcdCafile() {
			return etcdCafile;
		}

		public void setEtcdCafile(String etcdCafile) {
			this.etcdCafile = etcdCafile;
		}

		public String getEtcdCertfile() {
			return etcdCertfile;
		}

		public void setEtcdCertfile(String etcdCertfile) {
			this.etcdCertfile = etcdCertfile;
		}

		public String getEtcdKeyfile() {
			return etcdKeyfile;
		}

		public void setEtcdKeyfile(String etcdKeyfile) {
			this.etcdKeyfile = etcdKeyfile;
		}
		
	}
	
	public static class Scheduler {
		
		@JsonProperty(value = "address")
		private String address = "0.0.0.0";
		@JsonProperty(value = "master")
		private String master = "http://localhost:8080";
		
		public String getAddress() {
			return address;
		}
		
		public void setAddress(String address) {
			this.address = address;
		}
		
		public String getMaster() {
			return master;
		}
		
		public void setMaster(String master) {
			this.master = master;
		}
		
	}
	
	public static class ControllerManager {
		
		@JsonProperty(value = "address")
		private String address = "0.0.0.0";
		@JsonProperty(value = "master")
		private String master = "http://localhost:8080";
		@JsonProperty(value = "root-ca-file")
		private String rootCaFile = "/etc/k8s/ca.crt";
		@JsonProperty(value = "service-account-private-key-file")
		private String serviceAccountPrivateKeyFile = "/etc/k8s/server.key";
		
		public String getAddress() {
			return address;
		}
		
		public void setAddress(String address) {
			this.address = address;
		}
		
		public String getMaster() {
			return master;
		}
		
		public void setMaster(String master) {
			this.master = master;
		}

		public String getRootCaFile() {
			return rootCaFile;
		}

		public void setRootCaFile(String rootCaFile) {
			this.rootCaFile = rootCaFile;
		}

		public String getServiceAccountPrivateKeyFile() {
			return serviceAccountPrivateKeyFile;
		}

		public void setServiceAccountPrivateKeyFile(String serviceAccountPrivateKeyFile) {
			this.serviceAccountPrivateKeyFile = serviceAccountPrivateKeyFile;
		}
		
	}
	
	public static class Kubelet {
		
		@JsonProperty(value = "address")
		private String address = "0.0.0.0";
		@JsonProperty(value = "allow-privileged")
		private boolean allowPrivileged = true;
		@JsonProperty(value = "etwork-plugin")
		private String networkPlugin = "cni";
		@JsonProperty(value = "tls-private-key-file")
		private String tlsPrivateKeyFile = "/etc/k8s/server.key";
		@JsonProperty(value = "tls-cert-file")
		private String tlsCertFile = "/etc/k8s/server.crt ";
		@JsonProperty(value = "kubeconfig")
		private String kubeconfig = "/etc/k8s/kubeconfig.kubelet";
		
		public String getAddress() {
			return address;
		}
		
		public void setAddress(String address) {
			this.address = address;
		}
		
		public boolean isAllowPrivileged() {
			return allowPrivileged;
		}
		
		public void setAllowPrivileged(boolean allowPrivileged) {
			this.allowPrivileged = allowPrivileged;
		}
		
		public String getNetworkPlugin() {
			return networkPlugin;
		}
		
		public void setNetworkPlugin(String networkPlugin) {
			this.networkPlugin = networkPlugin;
		}
		
		public String getTlsPrivateKeyFile() {
			return tlsPrivateKeyFile;
		}
		
		public void setTlsPrivateKeyFile(String tlsPrivateKeyFile) {
			this.tlsPrivateKeyFile = tlsPrivateKeyFile;
		}
		
		public String getTlsCertFile() {
			return tlsCertFile;
		}
		
		public void setTlsCertFile(String tlsCertFile) {
			this.tlsCertFile = tlsCertFile;
		}
		
		public String getKubeconfig() {
			return kubeconfig;
		}
		
		public void setKubeconfig(String kubeconfig) {
			this.kubeconfig = kubeconfig;
		}
		
	}
	
	public static class KubeProxy {
		
		@JsonProperty(value = "bind-address")
		private String bindAddress = "0.0.0.0";
		@JsonProperty(value = "cluster-cidr")
		private String clusterCidr = "192.168.253.0/24 ";
		@JsonProperty(value = "kubeconfig")
		private String kubeconfig = "/etc/k8s/kubeconfig.kube-proxy";
		
		public String getBindAddress() {
			return bindAddress;
		}
		
		public void setBindAddress(String bindAddress) {
			this.bindAddress = bindAddress;
		}
		
		public String getClusterCidr() {
			return clusterCidr;
		}
		
		public void setClusterCidr(String clusterCidr) {
			this.clusterCidr = clusterCidr;
		}
		
		public String getKubeconfig() {
			return kubeconfig;
		}
		
		public void setKubeconfig(String kubeconfig) {
			this.kubeconfig = kubeconfig;
		}
		
	}

}
