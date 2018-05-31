/**
 * 
 */
package com.flyover.bootsy.core;

/**
 * @author mramach
 *
 */
public class Version {
	
	public static final String KUBE_VERSION = "1.10.3";
	public static final String DOCKER_REPO = "portr.ctnr.ctl.io/bootsy";
	
	public static String image(String name) {
		return String.format("%s/%s:%s", DOCKER_REPO, name, KUBE_VERSION);
	}

}
