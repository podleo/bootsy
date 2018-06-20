/**
 * 
 */
package com.flyover.bootsy.operator.ssh;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.Base64;

import net.schmizz.sshj.SSHClient;
import net.schmizz.sshj.connection.ConnectionException;
import net.schmizz.sshj.connection.channel.direct.Session;
import net.schmizz.sshj.connection.channel.direct.Session.Command;
import net.schmizz.sshj.sftp.SFTPClient;
import net.schmizz.sshj.transport.TransportException;
import net.schmizz.sshj.userauth.UserAuthException;
import net.schmizz.sshj.userauth.keyprovider.OpenSSHKeyFile;
import net.schmizz.sshj.xfer.FileSystemFile;
import net.schmizz.sshj.xfer.InMemorySourceFile;

import org.apache.commons.io.IOUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.util.StringUtils;

import com.flyover.bootsy.core.k8s.KubeNode;
import com.flyover.bootsy.core.k8s.Secret;
import com.flyover.bootsy.core.k8s.SecretRef;
import com.flyover.bootsy.operator.k8s.KubeAdapter;

/**
 * @author mramach
 *
 */
public class Connection {
	
	private static final Logger LOG = LoggerFactory.getLogger(Connection.class);

	private KubeNode kn;
	private KubeAdapter kubeAdapter;
	private SSHClient ssh = new SSHClient();
	
	public Connection(KubeAdapter kubeAdapter, KubeNode kn) {
		this.kn = kn;
		this.kubeAdapter = kubeAdapter;		
	}
	
	public void raw(String cmd) {
		
		try {
			
			SSHClient ssh = connect();
			execute(ssh.startSession(), cmd);
			ssh.close();
			
		} catch (Exception e) {
			throw new RuntimeException("failed to execute ssh command", e);
		}
		
	}
	
	public void put(File f, String dest) {
		
		try {
			
			SSHClient ssh = connect();
			SFTPClient sftp = ssh.newSFTPClient();
			sftp.put(new FileSystemFile(f), dest);
			ssh.close();
			
		} catch (Exception e) {
			throw new RuntimeException("failed to execute sftp put", e);
		}
		
	}
	
	public void put(String name, byte[] data, String dest) {
		
		try {
			
			SSHClient ssh = connect();
			SFTPClient sftp = ssh.newSFTPClient();
			sftp.put(new InMemorySourceFile() {
				
				@Override
				public String getName() {
					return name;
				}
				
				@Override
				public long getLength() {
					return data.length;
				}
				
				@Override
				public InputStream getInputStream() throws IOException {
					return new ByteArrayInputStream(data);
				}
				
			}, dest);
			ssh.close();
			
		} catch (Exception e) {
			throw new RuntimeException("failed to execute sftp put", e);
		}
		
	}
	
	private void execute(Session session, String cmd) throws ConnectionException, TransportException, IOException {
		
		Command c = session.exec(cmd);
		c.join();
		
		IOUtils.readLines(new InputStreamReader(c.getInputStream())).stream()
			.forEach(l -> LOG.debug(l));
		
		session.close();
		
	}
	
	private SSHClient connect() throws IOException, UserAuthException, TransportException {
		
		SecretRef ref = kn.getSpec().getConnector().getAuthSecret();
		Secret secret = kubeAdapter.getSecret(ref.getNamespace(), ref.getName());
		
		String username = new String(Base64.getDecoder().decode(secret.getData().getOrDefault("username", "bootsy")));
		String password = new String(Base64.getDecoder().decode(secret.getData().getOrDefault("password", "")));
		String _publicKey = new String(Base64.getDecoder().decode(secret.getData().getOrDefault("publicKey", "")));
		String _privateKey = new String(Base64.getDecoder().decode(secret.getData().getOrDefault("privateKey", "")));
		
		ssh.addHostKeyVerifier((h, p, k) -> true);
		ssh.connect(kn.getSpec().getIpAddress());
		
		if(StringUtils.hasLength(_privateKey)) {
			
			try {
				
				OpenSSHKeyFile keyProvider = new OpenSSHKeyFile();
				keyProvider.init(_privateKey, _publicKey); 
				
				ssh.authPublickey(username, keyProvider);
				
			} catch (Exception e) {
				LOG.error("failed to load bootstrap keypair {}", e.getMessage());
			}
			
		} else {
			ssh.authPassword(username, password);
		}
		
		return ssh;
		
	}

}
