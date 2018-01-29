/**
 * 
 */
package com.flyover.bootsy.operator.provders.centurylink;

import java.io.StringWriter;
import java.security.SecureRandom;
import java.util.Arrays;
import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import org.apache.velocity.Template;
import org.apache.velocity.VelocityContext;
import org.apache.velocity.app.VelocityEngine;
import org.apache.velocity.runtime.RuntimeConstants;
import org.apache.velocity.runtime.resource.loader.ClasspathResourceLoader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestTemplate;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.flyover.bootsy.operator.Operator;
import com.flyover.bootsy.operator.k8s.KubeAdapter;
import com.flyover.bootsy.operator.k8s.KubeMeta;
import com.flyover.bootsy.operator.k8s.KubeNode;
import com.flyover.bootsy.operator.k8s.KubeNodeConnector;
import com.flyover.bootsy.operator.k8s.KubeNodeProvider;
import com.flyover.bootsy.operator.k8s.Secret;
import com.flyover.bootsy.operator.k8s.SecretRef;
import com.flyover.bootsy.operator.provders.AbstractProvider;

/**
 * @author mramach
 *
 */
@SuppressWarnings("unchecked")
public class CenturyLinkProvider extends AbstractProvider {
	
	private static final Logger LOG = LoggerFactory.getLogger(Operator.class);
	private static final SecureRandom RANDOM = new SecureRandom();
	private static final String ALPHA_CAPS = "ABCDEFGHIJKLMNOPQRSTUVWXYZ";
    private static final String ALPHA = "abcdefghijklmnopqrstuvwxyz";
    private static final String NUMERIC = "0123456789";
    private static final String SPECIAL_CHARS = "!@#$%^&*_=+-/";
    private static final String DICT = ALPHA_CAPS + ALPHA + NUMERIC + SPECIAL_CHARS;
	
	private static final ObjectMapper MAPPER = new ObjectMapper(); 
	
	@Autowired
	private KubeAdapter kubeAdapter;
	@Value("${providers.centurylink.v2.endpoint:https://api.ctl.io}")
	private String endpoint;

	public CenturyLinkProvider() {
		super("centurylink");
	}

	@Override
	public boolean instanceCreated(KubeNodeProvider knp, KubeNode kn) {
		return Arrays.asList("creating", "created", "ready").contains(getInstanceInfo(kn).getStatus());
	}

	@Override
	public boolean instanceReady(KubeNodeProvider knp, KubeNode kn) {
		
		if(Arrays.asList("ready").contains(getInstanceInfo(kn).getStatus())) {
			return true;
		}
		
		CenturyLinkKubeNodeProviderSpec config = MAPPER
				.convertValue(knp.getSpec(), CenturyLinkKubeNodeProviderSpec.class);
		
		// ensure that the api credentials secret is present
		SecretRef secretRef = config.getCredentialsSecret();
		
		if(secretRef == null) {
			LOG.error("secretRef is required on a centurylink provider"); return false;
		}
		
		Secret credentials = kubeAdapter.getSecret(secretRef.getNamespace(), secretRef.getName());
		
		if(credentials == null) {
			LOG.error("credentials secret in namespace {} with name {} not found", secretRef.getNamespace(), secretRef.getName()); return false;
		}
		
		Authentication auth = login(
			new String(Base64.getDecoder().decode(credentials.getData().get("username"))), 
			new String(Base64.getDecoder().decode(credentials.getData().get("password"))));
		
		String serverId = getInstanceInfo(kn).getServerId();
		
		// check on the instance state to see if the instance is provisioned
		ServerDetailResponse details = getServerDetail(config, auth, serverId);
		
		boolean isReady = "active".equalsIgnoreCase(details.getStatus()) && 
			"started".equalsIgnoreCase(details.getDetails().getPowerState());
		
		if(isReady) {
			
			// set the instance state for future reference
			InstanceInfo info = getInstanceInfo(kn);
			info.setStatus("ready");
			
			setInstanceInfo(kn, info);
			
			IpAddress ipAddress = details.getDetails().getIpAddresses().stream()
				.filter(ip -> org.springframework.util.StringUtils.hasText(ip.getInternal()))
				.findFirst()
					.get();
			
			kn.getSpec().setIpAddress(ipAddress.getInternal());
			
			kubeAdapter.updateKubeNode(kn);
			
		}
		
		return isReady;
		
	}
	
	@Override
	public void createInstance(KubeNodeProvider knp, KubeNode kn) {
		
		CenturyLinkKubeNodeProviderSpec config = MAPPER
				.convertValue(knp.getSpec(), CenturyLinkKubeNodeProviderSpec.class);
		
		// ensure that the api credentials secret is present
		SecretRef secretRef = config.getCredentialsSecret();
		
		if(secretRef == null) {
			LOG.error("secretRef is required on a centurylink provider"); return;
		}
		
		Secret credentials = kubeAdapter.getSecret(secretRef.getNamespace(), secretRef.getName());
		
		if(credentials == null) {
			LOG.error("credentials secret in namespace {} with name {} not found", secretRef.getNamespace(), secretRef.getName()); return;
		}
		
		Authentication auth = login(
			new String(Base64.getDecoder().decode(credentials.getData().get("username"))), 
			new String(Base64.getDecoder().decode(credentials.getData().get("password"))));
		
		VelocityEngine velocityEngine = new VelocityEngine();
		velocityEngine.setProperty(RuntimeConstants.RESOURCE_LOADER, "classpath"); 
		velocityEngine.setProperty("classpath.resource.loader.class", ClasspathResourceLoader.class.getName());
		velocityEngine.init();
		
		Template template = velocityEngine.getTemplate("providers/centurylink/server_create.json");
		
		String password = password(16);
		
		VelocityContext context = new VelocityContext();
		context.put("name", "K8SN");
		context.put("description", "kubernetes node");
		context.put("groupId", config.getGroup());
		context.put("networkId", config.getNetwork());
		context.put("os", config.getOs());
		context.put("password", password);
		context.put("cpu", config.getCpu());
		context.put("memory", config.getMemoryGB());
		
		StringWriter value = new StringWriter();
		template.merge(context, value);
		
		try {
			
			Object payload = MAPPER.readValue(value.toString(), Map.class);
			CreateServerResponse res = createServer(config, auth, payload);
			
			Link self = res.getLinks().stream()
				.filter(l -> "self".equalsIgnoreCase(l.getRel()))
				.findFirst()
					.get();
			
			// set the server id for future reference
			InstanceInfo info = new InstanceInfo();
			info.setStatus("creating");
			info.setServerId(self.getId());
			
			setInstanceInfo(kn, info);

			// create an auth secret for future connections
			Secret authSecret = new Secret();
			authSecret.setMetadata(new KubeMeta());
			authSecret.getMetadata().setNamespace("bootsy");
			authSecret.getMetadata().setName(UUID.randomUUID().toString());
			authSecret.getData().put("username", Base64.getEncoder().encodeToString("root".getBytes()));
			authSecret.getData().put("password", Base64.getEncoder().encodeToString(password.getBytes()));
			
			kubeAdapter.createSecret(authSecret);
			
			// update the kube node spec with the connection details
			SecretRef ref = new SecretRef();
			ref.setNamespace("bootsy");
			ref.setName(authSecret.getMetadata().getName());
			
			kn.getSpec().setConnector(new KubeNodeConnector());
			kn.getSpec().getConnector().setAuthSecret(ref);
			
			kubeAdapter.updateKubeNode(kn);
						
		} catch (Exception e) {
			LOG.error("server provision request failed {}", e.getMessage());
		}
		
	}
	
	private InstanceInfo getInstanceInfo(KubeNode kn) {
		return MAPPER.convertValue(kn.getSpec().getInstanceInfo(), InstanceInfo.class);
	}
	
	private void setInstanceInfo(KubeNode kn, InstanceInfo instanceInfo) {
		kn.getSpec().setInstanceInfo(MAPPER.convertValue(instanceInfo, Map.class));
	}
	
	public static String password(int len) {
	    
		return IntStream.range(0, len)
			.mapToObj(i -> String.valueOf(DICT.charAt(RANDOM.nextInt(DICT.length()))))
				.collect(Collectors.joining(""));
		
	}
	
	public Authentication login(String username, String password) {

	    Map<String, String> request = new LinkedHashMap<String, String>();
	    request.put("username", username);
		request.put("password", password);

	    ResponseEntity<Authentication> response = new RestTemplate().exchange(
                endpoint + "/v2/authentication/login", 
                    HttpMethod.POST, new HttpEntity<Object>(request), Authentication.class);
	    
	    return response.getBody();
	    
	}
	
	public CreateServerResponse createServer(CenturyLinkKubeNodeProviderSpec config, Authentication auth, Object payload) {
		
		HttpHeaders headers = new HttpHeaders();
		headers.set("Authorization", String.format("Bearer %s", auth.getBearerToken()));
		headers.setContentType(MediaType.APPLICATION_JSON);
		
		ResponseEntity<CreateServerResponse> response = new RestTemplate().exchange(
                String.format("%s/v2/servers/%s", endpoint, config.getAccountAlias()), 
                    HttpMethod.POST, new HttpEntity<Object>(payload, headers), CreateServerResponse.class);
		
		return response.getBody();
		
	}
	
	public ServerDetailResponse getServerDetail(CenturyLinkKubeNodeProviderSpec config, Authentication auth, String serverId) {
		
		HttpHeaders headers = new HttpHeaders();
		headers.set("Authorization", String.format("Bearer %s", auth.getBearerToken()));
		headers.setContentType(MediaType.APPLICATION_JSON);
		
		ResponseEntity<ServerDetailResponse> response = new RestTemplate().exchange(
                String.format("%s/v2/servers/%s/%s?uuid=true", endpoint, config.getAccountAlias(), serverId), 
                    HttpMethod.GET, new HttpEntity<>(headers), ServerDetailResponse.class);
		
		return response.getBody();
		
	}

}
