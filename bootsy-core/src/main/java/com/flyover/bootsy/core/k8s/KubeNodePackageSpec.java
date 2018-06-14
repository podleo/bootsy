/**
 * 
 */
package com.flyover.bootsy.core.k8s;

import java.util.LinkedList;
import java.util.List;

/**
 * @author mramach
 *
 */
public class KubeNodePackageSpec extends Model {
	
	private String checksum = "";
	private List<String> packages = new LinkedList<>();
	
	public String getChecksum() {
		return checksum;
	}
	
	public void setChecksum(String checksum) {
		this.checksum = checksum;
	}

	public List<String> getPackages() {
		return packages;
	}

	public void setPackages(List<String> packages) {
		this.packages = packages;
	}

}
