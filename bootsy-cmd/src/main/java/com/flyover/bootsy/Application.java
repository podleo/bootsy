package com.flyover.bootsy;

import static net.sourceforge.argparse4j.impl.Arguments.*;
import net.sourceforge.argparse4j.ArgumentParsers;
import net.sourceforge.argparse4j.helper.HelpScreenException;
import net.sourceforge.argparse4j.inf.ArgumentParser;
import net.sourceforge.argparse4j.inf.MutuallyExclusiveGroup;
import net.sourceforge.argparse4j.inf.Namespace;

/**
 * @author mramach
 *
 */
public class Application {
	
    public static void main( String[] args ) throws Exception {
        
    	ArgumentParser parser = ArgumentParsers.newFor("bootsy").build()
    			.defaultHelp(true)
    			.description("A Kubernetes cluster bootstrap tool.");

    	parser.addArgument("--type")
			.help("Initialize a Kubernetes node on the current host.")
			.choices("master", "node")
			.setDefault("node");
		parser.addArgument("--api-server-endpoint")
			.help("The api server endpoint to use during kubelet and kube-proxy initialization.")
			.setDefault("https://127.0.0.1");
    	
    	MutuallyExclusiveGroup group = parser.addMutuallyExclusiveGroup();
    	
    	group.addArgument("--init")
    		.help("Initialize a Kubernetes master on the current host.")
    		.action(storeTrue());
    	group.addArgument("--reconfigure")
			.help("Reconfigure a Kubernetes master using the local bootsy.config.")
			.action(storeTrue());
    	group.addArgument("--destroy")
			.help("Destory a Kubernetes cluster on the current host.")
			.action(storeTrue());
    	
    	try {
    		
			Namespace namespace = parser.parseArgs(args);
			
			if(namespace.getBoolean("init") && "master".equals(namespace.getString("type"))) {
				
				new K8sMaster().init(namespace.getString("api_server_endpoint"));
				
			} else if(namespace.getBoolean("reconfigure") && "master".equals(namespace.getString("type"))) {
				
				new K8sMaster().update();
				
			} else if(namespace.getBoolean("init") && "node".equals(namespace.getString("type"))) {
				
				new K8sNode().init(namespace.getString("api_server_endpoint"));
				
			} else if(namespace.getBoolean("reconfigure") && "node".equals(namespace.getString("type"))) {
				
				new K8sNode().update(namespace.getString("api_server_endpoint"));
				
			} else if(namespace.getBoolean("destroy")) {
				
				new K8sMaster().destroy();
				
			} else {
				
				parser.printHelp();
				
			}
			
		} catch (HelpScreenException e) {
			// do nothing, just displaying help
		}
    	
    }
    
}
