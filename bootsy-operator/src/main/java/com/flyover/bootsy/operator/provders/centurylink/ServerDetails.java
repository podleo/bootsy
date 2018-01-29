/**
 * 
 */
package com.flyover.bootsy.operator.provders.centurylink;

import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import com.fasterxml.jackson.annotation.JsonAnyGetter;
import com.fasterxml.jackson.annotation.JsonAnySetter;

/**
 * @author mramach
 *
 */
public class ServerDetails {
	
	private String powerState;
	private List<IpAddress> ipAddresses = new LinkedList<>();
	private Map<String, Object> any = new LinkedHashMap<String, Object>();

	public String getPowerState() {
		return powerState;
	}

	public void setPowerState(String powerState) {
		this.powerState = powerState;
	}

	public List<IpAddress> getIpAddresses() {
		return ipAddresses;
	}

	public void setIpAddresses(List<IpAddress> ipAddresses) {
		this.ipAddresses = ipAddresses;
	}

	@JsonAnyGetter
	public Map<String,Object> any() {
	    return any;
	}

	@JsonAnySetter
	public void set(String name, Object value) {
	    any.put(name, value);
	}

}
