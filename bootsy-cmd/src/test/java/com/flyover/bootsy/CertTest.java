/**
 * 
 */
package com.flyover.bootsy;

import java.io.ByteArrayOutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.security.KeyPair;
import java.security.cert.X509Certificate;

import org.bouncycastle.asn1.x500.X500Name;
import org.bouncycastle.asn1.x509.GeneralName;
import org.junit.Test;

/**
 * @author mramach
 *
 */
public class CertTest {
	
	@Test
	public void test() throws Exception {
		
		Path dir = java.nio.file.Paths.get("/Users/mramach/ssl");
		
		if(!dir.toFile().exists()) {
			Files.createDirectories(dir);
		}
		
		Path caKeyPath = dir.resolve("ca.key");
		Path caCertPath = dir.resolve("ca.crt");
		Path serverKeyPath = dir.resolve("server.key");
		Path serverCertPath = dir.resolve("server.crt");
		Path kubeletKeyPath = dir.resolve("kubelet.key");
		Path kubeletCertPath = dir.resolve("kubelet.crt");
		
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
		
		X509Certificate[] serverChain = SSL.generateSSLCertificate(caKey, caCert, serverKey, "192.168.253.1", subject,
//				new GeneralName(GeneralName.iPAddress, getIpAddress().getHostAddress()),
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
		
		// kubelet client certificate
		out.reset();
		
		KeyPair kubeletKey = SSL.generateRSAKeyPair();
		SSL.write(out, kubeletKey);
		
		Files.write(kubeletKeyPath, out.toByteArray(), StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
		
		out.reset();
		
		subject = new X500Name(String.format("CN=kubelet, O=system:nodes"));
		
		X509Certificate[] kubeletChain = SSL.generateClientCertificate(
				caKey, caCert, kubeletKey, "192.168.253.1", subject);
		
		SSL.write(out, kubeletChain);
		
		Files.write(kubeletCertPath, out.toByteArray(), StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
		
	}

}
