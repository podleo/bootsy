/**
 * 
 */
package com.flyover.bootsy.core;

import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.math.BigInteger;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.PrivateKey;
import java.security.SecureRandom;
import java.security.Security;
import java.security.cert.X509Certificate;
import java.util.Calendar;
import java.util.Date;

import javax.security.auth.x500.X500Principal;

import org.bouncycastle.asn1.ASN1Sequence;
import org.bouncycastle.asn1.x500.X500Name;
import org.bouncycastle.asn1.x509.BasicConstraints;
import org.bouncycastle.asn1.x509.ExtendedKeyUsage;
import org.bouncycastle.asn1.x509.Extension;
import org.bouncycastle.asn1.x509.GeneralName;
import org.bouncycastle.asn1.x509.GeneralNames;
import org.bouncycastle.asn1.x509.KeyPurposeId;
import org.bouncycastle.asn1.x509.KeyUsage;
import org.bouncycastle.asn1.x509.SubjectPublicKeyInfo;
import org.bouncycastle.cert.X509v1CertificateBuilder;
import org.bouncycastle.cert.X509v3CertificateBuilder;
import org.bouncycastle.cert.jcajce.JcaX509CertificateConverter;
import org.bouncycastle.cert.jcajce.JcaX509ExtensionUtils;
import org.bouncycastle.openssl.jcajce.JcaPEMWriter;
import org.bouncycastle.operator.ContentSigner;
import org.bouncycastle.operator.jcajce.JcaContentSignerBuilder;
import org.bouncycastle.pkcs.PKCS10CertificationRequest;
import org.bouncycastle.pkcs.PKCS10CertificationRequestBuilder;

/**
 * @author mramach
 *
 */
public class SSL {
	
	static {
		
		Security.addProvider(new org.bouncycastle.jce.provider.BouncyCastleProvider());
		
	}

	public static KeyPair generateRSAKeyPair() throws Exception {

		KeyPairGenerator kpGen = KeyPairGenerator.getInstance("RSA");
		kpGen.initialize(2048, new SecureRandom());

		return kpGen.generateKeyPair();

	}

	public static PKCS10CertificationRequest generateRequest(KeyPair pair) throws Exception {

		PKCS10CertificationRequestBuilder builder = new PKCS10CertificationRequestBuilder(
				new X500Name("CN=kubernetes-master"),
				SubjectPublicKeyInfo.getInstance(ASN1Sequence.getInstance(pair
						.getPublic().getEncoded())));

		ContentSigner signer = new JcaContentSignerBuilder("SHA256withRSA")
				.build(pair.getPrivate());

		return builder.build(signer);

	}

	public static X509Certificate generateV1Certificate(KeyPair pair, String commonName) throws Exception {

		Calendar c = Calendar.getInstance();
		c.add(Calendar.YEAR, 10);
		
		X509v1CertificateBuilder builder = new X509v1CertificateBuilder(
				new X500Name(String.format("CN=%s", commonName)), 
				BigInteger.valueOf(System.currentTimeMillis()), 
				new Date(), c.getTime(), 
				new X500Name(String.format("CN=%s", commonName)),
				SubjectPublicKeyInfo.getInstance(ASN1Sequence.getInstance(pair.getPublic().getEncoded())));

		ContentSigner signer = new JcaContentSignerBuilder("SHA256withRSA").build(pair.getPrivate());

		return new JcaX509CertificateConverter().getCertificate(builder.build(signer));

	}

	public static X509Certificate[] generateSSLCertificate(PrivateKey rootKey, X509Certificate rootCert, KeyPair serverPair, 
			String issuer, X500Name subject, GeneralName...alternativeNames) throws Exception {

		Calendar c = Calendar.getInstance();
		c.add(Calendar.YEAR, 10);
		
		X509v3CertificateBuilder builder = new X509v3CertificateBuilder(
				new X500Name(String.format("CN=%s", issuer)), 
				BigInteger.valueOf(System.currentTimeMillis()), 
				new Date(), c.getTime(), 
				subject,
				SubjectPublicKeyInfo.getInstance(ASN1Sequence.getInstance(serverPair.getPublic().getEncoded())));

		builder.addExtension(Extension.authorityKeyIdentifier, false,
				new JcaX509ExtensionUtils()
						.createAuthorityKeyIdentifier(
								rootCert.getPublicKey(),
								new X500Principal(String.format("CN=%s", issuer)),
								rootCert.getSerialNumber()));

		builder.addExtension(Extension.basicConstraints, false, new BasicConstraints(false));

		builder.addExtension(Extension.keyUsage, false, new KeyUsage(
				KeyUsage.keyEncipherment | KeyUsage.dataEncipherment));

		builder.addExtension(Extension.extendedKeyUsage, false,
				new ExtendedKeyUsage(new KeyPurposeId[]{ KeyPurposeId.id_kp_serverAuth, KeyPurposeId.id_kp_clientAuth }));

		builder.addExtension(Extension.subjectAlternativeName, false, new GeneralNames(alternativeNames));

		ContentSigner signer = new JcaContentSignerBuilder(
				"SHA1WithRSAEncryption").build(rootKey);

		X509Certificate issuedCert = new JcaX509CertificateConverter()
				.getCertificate(builder.build(signer));

		return new X509Certificate[] { issuedCert, rootCert };

	}
	
	public static X509Certificate[] generateClientCertificate(PrivateKey caKey, X509Certificate caCert, KeyPair clientKey, 
			String issuer, X500Name subject) throws Exception {

		Calendar c = Calendar.getInstance();
		c.add(Calendar.YEAR, 10);
		
		X509v3CertificateBuilder builder = new X509v3CertificateBuilder(
				new X500Name(String.format("CN=%s", issuer)), 
				BigInteger.valueOf(System.currentTimeMillis()), 
				new Date(), c.getTime(), 
				subject,
				SubjectPublicKeyInfo.getInstance(ASN1Sequence.getInstance(clientKey.getPublic().getEncoded())));

		builder.addExtension(Extension.authorityKeyIdentifier, false,
				new JcaX509ExtensionUtils()
						.createAuthorityKeyIdentifier(
								caCert.getPublicKey(),
								new X500Principal(String.format("CN=%s", issuer)),
								caCert.getSerialNumber()));

		builder.addExtension(Extension.basicConstraints, false, new BasicConstraints(false));

		builder.addExtension(Extension.keyUsage, false, new KeyUsage(
				KeyUsage.keyEncipherment | KeyUsage.dataEncipherment));

		builder.addExtension(Extension.extendedKeyUsage, false,
				new ExtendedKeyUsage(new KeyPurposeId[]{ 
						KeyPurposeId.id_kp_serverAuth, KeyPurposeId.id_kp_clientAuth }));

		ContentSigner signer = new JcaContentSignerBuilder(
				"SHA1WithRSAEncryption").build(caKey);

		X509Certificate issuedCert = new JcaX509CertificateConverter()
				.getCertificate(builder.build(signer));

		return new X509Certificate[] { issuedCert, caCert };

	}
	
	public static void write(OutputStream out, Object o) throws Exception {
		write(out, new Object[]{o});
	}
	
	public static void write(OutputStream out, Object[] objects) throws Exception {
		
		JcaPEMWriter pem = new JcaPEMWriter(new OutputStreamWriter(out));

		for (Object o : objects) {
			pem.writeObject(o);
		}
		
	    pem.close();
		
	}

}
